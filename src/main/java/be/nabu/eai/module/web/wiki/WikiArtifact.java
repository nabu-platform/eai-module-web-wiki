package be.nabu.eai.module.web.wiki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nabu.frameworks.datastore.Services;
import nabu.web.wiki.types.WikiArticle;
import nabu.web.wiki.types.WikiDirectory;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.Templater;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheEntry;
import be.nabu.libs.cache.api.ExplorableCache;
import be.nabu.libs.cache.impl.ByteSerializer;
import be.nabu.libs.cache.impl.LastModifiedTimeoutManager;
import be.nabu.libs.cache.impl.StringSerializer;
import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.DocumentCacheManager;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.converters.DXFToStandaloneHTML;
import be.nabu.libs.dms.converters.MarkdownToDXF;
import be.nabu.libs.dms.converters.WikiToDXF;
import be.nabu.libs.dms.converters.WikiToEHTML;
import be.nabu.libs.dms.utils.SimpleDocumentManager;
import be.nabu.libs.events.EventDispatcherFactory;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.DetachableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.utils.KeyValuePairImpl;
import be.nabu.libs.vfs.api.File;
import be.nabu.libs.vfs.api.FileSystem;
import be.nabu.libs.vfs.resources.impl.ResourceFileSystem;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.IOUtils;

public class WikiArtifact extends JAXBArtifact<WikiConfiguration> {
	
	static {
		ContentTypeMap.getInstance().registerContentType(WikiToEHTML.EDITABLE_HTML, "ehtml");
		ContentTypeMap.getInstance().registerContentType(WikiToDXF.WIKI_CONTENT_TYPE, "wiki");
		ContentTypeMap.getInstance().registerContentType(MarkdownToDXF.CONTENT_TYPE, "markdown");
		ContentTypeMap.getInstance().registerContentType("application/html+slides", "slides");
		ContentTypeMap.getInstance().registerContentType("text/html+standalone", "shtml");
		ContentTypeMap.getInstance().registerContentType("text/x-script.glue", "glue");
		ContentTypeMap.register();
	}
	
	public WikiArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "wiki.xml", WikiConfiguration.class);
	}
	
	private FileSystem fileSystem;
	private SimpleDocumentManager documentManager;

	public void setFileSystem(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public FileSystem getFileSystem() {
		if (fileSystem == null) {
			synchronized(this) {
				if (fileSystem == null) {
					try {
						ResourceContainer<?> targetDirectory = getConfiguration().getSource() != null 
							? ("$internal".equals(getConfig().getSource().toString()) ? RepositoryDocumentation.getInternal().getRoot() : ResourceUtils.mkdir(getConfiguration().getSource(), null)) 
							: ResourceUtils.mkdirs(getDirectory(), "public");
						if (targetDirectory instanceof DetachableResource) {
							targetDirectory = (ResourceContainer<?>) ((DetachableResource) targetDirectory).detach();
						}
						fileSystem = new ResourceFileSystem(EventDispatcherFactory.getInstance().getEventDispatcher(), targetDirectory, null);
					}
					catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return fileSystem;
	}
	
	public DocumentManager getDocumentManager() {
		if (documentManager == null) {
			synchronized(this) {
				if (documentManager == null) {
					this.documentManager = new SimpleDocumentManager();
					try {
						if (getConfiguration().getCacheProvider() != null) {
							Cache cache = getConfiguration().getCacheProvider().create(
								getId(), 
								// defaults to 100 mb
								getConfiguration().getMaxTotalSize() == null ? 1024*1024*100 : getConfiguration().getMaxTotalSize(),
								// defaults to 10 mb
								getConfiguration().getMaxEntrySize() == null ? 1024*1024*5 : getConfiguration().getMaxEntrySize(),
								new StringSerializer(), 
								new ByteSerializer(), 
								null, 
								new LastModifiedTimeoutManager(getConfiguration().getCacheTimeout() == null ? 1000*60*60 : getConfiguration().getCacheTimeout())
							);
							documentManager.setCacheManager(new CentralDocumentCacher(cache));
						}
						documentManager.setDatastore(Services.getAsDatastore(getRepository().newExecutionContext(SystemPrincipal.ROOT)));
						
						for (Templater templater : EAIResourceRepository.getInstance().getTemplaters()) {
							documentManager.getTemplaters().add(new be.nabu.libs.dms.api.Templater() {
								@Override
								public String template(String content) {
									return templater.template(content);
								}
							});
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return documentManager;
	}
	
	public Charset getCharset() {
		try {
			return getConfiguration().getCharset() != null ? getConfiguration().getCharset() : Charset.forName("UTF-8");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String cleanup(String html) {
		return html.replace("<br>", "<br/>").replace("<hr>", "<hr/>");
	}
	
	public WikiDirectory list(String path, boolean recursive, boolean includeContent) throws IOException {
		WikiDirectory listing = new WikiDirectory();
		listing.setContentType(Resource.CONTENT_TYPE_DIRECTORY);
		File root;
		if (path == null) {
			listing.setPath("/");
			listing.setName("/");
			root = getFileSystem().resolve("/");
		}
		else {
			listing.setPath(path);
			root = getFileSystem().resolve(path);
			if (root == null || !root.isDirectory()) {
				return null;
			}
			listing.setName(root.getName());
		}
		loadListing(listing, root, recursive, includeContent);
		return listing;
	}
	
	public void mkdir(String path) throws IOException {
		File file = getFileSystem().resolve(path);
		if (!file.exists()) {
			file.mkdir();
		}
	}
	
	public void delete(String path) throws IOException {
		File file = getFileSystem().resolve(path);
		if (!file.exists()) {
			file.delete();
		}
	}
	
	private void loadListing(WikiDirectory parent, File file, boolean recursive, boolean includeContent) throws IOException {
		List<WikiDirectory> directories = new ArrayList<WikiDirectory>();
		List<WikiArticle> articles = new ArrayList<>();
		for (File child : file) {
			// ignore hidden files
			if (!child.getName().startsWith(".")) {
				String path = parent.getPath() + "/" + child.getName();
				// for the root
				if (path.startsWith("//")) {
					path = path.substring(1);
				}
				if (child.isDirectory()) {
					WikiDirectory directory = new WikiDirectory();
					directory.setContentType(Resource.CONTENT_TYPE_DIRECTORY);
					directory.setName(child.getName());
					directory.setPath(path);
					if (recursive) {
						loadListing(directory, child, recursive, includeContent);
					}
					directories.add(directory);
				}
				else {
					WikiArticle article = new WikiArticle();
					article.setName(child.getName());
					article.setContentType(child.getContentType());
					article.setSize(child.getSize());
					article.setLastModified(child.getLastModified());
					article.setPath(path);
					// extract metadata
					if ("text/x-markdown".equals(child.getContentType())) {
						List<KeyValuePair> meta = new ArrayList<KeyValuePair>();
						try {
							byte[] content = getArticle(path, "text/x-markdown", null);
							Pattern pattern = Pattern.compile("(?m)^[\\s]*@([^\\s]+)[\\s]*(.*)$");
							// we only want the metadata at the top
							Matcher matcher = pattern.matcher(new String(content, "UTF-8").split("\n\n")[0]);
							while (matcher.find()) {
								meta.add(new KeyValuePairImpl(matcher.group(1), matcher.groupCount() == 1 ? "true" : matcher.group(2)));
							}
							if (includeContent) {
								article.setContent(content);
							}
						}
						catch (FormatException e) {
							// could not load entry
							e.printStackTrace();
						}
						article.setMeta(meta);
					}
					if (article.getMeta() != null) {
						Iterator<KeyValuePair> iterator = article.getMeta().iterator();
						while (iterator.hasNext()) {
							KeyValuePair pair = iterator.next();
							if ("tags".equals(pair.getKey()) && pair.getValue() != null) {
								article.setTags(Arrays.asList(pair.getValue().split("[\\s]*,[\\s]*")));
								iterator.remove();
							}
						}
					}
					if (includeContent && article.getContent() == null) {
						try {
							article.setContent(getArticle(path, child.getContentType(), null));
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					articles.add(article);
				}
			}
		}
		parent.setArticles(articles);
		parent.setDirectories(directories);
	}
	
	public void setArticle(String path, InputStream content, Map<String, String> properties, String contentType) throws IOException, FormatException {
		// guess the type from the extension
		if (contentType == null) {
			contentType = URLConnection.guessContentTypeFromName(path);
		}
		if (contentType == null || Resource.CONTENT_TYPE_DIRECTORY.equals(contentType)) {
			throw new IOException("This content type is unknown or not allowed: " + contentType);
		}
		// do conversion in memory to avoid overwriting file if it fails
		File file = getFileSystem().resolve(path);
		byte [] input = IOUtils.toBytes(IOUtils.wrap(content));
		input = cleanup(new String(input, getCharset())).getBytes(getCharset());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			getDocumentManager().convert(new MemoryFileFragment(file, input, "new", WikiToEHTML.EDITABLE_HTML), contentType, output, properties);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		if (input.length != 0 && output.toByteArray().length == 0) {
			throw new FormatException("Unknown formatting exception occurred");
		}
		OutputStream fileOutput = file.getOutputStream();
		try {
			fileOutput.write(output.toByteArray());
		}
		finally {
			fileOutput.close();
		}
	}
	
	public byte [] getArticle(String path, String contentType, Map<String, String> properties) throws IOException, FormatException {
		File file = getFileSystem().resolve(path);
		if (!file.exists()) {
			return null;
		}
		else if (!file.isFile()) {
			throw new IllegalArgumentException(path + " is not a file");
		}
		if (contentType == null) {
			contentType = file.getContentType();
		}
		// if we have no known content type, we just use the original content
		if (file.getContentType() == null || contentType == null) {
			return IOUtils.toBytes(IOUtils.wrap(file.getInputStream()));
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			if (properties == null) {
				properties = new HashMap<String, String>();
			}
			// slides
			if (!properties.containsKey("fragment")) {
				properties.put("fragment", "p,h2,h3,h4,h5,h6,h7,img,li,blockquote,table");
			}
			if (!properties.containsKey("mouseWheel")) {
				properties.put("mouseWheel", "false");
			}
			if (!properties.containsKey("style")) {
				// standalone
				properties.put("style", DXFToStandaloneHTML.getAdditionalStyles("slides/custom.css", "slides/tables.css"));
			}
			getDocumentManager().convert(file, contentType, output, properties);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return output.toByteArray();
	}
	
	public File getFile(String path) throws IOException {
		return getFileSystem().resolve(path);
	}

	public byte[] getTableOfContents(String path) throws IOException, FormatException {
		String html = new String(getArticle(path, "text/html", null), getCharset());
		String toc = DXFToStandaloneHTML.getTableOfContents(html);
		return toc.getBytes(getCharset());
	}
	
	private static class CentralDocumentCacher implements DocumentCacheManager {

		private Cache cache;

		public CentralDocumentCacher(Cache cache) {
			this.cache = cache;
		}
		
		@Override
		public byte[] getCached(File file, String contentType) {
			try {
				return (byte[]) cache.get(file.getPath() + ":" + contentType);
			}
			catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Date getAge(File file, String contentType) {
			if (cache instanceof ExplorableCache) {
				for (CacheEntry entry : ((ExplorableCache) cache).getEntries()) {
					return entry.getLastModified();
				}
			}
			return null;
		}

		@Override
		public void setCached(File file, String contentType, byte[] content) {
			try {
				cache.put(file.getPath() + ":" + contentType, content);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}

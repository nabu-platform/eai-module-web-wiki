package nabu.web.wiki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.wiki.types.WikiArticle;
import nabu.web.wiki.types.WikiContent;
import nabu.web.wiki.types.WikiDirectory;
import nabu.web.wiki.types.WikiGroup;
import be.nabu.eai.module.web.wiki.RepositoryDocumentation;
import be.nabu.eai.module.web.wiki.WikiArtifact;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.SimpleDocumentManager;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.utils.io.IOUtils;

@WebService
public class Services {
	
	private ExecutionContext context;
	
	@WebResult(name = "groups")
	public List<WikiGroup> group(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "key") String key) throws IOException {
		WikiDirectory list = list(wikiId, path, recursive, false, false);
		Map<String, List<WikiArticle>> grouped = new HashMap<String, List<WikiArticle>>();
		group(list, grouped, key);
		List<WikiGroup> groups = new ArrayList<WikiGroup>();
		for (String single : grouped.keySet()) {
			WikiGroup group = new WikiGroup();
			group.setKey(single);
			group.setArticles(grouped.get(single));
			groups.add(group);
		}
		return groups;
	}
	
	private void group(WikiDirectory directory, Map<String, List<WikiArticle>> grouped, String key) {
		if (directory != null && directory.getArticles() != null) {
			for (WikiArticle article : directory.getArticles()) {
				if (key == null || key.equals("tag") || key.equals("tags")) {
					if (article.getTags() != null) {
						for (String tag : article.getTags()) {
							if (!grouped.containsKey(tag)) {
								grouped.put(tag, new ArrayList<WikiArticle>());
							}
							grouped.get(tag).add(article);
						}
					}
				}
				else if (article.getMeta() != null) {
					for (KeyValuePair pair : article.getMeta()) {
						if (key.equals(pair.getKey())) {
							if (!grouped.containsKey(pair.getValue())) {
								grouped.put(pair.getValue(), new ArrayList<WikiArticle>());
							}
							grouped.get(pair.getValue()).add(article);
						}
					}
				}
			}
		}
		if (directory != null && directory.getDirectories() != null) {
			for (WikiDirectory child : directory.getDirectories()) {
				group(child, grouped, key);
			}
		}
	}
	
	@WebResult(name = "listing")
	public WikiDirectory list(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "flatten") Boolean flatten, @WebParam(name = "includeContent") Boolean includeContent) throws IOException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		if (recursive == null) {
			recursive = false;
		}
		WikiDirectory list = resolved.list(path, recursive, includeContent != null && includeContent);
		if (flatten != null && flatten) {
			List<WikiArticle> articles = new ArrayList<WikiArticle>();
			flatten(list, articles);
			WikiDirectory newList = new WikiDirectory();
			newList.setContentType(Resource.CONTENT_TYPE_DIRECTORY);
			newList.setName("$flattened");
			newList.setPath(list.getPath());
			newList.setArticles(articles);
			list = newList;
		}
		return list;
	}

	private WikiArtifact resolve(String wikiId) {
		return "$internal".equals(wikiId) ? RepositoryDocumentation.getInternal().getWiki() : context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
	}
	
	private void flatten(WikiDirectory parent, List<WikiArticle> articles) {
		if (parent.getArticles() != null) {
			articles.addAll(parent.getArticles());
		}
		if (parent.getDirectories() != null) {
			for (WikiDirectory child : parent.getDirectories()) {
				flatten(child, articles);
			}
		}
	}
	
	public void create(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.mkdir(path);
	}
	
	public void delete(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.delete(path);
	}
	
	@WebResult(name = "tableOfContents")
	public WikiContent tableOfContents(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException, FormatException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		return new WikiContent(resolved.getTableOfContents(path), "text/html", resolved.getCharset());
	}
	
	@WebResult(name = "content")
	public WikiContent read(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) {
			for (KeyValuePair property : properties) {
				map.put(property.getKey(), property.getValue());
			}
		}
		byte[] article = resolved.getArticle(path, contentType, map);
		if (contentType == null) {
			contentType = resolved.getFile(path).getContentType();
		}
		return new WikiContent(article, contentType, resolved.getCharset());
	}
	
	public void write(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType, @WebParam(name = "content") InputStream content, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) {
			for (KeyValuePair property : properties) {
				map.put(property.getKey(), property.getValue());
			}
		}
		resolved.setArticle(path, content, map, contentType);
	}
	
	/**
	 * You need to fill in a wiki backend because:
	 * - we can reuse the caching (based on your content id)
	 * - we reuse the charset
	 * - we need to reuse the filesystem (you can set up memory if you want for this) 
	 */
	@WebResult(name = "converted")
	public WikiContent convert(@NotNull @WebParam(name = "wikiId") String wikiId, @NotNull @WebParam(name = "contentId") String contentId, @WebParam(name = "content") InputStream content, @WebParam(name = "fromContentType") String fromContentType, @WebParam(name = "toContentType") String toContentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		WikiArtifact resolved = resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) {
			for (KeyValuePair property : properties) {
				map.put(property.getKey(), property.getValue());
			}
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// we add the "tocontentype" as fragment name
		resolved.getDocumentManager().convert(
			new MemoryFileFragment(resolved.getFileSystem().resolve(contentId), IOUtils.toBytes(IOUtils.wrap(content)), toContentType, fromContentType), 
			toContentType,
			output,
			map
		);
		return new WikiContent(output.toByteArray(), toContentType, resolved.getCharset());
	}

	@WebResult(name = "transformed")
	public WikiContent transform(@WebParam(name = "content") InputStream content, @NotNull @WebParam(name = "fromContentType") String fromContentType, @NotNull @WebParam(name = "toContentType") String toContentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		if (content == null) {
			return null;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) {
			for (KeyValuePair property : properties) {
				map.put(property.getKey(), property.getValue());
			}
		}
		SimpleDocumentManager simpleDocumentManager = new SimpleDocumentManager();
		simpleDocumentManager.setDatastore(nabu.frameworks.datastore.Services.getAsDatastore(EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT)));
		simpleDocumentManager.convert(
			new MemoryFileFragment(null, IOUtils.toBytes(IOUtils.wrap(content)), toContentType, fromContentType),
			toContentType, output, map);
		return new WikiContent(output.toByteArray(), toContentType, Charset.defaultCharset());
	}
}

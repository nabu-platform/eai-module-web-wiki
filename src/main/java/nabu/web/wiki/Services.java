package nabu.web.wiki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.wiki.types.WikiContent;
import nabu.web.wiki.types.WikiDirectory;
import be.nabu.eai.module.web.wiki.WikiArtifact;
import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.SimpleDocumentManager;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;

@WebService
public class Services {
	
	private ExecutionContext context;
	
	@WebResult(name = "listing")
	public WikiDirectory list(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "recursive") Boolean recursive) throws IOException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		if (recursive == null) {
			recursive = false;
		}
		return resolved.list(path, recursive);
	}
	
	public void create(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.mkdir(path);
	}
	
	public void delete(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.delete(path);
	}
	
	@WebResult(name = "tableOfContents")
	public WikiContent tableOfContents(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		return new WikiContent(resolved.getTableOfContents(path), "text/html", resolved.getCharset());
	}
	
	@WebResult(name = "content")
	public WikiContent read(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
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
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
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
	public WikiContent convert(@NotNull @WebParam(name = "wikiId") String wikiId, @NotNull @WebParam(name = "contentId") String contentId, @WebParam(name = "content") byte [] content, @WebParam(name = "fromContentType") String fromContentType, @WebParam(name = "toContentType") String toContentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
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
			new MemoryFileFragment(resolved.getFileSystem().resolve(contentId), content, toContentType, fromContentType), 
			toContentType,
			output,
			map
		);
		return new WikiContent(output.toByteArray(), toContentType, resolved.getCharset());
	}

	@WebResult(name = "transformed")
	public WikiContent transform(@WebParam(name = "content") byte [] content, @NotNull @WebParam(name = "fromContentType") String fromContentType, @NotNull @WebParam(name = "toContentType") String toContentType, @WebParam(name = "properties") List<KeyValuePair> properties) throws IOException, FormatException {
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
		new SimpleDocumentManager().convert(
			new MemoryFileFragment(null, content, toContentType, fromContentType),
			toContentType, output, map);
		return new WikiContent(output.toByteArray(), toContentType, Charset.defaultCharset());
	}
}

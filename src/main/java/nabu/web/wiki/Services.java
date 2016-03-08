package nabu.web.wiki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.wiki.types.WikiContent;
import nabu.web.wiki.types.WikiDirectory;
import be.nabu.eai.module.web.wiki.WikiArtifact;
import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.services.api.ExecutionContext;

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
	public WikiContent read(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		return new WikiContent(resolved.getArticle(path, contentType), contentType, resolved.getCharset());
	}
	
	public void write(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType, @WebParam(name = "content") InputStream content) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.setArticle(path, content);
	}
	
	/**
	 * You need to fill in a wiki backend because:
	 * - we can reuse the caching (based on your content id)
	 * - we reuse the charset
	 * - we need to reuse the filesystem (you can set up memory if you want for this) 
	 */
	@WebResult(name = "converted")
	public WikiContent convert(@NotNull @WebParam(name = "wikiId") String wikiId, @NotNull @WebParam(name = "contentId") String contentId, @WebParam(name = "content") byte [] content, @WebParam(name = "fromContentType") String fromContentType, @WebParam(name = "toContentType") String toContentType) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// we add the "tocontentype" as fragment name
		resolved.getDocumentManager().convert(
			new MemoryFileFragment(resolved.getFileSystem().resolve(contentId), content, toContentType, fromContentType), 
			toContentType,
			output,
			new HashMap<String, String>()
		);
		return new WikiContent(output.toByteArray(), toContentType, resolved.getCharset());
	}
}

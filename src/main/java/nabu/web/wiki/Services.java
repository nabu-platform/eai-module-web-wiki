package nabu.web.wiki;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.web.wiki.WikiArtifact;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.services.api.ExecutionContext;
import nabu.web.wiki.types.WikiDirectory;

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
	public InputStream tableOfContents(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		return resolved.getTableOfContents(path);
	}
	
	@WebResult(name = "content")
	public InputStream read(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		return new ByteArrayInputStream(resolved.getArticle(path, contentType));
	}
	
	public void write(@NotNull @WebParam(name = "wikiId") String wikiId, @WebParam(name = "path") String path, @WebParam(name = "contentType") String contentType, @WebParam(name = "content") InputStream content) throws IOException, FormatException {
		WikiArtifact resolved = context.getServiceContext().getResolver(WikiArtifact.class).resolve(wikiId);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find wiki: " + wikiId);
		}
		resolved.setArticle(path, content);
	}
}

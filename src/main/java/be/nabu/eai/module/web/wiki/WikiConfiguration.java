package be.nabu.eai.module.web.wiki;

import java.net.URI;
import java.nio.charset.Charset;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "wiki")
@XmlType(propOrder = { "source", "cacheProvider", "maxEntrySize", "maxTotalSize", "cacheTimeout", "charset" })
public class WikiConfiguration {
	
	private CacheProviderArtifact cacheProvider;
	
	private Long maxEntrySize, maxTotalSize, cacheTimeout;
	
	private Charset charset;
	
	private URI source;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getCacheProvider() {
		return cacheProvider;
	}
	public void setCacheProvider(CacheProviderArtifact cacheProvider) {
		this.cacheProvider = cacheProvider;
	}
	
	@EnvironmentSpecific
	public Long getMaxEntrySize() {
		return maxEntrySize;
	}
	public void setMaxEntrySize(Long maxEntrySize) {
		this.maxEntrySize = maxEntrySize;
	}
	
	@EnvironmentSpecific
	public Long getMaxTotalSize() {
		return maxTotalSize;
	}
	public void setMaxTotalSize(Long maxTotalSize) {
		this.maxTotalSize = maxTotalSize;
	}
	
	@EnvironmentSpecific
	public Long getCacheTimeout() {
		return cacheTimeout;
	}
	public void setCacheTimeout(Long cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
	}
	
	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	public URI getSource() {
		return source;
	}
	public void setSource(URI source) {
		this.source = source;
	}

}

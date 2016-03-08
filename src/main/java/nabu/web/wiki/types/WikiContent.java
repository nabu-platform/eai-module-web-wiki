package nabu.web.wiki.types;

import java.nio.charset.Charset;

public class WikiContent {
	private byte [] bytes;
	private String contentType;
	private long size;
	private Charset charset;
	
	public WikiContent() {
		// auto construct
	}
	
	public WikiContent(byte [] bytes, String contentType, Charset charset) {
		this.bytes = bytes;
		this.contentType = contentType;
		this.charset = charset;
		this.size = bytes.length;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}

	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
}

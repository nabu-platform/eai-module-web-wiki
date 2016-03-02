package nabu.web.wiki.types;

import java.util.Date;
import java.util.List;

public class WikiArticle extends WikiEntry {

	private long size;
	private List<String> tags;
	private Date lastModified;
	
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
}

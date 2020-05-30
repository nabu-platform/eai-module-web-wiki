package nabu.web.wiki.types;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WikiGroup {
	private String key;
	private List<WikiArticle> articles;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public List<WikiArticle> getArticles() {
		return articles;
	}
	public void setArticles(List<WikiArticle> articles) {
		this.articles = articles;
	}
}

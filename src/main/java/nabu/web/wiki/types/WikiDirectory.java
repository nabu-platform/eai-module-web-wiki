package nabu.web.wiki.types;

import java.util.List;

public class WikiDirectory extends WikiEntry {
	
	private List<WikiArticle> articles;
	
	private List<WikiDirectory> directories;

	public List<WikiArticle> getArticles() {
		return articles;
	}
	public void setArticles(List<WikiArticle> articles) {
		this.articles = articles;
	}
	
	public List<WikiDirectory> getDirectories() {
		return directories;
	}
	public void setDirectories(List<WikiDirectory> directories) {
		this.directories = directories;
	}

}

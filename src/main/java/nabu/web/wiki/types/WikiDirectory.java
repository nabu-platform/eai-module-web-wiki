/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package nabu.web.wiki.types;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
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

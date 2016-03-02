package be.nabu.eai.module.web.wiki;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WikiManager extends JAXBArtifactManager<WikiConfiguration, WikiArtifact> {

	public WikiManager() {
		super(WikiArtifact.class);
	}

	@Override
	protected WikiArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WikiArtifact(id, container, repository);
	}

}

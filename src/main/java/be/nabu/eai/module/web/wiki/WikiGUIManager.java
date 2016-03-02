package be.nabu.eai.module.web.wiki;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class WikiGUIManager extends BaseJAXBGUIManager<WikiConfiguration, WikiArtifact> {

	public WikiGUIManager() {
		super("Wiki Repository", WikiArtifact.class, new WikiManager(), WikiConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected WikiArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new WikiArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Web";
	}
}

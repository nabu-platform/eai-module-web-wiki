package be.nabu.eai.module.web.wiki;

import java.net.URI;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.events.EventDispatcherFactory;
import be.nabu.libs.resources.VirtualContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.memory.MemoryDirectory;
import be.nabu.libs.vfs.api.FileSystem;
import be.nabu.libs.vfs.resources.impl.ResourceFileSystem;

public class RepositoryDocumentation {
	
	private static RepositoryDocumentation internal;
	
	public static RepositoryDocumentation getInternal() {
		if (internal == null) {
			synchronized(RepositoryDocumentation.class) {
				if (internal == null) {
					internal = new RepositoryDocumentation();
				}
			}
		}
		return internal;
	}
	
	private VirtualContainer<Resource> root;
	private WikiArtifact wiki;
	
	public ResourceContainer<?> getRoot() {
		if (root == null) {
			synchronized(this) {
				if (root == null) {
					try {
						VirtualContainer<Resource> root = new VirtualContainer<Resource>(new URI("/"));
						resynchronize(EAIResourceRepository.getInstance().getRoot(), root);
						this.root = root;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return root;
	}
	
	// at each level we check for the folder "protected/documentation", if it exists, we add the contents to this one
	// any non-internal folder is scanned and if it contains anything, it is added, we can base this on the entries
	@SuppressWarnings("resource")
	private void resynchronize(Entry entry, VirtualContainer<Resource> root) {
		// start by clearing all ze children
		root.clear();
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
			ResourceContainer<?> protectedFolder = (ResourceContainer<?>) container.getChild(EAIResourceRepository.PROTECTED);
			ResourceContainer<?> documentation = protectedFolder == null ? null : (ResourceContainer<?>) protectedFolder.getChild("documentation");
			if (documentation != null) {
				for (Resource child : documentation) {
					// the folder "attachments" is reserved to store...attachments
					if (child.getName().equals("attachments")) {
						continue;
					}
					root.addChild(child.getName(), child);
				}
			}
		}
		// let's look in the children
		for (Entry child : entry) {
			VirtualContainer<Resource> childContainer = new VirtualContainer<Resource>(root, child.getName());
			resynchronize(child, childContainer);
			// if we have any children, mount it
			if (childContainer.iterator().hasNext()) {
				root.addChild(childContainer.getName(), childContainer);
			}
		}
	}
	
	public WikiArtifact getWiki() {
		try {
			if (wiki == null) {
				synchronized(this) {
					if (wiki == null) {
						FileSystem fileSystem = new ResourceFileSystem(EventDispatcherFactory.getInstance().getEventDispatcher(), getRoot(), null);
						WikiArtifact wiki = new WikiArtifact("$internal", new MemoryDirectory(), EAIResourceRepository.getInstance());
						wiki.setFileSystem(fileSystem);
						this.wiki = wiki;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return wiki;
	}
}
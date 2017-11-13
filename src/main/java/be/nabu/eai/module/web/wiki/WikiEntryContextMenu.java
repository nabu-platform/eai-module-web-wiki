package be.nabu.eai.module.web.wiki;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.repository.api.Entry;

public class WikiEntryContextMenu implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(final Entry entry) {
		Menu menu = new Menu("Wiki");
		MenuItem item = new MenuItem("Generate Documentation");
		item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				
			}
		});
		menu.getItems().add(item);
		return menu;
	}

}

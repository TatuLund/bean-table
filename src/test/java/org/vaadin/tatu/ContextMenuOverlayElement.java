package org.vaadin.tatu;

import java.util.List;
import java.util.Optional;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a
 * <code>&lt;vaadin-context-menu-overlay&gt;</code> element.
 *
 * @author Vaadin Ltd
 *
 */
@Element("vaadin-context-menu-overlay")
public class ContextMenuOverlayElement extends TestBenchElement {

    /**
     * Get the first menu item matching the caption.
     *
     * @return Optional menu item.
     */
    public Optional<ContextMenuItemElement> getMenuItem(String caption) {
        return getMenuItems().stream()
                .filter(item -> item.getText().equals(caption)).findFirst();
    }

    /**
     * Get the MenuItems of this ContextMenuOverlayElement.
     *
     * @return List of ContextMenuItemElement.
     */
    public List<ContextMenuItemElement> getMenuItems() {
        return $(ContextMenuItemElement.class).all();
    }
}
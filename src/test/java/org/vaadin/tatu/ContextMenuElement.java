package org.vaadin.tatu;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;vaadin-context-menu&gt;</code>
 * element.
 *
 * @author Vaadin Ltd
 *
 */
@Element("vaadin-context-menu")
public class ContextMenuElement extends TestBenchElement {

    /**
     * This is an utility method, which will produce context click on the target
     * element. If the target had ContextMenu, after opening the last
     * ContextMenuOverlayElement can be used to find its menu items.
     *
     * @param target
     *            The element to which the ContextMenu has been hooked to.
     */
    public static void openByRightClick(TestBenchElement target) {
        Actions action = new Actions(target.getDriver());
        action.contextClick(target).perform();
    }

    /**
     * Check if the ContextMenu is open.
     *
     * @return boolean True if menu is open.
     */
    public boolean isOpen() {
        try {
            return getAttribute("opened").equals("true");
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }
}
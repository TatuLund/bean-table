package org.vaadin.tatu;

import org.openqa.selenium.WebElement;
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
@Element("vaadin-context-menu-item")
public class ContextMenuItemElement extends TestBenchElement {

    /**
     * Open the potential sub menu of the this item by hovering. If there was a
     * submenu, after opening the last ContextMenuOverlayElement can be used to
     * find its menu items.
     */
    public void openSubMenu() {
        hoverOn(this);
    }

    protected void hoverOn(WebElement element) {
        Actions action = new Actions(getDriver());
        action.moveToElement(element).perform();
    }

    public boolean isChecked() {
        return hasAttribute("menu-item-checked");        
    }
}
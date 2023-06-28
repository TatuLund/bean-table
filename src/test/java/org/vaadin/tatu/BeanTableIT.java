package org.vaadin.tatu;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;

/**
 * These integration tests cover only a small subset of the BeanTable
 * functionality that cannot be covered by unit tests in BeantTableTest.java.
 */
public class BeanTableIT extends AbstractViewTest {

    public BeanTableIT() {
        super("lazy");
    }

    @Override
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    public void itemClick() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(2);

        checkbox.setChecked(true);

        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Clicked Bentley", getLastNotificationText());

        actions.click(table.getCell(2, 1)).perform();
        Assert.assertEquals("Clicked Brandon", getLastNotificationText());

        actions.sendKeys(Keys.ARROW_UP, Keys.SPACE).perform();
        Assert.assertEquals("Clicked Bentley", getLastNotificationText());
    }

    @Test
    public void preserveOnRefresh() {
        Actions actions = new Actions(getDriver());

        // Go to page 3
        $(ButtonElement.class).first().click();

        // Reload the page
        getDriver().navigate().refresh();

        // Table is still found
        TableElement table = $(TableElement.class).first();

        // Assert that keyboard navigation works and we are still
        // on page 3
        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Layla", focusedElement().getText());
        actions.sendKeys(Keys.END).perform();
        Assert.assertEquals("Washington", focusedElement().getText());
        actions.sendKeys(Keys.HOME).perform();
        Assert.assertEquals("Layla", focusedElement().getText());
    }

    @Test
    public void keyboardNavigationHomeEnd() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
        actions.sendKeys(Keys.END).perform();
        Assert.assertEquals("Washington", focusedElement().getText());
        actions.sendKeys(Keys.HOME).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
    }

    @Test
    public void keyboardNavigationRightLeft() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        // Traverse right
        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
            Assert.assertEquals(table.getCellText(1, i + 2),
                    focusedElement().getText());
        }
        Assert.assertEquals("Washington", focusedElement().getText());
        // We are at the end
        actions.sendKeys(Keys.ARROW_RIGHT).perform();
        Assert.assertEquals("Washington", focusedElement().getText());

        // Traverse left
        for (int i = 6; i > 0; i--) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
            Assert.assertEquals(table.getCellText(1, i),
                    focusedElement().getText());
        }
        Assert.assertEquals("Bentley", focusedElement().getText());
        // We are at start
        actions.sendKeys(Keys.ARROW_LEFT).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
    }

    @Test
    public void keyboardNavigationUpDown() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        // Traverse down
        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
        for (int i = 1; i < 20; i++) {
            actions.sendKeys(Keys.ARROW_DOWN).perform();
            Assert.assertEquals(table.getCellText(i + 1, 1),
                    focusedElement().getText());
        }
        Assert.assertEquals("Evan", focusedElement().getText());
        // We are at the end
        actions.sendKeys(Keys.ARROW_DOWN).perform();
        Assert.assertEquals("Evan", focusedElement().getText());

        // Traverse up
        for (int i = 19; i > 0; i--) {
            actions.sendKeys(Keys.ARROW_UP).perform();
            Assert.assertEquals(table.getCellText(i, 1),
                    focusedElement().getText());
        }
        Assert.assertEquals("Bentley", focusedElement().getText());
        // We are at start
        actions.sendKeys(Keys.ARROW_UP).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
    }

    @Test
    public void keyboardNavigationPageUpDown() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        actions.sendKeys(Keys.PAGE_DOWN).perform();
        waitASecond();
        Assert.assertEquals("Faith", focusedElement().getText());
        Assert.assertEquals("Page 2 of 6", table.getFooterElement(2).getText());

        actions.sendKeys(Keys.PAGE_UP).perform();
        waitASecond();
        Assert.assertEquals("Bentley", focusedElement().getText());
        Assert.assertEquals("Page 1 of 6", table.getFooterElement(2).getText());
    }

    @Test
    public void pagingButtons() {
        TableElement table = $(TableElement.class).first();

        table.getFooterElement(3).click();
        waitASecond();
        Assert.assertEquals("Faith", focusedElement().getText());
        Assert.assertEquals("Page 2 of 6", table.getFooterElement(2).getText());

        table.getFooterElement(1).click();
        waitASecond();
        Assert.assertEquals("Bentley", focusedElement().getText());
        Assert.assertEquals("Page 1 of 6", table.getFooterElement(2).getText());

        table.getFooterElement(4).click();
        waitASecond();
        Assert.assertEquals("Grace", focusedElement().getText());
        Assert.assertEquals("Page 6 of 6", table.getFooterElement(2).getText());

        table.getFooterElement(0).click();
        waitASecond();
        Assert.assertEquals("Bentley", focusedElement().getText());
        Assert.assertEquals("Page 1 of 6", table.getFooterElement(2).getText());
    }

    @Test
    public void selectionByMouse() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(1);
        checkbox.setChecked(true);

        Actions actions = new Actions(getDriver());
        actions.click(table.getCell(1, 1)).perform();

        Assert.assertEquals("Selection size: 1 Names: Bentley",
                getLastNotificationText());

        actions.click(table.getCell(2, 1)).perform();
        Assert.assertEquals("Selection size: 2 Names: Bentley,Brandon",
                getLastNotificationText());

        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Selection size: 1 Names: Brandon",
                getLastNotificationText());

    }

    @Test
    public void selectionByKeyboard() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(1);

        Actions actions = new Actions(getDriver());
        actions.click(table.getCell(1, 1)).perform();

        checkbox.setChecked(true);

        actions.sendKeys(Keys.SPACE).perform();
        Assert.assertEquals("Selection size: 1 Names: Bentley",
                getLastNotificationText());

        actions.sendKeys(Keys.ARROW_DOWN, Keys.SPACE).perform();
        Assert.assertEquals("Selection size: 2 Names: Bentley,Brandon",
                getLastNotificationText());

        actions.sendKeys(Keys.ARROW_UP, Keys.SPACE).perform();
        Assert.assertEquals("Selection size: 1 Names: Brandon",
                getLastNotificationText());
    }

    private String getLastNotificationText() {
        return $(NotificationElement.class).last().getText();
    }

    @Test
    public void menuColumnHideTest() {
        TableElement table = $(TableElement.class).first();
        table.getMenuButton().click();
        ContextMenuElement menu = $(ContextMenuElement.class).first();
        menu.isOpen();

        // Assert that menu has 7 items
        ContextMenuOverlayElement overlay = $(ContextMenuOverlayElement.class)
                .first();
        Assert.assertEquals(7, overlay.getMenuItems().size());
        overlay.getMenuItems()
                .forEach(item -> Assert.assertTrue(item.isChecked()));

        // Hide column
        overlay.getMenuItems().get(2).click();
        for (int i = 0; i < 20; i++) {
            Assert.assertFalse(table.getCell(i, 3).isDisplayed());
        }
        Assert.assertEquals("menu-button",
                focusedElement().getAttribute("class"));

        Actions actions = new Actions(getDriver());

        // It takes five presses to traverse to Washington as column is hidden
        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 5; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
        }
        Assert.assertEquals("Washington", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
        }
        Assert.assertEquals("Bentley", focusedElement().getText());

        // Un-hide column
        table.getMenuButton().click();
        overlay = $(ContextMenuOverlayElement.class).first();
        Assert.assertFalse(overlay.getMenuItems().get(2).isChecked());
        overlay.getMenuItems().get(2).click();
        for (int i = 0; i < 20; i++) {
            Assert.assertTrue(table.getCell(i, 3).isDisplayed());
        }

        // It takes now six presses to traverse to Washington
        actions.click(table.getCell(1, 1)).perform();
        Assert.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
        }
        Assert.assertEquals("Washington", focusedElement().getText());
        for (int i = 0; i < 7; i++) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
        }
        Assert.assertEquals("Bentley", focusedElement().getText());
    }

    private WebElement focusedElement() {
        return getDriver().switchTo().activeElement();
    }

    private void waitASecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
}
package org.vaadin.tatu;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.combobox.testbench.MultiSelectComboBoxElement;
import com.vaadin.flow.component.contextmenu.testbench.ContextMenuElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.screenshot.ImageFileUtil;

/**
 * These integration tests cover only a small subset of the BeanTable
 * functionality that cannot be covered by unit tests in BeantTableTest.java.
 */
public class BeanTableIT extends AbstractViewTest {

    public BeanTableIT() {
        super("lazy");
    }

    @BrowserTest
    public void canTabToTheFirsDataCell() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(0, 1)).perform();
        Assertions.assertEquals("First Name", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Last Name", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Age", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Phone Number", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Marital Status", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Postal Code", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("City", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("vaadin-button", focusedElement().getTagName());
        Assertions.assertEquals("menu-button",
                focusedElement().getDomAttribute("class"));
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        actions.sendKeys(Keys.TAB).perform();
        Assertions.assertEquals("vaadin-button", focusedElement().getTagName());
    }

    @BrowserTest
    public void keyboardNavigationHomeEnd() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        actions.sendKeys(Keys.END).perform();
        Assertions.assertEquals("Washington", focusedElement().getText());
        actions.sendKeys(Keys.HOME).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
    }

    @BrowserTest
    public void keyboardNavigationRightLeft() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        // Traverse right
        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
            Assertions.assertEquals(table.getCellText(1, i + 2),
                    focusedElement().getText());
        }
        Assertions.assertEquals("Washington", focusedElement().getText());
        // We are at the end
        actions.sendKeys(Keys.ARROW_RIGHT).perform();
        Assertions.assertEquals("Washington", focusedElement().getText());

        // Traverse left
        for (int i = 6; i > 0; i--) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
            Assertions.assertEquals(table.getCellText(1, i),
                    focusedElement().getText());
        }
        Assertions.assertEquals("Bentley", focusedElement().getText());
        // We are at start
        actions.sendKeys(Keys.ARROW_LEFT).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
    }

    @BrowserTest
    public void keyboardNavigationUpDown() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        // Traverse down
        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        for (int i = 1; i < 20; i++) {
            actions.sendKeys(Keys.ARROW_DOWN).perform();
            Assertions.assertEquals(table.getCellText(i + 1, 1),
                    focusedElement().getText());
        }
        Assertions.assertEquals("Evan", focusedElement().getText());
        // We are at the end
        actions.sendKeys(Keys.ARROW_DOWN).perform();
        Assertions.assertEquals("Evan", focusedElement().getText());

        // Traverse up
        for (int i = 19; i > 0; i--) {
            actions.sendKeys(Keys.ARROW_UP).perform();
            Assertions.assertEquals(table.getCellText(i, 1),
                    focusedElement().getText());
        }
        Assertions.assertEquals("Bentley", focusedElement().getText());
        // We are at start
        actions.sendKeys(Keys.ARROW_UP).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
    }

    @BrowserTest
    public void keyboardNavigationPageUpDown() {
        TableElement table = $(TableElement.class).first();
        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        actions.sendKeys(Keys.PAGE_DOWN).perform();
        waitForPageAndFocus(table, "Page 2 of 6", "Faith");

        actions.sendKeys(Keys.PAGE_UP).perform();
        waitForPageAndFocus(table, "Page 1 of 6", "Bentley");
    }

    @BrowserTest
    public void pagingButtons() {
        TableElement table = $(TableElement.class).first();

        table.getFooterElement(3).click();
        waitForPageAndFocus(table, "Page 2 of 6", "Faith");

        table.getFooterElement(1).click();
        waitForPageAndFocus(table, "Page 1 of 6", "Bentley");

        table.getFooterElement(4).click();
        waitForPageAndFocus(table, "Page 6 of 6", "Grace");

        table.getFooterElement(0).click();
        waitForPageAndFocus(table, "Page 1 of 6", "Bentley");
    }

    @BrowserTest
    public void selectionByMouse() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(1);
        checkbox.setChecked(true);

        Actions actions = new Actions(getDriver());
        actions.click(table.getCell(1, 1)).perform();

        Assertions.assertEquals("Selection size: 1 Names: Bentley",
                getLastNotificationText());

        actions.click(table.getCell(2, 1)).perform();
        Assertions.assertEquals("Selection size: 2 Names: Bentley,Brandon",
                getLastNotificationText());

        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Selection size: 1 Names: Brandon",
                getLastNotificationText());

    }

    @BrowserTest
    public void selectionByKeyboard() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(1);

        Actions actions = new Actions(getDriver());
        actions.click(table.getCell(1, 1)).perform();

        checkbox.setChecked(true);

        actions.sendKeys(Keys.SPACE).perform();
        Assertions.assertEquals("Selection size: 1 Names: Bentley",
                getLastNotificationText());

        actions.sendKeys(Keys.ARROW_DOWN, Keys.SPACE).perform();
        Assertions.assertEquals("Selection size: 2 Names: Bentley,Brandon",
                getLastNotificationText());

        actions.sendKeys(Keys.ARROW_UP, Keys.SPACE).perform();
        Assertions.assertEquals("Selection size: 1 Names: Brandon",
                getLastNotificationText());
    }

    private String getLastNotificationText() {
        return $(NotificationElement.class).last().getText();
    }

    @BrowserTest
    public void menuColumnHideTest() {
        TableElement table = $(TableElement.class).first();
        table.getMenuButton().click();
        ContextMenuElement menu = $(ContextMenuElement.class).first();
        menu.isOpen();

        // Assert that menu has 7 items
        Assertions.assertEquals(7, menu.getMenuItems().size());
        menu.getMenuItems()
                .forEach(item -> Assertions.assertTrue(item.isChecked()));

        // Hide column
        menu.getMenuItems().get(2).click();
        for (int i = 0; i < 20; i++) {
            Assertions.assertFalse(table.getCell(i, 3).isDisplayed());
        }

        Actions actions = new Actions(getDriver());

        // It takes five presses to traverse to Washington as column is hidden
        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 5; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
        }
        Assertions.assertEquals("Washington", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
        }
        Assertions.assertEquals("Bentley", focusedElement().getText());

        // Un-hide column
        table.getMenuButton().click();
        menu = $(ContextMenuElement.class).first();
        Assertions.assertFalse(menu.getMenuItems().get(2).isChecked());
        menu.getMenuItems().get(2).click();
        for (int i = 0; i < 20; i++) {
            Assertions.assertTrue(table.getCell(i, 3).isDisplayed());
        }

        // It takes now six presses to traverse to Washington
        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Bentley", focusedElement().getText());
        for (int i = 0; i < 6; i++) {
            actions.sendKeys(Keys.ARROW_RIGHT).perform();
        }
        Assertions.assertEquals("Washington", focusedElement().getText());
        for (int i = 0; i < 7; i++) {
            actions.sendKeys(Keys.ARROW_LEFT).perform();
        }
        Assertions.assertEquals("Bentley", focusedElement().getText());
    }

    @BrowserTest
    public void itemClick() {
        TableElement table = $(TableElement.class).first();
        CheckboxElement checkbox = $(CheckboxElement.class).get(2);

        checkbox.setChecked(true);

        Actions actions = new Actions(getDriver());

        actions.click(table.getCell(1, 1)).perform();
        Assertions.assertEquals("Clicked Bentley", getLastNotificationText());

        actions.click(table.getCell(2, 1)).perform();
        Assertions.assertEquals("Clicked Brandon", getLastNotificationText());

        actions.sendKeys(Keys.ARROW_UP, Keys.SPACE).perform();
        Assertions.assertEquals("Clicked Bentley", getLastNotificationText());
    }

    @BrowserTest
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
        Assertions.assertEquals("Layla", focusedElement().getText());
        actions.sendKeys(Keys.END).perform();
        Assertions.assertEquals("Washington", focusedElement().getText());
        actions.sendKeys(Keys.HOME).perform();
        Assertions.assertEquals("Layla", focusedElement().getText());
    }

    // @BrowserTest
    public void themeVariants() throws IOException {
        TableElement table = $(TableElement.class).first();

        MultiSelectComboBoxElement variants = $(
                MultiSelectComboBoxElement.class).first();

        for (List<String> perm : getPermutations()) {
            variants.deselectAll();
            String fileName = "default.png";
            if (perm.size() > 0) {
                fileName = perm.stream().map(String::toLowerCase)
                        .collect(Collectors.joining("-")) + ".png";
            }
            for (String name : perm) {
                variants.selectByText(name);
            }
            Assertions.assertTrue(table.compareScreen(
                    ImageFileUtil.getReferenceScreenshotFile(fileName)));
        }
    }

    public List<List<String>> getPermutations() {
        List<String> variantNames = Arrays.asList("NO_BORDER", "NO_ROW_BORDERS",
                "COLUMN_BORDERS", "ROW_STRIPES", "PADDING");
        List<List<String>> permutations = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, variantNames.size()); i++) {
            List<String> permutation = new ArrayList<>();
            for (int j = 0; j < variantNames.size(); j++) {
                if ((i & (1 << j)) != 0) {
                    permutation.add(variantNames.get(j));
                }
            }
            permutations.add(permutation);
        }
        return permutations;
    }

    private WebElement focusedElement() {
        return getDriver().switchTo().activeElement();
    }

    private void waitForPageAndFocus(TableElement table, String pageLabel,
            String focusedText) {
        new WebDriverWait(getDriver(), Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .withMessage("Expected " + pageLabel + " with focus on "
                        + focusedText)
                .until(driver -> pageLabel
                        .equals(table.getFooterElement(2).getText())
                        && focusedText.equals(focusedElement().getText()));
    }
}

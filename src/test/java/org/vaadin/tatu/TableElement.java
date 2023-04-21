package org.vaadin.tatu;

import org.openqa.selenium.WebElement;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("table")
public class TableElement extends TestBenchElement {

    public String getCellText(int row, int column) {
        return getCell(row, column).getText();
    }

    public WebElement getCell(int row, int column) {
        return (WebElement) executeScript(
                "return arguments[0].rows[arguments[1]].cells[arguments[2]]",
                this, row, column);
    }

    public String getHeaderText(int column) {
        return getHeaderCell(column).getText();
    }

    public WebElement getHeaderCell(int column) {
        return getCell(0, column);
    }

    public WebElement getMenuButton() {
        return (WebElement) executeScript(
                "return arguments[0].tHead.children[1]", this);
    }

    public WebElement getFooterElement(int index) {
        return (WebElement) executeScript(
                "return arguments[0].tFoot.rows[0].cells[0].children[0].children[arguments[1]]",
                this, index);
    }
}

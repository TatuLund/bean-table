package org.vaadin.tatu;

import java.io.Serializable;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

@JsDefinition
interface BeanTableJs extends Serializable {

  @JsExpression("""
      this.addEventListener('click', (e) => {
        Array.from(this.rows).forEach(row => Array.from(row.cells).forEach(cell => cell.setAttribute('tabindex','-1')));
        const cell = e.target;
        cell.setAttribute('tabindex','0');
      });
      this.addEventListener('keydown', (e) => {
        if (e.keyCode == 39) {
          e.preventDefault();
          let cell = document.activeElement;
          const previous = cell;
          do {
            cell = cell.nextSibling;
          } while (cell && (cell.style.display === 'none'));
          if (cell) {
            previous.setAttribute('tabindex','-1');
            cell.setAttribute('tabindex','0');
            cell.focus();
          }
        } else if (e.keyCode == 37) {
          e.preventDefault();
          let cell = document.activeElement;
          const previous = cell;
          do {
            cell = cell.previousSibling;
          } while (cell && (cell.style.display === 'none'));
          if (cell) {
            previous.setAttribute('tabindex','-1');
            cell.setAttribute('tabindex','0');
            cell.focus();
          }
        } else if (e.keyCode == 36) {
          e.preventDefault();
          let row = document.activeElement.closest('tr');
          let col=1;
          const previous = row.cells[col];
          while (col < row.cells.length-1 && row.cells[col].style.display && row.cells[col].style.display === 'none') {
            col++;
          }
          if (row) {
            previous.setAttribute('tabindex','-1');
            row.cells[col].setAttribute('tabindex','0');
            row.cells[col].focus();
          }
        } else if (e.keyCode == 35) {
          e.preventDefault();
          let row = document.activeElement.closest('tr');
          let col=row.cells.length-1;
          const previous = row.cells[col];
          while (col > 1 && row.cells[col].style.display && row.cells[col].style.display === 'none') {
            col--;
          }
          if (row) {
            previous.setAttribute('tabindex','-1');
            row.cells[col].setAttribute('tabindex','0');
            row.cells[col].focus();
          }
        } else if (e.keyCode == 40) {
          e.preventDefault();
          let col = document.activeElement.cellIndex;
          let rowIndex = document.activeElement.closest('tr').rowIndex;
          let row = this.rows[rowIndex];
          const previous = this.rows[rowIndex-1].cells[col];
          if (row) {
            previous.setAttribute('tabindex','-1');
            row.cells[col].setAttribute('tabindex','0');
            row.cells[col].focus();
          }
        } else if (e.keyCode == 38) {
          e.preventDefault();
          let col = document.activeElement.cellIndex;
          let rowIndex = document.activeElement.closest('tr').rowIndex;
          let row = this.rows[rowIndex - 2];
          const previous = this.rows[rowIndex-1].cells[col];
          if (row) {
            previous.setAttribute('tabindex','-1');
            row.cells[col].setAttribute('tabindex','0');
            row.cells[col].focus();
          }
        }
      })""")
  void enableKeyboardNavigation();

  @JsExpression("""
      const rootElement = document.documentElement;
      const style = getComputedStyle(rootElement);
      let theme = "";
      theme =
          style.getPropertyValue("--vaadin-aura-theme").trim() === "1" ? "aura" : "";
      if (theme === "") {
          theme =
              style.getPropertyValue("--vaadin-lumo-theme").trim() === "1"
                  ? "lumo"
                  : "";
      }
      this.classList.add(theme);
      """)
  void detectTheme();

  @JsExpression("""
      setTimeout(() => {
        Array.from(this.rows).forEach(row => Array.from(row.cells).forEach(cell => cell.setAttribute('tabindex','-1')));
        let row = this.rows[$0];
        if (row) {
          let cell = row.cells[$1];
          if (cell) {
            cell.setAttribute('tabindex','0');
            cell.click();
            cell.focus()
          }
        }
      }, 0);
      """)
  void focus(Integer row, Integer col);
}

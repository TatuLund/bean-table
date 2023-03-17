package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.tatu.BeanTable.ColumnAlignment;
import org.vaadin.tatu.BeanTable.FocusBehavior;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;

@Route("")
public class View extends VerticalLayout {

    int year = 2000;
    private List<MonthlyExpense> data;
    private int index = 0;
    private int nextYear = 2025;
    private BeanTableListDataView<MonthlyExpense> dataView;

    public View() {
        setSizeFull();
        BeanTable<MonthlyExpense> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.setFocusBehavior(FocusBehavior.BODY);
        table.addColumn("Year", MonthlyExpense::getYear)
                .setClassNameProvider(
                        item -> item.getYear() % 10 == 0 ? "millenium" : "")
                .setAlignment(ColumnAlignment.CENTER).setWidth("100px");
        table.addColumn("Month", expense -> "<i>" + expense.getMonth() + "</i>")
                .setRowHeader(true);
        table.addColumn("Expenses", expense -> expense.getExpenses())
                .setTooltipProvider(item -> "Expenses of " + item.getMonth()
                        + " were " + item.getExpenses());
        table.setClassNameProvider(
                item -> item.getExpenses() > 600 ? "expenses" : "");
        table.setCaption("Monthly Expenses");
        // table.addComponentColumn("expense", expense -> {
        // NumberField field = new NumberField();
        // field.setValue(expense.getExpenses());
        // field.addValueChangeListener(event -> {
        // expense.setExpenses(event.getValue());
        // });
        // return field;
        // });
        table.addComponentColumn(null, expense -> {
            Button edit = new Button("edit");
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            edit.addClickListener(event -> {
                Dialog dialog = new Dialog();
                index = data.indexOf(expense);
                dialog.add(createForm());
                dialog.open();
            });
            return edit;
        }).setHeader(new Html("<span style='color: blue'>Edit</span>"))
                .setAlignment(ColumnAlignment.CENTER).setWidth("100px");
        // table.setColumns("year","month","expenses");
        data = getData(25);
        dataView = table.setItems(data);
        MyButton plus = new MyButton("+");
        MyButton minus = new MyButton("-");
        dataView.setFilter(expense -> expense.getYear() == year);
        plus.addSingleClickListener(event -> {
            year++;
            dataView.setFilter(expense -> expense.getYear() == year);
        });
        minus.addSingleClickListener(event -> {
            year--;
            dataView.setFilter(expense -> expense.getYear() == year);
        });
        table.setWidthFull();
        
        Button newData = new Button("Add " + nextYear);
        newData.addClickListener(event -> {
            dataView.addItems(getNewData(nextYear++));
            newData.setText("Add " + nextYear);
        });
        RouterLink lazy = new RouterLink("Lazy load demo", LazyView.class);

        table.focus();
        add(plus, minus, table, newData, lazy);
    }

    private HorizontalLayout createForm() {
        HorizontalLayout layout = new HorizontalLayout();
        Button plus = new Button("+");
        Button minus = new Button("-");
        Button save = new Button("Save");
        Span year = new Span();
        Span month = new Span();
        NumberField expenseField = new NumberField();
        populateForm(year, month, expenseField);
        plus.addClickListener(event -> {
            if (index < data.size())
                index++;
            populateForm(year, month, expenseField);
        });
        minus.addClickListener(event -> {
            if (index > 0)
                index--;
            populateForm(year, month, expenseField);
        });
        save.addClickListener(event -> {
            data.get(index).setExpenses(expenseField.getValue());
            dataView.refreshItem(data.get(index));
        });
        layout.setWidthFull();
        layout.add(plus, minus, year, month, expenseField, save);
        return layout;
    }

    private void populateForm(Span year, Span month, NumberField expenseField) {
        MonthlyExpense expense = data.get(index);
        year.setText(Integer.toString(expense.getYear()));
        month.setText(expense.getMonth());
        expenseField.setValue(expense.getExpenses());
    }

    public List<MonthlyExpense> getData(int years) {
        String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        List<MonthlyExpense> data = new ArrayList<>();
        for (int year = 2000; year < (2000 + years); year++) {
            for (int month = 0; month < 12; month++) {
                data.add(new MonthlyExpense(monthNames[month], year,
                        getExpenses()));
            }
        }
        return data;
    }

    public List<MonthlyExpense> getNewData(int year) {
        String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        List<MonthlyExpense> data = new ArrayList<>();
        for (int month = 0; month < 12; month++) {
            data.add(
                    new MonthlyExpense(monthNames[month], year, getExpenses()));
        }
        return data;
    }

    public Double getExpenses() {
        return Math.floor((Math.random() * 1000) % 500 + 300);
    }

    public class MonthlyExpense {
        private String month;
        private Double expenses;
        private int year;
        private String status = "Open";

        public MonthlyExpense(String month, int year, Double expenses) {
            setMonth(month);
            setExpenses(expenses);
            setYear(year);
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Double getExpenses() {
            return expenses;
        }

        public void setExpenses(Double expenses) {
            this.expenses = expenses;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

    }

    class MyButton extends Button {

        public MyButton(String caption) {
            super(caption);
            this.addClickListener(e -> {
                if (e.getClickCount() == 1) {
                    setEnabled(false);
                    try {
                        fireEvent(new SingleClickEvent(this, true));
                    } finally {
                        setEnabled(true);
                    }
                }
            });
        }

        public Registration addSingleClickListener(
                ComponentEventListener<SingleClickEvent> listener) {
            return addListener(SingleClickEvent.class, listener);
        }

    }

    public static class SingleClickEvent extends ComponentEvent<MyButton> {
        public SingleClickEvent(MyButton source, boolean isFromClient) {
            super(source, isFromClient);
        }
    }
}

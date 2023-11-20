package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Text;
import org.vaadin.tatu.BeanTable.ColumnAlignment;
import org.vaadin.tatu.BeanTable.FocusBehavior;

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

@Route("")
public class View extends VerticalLayout {

    private final Span yearLabel;
    int year = 2000;
    private List<MonthlyExpense> data;
    private int index = 0;
    private int nextYear = 2025;

    BeanTable<MonthlyExpense> table;

    public View() {
        setSizeFull();
        table = new BeanTable<>();
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
        }).setHeader(new Html("""
                <span style='color: blue'>Edit</span>
                """)).setAlignment(ColumnAlignment.CENTER).setWidth("100px");
        // table.setColumns("year","month","expenses");
        data = generateData(25);
        Button plus = new Button("+");
        yearLabel = new Span();
        Button minus = new Button("-");
        HorizontalLayout yearFilter = new HorizontalLayout(new Text("Year:"), plus, yearLabel, minus);
        showYear(year);
        plus.addClickListener(event -> {
            showYear(year + 1);
        });
        minus.addClickListener(event -> {
            showYear(year - 1);
        });
        table.setWidthFull();

        Button newData = new Button("Add for year" + nextYear);
        newData.addClickListener(event -> {
            data.addAll(generateMonthlyExpensesForYear(nextYear));
            showYear(nextYear);
            nextYear++;
            newData.setText("Add " + nextYear);
        });
        RouterLink lazy = new RouterLink("Lazy load demo", LazyView.class);

        table.focus();
        add(yearFilter, table, newData, lazy);
    }

    private void showYear(int year) {
        this.year = year;
        table.setItems(data.stream().filter(expense -> expense.getYear() == year));
        yearLabel.setText("" + year);
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
            // Note, this is rather cumbersome and don't really
            // provide any benefit over new setItems call
            table.getGenericDataView().refreshItem(data.get(index));
            //.. so this could simply be for example
            // showYear(View.this.year);
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

    public List<MonthlyExpense> generateData(int years) {
        String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        List<MonthlyExpense> data = new ArrayList<>();
        for (int year = 2000; year < (2000 + years); year++) {
            for (int month = 0; month < 12; month++) {
                data.add(new MonthlyExpense(monthNames[month], year,
                        createRandomeNumberForExpenses()));
            }
        }
        return data;
    }

    public List<MonthlyExpense> generateMonthlyExpensesForYear(int year) {
        String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        List<MonthlyExpense> data = new ArrayList<>();
        for (int month = 0; month < 12; month++) {
            data.add(
                    new MonthlyExpense(monthNames[month], year, createRandomeNumberForExpenses()));
        }
        return data;
    }

    public Double createRandomeNumberForExpenses() {
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
}

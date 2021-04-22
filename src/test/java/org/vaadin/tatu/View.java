package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;

@Route("")
public class View extends VerticalLayout {

    int year = 2000;
    private List<MonthlyExpense> data;
    private ListDataProvider<MonthlyExpense> dp;
    private int index = 0;
    
    public View() {
        setSizeFull();
        BeanTable<MonthlyExpense> table = new BeanTable<>(MonthlyExpense.class,false);
        table.setHtmlAllowed(true);
        table.addColumn("Year", MonthlyExpense::getYear);
        table.addColumn("Month", expense -> "<i>" + expense.getMonth() + "</i>");
        table.addColumn("expenses");
//        table.addComponentColumn("expense", expense -> {
//            NumberField field = new NumberField();
//            field.setValue(expense.getExpenses());
//            field.addValueChangeListener(event -> {
//                expense.setExpenses(event.getValue());
//            });
//            return field;
//        });
        table.addComponentColumn(null, expense -> {
           Button edit = new Button("edit");
           edit.addClickListener(event -> {
              Dialog dialog = new Dialog();
              index = data.indexOf(expense);
              dialog.add(createForm());
              dialog.open();
           });
           return edit;
        });
//        table.setColumns("year","month","expenses");
        data = getData();
        table.setItems(data);
        Button plus = new Button("+");
        Button minus = new Button("-");
        dp = (ListDataProvider<MonthlyExpense>) table.getDataProvider();
//        dp.setFilter(expense -> expense.getYear() == year);
        plus.addClickListener(event -> {
            year++;
            dp.setFilter(expense -> expense.getYear() == year);
        });
        minus.addClickListener(event -> {
            year--;
            dp.setFilter(expense -> expense.getYear() == year);
        });
        table.setWidthFull();
        add(plus,minus,table);        
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
            if (index < data.size()) index++;
            populateForm(year, month, expenseField);
        });
        minus.addClickListener(event -> {
            if (index > 0) index--;
            populateForm(year, month, expenseField);
        });
        save.addClickListener(event -> {
           data.get(index).setExpenses(expenseField.getValue());
           dp.refreshItem(data.get(index));
        });
        layout.setWidthFull();
        layout.add(plus,minus,year,month,expenseField,save);
        return layout;
    }

    private void populateForm(Span year, Span month, NumberField expenseField) {
        MonthlyExpense expense = data.get(index);
        year.setText(Integer.toString(expense.getYear()));
        month.setText(expense.getMonth());
        expenseField.setValue(expense.getExpenses());
    }

    public List<MonthlyExpense> getData() {
        String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        List<MonthlyExpense> data = new ArrayList<>();
        for (int year = 2000; year < 2020; year++) {
            for (int month = 0; month < 12; month++) {
                data.add(new MonthlyExpense(monthNames[month], year,
                        getExpenses()));
            }
        }
        return data;
    }

    public  Double getExpenses() {
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

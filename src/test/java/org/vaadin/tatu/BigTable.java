package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.vaadin.tatu.BeanTable.ColumnSelectMenu;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;

@Route(value = "big-table")
public class BigTable extends Div {

    public BigTable() {
        setSizeFull();
        BeanTable<Map<String, Integer>> table = new BeanTable<>(35);
        table.addThemeVariants(BeanTableVariant.ROW_STRIPES,
                BeanTableVariant.COLUMN_BORDERS);
        table.getElement().getStyle().set("font-size", "12px");
        int columns = 59;

        List<Map<String, Integer>> items = createData(columns);
        MyDataProvider<Map<String, Integer>> dataProvider = new MyDataProvider<>(
                items);

        for (int i = 0; i < columns; i++) {
            final int index = i;
            table.addColumn("C" + index, map -> map.get("col" + index))
                    .setWidth("40px");
        }
        table.addColumn("Sum", map -> map.get("sum"));

        table.setDataProvider(dataProvider);
        table.setColumnSelectionMenu(ColumnSelectMenu.CONTEXT);

        add(table);
    }

    // Generate some mock data
    private List<Map<String, Integer>> createData(int columns) {
        Random random = new Random();
        List<Map<String, Integer>> items = new ArrayList<>();
        for (int j = 0; j < 1000; j++) {
            final Map<String, Integer> values = new HashMap<>();
            values.put("id", j);
            int sum = 0;
            for (int i = 0; i < columns; i++) {
                int number = random.nextInt(10000);
                values.put("col" + i, number);
                sum = sum + number;
            }
            values.put("sum", sum);
            items.add(values);
        }
        return items;
    }

    // HashMap identity does not play nice with Grid by default, thus
    // we use option to override getId method of DataProvider to return value
    // of "id" key as identity.
    public class MyDataProvider<T> extends ListDataProvider<T> {

        public MyDataProvider(Collection<T> items) {
            super(items);
        }

        @Override
        public String getId(T item) {
            Objects.requireNonNull(item,
                    "Cannot provide an id for a null item.");
            if (item instanceof Map<?, ?>) {
                if (((Map<String, ?>) item).get("id") != null)
                    return ((Map<String, ?>) item).get("id").toString();
                else
                    return item.toString();
            } else {
                return item.toString();
            }
        }

    }
}
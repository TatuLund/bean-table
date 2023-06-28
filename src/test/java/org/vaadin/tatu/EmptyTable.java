package org.vaadin.tatu;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.vaadin.tatu.BeanTable.FocusBehavior;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;

@Route("empty")
public class EmptyTable extends Div {

    public EmptyTable() {
        DataService service = new DataService();
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", item -> item.getName());
        table.addColumn("Data", item -> item.getData());
        table.setFocusBehavior(FocusBehavior.BODY);
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setCaption("Items");
        table.setWidth("400px");
        table.setMinHeight("300px");

        Button add = new Button("Add items", e -> {
            table.setItems(items);
        });

        Button error = new Button("Error", e -> {
            CallbackDataProvider<DataItem, Void> dataProvider = DataProvider
                    .fromCallbacks(query -> service.fetchData(),
                            query -> service.count());

            table.setDataProvider(dataProvider);
        });

        add(table, add, error);
    }

    public class DataService {
        DataRepository repository = new DataRepository();

        public Stream<DataItem> fetchData() {
            return repository.fetch().stream();
        }

        public int count() {
            return 100;
        }
    }

    public class DataRepository {
        public List<DataItem> fetch() {
            return null;
        }
    }

    public class DataItem {
        private String name;
        private String data;

        public DataItem(String name, String data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

}
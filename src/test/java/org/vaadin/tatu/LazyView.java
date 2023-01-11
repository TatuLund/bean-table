package org.vaadin.tatu;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.vaadin.tatu.BeanTable.BeanTableI18n;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("lazy")
public class LazyView extends VerticalLayout {

    public LazyView() {
        setSizeFull();
        BeanTable<Person> table = new BeanTable<>(Person.class, false, 20);
        PersonService personService = new PersonService();
        table.setColumns("firstName", "lastName", "age", "phoneNumber",
                "maritalStatus");
        table.addColumn("Postal Code",
                person -> person.getAddress() == null ? ""
                        : person.getAddress().getPostalCode());
        table.addColumn("City", person -> person.getAddress() == null ? ""
                : person.getAddress().getCity());
        table.setI18n(BeanTableI18n.getDefault());

        CallbackDataProvider<Person, String> dataProvider = DataProvider
                .fromFilteringCallbacks(
                        query -> personService.fetch(query.getOffset(),
                                query.getLimit(), query.getFilter()).stream(),
                        query -> personService.count(query.getFilter()));

        ConfigurableFilterDataProvider<Person, Void, String> dp = dataProvider
                .withConfigurableFilter();

        BeanTableDataView<Person> dataView = table.setItems(dp);

        table.setWidthFull();

        TextField filter = new TextField("Filter");
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(event -> {
            dp.setFilter(event.getValue());
        });
        dataView.addItemCountChangeListener(event -> {
            Notification.show("Count: " + event.getItemCount());
        });

        Button button = new Button("3");
        button.addClickListener(e -> {
            table.setPage(2);
        });

        add(filter, table, button);
    }
}

package org.vaadin.tatu;

import org.vaadin.tatu.BeanTable.BeanTableI18n;
import org.vaadin.tatu.BeanTable.FocusBehavior;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("lazy")
public class LazyView extends VerticalLayout {

    public LazyView() {
        setSizeFull();
        BeanTable<Person> table = new BeanTable<>(Person.class, false, 20);
        PersonService personService = new PersonService();
        table.setFocusBehavior(FocusBehavior.BODY_AND_HEADER);
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
            if (!event.getValue().isEmpty()) {
                table.setCaption("Filtered results " + table.getRowCount());
            } else {
                table.setCaption(null);
            }
        });
        dataView.addItemCountChangeListener(event -> {
            Notification.show("Count: " + event.getItemCount());
        });

        Button button = new Button("Go to page 3");
        button.addClickListener(e -> {
            table.setPage(2);
        });

        MultiSelectComboBox<BeanTableVariant> variants = new MultiSelectComboBox<>(
                "Variants");
        variants.setItems(BeanTableVariant.values());
        variants.addValueChangeListener(e -> {
            table.removeThemeVariants(BeanTableVariant.values());
            variants.getValue()
                    .forEach(variant -> table.addThemeVariants(variant));
        });

        HorizontalLayout tools = new HorizontalLayout();
        tools.addClassName(LumoUtility.AlignItems.BASELINE);
        tools.add(filter, variants, button);

        RouterLink big = new RouterLink("Big table demo", BigTable.class);

        add(tools, table, big);
    }
}

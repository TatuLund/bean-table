package org.vaadin.tatu;

import java.util.stream.Collectors;

import org.vaadin.gatanaso.MultiselectComboBox;
import org.vaadin.tatu.BeanTable.BeanTableI18n;
import org.vaadin.tatu.BeanTable.ColumnSelectMenu;
import org.vaadin.tatu.BeanTable.FocusBehavior;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
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

        table.setDataProvider(dp);

        table.setWidth("1000px");
        table.setColumnSelectionMenu(ColumnSelectMenu.BUTTON);

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

        Button button = new Button("Go to page 3");
        button.addClickListener(e -> {
            table.setPage(2);
        });

        Checkbox phoneNumber = new Checkbox("Phone Number");
        phoneNumber.setValue(true);
        phoneNumber.addValueChangeListener(e -> {
            table.getColumn("phoneNumber")
                    .ifPresent(col -> col.setVisible(e.getValue()));
        });

        Checkbox selection = new Checkbox("Selection");
        selection.addValueChangeListener(e -> {
            table.setSelectionEnabled(e.getValue());
        });

        MultiselectComboBox<BeanTableVariant> variants = new MultiselectComboBox<>(
                "Variants");
        variants.setId("variants");
        variants.setItems(BeanTableVariant.values());
        variants.addValueChangeListener(e -> {
            table.removeThemeVariants(BeanTableVariant.values());
            variants.getValue()
                    .forEach(variant -> table.addThemeVariants(variant));
        });

        HorizontalLayout tools = new HorizontalLayout();
        tools.getElement().getStyle().set("align-items", "baseline");
        tools.add(filter, variants, button, phoneNumber, selection);

        table.addSelectionChangedListener(event -> {
            String names = event.getSelected().stream()
                    .map(item -> item.getFirstName())
                    .collect(Collectors.joining(","));
            Notification.show("Selection size: " + event.getSelected().size()
                    + " Names: " + names);
        });

        RouterLink big = new RouterLink("Big table demo", BigTable.class);

        add(tools, table, big);
    }
}
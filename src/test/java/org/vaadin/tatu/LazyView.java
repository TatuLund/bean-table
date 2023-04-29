package org.vaadin.tatu;

import java.util.stream.Collectors;

import org.vaadin.tatu.BeanTable.BeanTableI18n;
import org.vaadin.tatu.BeanTable.ColumnSelectMenu;
import org.vaadin.tatu.BeanTable.FocusBehavior;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PreserveOnRefresh
@Route("lazy")
public class LazyView extends VerticalLayout {

    public LazyView() {
        setSizeFull();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

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

        table.setWidth("1000px");
        table.setColumnSelectionMenu(ColumnSelectMenu.BUTTON);
        layout.add(table);

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

        Checkbox click = new Checkbox("Click");

        MultiSelectComboBox<BeanTableVariant> variants = new MultiSelectComboBox<>(
                "Variants");
        variants.setItems(BeanTableVariant.values());
        variants.addValueChangeListener(e -> {
            table.removeThemeVariants(BeanTableVariant.values());
            variants.getValue()
                    .forEach(variant -> table.addThemeVariants(variant));
        });

        Button select = new Button("Select 10,12");
        select.addClickListener(e -> {
            table.select(dataView.getItem(10), dataView.getItem(12));
        });

        HorizontalLayout tools = new HorizontalLayout();
        tools.addClassName(LumoUtility.AlignItems.BASELINE);
        tools.add(filter, variants, button, select, phoneNumber, selection, click);

        RouterLink big = new RouterLink("Big table demo", BigTable.class);

        table.addSelectionChangedListener(event -> {
            String names = event.getSelected().stream()
                    .map(item -> item.getFirstName())
                    .collect(Collectors.joining(","));
            Notification.show("Selection size: " + event.getSelected().size()
                    + " Names: " + names);
        });

        table.addItemClickedListener(event -> {
            if (click.getValue()) {
                Notification.show("Clicked " + event.getItem().getFirstName());
            }
        });

        add(tools, layout, big);
    }
}

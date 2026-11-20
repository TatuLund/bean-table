package org.vaadin.tatu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.tatu.BeanTable.BeanTableI18n;
import org.vaadin.tatu.BeanTable.Column;
import org.vaadin.tatu.BeanTable.ColumnAlignment;
import org.vaadin.tatu.BeanTable.ColumnSelectMenu;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * As most of the functionality of the BeanTable is implemented by using Element
 * API, it is possible to cover that functionality by unit tests. There is some
 * things that can't be covered by unit tests, and those are verified by
 * BeanTableIT.java
 */
public class BeanTableTest {

    private int count;
    private MockUI ui;
    private Set<DataItem> selected = null;

    @BeforeEach
    public void init() {
        ui = new MockUI();
        UI.setCurrent(ui);
    }

    @Test
    public void basicBeanTable() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", DataItem::getName).setRowHeader(true);
        BeanTable<DataItem>.Column<DataItem> col = table.addColumn("Data",
                DataItem::getData);
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i)).toList();
        table.setItems(items);
        table.setCaption("Items");
        ui.add(table);
        fakeClientCommunication();

        assertBasicTable(table, col);
    }

    @Test
    public void recordBeanTable() {
        BeanTable<DataRecord> table = new BeanTable<>();
        table.addColumn("Name", item -> item.name).setRowHeader(true);
        BeanTable<DataRecord>.Column<DataRecord> col = table.addColumn("Data",
                item -> item.data);
        List<DataRecord> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataRecord("name" + i, "data" + i)).toList();
        table.setItems(items);
        table.setCaption("Items");
        ui.add(table);
        fakeClientCommunication();

        assertBasicTable(table, col);
    }

    @SuppressWarnings("rawtypes")
    private void assertBasicTable(BeanTable table, Column col) {
        // Assert the top level DOM
        Assertions.assertEquals("table", table.getElement().getTag());

        Assertions.assertEquals("Items", table.captionElement.getText());
        Assertions.assertEquals(table.getElement().getAttribute("aria-labelledby"),
                table.captionElement.getAttribute("id"));

        // Assert header composition
        Assertions.assertEquals("thead", table.headerElement.getTag());
        Assertions.assertEquals("rowgroup",
                table.headerElement.getAttribute("role"));
        Assertions.assertEquals("tr", table.headerElement.getChild(0).getTag());
        Assertions.assertEquals("th",
                table.headerElement.getChild(0).getChild(0).getTag());
        Assertions.assertEquals("#",
                table.headerElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals("th",
                table.headerElement.getChild(0).getChild(1).getTag());
        Assertions.assertEquals("columnheader", table.headerElement.getChild(0)
                .getChild(1).getAttribute("role"));
        Assertions.assertEquals("Name",
                table.headerElement.getChild(0).getChild(1).getText());
        Assertions.assertEquals("th",
                table.headerElement.getChild(0).getChild(2).getTag());
        Assertions.assertEquals("columnheader", table.headerElement.getChild(0)
                .getChild(2).getAttribute("role"));
        Assertions.assertEquals("Data",
                table.headerElement.getChild(0).getChild(2).getText());

        Assertions.assertEquals("10",
                table.getElement().getAttribute("aria-rowcount"));

        Assertions.assertEquals(10, table.bodyElement.getChildCount());
        Assertions.assertEquals("rowgroup", table.bodyElement.getAttribute("role"));

        assertBodyStrucure(table, null);

        // Hide column
        col.setVisible(false);
        fakeClientCommunication();

        // Assert that the change is reflected to DOM
        Assertions.assertEquals("none", table.headerElement.getChild(0).getChild(2)
                .getStyle().get("display"));
        assertBodyStrucure(table, "none");
        Assertions.assertFalse(table.menu.getItems().get(1).isChecked());
        Assertions.assertFalse(col.isVisible());

        // Set it back visible
        col.setVisible(true);
        fakeClientCommunication();

        // Assert that the change is reflected to DOM
        Assertions.assertEquals(null, table.headerElement.getChild(0).getChild(2)
                .getStyle().get("display"));
        assertBodyStrucure(table, null);
        Assertions.assertTrue(col.isVisible());

        Assertions.assertEquals("Name", table.menu.getItems().get(0).getText());
        Assertions.assertTrue(table.menu.getItems().get(0).isChecked());
        Assertions.assertEquals("Data", table.menu.getItems().get(1).getText());
        Assertions.assertTrue(table.menu.getItems().get(1).isChecked());
    }

    @SuppressWarnings("rawtypes")
    private void assertBodyStrucure(BeanTable table, String display) {
        // Helper method to assert the body DOM
        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            if (index % 2 == 0) {
                Assertions.assertTrue(row.getClassList().contains("even"));
            }
            Assertions.assertEquals("tr", row.getTag());
            Assertions.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assertions.assertEquals(3, row.getChildCount());
            Assertions.assertEquals("td", row.getChild(0).getTag());
            Assertions.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assertions.assertEquals("th", row.getChild(1).getTag());
            Assertions.assertEquals("rowheader",
                    row.getChild(1).getAttribute("role"));
            Assertions.assertEquals("name" + index, row.getChild(1).getText());
            Assertions.assertEquals(null,
                    row.getChild(1).getStyle().get("display"));
            Assertions.assertEquals("td", row.getChild(2).getTag());
            Assertions.assertEquals("cell", row.getChild(2).getAttribute("role"));
            Assertions.assertEquals("data" + index, row.getChild(2).getText());
            Assertions.assertEquals(display,
                    row.getChild(2).getStyle().get("display"));
        });
    }

    @Test
    public void beanTableWithComponents() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.addComponentColumn("Name", item -> {
            Div div = new Div();
            Span span1 = new Span(item.getName());
            Span span2 = new Span(item.getData());
            div.add(span1, span2);
            return div;
        });
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i)).toList();
        table.setItems(items);
        table.setCaption("Items");

        ui.add(table);
        fakeClientCommunication();

        Assertions.assertEquals("Name",
                table.headerElement.getChild(0).getChild(1).getText());

        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            Assertions.assertEquals("tr", row.getTag());
            Assertions.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assertions.assertEquals(2, row.getChildCount());
            Assertions.assertEquals("td", row.getChild(0).getTag());
            Assertions.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assertions.assertEquals("td", row.getChild(1).getTag());
            Assertions.assertEquals("cell", row.getChild(1).getAttribute("role"));
            Element div = row.getChild(1).getChild(0);
            Assertions.assertEquals("div", div.getTag());
            Assertions.assertEquals(2, div.getChildCount());
            Assertions.assertEquals("span", div.getChild(0).getTag());
            Assertions.assertEquals("name" + index, div.getChild(0).getText());
            Assertions.assertEquals("span", div.getChild(1).getTag());
            Assertions.assertEquals("data" + index, div.getChild(1).getText());
        });
    }

    @Test
    public void beanTableWithHtmlAndTooltips() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.addColumn("Name", DataItem::getName)
                .setTooltipProvider(DataItem::getName);
        table.addColumn("Data", item -> "<b>" + item.getData() + "</b>");
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i)).toList();
        table.setItems(items);
        table.setCaption("Items");

        ui.add(table);
        fakeClientCommunication();

        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            Assertions.assertEquals("tr", row.getTag());
            Assertions.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assertions.assertEquals(3, row.getChildCount());
            Assertions.assertEquals("td", row.getChild(0).getTag());
            Assertions.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assertions.assertEquals("td", row.getChild(1).getTag());
            Assertions.assertEquals("cell", row.getChild(1).getAttribute("role"));
            Assertions.assertEquals("span", row.getChild(1).getChild(0).getTag());
            String id = row.getChild(1).getChild(0).getAttribute("id");
            String html = row.getChild(1).getChild(0).getOuterHTML();
            Assertions.assertTrue(html.startsWith("<span"));
            Assertions.assertTrue(html.contains(">name" + index));
            Assertions.assertTrue(html.contains("<vaadin-tooltip"));
            Assertions.assertTrue(html.contains("for=\"" + id + "\""));
            Assertions.assertTrue(html.contains("text=\"name" + index + "\""));
            Assertions.assertTrue(html.contains("</vaadin-tooltip>"));
            Assertions.assertTrue(html.endsWith("</span>"));
            Assertions.assertEquals("td", row.getChild(2).getTag());
            Assertions.assertEquals("cell", row.getChild(2).getAttribute("role"));
            Assertions.assertEquals("<span><b>data" + index + "</b></span>",
                    row.getChild(2).getChild(0).getOuterHTML());
        });
    }

    @Test
    public void lazyDataView() {
        BeanTable<Person> table = new BeanTable<>(Person.class, true, 20);
        PersonService personService = new PersonService();
        AtomicInteger fetches = new AtomicInteger(0);
        table.setItems(query -> {
            fetches.incrementAndGet();
            return personService.fetch(query.getOffset(), query.getLimit(),
                    Optional.empty()).stream();
        });
        table.getLazyDataView().setItemCountEstimate(100);

        ui.add(table);
        fakeClientCommunication();

        Assertions.assertEquals(1, fetches.get());
        Assertions.assertEquals(100,
                table.getLazyDataView().getItemCountEstimate());

        // Initially shown 5 pages
        Element div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("1/5", div.getChild(2).getText());

        // Autogenerated properties
        Assertions.assertEquals("#",
                table.headerElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals("Address",
                table.headerElement.getChild(0).getChild(1).getText());
        Assertions.assertEquals("Age",
                table.headerElement.getChild(0).getChild(2).getText());
        Assertions.assertEquals("Birth Date",
                table.headerElement.getChild(0).getChild(3).getText());
        Assertions.assertEquals("Email",
                table.headerElement.getChild(0).getChild(4).getText());
        Assertions.assertEquals("First Name",
                table.headerElement.getChild(0).getChild(5).getText());
        Assertions.assertEquals("Id",
                table.headerElement.getChild(0).getChild(6).getText());
        Assertions.assertEquals("Image",
                table.headerElement.getChild(0).getChild(7).getText());
        Assertions.assertEquals("Last Name",
                table.headerElement.getChild(0).getChild(8).getText());
        Assertions.assertEquals("Marital Status",
                table.headerElement.getChild(0).getChild(9).getText());
        Assertions.assertEquals("Phone Number",
                table.headerElement.getChild(0).getChild(10).getText());
        Assertions.assertEquals("Subscriber",
                table.headerElement.getChild(0).getChild(11).getText());

        Assertions.assertEquals("100",
                table.getElement().getAttribute("aria-rowcount"));
        Assertions.assertEquals(100, table.getRowCount());
        Assertions.assertEquals(20, table.bodyElement.getChildCount());

        Assertions.assertEquals("Brayden",
                table.bodyElement.getChild(2).getChild(5).getText());
        Assertions.assertEquals("Wilder",
                table.bodyElement.getChild(2).getChild(8).getText());
        Assertions.assertEquals("59",
                table.bodyElement.getChild(2).getChild(2).getText());

        table.setPage(2);
        Assertions.assertEquals(2, fetches.get());

        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("3/5", div.getChild(2).getText());

        Assertions.assertEquals("41",
                table.bodyElement.getChild(0).getAttribute("aria-rowindex"));
        Assertions.assertEquals("41",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals("Layla",
                table.bodyElement.getChild(0).getChild(5).getText());
        Assertions.assertEquals("Harmon",
                table.bodyElement.getChild(0).getChild(8).getText());
        Assertions.assertEquals("30",
                table.bodyElement.getChild(0).getChild(2).getText());

        // Can go to 6th page despite estimate being too small
        table.setPage(5);
        Assertions.assertEquals(3, fetches.get());

        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("6/5", div.getChild(2).getText());

        Assertions.assertEquals("101",
                table.bodyElement.getChild(0).getAttribute("aria-rowindex"));
        Assertions.assertEquals("101",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals(9, table.bodyElement.getChildCount());

        table.getLazyDataView().setItemIndexProvider(
                (item, query) -> personService.fetchAll().indexOf(item));
        // Can get nth item, on different pages, hence two fetches
        Person item = table.getLazyDataView().getItem(40);
        var page = table.getLazyDataView().getPageIndex(item);
        var index = table.getLazyDataView().getItemIndex(item);
        Assertions.assertEquals(Integer.valueOf(40), index.get());
        Assertions.assertEquals(Integer.valueOf(2), page.get());
        Assertions.assertEquals("Layla", item.getFirstName());
        item = table.getLazyDataView().getItem(2);
        Assertions.assertEquals("Brayden", item.getFirstName());
        Assertions.assertEquals(5, fetches.get());

        // Can get all items, fetched by one query
        Assertions.assertEquals(109, table.getLazyDataView().getItems().count());
        Assertions.assertEquals(6, fetches.get());
    }

    @Test
    public void lazyFilteringAndPaging() {
        BeanTable<Person> table = new BeanTable<>(Person.class, false, 20);
        PersonService personService = new PersonService();
        table.setColumns("firstName", "lastName", "age", "phoneNumber",
                "maritalStatus");
        table.addColumn("Postal Code",
                person -> person.getAddress() == null ? ""
                        : person.getAddress().getPostalCode());
        table.addColumn("City", person -> person.getAddress() == null ? ""
                : person.getAddress().getCity());

        CallbackDataProvider<Person, String> dataProvider = DataProvider
                .fromFilteringCallbacks(
                        query -> personService.fetch(query.getOffset(),
                                query.getLimit(), query.getFilter()).stream(),
                        query -> personService.count(query.getFilter()));

        ConfigurableFilterDataProvider<Person, Void, String> dp = dataProvider
                .withConfigurableFilter();

        // Test localization object
        Assertions.assertEquals(null, table.getI18n());
        BeanTableI18n i18n = BeanTableI18n.getDefault();
        table.setI18n(i18n);
        Assertions.assertEquals(i18n, table.getI18n());

        BeanTableDataView<Person> dataView = table.setItems(dp);

        dataView.addItemCountChangeListener(event -> {
            count = event.getItemCount();
        });

        ui.add(table);
        fakeClientCommunication();

        // Test correctly localized footer text
        Element div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("Page 1 of 6", div.getChild(2).getText());

        // Assert headers
        Assertions.assertEquals("#",
                table.headerElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals("First Name",
                table.headerElement.getChild(0).getChild(1).getText());
        Assertions.assertEquals("Last Name",
                table.headerElement.getChild(0).getChild(2).getText());
        Assertions.assertEquals("Age",
                table.headerElement.getChild(0).getChild(3).getText());
        Assertions.assertEquals("Phone Number",
                table.headerElement.getChild(0).getChild(4).getText());
        Assertions.assertEquals("Marital Status",
                table.headerElement.getChild(0).getChild(5).getText());
        Assertions.assertEquals("Postal Code",
                table.headerElement.getChild(0).getChild(6).getText());
        Assertions.assertEquals("City",
                table.headerElement.getChild(0).getChild(7).getText());

        // Assert column selector menu
        Assertions.assertEquals(7, table.menu.getItems().size());
        Assertions.assertEquals("First Name",
                table.menu.getItems().get(0).getText());
        Assertions.assertEquals("Last Name",
                table.menu.getItems().get(1).getText());
        Assertions.assertEquals("Age", table.menu.getItems().get(2).getText());
        Assertions.assertEquals("Phone Number",
                table.menu.getItems().get(3).getText());
        Assertions.assertEquals("Marital Status",
                table.menu.getItems().get(4).getText());
        Assertions.assertEquals("Postal Code",
                table.menu.getItems().get(5).getText());
        Assertions.assertEquals("City", table.menu.getItems().get(6).getText());

        // Assert row count
        Assertions.assertEquals("109",
                table.getElement().getAttribute("aria-rowcount"));
        Assertions.assertEquals(109, table.getRowCount());
        Assertions.assertEquals(20, table.bodyElement.getChildCount());

        // Test that 3rd item is correct
        Assertions.assertEquals("Brayden",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assertions.assertEquals("Wilder",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assertions.assertEquals("59",
                table.bodyElement.getChild(2).getChild(3).getText());
        Assertions.assertEquals("915-088-178",
                table.bodyElement.getChild(2).getChild(4).getText());

        // Go to 3rd page and assert footer
        table.setPage(2);
        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("Page 3 of 6", div.getChild(2).getText());

        // Test that first item is correct and starting row offset for
        // A11y is
        // correct
        Assertions.assertEquals("41",
                table.bodyElement.getChild(0).getAttribute("aria-rowindex"));
        Assertions.assertEquals("41",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals("Layla",
                table.bodyElement.getChild(0).getChild(1).getText());
        Assertions.assertEquals("Harmon",
                table.bodyElement.getChild(0).getChild(2).getText());
        Assertions.assertEquals("30",
                table.bodyElement.getChild(0).getChild(3).getText());
        Assertions.assertEquals("225-075-734",
                table.bodyElement.getChild(0).getChild(4).getText());
        Assertions.assertEquals(2, table.getPage());

        // Set filter
        var filter = "ben";
        dp.setFilter(filter);
        fakeClientCommunication();

        // Assert that page is now the first one
        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assertions.assertEquals("Page 1 of 1", div.getChild(2).getText());

        // Assert that item count is updated to match filtered items
        Assertions.assertEquals(3, table.bodyElement.getChildCount());
        Assertions.assertEquals(3, count);
        Assertions.assertEquals(0, table.getPage());
        Assertions.assertEquals(3, dataView.getItems().count());

        // Assert that 3rd filtered item is correct
        Assertions.assertEquals("Bentley",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assertions.assertEquals("Pittman",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assertions.assertEquals("86",
                table.bodyElement.getChild(2).getChild(3).getText());
        Assertions.assertEquals("643-754-1623",
                table.bodyElement.getChild(2).getChild(4).getText());

        // Assert that we get the same item as rendered 3rd also from
        // the data
        // view
        Person item = dataView.getItem(2);
        Assertions.assertEquals("Bentley", item.getFirstName());

        // Lazy data view itself does not support filtering by Query,
        // but we are
        // testing that item index provider can be used nevertheless.
        BeanTableLazyDataView<Person> lazyView = table.getLazyDataView();
        lazyView.setItemIndexProvider((i, query) -> personService
                .fetch(0, Integer.MAX_VALUE, Optional.of(filter)).indexOf(i));
        var index = table.getLazyDataView().getItemIndex(item).get();
        Assertions.assertEquals(Integer.valueOf(2), index);

        // Assert localized tooltips of the footer
        Assertions.assertEquals("8", table.footerElement.getChild(0).getChild(0)
                .getAttribute("colspan"));
        Assertions.assertTrue(div.getChild(0).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("First page")));
        Assertions.assertTrue(div.getChild(1).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Previous page")));
        Assertions.assertTrue(div.getChild(3).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Next page")));
        Assertions.assertTrue(div.getChild(4).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Last page")));

        // Assert that "No data" is shown when no match
        dp.setFilter("bentz");
        fakeClientCommunication();
        Assertions.assertEquals("No data",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assertions.assertEquals(0, table.footerElement.getChildCount());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void addThemeVariant_themeNamesContainsThemeVariant() {
        BeanTable table = new BeanTable();
        table.addThemeVariants(BeanTableVariant.PADDING);

        ThemeList themeNames = table.getThemeNames();
        Assertions.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void addThemeVariant_removeThemeVariant_themeNamesDoesNotContainThemeVariant() {
        BeanTable table = new BeanTable();
        table.addThemeVariants(BeanTableVariant.NO_BORDER);
        table.addThemeVariants(BeanTableVariant.PADDING);
        ThemeList themeNames = table.getThemeNames();
        Assertions.assertTrue(themeNames
                .contains(BeanTableVariant.NO_BORDER.getVariantName()));
        themeNames = table.getThemeNames();
        Assertions.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
        table.removeThemeVariants(BeanTableVariant.NO_BORDER);

        themeNames = table.getThemeNames();
        Assertions.assertFalse(themeNames
                .contains(BeanTableVariant.NO_BORDER.getVariantName()));
        Assertions.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
    }

    @Test
    public void tableSerializable() throws IOException, ClassNotFoundException {
        BeanTable<String> table = new BeanTable<>();
        table.addColumn("Hello", item -> "Hello");
        table.setItems("World");
        var bs = new ByteArrayOutputStream();
        var os = new ObjectOutputStream(bs);
        os.writeObject(table);
        os.flush();
        os.close();

        var a = bs.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(a);
        ObjectInputStream in = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        var v = (BeanTable<String>) in.readObject();
        Assertions.assertEquals("World", v.getListDataView().getItem(0));
    }

    @Test
    public void tableI18nSerializable() throws IOException {
        BeanTableI18n tableI18n = BeanTableI18n.getDefault();
        new ObjectOutputStream(new ByteArrayOutputStream())
                .writeObject(tableI18n);
    }

    @Test
    public void toggleSelection() {
        BeanTable<DataItem> table = new BeanTable<>();
        List<DataItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(data -> new DataItem(data, data)).toList();
        table.setItems(items);
        table.setSelectionEnabled(true);
        ui.add(table);
        fakeClientCommunication();

        count = 0;
        table.addSelectionChangedListener(event -> {
            selected = event.getSelected();
            Assertions.assertTrue(event.isFromClient());
            count++;
        });

        // Toggle selection is an internal method to change selection
        // status of
        // a row. This method is called by both programmatic selection
        // and click
        // event hooked to row.
        table.rows.get(0).toggleSelection();

        Assertions.assertEquals(1, count);
        Assertions.assertEquals(1, selected.size());
        Assertions.assertEquals(Set.of(items.get(0)), selected);

        table.rows.get(2).toggleSelection();

        Assertions.assertEquals(2, count);
        Assertions.assertEquals(2, selected.size());
        Assertions.assertEquals(Set.of(items.get(0), items.get(2)), selected);

        table.rows.get(0).toggleSelection();

        Assertions.assertEquals(3, count);
        Assertions.assertEquals(1, selected.size());
        Assertions.assertEquals(Set.of(items.get(2)), selected);
    }

    @Test
    public void refreshItem() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(TestItem::new);
        table.addColumn("Number", TestItem::getData);
        table.setItems(items);
        table.getGenericDataView().setIdentifierProvider(TestItem::getId);

        ui.add(table);
        fakeClientCommunication();

        Element rows = table.bodyElement;
        Assertions.assertEquals(3, rows.getChildCount());
        Assertions.assertEquals("One", rows.getChild(0).getChild(1).getText());
        Assertions.assertEquals("Two", rows.getChild(1).getChild(1).getText());
        Assertions.assertEquals("Three", rows.getChild(2).getChild(1).getText());

        // Change property value from "One" to "Zero" and assert that
        // the change
        // is reflected to DOM after calling refresh item
        TestItem item = table.getGenericDataView().getItem(0);
        item.setData("Zero");
        table.getGenericDataView().refreshItem(item);
        Assertions.assertEquals("Zero", rows.getChild(0).getChild(1).getText());

        ui.remove(table);
        fakeClientCommunication();

        // Data provider listener is un-registered on detach, this
        // refresh item
        // is not applied
        item = table.getGenericDataView().getItem(0);
        item.setData("NonZero");
        table.getGenericDataView().refreshItem(item);
        Assertions.assertEquals("Zero", rows.getChild(0).getChild(1).getText());
    }

    @Test
    public void menuButton() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(TestItem::new);
        table.addColumn("Number", TestItem::getData).setKey("number");
        table.setItems(items);

        Assertions.assertFalse(table.headerElement.getChild(1).isVisible());

        table.setColumnSelectionMenu(ColumnSelectMenu.BUTTON);

        Assertions.assertEquals(2, table.headerElement.getChildCount());
        Assertions.assertTrue(table.headerElement.getChild(1).isVisible());
        Assertions.assertEquals("vaadin-button",
                table.headerElement.getChild(1).getTag());

        table.getColumn("number").get().setVisible(false);

        Assertions.assertEquals(2, table.headerElement.getChildCount());
        Assertions.assertTrue(table.headerElement.getChild(1).isVisible());
        Assertions.assertEquals("vaadin-button",
                table.headerElement.getChild(1).getTag());

        table.setColumnSelectionMenu(ColumnSelectMenu.CONTEXT);

        Assertions.assertFalse(table.headerElement.getChild(1).isVisible());
    }

    @Test
    public void column() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(TestItem::new);
        // Configure column
        table.addColumn("Number", TestItem::getData)
                .setClassNameProvider(TestItem::getData)
                .setAlignment(ColumnAlignment.RIGHT).setHeader("Header")
                .setWidth("100px");
        table.setClassNameProvider(item -> "class");

        BeanTable<TestItem>.Column<TestItem> col = table.getColumns().get(0);
        table.setItems(items);

        ui.add(table);

        // Assert that header is the same as configured
        Text header = (Text) col.getHeader();
        Assertions.assertEquals("Header", header.getText());
        Assertions.assertEquals("Header",
                table.headerElement.getChild(0).getChild(1).getText());
        Assertions.assertEquals("100px", table.headerElement.getChild(0).getChild(1)
                .getStyle().get("width"));

        // Assert that classname generator has been applied on the
        // column
        Element rows = table.bodyElement;
        Assertions.assertEquals(3, rows.getChildCount());
        Assertions.assertTrue(
                rows.getChild(0).getChild(1).getClassList().contains("One"));
        Assertions.assertTrue(
                rows.getChild(1).getChild(1).getClassList().contains("Two"));
        Assertions.assertTrue(
                rows.getChild(2).getChild(1).getClassList().contains("Three"));

        // Assert that classname generator has been applied on the row
        Assertions.assertTrue(rows.getChild(0).getClassList().contains("class"));
        Assertions.assertTrue(rows.getChild(1).getClassList().contains("class"));
        Assertions.assertTrue(rows.getChild(2).getClassList().contains("class"));

        // Assert that column alignment has been set correctly
        Assertions.assertEquals("right",
                rows.getChild(0).getChild(1).getStyle().get("text-align"));
        Assertions.assertEquals("right",
                rows.getChild(1).getChild(1).getStyle().get("text-align"));
        Assertions.assertEquals("right",
                rows.getChild(2).getChild(1).getStyle().get("text-align"));

        // Change the column header to be component and change the class
        // name
        // provider
        col.setClassNameProvider(item -> "class").setHeader(new Span("Header"));

        // Assert that change is reflected to DOM
        Span newHeader = (Span) col.getHeader();
        Assertions.assertEquals("Header", newHeader.getText());

        Assertions.assertEquals("span", table.headerElement.getChild(0).getChild(1)
                .getChild(0).getTag());
        Assertions.assertEquals("Header", table.headerElement.getChild(0)
                .getChild(1).getChild(0).getText());

        table.getDataProvider().refreshAll();

        // Assert that new classname provider has been applied
        Assertions.assertTrue(
                rows.getChild(0).getChild(1).getClassList().contains("class"));
        Assertions.assertTrue(
                rows.getChild(1).getChild(1).getClassList().contains("class"));
        Assertions.assertTrue(
                rows.getChild(2).getChild(1).getClassList().contains("class"));
    }

    @Test
    public void dataView() {
        AtomicInteger refreshed = new AtomicInteger(0);
        BeanTable<String> table = new BeanTable<>();
        BeanTableListDataView<String> dataView = table.setItems("One", "Two",
                "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten");
        table.getDataProvider().addDataProviderListener(e -> {
            refreshed.incrementAndGet();
        });

        Assertions.assertEquals(10, dataView.getItemCount());
        Assertions.assertEquals("One", dataView.getItem(0));
        Assertions.assertEquals("Two", dataView.getItem(1));
        Assertions.assertEquals("Three", dataView.getItem(2));

        Assertions.assertEquals("One", table.getGenericDataView().getItem(0));
        Assertions.assertEquals("Two", table.getGenericDataView().getItem(1));
        Assertions.assertEquals("Three", table.getGenericDataView().getItem(2));

        dataView.setFilter(item -> item.startsWith("T"));
        Assertions.assertEquals(1, refreshed.get());

        Assertions.assertEquals(3, dataView.getItemCount());
        Assertions.assertEquals("Two", dataView.getItem(0));
        Assertions.assertEquals("Three", dataView.getItem(1));
        Assertions.assertEquals("Ten", dataView.getItem(2));

        Assertions.assertEquals(3, table.getGenericDataView().getItems().count());
        Assertions.assertEquals("Two", table.getGenericDataView().getItem(0));
        Assertions.assertEquals("Three", table.getGenericDataView().getItem(1));
        Assertions.assertEquals("Ten", table.getGenericDataView().getItem(2));

        dataView.setFilter(null);
        Assertions.assertEquals(10, dataView.getItemCount());
        Assertions.assertEquals("One", dataView.getItem(0));
        Assertions.assertEquals("Two", dataView.getItem(1));
        Assertions.assertEquals("Three", dataView.getItem(2));
    }

    @Test
    public void selection() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.addColumn("Name", DataItem::getName)
                .setTooltipProvider(DataItem::getName);
        table.addColumn("Data", item -> "<b>" + item.getData() + "</b>");
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i)).toList();
        table.setItems(items);
        table.setSelectionEnabled(true);

        selected = null;
        count = 0;
        table.addSelectionChangedListener(event -> {
            selected = event.getSelected();
            Assertions.assertFalse(event.isFromClient());
            count++;
        });

        ui.add(table);
        fakeClientCommunication();

        assertSelectedThemeNotSet(table, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        // Select four items and assert that event was fired once,
        // assert
        // selection
        table.select(items.get(3), items.get(5), items.get(7), items.get(9));
        Assertions.assertEquals(1, count);
        Assertions.assertEquals(
                Set.of(items.get(3), items.get(5), items.get(7), items.get(9)),
                selected);
        Assertions.assertEquals(
                Set.of(items.get(3), items.get(5), items.get(7), items.get(9)),
                table.getSelected());
        assertSelectedThemeSet(table, 3, 5, 7, 9);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 8);

        // Select item already in selection, assert no event fired
        table.select(items.get(5));
        Assertions.assertEquals(1, count);
        assertSelectedThemeSet(table, 3, 5, 7, 9);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 8);

        // De-select two item, assert that event is fired once
        table.deselect(items.get(7), items.get(9));
        Assertions.assertEquals(2, count);
        Assertions.assertEquals(Set.of(items.get(3), items.get(5)), selected);
        assertSelectedThemeSet(table, 3, 5);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 7, 8, 9);

        // De-select item not in selection, assert no event fired
        table.deselect(items.get(7));
        Assertions.assertEquals(2, count);
        assertSelectedThemeSet(table, 3, 5);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 7, 8, 9);

        // De-select all, assert that selection is empty
        table.deselectAll();
        Assertions.assertEquals(3, count);
        Assertions.assertTrue(selected.isEmpty());
        assertSelectedThemeNotSet(table, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void emptyTable() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", DataItem::getName);
        table.addColumn("Data", DataItem::getData);

        ui.add(table);
        fakeClientCommunication();

        Element alertCell = table.bodyElement.getChild(0).getChild(0);
        Assertions.assertEquals("No data", alertCell.getText());
        Assertions.assertEquals("alert", alertCell.getAttribute("role"));
        Assertions.assertEquals("assertive", alertCell.getAttribute("aria-live"));
        Assertions.assertEquals("3", alertCell.getAttribute("colspan"));
    }

    @Test
    public void errorTable() {
        // Note, an error and stacktrace will be logged when the test is
        // run,
        // that is intentional
        FaultyDataService service = new FaultyDataService();
        BeanTable<DataItem> table = new BeanTable<>(20);
        table.addColumn("Name", DataItem::getName);
        table.addColumn("Data", DataItem::getData);
        table.setItems(query -> service.fetchPersons());
        table.getLazyDataView().setItemCountEstimate(100);

        ui.add(table);

        Element alertCell = table.bodyElement.getChild(0).getChild(0);
        Assertions.assertEquals("Failed fetching data", alertCell.getText());
        Assertions.assertEquals("alert", alertCell.getAttribute("role"));
        Assertions.assertEquals("assertive", alertCell.getAttribute("aria-live"));
        Assertions.assertEquals("3", alertCell.getAttribute("colspan"));
    }

    private void assertSelectedThemeSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Element row = table.bodyElement.getChild(item);
            Assertions.assertEquals("selected", row.getAttribute("theme"),
                    "item at index " + item + " should be selected");
            for (int i = 1; i < row.getChildCount(); i++) {
                Assertions.assertEquals("true",
                        row.getChild(i).getAttribute("aria-selected"));
            }
        }
    }

    private void assertSelectedThemeNotSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Element row = table.bodyElement.getChild(item);
            Assertions.assertEquals(null, row.getAttribute("theme"),
                    "item at index " + item + " should not be selected");
            for (int i = 1; i < row.getChildCount(); i++) {
                Assertions.assertEquals("false",
                        row.getChild(i).getAttribute("aria-selected"));
            }
        }
    }

    public class TestItem {
        private UUID id = UUID.randomUUID();
        private String data;

        public TestItem(String data) {
            this.setData(data);
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

    public class FaultyDataService {
        FaultyDataRepository repository = new FaultyDataRepository();

        public Stream<DataItem> fetchPersons() {
            return repository.fetch().stream();
        }
    }

    public class FaultyDataRepository {
        public List<DataItem> fetch() {
            return null;
        }
    }

    record DataRecord(String name, String data) {
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    @SuppressWarnings("serial")
    public static class MockUI extends UI {

        public MockUI() {
            this(findOrcreateSession());
        }

        public MockUI(VaadinSession session) {
            getInternals().setSession(session);
            setCurrent(this);
        }

        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }

        private static VaadinSession findOrcreateSession() {
            VaadinSession session = VaadinSession.getCurrent();
            if (session == null) {
                session = new AlwaysLockedVaadinSession(null);
                VaadinSession.setCurrent(session);
            }
            return session;
        }
    }

    @SuppressWarnings("serial")
    public static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }
    }

    @SuppressWarnings("serial")
    public static class MockVaadinSession extends VaadinSession {
        /*
         * Used to make sure there's at least one reference to the mock session
         * while it's locked. This is used to prevent the session from being
         * eaten by GC in tests where @BeforeEach creates a session and sets it as
         * the current instance without keeping any direct reference to it. This
         * pattern has a chance of leaking memory if the session is not unlocked
         * in the right way, but it should be acceptable for testing use.
         */
        private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public void close() {
            super.close();
            closeCount++;
        }

        public int getCloseCount() {
            return closeCount;
        }

        @Override
        public Lock getLockInstance() {
            return lock;
        }

        @Override
        public void lock() {
            super.lock();
            referenceKeeper.set(this);
        }

        @Override
        public void unlock() {
            super.unlock();
            referenceKeeper.remove();
        }

        private int closeCount;

        private ReentrantLock lock = new ReentrantLock();

        public DeploymentConfiguration getConfiguration() {
            return null;
        }
    }
}
package org.vaadin.tatu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class BeanTableTest {

    private int count;
    private MockUI ui;
    private Set<DataItem> selected = null;

    @Before
    public void init() {
        ui = new MockUI();
    }

    @Test
    public void basicBeanTable() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", item -> item.getName()).setRowHeader(true);
        BeanTable<DataItem>.Column<DataItem> col = table.addColumn("Data",
                item -> item.getData());
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setCaption("Items");
        ui.add(table);
        fakeClientCommunication();

        assertBasicTable(table, col);
    }

    private void assertBasicTable(BeanTable table, Column col) {
        // Assert the top level DOM
        Assert.assertEquals("table", table.getElement().getTag());

        Assert.assertEquals("Items", table.captionElement.getText());
        Assert.assertEquals(table.getElement().getAttribute("aria-labelledby"),
                table.captionElement.getAttribute("id"));

        // Assert header composition
        Assert.assertEquals("thead", table.headerElement.getTag());
        Assert.assertEquals("rowgroup",
                table.headerElement.getAttribute("role"));
        Assert.assertEquals("tr", table.headerElement.getChild(0).getTag());
        Assert.assertEquals("th",
                table.headerElement.getChild(0).getChild(0).getTag());
        Assert.assertEquals("#",
                table.headerElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("th",
                table.headerElement.getChild(0).getChild(1).getTag());
        Assert.assertEquals("columnheader", table.headerElement.getChild(0)
                .getChild(1).getAttribute("role"));
        Assert.assertEquals("Name",
                table.headerElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("th",
                table.headerElement.getChild(0).getChild(2).getTag());
        Assert.assertEquals("columnheader", table.headerElement.getChild(0)
                .getChild(2).getAttribute("role"));
        Assert.assertEquals("Data",
                table.headerElement.getChild(0).getChild(2).getText());

        Assert.assertEquals("10",
                table.getElement().getAttribute("aria-rowcount"));

        Assert.assertEquals(10, table.bodyElement.getChildCount());
        Assert.assertEquals("rowgroup", table.bodyElement.getAttribute("role"));

        assertBodyStrucure(table, null);

        // Hide column
        col.setVisible(false);
        fakeClientCommunication();

        // Assert that the change is reflected to DOM
        Assert.assertEquals("none", table.headerElement.getChild(0).getChild(2)
                .getStyle().get("display"));
        assertBodyStrucure(table, "none");
        Assert.assertFalse(table.menu.getItems().get(1).isChecked());
        Assert.assertFalse(col.isVisible());

        // Set it back visible
        col.setVisible(true);
        fakeClientCommunication();

        // Assert that the change is reflected to DOM
        Assert.assertEquals(null, table.headerElement.getChild(0).getChild(2)
                .getStyle().get("display"));
        assertBodyStrucure(table, null);
        Assert.assertTrue(col.isVisible());

        Assert.assertEquals("Name", table.menu.getItems().get(0).getText());
        Assert.assertTrue(table.menu.getItems().get(0).isChecked());
        Assert.assertEquals("Data", table.menu.getItems().get(1).getText());
        Assert.assertTrue(table.menu.getItems().get(1).isChecked());
    }

    private void assertBodyStrucure(BeanTable table, String display) {
        // Helper method to assert the body DOM
        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            if (index % 2 == 0) {
                Assert.assertTrue(row.getClassList().contains("even"));
            }
            Assert.assertEquals("tr", row.getTag());
            Assert.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assert.assertEquals(3, row.getChildCount());
            Assert.assertEquals("td", row.getChild(0).getTag());
            Assert.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assert.assertEquals("th", row.getChild(1).getTag());
            Assert.assertEquals("rowheader",
                    row.getChild(1).getAttribute("role"));
            Assert.assertEquals("name" + index, row.getChild(1).getText());
            Assert.assertEquals(null,
                    row.getChild(1).getStyle().get("display"));
            Assert.assertEquals("td", row.getChild(2).getTag());
            Assert.assertEquals("cell", row.getChild(2).getAttribute("role"));
            Assert.assertEquals("data" + index, row.getChild(2).getText());
            Assert.assertEquals(display,
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
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setCaption("Items");

        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            Assert.assertEquals("tr", row.getTag());
            Assert.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assert.assertEquals(2, row.getChildCount());
            Assert.assertEquals("td", row.getChild(0).getTag());
            Assert.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assert.assertEquals("td", row.getChild(1).getTag());
            Assert.assertEquals("cell", row.getChild(1).getAttribute("role"));
            Element div = row.getChild(1).getChild(0);
            Assert.assertEquals("div", div.getTag());
            Assert.assertEquals(2, div.getChildCount());
            Assert.assertEquals("span", div.getChild(0).getTag());
            Assert.assertEquals("name" + index, div.getChild(0).getText());
            Assert.assertEquals("span", div.getChild(1).getTag());
            Assert.assertEquals("data" + index, div.getChild(1).getText());
        });
    }

    @Test
    public void beanTableWithHtmlAndTooltips() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.addColumn("Name", item -> item.getName())
                .setTooltipProvider(item -> item.getName());
        table.addColumn("Data", item -> "<b>" + item.getData() + "</b>");
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setCaption("Items");

        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            Assert.assertEquals("tr", row.getTag());
            Assert.assertEquals("" + (index + 1),
                    row.getAttribute("aria-rowindex"));
            Assert.assertEquals(3, row.getChildCount());
            Assert.assertEquals("td", row.getChild(0).getTag());
            Assert.assertEquals("" + (index + 1), row.getChild(0).getText());
            Assert.assertEquals("td", row.getChild(1).getTag());
            Assert.assertEquals("cell", row.getChild(1).getAttribute("role"));
            Assert.assertEquals("<span>name" + index + "</span>",
                    row.getChild(1).getChild(0).getOuterHTML());
            Assert.assertEquals("name" + index,
                    row.getChild(1).getAttribute("title"));
            Assert.assertEquals("name" + index,
                    row.getChild(1).getAttribute("aria-label"));
            Assert.assertEquals("td", row.getChild(2).getTag());
            Assert.assertEquals("cell", row.getChild(2).getAttribute("role"));
            Assert.assertEquals("<span><b>data" + index + "</b></span>",
                    row.getChild(2).getChild(0).getOuterHTML());
        });
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

        Assert.assertEquals(null, table.getI18n());
        BeanTableI18n i18n = BeanTableI18n.getDefault();
        table.setI18n(i18n);
        Assert.assertEquals(i18n, table.getI18n());

        table.setDataProvider(dp);

        Element div = table.footerElement.getChild(0).getChild(0).getChild(0);

        ui.add(table);

        Assert.assertEquals("Page 1 of 6", div.getChild(2).getText());

        Assert.assertEquals("#",
                table.headerElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("First Name",
                table.headerElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("Last Name",
                table.headerElement.getChild(0).getChild(2).getText());
        Assert.assertEquals("Age",
                table.headerElement.getChild(0).getChild(3).getText());
        Assert.assertEquals("Phone Number",
                table.headerElement.getChild(0).getChild(4).getText());
        Assert.assertEquals("Marital Status",
                table.headerElement.getChild(0).getChild(5).getText());
        Assert.assertEquals("Postal Code",
                table.headerElement.getChild(0).getChild(6).getText());
        Assert.assertEquals("City",
                table.headerElement.getChild(0).getChild(7).getText());

        Assert.assertEquals("First Name",
                ((Text) table.getColumn("firstName").get().getHeader())
                        .getText());

        Assert.assertEquals(7, table.menu.getItems().size());
        Assert.assertEquals("First Name",
                table.menu.getItems().get(0).getText());
        Assert.assertEquals("Last Name",
                table.menu.getItems().get(1).getText());
        Assert.assertEquals("Age", table.menu.getItems().get(2).getText());
        Assert.assertEquals("Phone Number",
                table.menu.getItems().get(3).getText());
        Assert.assertEquals("Marital Status",
                table.menu.getItems().get(4).getText());
        Assert.assertEquals("Postal Code",
                table.menu.getItems().get(5).getText());
        Assert.assertEquals("City", table.menu.getItems().get(6).getText());

        Assert.assertEquals("109",
                table.getElement().getAttribute("aria-rowcount"));
        Assert.assertEquals(20, table.bodyElement.getChildCount());

        Assert.assertEquals("Brayden",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assert.assertEquals("Wilder",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assert.assertEquals("59",
                table.bodyElement.getChild(2).getChild(3).getText());
        Assert.assertEquals("915-088-178",
                table.bodyElement.getChild(2).getChild(4).getText());

        table.setPage(2);

        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assert.assertEquals("Page 3 of 6", div.getChild(2).getText());

        Assert.assertEquals("41",
                table.bodyElement.getChild(0).getAttribute("aria-rowindex"));
        Assert.assertEquals("41",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("Layla",
                table.bodyElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("Harmon",
                table.bodyElement.getChild(0).getChild(2).getText());
        Assert.assertEquals("30",
                table.bodyElement.getChild(0).getChild(3).getText());
        Assert.assertEquals("225-075-734",
                table.bodyElement.getChild(0).getChild(4).getText());

        dp.setFilter("ben");
        fakeClientCommunication();

        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assert.assertEquals("Page 1 of 1", div.getChild(2).getText());

        Assert.assertEquals(3, table.bodyElement.getChildCount());

        Assert.assertEquals(0, table.getPage());

        Assert.assertEquals("Bentley",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assert.assertEquals("Pittman",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assert.assertEquals("86",
                table.bodyElement.getChild(2).getChild(3).getText());
        Assert.assertEquals("643-754-1623",
                table.bodyElement.getChild(2).getChild(4).getText());

        Assert.assertEquals("8", table.footerElement.getChild(0).getChild(0)
                .getAttribute("colspan"));

        Assert.assertEquals("First page",
                div.getChild(0).getAttribute("title"));
        Assert.assertEquals("Previous page",
                div.getChild(1).getAttribute("title"));
        Assert.assertEquals("Next page", div.getChild(3).getAttribute("title"));
        Assert.assertEquals("Last page", div.getChild(4).getAttribute("title"));

        // Assert that "No data" is shown when no match
        dp.setFilter("bentz");
        fakeClientCommunication();
        Assert.assertEquals("No data",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assert.assertEquals(0, table.footerElement.getChildCount());
    }

    @Test
    public void addThemeVariant_themeNamesContainsThemeVariant() {
        BeanTable table = new BeanTable();
        table.addThemeVariants(BeanTableVariant.PADDING);

        ThemeList themeNames = table.getThemeNames();
        Assert.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
    }

    @Test
    public void addThemeVariant_removeThemeVariant_themeNamesDoesNotContainThemeVariant() {
        BeanTable table = new BeanTable();
        table.addThemeVariants(BeanTableVariant.NO_BORDER);
        table.addThemeVariants(BeanTableVariant.PADDING);
        ThemeList themeNames = table.getThemeNames();
        Assert.assertTrue(themeNames
                .contains(BeanTableVariant.NO_BORDER.getVariantName()));
        themeNames = table.getThemeNames();
        Assert.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
        table.removeThemeVariants(BeanTableVariant.NO_BORDER);

        themeNames = table.getThemeNames();
        Assert.assertFalse(themeNames
                .contains(BeanTableVariant.NO_BORDER.getVariantName()));
        Assert.assertTrue(
                themeNames.contains(BeanTableVariant.PADDING.getVariantName()));
    }

    @Test
    public void tableSerializable() throws IOException {
        BeanTable<String> table = new BeanTable<>();
        table.addColumn("Hello", item -> "Hello");
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(table);
    }

    @Test
    public void tableI18nSerializable() throws IOException {
        BeanTableI18n tableI18n = BeanTableI18n.getDefault();
        new ObjectOutputStream(new ByteArrayOutputStream())
                .writeObject(tableI18n);
    }

    @Test
    public void menuButton() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(data -> new TestItem(data));
        table.addColumn("Number", TestItem::getData).setKey("number");
        table.setItems(items);

        Assert.assertFalse(table.headerElement.getChild(1).isVisible());

        table.setColumnSelectionMenu(ColumnSelectMenu.BUTTON);

        Assert.assertEquals(2, table.headerElement.getChildCount());
        Assert.assertTrue(table.headerElement.getChild(1).isVisible());
        Assert.assertEquals("vaadin-button",
                table.headerElement.getChild(1).getTag());

        table.getColumn("number").get().setVisible(false);

        Assert.assertEquals(2, table.headerElement.getChildCount());
        Assert.assertTrue(table.headerElement.getChild(1).isVisible());
        Assert.assertEquals("vaadin-button",
                table.headerElement.getChild(1).getTag());

        table.setColumnSelectionMenu(ColumnSelectMenu.CONTEXT);

        Assert.assertFalse(table.headerElement.getChild(1).isVisible());
    }

    @Test
    public void column() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(data -> new TestItem(data));
        table.addColumn("Number", TestItem::getData)
                .setClassNameProvider(item -> item.getData())
                .setAlignment(ColumnAlignment.RIGHT).setHeader("Header")
                .setWidth("100px");
        table.setClassNameProvider(item -> "class");

        BeanTable<TestItem>.Column<TestItem> col = table.getColumns().get(0);
        table.setItems(items);

        ui.add(table);

        Text header = (Text) col.getHeader();
        Assert.assertEquals("Header", header.getText());
        Assert.assertEquals("Header",
                table.headerElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("100px", table.headerElement.getChild(0).getChild(1)
                .getStyle().get("width"));

        Element rows = table.bodyElement;
        Assert.assertEquals(3, rows.getChildCount());
        Assert.assertTrue(
                rows.getChild(0).getChild(1).getClassList().contains("One"));
        Assert.assertTrue(
                rows.getChild(1).getChild(1).getClassList().contains("Two"));
        Assert.assertTrue(
                rows.getChild(2).getChild(1).getClassList().contains("Three"));

        Assert.assertTrue(rows.getChild(0).getClassList().contains("class"));
        Assert.assertTrue(rows.getChild(1).getClassList().contains("class"));
        Assert.assertTrue(rows.getChild(2).getClassList().contains("class"));

        Assert.assertEquals("right",
                rows.getChild(0).getChild(1).getStyle().get("text-align"));
        Assert.assertEquals("right",
                rows.getChild(1).getChild(1).getStyle().get("text-align"));
        Assert.assertEquals("right",
                rows.getChild(2).getChild(1).getStyle().get("text-align"));

        col.setClassNameProvider(item -> "class").setHeader(new Span("Header"));

        Span newHeader = (Span) col.getHeader();
        Assert.assertEquals("Header", newHeader.getText());

        Assert.assertEquals("span", table.headerElement.getChild(0).getChild(1)
                .getChild(0).getTag());
        Assert.assertEquals("Header", table.headerElement.getChild(0)
                .getChild(1).getChild(0).getText());

        table.getDataProvider().refreshAll();

        Assert.assertTrue(
                rows.getChild(0).getChild(1).getClassList().contains("class"));
        Assert.assertTrue(
                rows.getChild(1).getChild(1).getClassList().contains("class"));
        Assert.assertTrue(
                rows.getChild(2).getChild(1).getClassList().contains("class"));
    }

    @Test
    public void selection() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.setHtmlAllowed(true);
        table.addColumn("Name", item -> item.getName())
                .setTooltipProvider(item -> item.getName());
        table.addColumn("Data", item -> "<b>" + item.getData() + "</b>");
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setSelectionEnabled(true);

        selected = null;
        count = 0;
        table.addSelectionChangedListener(event -> {
            selected = event.getSelected();
            Assert.assertFalse(event.isFromClient());
            count++;
        });
        assertSelectedThemeNotSet(table, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        // Select four items and assert that event was fired once, assert
        // selection
        table.select(items.get(3), items.get(5), items.get(7), items.get(9));
        Assert.assertEquals(1, count);
        Assert.assertEquals(
                Set.of(items.get(3), items.get(5), items.get(7), items.get(9)),
                selected);
        assertSelectedThemeSet(table, 3, 5, 7, 9);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 8);

        // Select item already in selection, assert no event fired
        table.select(items.get(5));
        Assert.assertEquals(1, count);
        assertSelectedThemeSet(table, 3, 5, 7, 9);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 8);

        // De-select two item, assert that event is fired once
        table.deselect(items.get(7), items.get(9));
        Assert.assertEquals(2, count);
        Assert.assertEquals(Set.of(items.get(3), items.get(5)), selected);
        assertSelectedThemeSet(table, 3, 5);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 7, 8, 9);

        // De-select item not in selection, assert no event fired
        table.deselect(items.get(7));
        Assert.assertEquals(2, count);
        assertSelectedThemeSet(table, 3, 5);
        assertSelectedThemeNotSet(table, 0, 1, 2, 4, 6, 7, 8, 9);

        // De-select all, assert that selection is empty
        table.deselectAll();
        Assert.assertEquals(3, count);
        Assert.assertTrue(selected.isEmpty());
        assertSelectedThemeNotSet(table, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void emptyTable() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", item -> item.getName());
        table.addColumn("Data", item -> item.getData());

        ui.add(table);
        fakeClientCommunication();

        Element alertCell = table.bodyElement.getChild(0).getChild(0);
        Assert.assertEquals("No data", alertCell.getText());
        Assert.assertEquals("alert", alertCell.getAttribute("role"));
        Assert.assertEquals("assertive", alertCell.getAttribute("aria-live"));
        Assert.assertEquals("3", alertCell.getAttribute("colspan"));
    }

    @Test
    public void errorTable() {
        // Note, an error and stacktrace will be logged when the test is run,
        // that is intentional
        FaultyDataService service = new FaultyDataService();
        BeanTable<DataItem> table = new BeanTable<>(20);
        table.addColumn("Name", item -> item.getName());
        table.addColumn("Data", item -> item.getData());

        CallbackDataProvider<DataItem, Void> dataProvider = DataProvider
                .fromCallbacks(query -> service.fetchData(),
                        query -> service.count());

        table.setDataProvider(dataProvider);

        ui.add(table);

        Element alertCell = table.bodyElement.getChild(0).getChild(0);
        Assert.assertEquals("Failed fetching data", alertCell.getText());
        Assert.assertEquals("alert", alertCell.getAttribute("role"));
        Assert.assertEquals("assertive", alertCell.getAttribute("aria-live"));
        Assert.assertEquals("3", alertCell.getAttribute("colspan"));
    }

    private void assertSelectedThemeSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Element row = table.bodyElement.getChild(item);
            Assert.assertEquals("item at index " + item + " should be selected",
                    "selected", row.getAttribute("theme"));
            for (int i = 1; i < row.getChildCount(); i++) {
                Assert.assertEquals("true",
                        row.getChild(i).getAttribute("aria-selected"));
            }
        }
    }

    private void assertSelectedThemeNotSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Element row = table.bodyElement.getChild(item);
            Assert.assertEquals(
                    "item at index " + item + " should not be selected", null,
                    row.getAttribute("theme"));
            for (int i = 1; i < row.getChildCount(); i++) {
                Assert.assertEquals("false",
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

        public Stream<DataItem> fetchData() {
            return repository.fetch().stream();
        }

        public int count() {
            return 100;
        }
    }

    public class FaultyDataRepository {
        public List<DataItem> fetch() {
            return null;
        }
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

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

    public static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }
    }

    public static class MockVaadinSession extends VaadinSession {
        /*
         * Used to make sure there's at least one reference to the mock session
         * while it's locked. This is used to prevent the session from being
         * eaten by GC in tests where @Before creates a session and sets it as
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
    }
}
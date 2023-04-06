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
        UI.setCurrent(ui);
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
        
        assertBasicTable(table, col);
    }

    @Test
    public void recordBeanTable() {
        BeanTable<DataRecord> table = new BeanTable<>();
        table.addColumn("Name", item -> item.name).setRowHeader(true);
        BeanTable<DataRecord>.Column<DataRecord> col = table.addColumn("Data",
                item -> item.data);
        List<DataRecord> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataRecord("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setCaption("Items");
        ui.add(table);
        
        assertBasicTable(table, col);
    }

    private void assertBasicTable(BeanTable table, Column col) {
        Assert.assertEquals("table", table.getElement().getTag());

        Assert.assertEquals("Items", table.captionElement.getText());
        Assert.assertEquals(table.getElement().getAttribute("aria-labelledby"),
                table.captionElement.getAttribute("id"));

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

        col.setVisible(false);
        this.fakeClientCommunication();

        Assert.assertEquals("none", table.headerElement.getChild(0).getChild(2)
                .getStyle().get("display"));
        assertBodyStrucure(table, "none");
        Assert.assertFalse(table.menu.getItems().get(1).isChecked());
        Assert.assertFalse(col.isVisible());

        col.setVisible(true);
        this.fakeClientCommunication();

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
        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
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
            Assert.assertEquals("span", row.getChild(1).getChild(0).getTag());
            String id = row.getChild(1).getChild(0).getAttribute("id");
            String html = row.getChild(1).getChild(0).getOuterHTML();
            Assert.assertTrue(html.startsWith("<span"));
            Assert.assertTrue(html.contains(">name" + index));
            Assert.assertTrue(html.contains("<vaadin-tooltip"));
            Assert.assertTrue(html.contains("for=\"" + id + "\""));
            Assert.assertTrue(html.contains("text=\"name" + index + "\""));
            Assert.assertTrue(html.contains("</vaadin-tooltip>"));
            Assert.assertTrue(html.endsWith("</span>"));
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

        table.setI18n(BeanTableI18n.getDefault());
        BeanTableDataView<Person> dataView = table.setItems(dp);

        dataView.addItemCountChangeListener(event -> {
            count = event.getItemCount();
        });
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

        Assert.assertEquals(2, table.getPage());

        dp.setFilter("ben");
        fakeClientCommunication();

        div = table.footerElement.getChild(0).getChild(0).getChild(0);
        Assert.assertEquals("Page 1 of 1", div.getChild(2).getText());

        Assert.assertEquals(3, table.bodyElement.getChildCount());
        Assert.assertEquals(3, count);
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
        Assert.assertTrue(div.getChild(0).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("First page")));
        Assert.assertTrue(div.getChild(1).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Previous page")));
        Assert.assertTrue(div.getChild(3).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Next page")));
        Assert.assertTrue(div.getChild(4).getChildren()
                .anyMatch(child -> child.getTag().equals("vaadin-tooltip")
                        && child.getProperty("text").equals("Last page")));
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
    public void toggleSelection() throws IOException {
        BeanTable<DataItem> table = new BeanTable<>();
        List<DataItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(data -> new DataItem(data, data))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setSelectionEnabled(true);
        ui.add(table);

        count = 0;
        table.addSelectionChangedListener(event -> {
            selected = event.getSelected();
            Assert.assertTrue(event.isFromClient());
            count++;
        });

        table.rows.get(0).toggleSelection();

        Assert.assertEquals(1, count);
        Assert.assertEquals(1, selected.size());
        Assert.assertEquals(Set.of(items.get(0)), selected);

        table.rows.get(2).toggleSelection();

        Assert.assertEquals(2, count);
        Assert.assertEquals(2, selected.size());
        Assert.assertEquals(Set.of(items.get(0), items.get(2)), selected);

        table.rows.get(0).toggleSelection();

        Assert.assertEquals(3, count);
        Assert.assertEquals(1, selected.size());
        Assert.assertEquals(Set.of(items.get(2)), selected);
    }

    @Test
    public void refreshItem() {
        BeanTable<TestItem> table = new BeanTable<>();
        Stream<TestItem> items = Arrays.asList("One", "Two", "Three").stream()
                .map(data -> new TestItem(data));
        table.addColumn("Number", TestItem::getData);
        table.setItems(items);
        table.getGenericDataView().setIdentifierProvider(TestItem::getId);

        Element rows = table.bodyElement;
        Assert.assertEquals(3, rows.getChildCount());
        Assert.assertEquals("One", rows.getChild(0).getChild(1).getText());
        Assert.assertEquals("Two", rows.getChild(1).getChild(1).getText());
        Assert.assertEquals("Three", rows.getChild(2).getChild(1).getText());

        TestItem item = table.getGenericDataView().getItem(0);
        item.setData("Zero");
        table.getGenericDataView().refreshItem(item);
        Assert.assertEquals("Zero", rows.getChild(0).getChild(1).getText());
    }

    @Test
    public void dataView() {
        BeanTable<String> table = new BeanTable<>();
        BeanTableListDataView<String> dataView = table.setItems("One", "Two",
                "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten");

        Assert.assertEquals(10, dataView.getItemCount());
        Assert.assertEquals("One", dataView.getItem(0));
        Assert.assertEquals("Two", dataView.getItem(1));
        Assert.assertEquals("Three", dataView.getItem(2));

        Assert.assertEquals("One", table.getGenericDataView().getItem(0));
        Assert.assertEquals("Two", table.getGenericDataView().getItem(1));
        Assert.assertEquals("Three", table.getGenericDataView().getItem(2));

        dataView.setFilter(item -> item.startsWith("T"));
        Assert.assertEquals(3, dataView.getItemCount());
        Assert.assertEquals("Two", dataView.getItem(0));
        Assert.assertEquals("Three", dataView.getItem(1));
        Assert.assertEquals("Ten", dataView.getItem(2));

        Assert.assertEquals(3, table.getGenericDataView().getItems().count());
        Assert.assertEquals("Two", table.getGenericDataView().getItem(0));
        Assert.assertEquals("Three", table.getGenericDataView().getItem(1));
        Assert.assertEquals("Ten", table.getGenericDataView().getItem(2));

        dataView.setFilter(null);
        Assert.assertEquals(10, dataView.getItemCount());
        Assert.assertEquals("One", dataView.getItem(0));
        Assert.assertEquals("Two", dataView.getItem(1));
        Assert.assertEquals("Three", dataView.getItem(2));
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

    private void assertSelectedThemeSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Assert.assertEquals("item at index " + item + " should be selected",
                    "selected",
                    table.bodyElement.getChild(item).getAttribute("theme"));
        }
    }

    private void assertSelectedThemeNotSet(BeanTable<DataItem> table,
            int... items) {
        for (int item : items) {
            Assert.assertEquals(
                    "item at index " + item + " should not be selected", null,
                    table.bodyElement.getChild(item).getAttribute("theme"));
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

    record DataRecord(String name, String data) {
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
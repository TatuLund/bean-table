package org.vaadin.tatu;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class BeanTableTest {

    private int count;
    private MockUI ui;

    @Before
    public void init() {
        ui = new MockUI();
    }

    @Test
    public void basicBeanTable() {
        BeanTable<DataItem> table = new BeanTable<>();
        table.addColumn("Name", item -> item.getName()).setRowHeader(true);
        table.addColumn("Data", item -> item.getData());
        List<DataItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new DataItem("name" + i, "data" + i))
                .collect(Collectors.toList());
        table.setItems(items);
        table.setCaption("Items");

        Assert.assertEquals("table", table.getElement().getTag());

        Assert.assertEquals("Items", table.captionElement.getText());
        Assert.assertEquals(table.getElement().getAttribute("aria-labelledby"),
                table.captionElement.getAttribute("id"));

        Assert.assertEquals("thead", table.headerElement.getTag());
        Assert.assertEquals("rowgroup", table.headerElement.getAttribute("role"));
        Assert.assertEquals("tr", table.headerElement.getChild(0).getTag());
        Assert.assertEquals("th",
                table.headerElement.getChild(0).getChild(0).getTag());
        Assert.assertEquals("columnheader",
                table.headerElement.getChild(0).getChild(0).getAttribute("role"));
        Assert.assertEquals("Name",
                table.headerElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("th",
                table.headerElement.getChild(0).getChild(1).getTag());
        Assert.assertEquals("columnheader",
                table.headerElement.getChild(0).getChild(1).getAttribute("role"));
        Assert.assertEquals("Data",
                table.headerElement.getChild(0).getChild(1).getText());

        Assert.assertEquals("10",
                table.getElement().getAttribute("aria-rowcount"));

        Assert.assertEquals(10, table.bodyElement.getChildCount());
        Assert.assertEquals("rowgroup", table.bodyElement.getAttribute("role"));

        AtomicInteger counter = new AtomicInteger(0);
        table.bodyElement.getChildren().forEach(row -> {
            int index = counter.getAndIncrement();
            Assert.assertEquals("tr", row.getTag());
            Assert.assertEquals(""+(index+2), row.getAttribute("aria-rowindex"));
            Assert.assertEquals(2, row.getChildCount());
            Assert.assertEquals("th", row.getChild(0).getTag());
            Assert.assertEquals("rowheader", row.getChild(0).getAttribute("role"));
            Assert.assertEquals("name" + index, row.getChild(0).getText());
            Assert.assertEquals("td", row.getChild(1).getTag());
            Assert.assertEquals("cell", row.getChild(1).getAttribute("role"));
            Assert.assertEquals("data" + index, row.getChild(1).getText());
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

        BeanTableDataView<Person> dataView = table.setItems(dp);

        dataView.addItemCountChangeListener(event -> {
            count = event.getItemCount();
        });

        ui.add(table);

        Assert.assertEquals("First Name",
                table.headerElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("Last Name",
                table.headerElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("Age",
                table.headerElement.getChild(0).getChild(2).getText());
        Assert.assertEquals("Phone Number",
                table.headerElement.getChild(0).getChild(3).getText());
        Assert.assertEquals("Marital Status",
                table.headerElement.getChild(0).getChild(4).getText());
        Assert.assertEquals("Postal Code",
                table.headerElement.getChild(0).getChild(5).getText());
        Assert.assertEquals("City",
                table.headerElement.getChild(0).getChild(6).getText());

        Assert.assertEquals("20",
                table.getElement().getAttribute("aria-rowcount"));
        Assert.assertEquals(20, table.bodyElement.getChildCount());

        Assert.assertEquals("Brayden",
                table.bodyElement.getChild(2).getChild(0).getText());
        Assert.assertEquals("Wilder",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assert.assertEquals("59",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assert.assertEquals("915-088-178",
                table.bodyElement.getChild(2).getChild(3).getText());

        table.setPage(2);
        
        Assert.assertEquals("Layla",
                table.bodyElement.getChild(0).getChild(0).getText());
        Assert.assertEquals("Harmon",
                table.bodyElement.getChild(0).getChild(1).getText());
        Assert.assertEquals("30",
                table.bodyElement.getChild(0).getChild(2).getText());
        Assert.assertEquals("225-075-734",
                table.bodyElement.getChild(0).getChild(3).getText());

        dp.setFilter("ben");
        fakeClientCommunication();

        Assert.assertEquals("3",
                table.getElement().getAttribute("aria-rowcount"));
        Assert.assertEquals(3, table.bodyElement.getChildCount());
        Assert.assertEquals(3, count);
        Assert.assertEquals(0,  table.getPage());

        Assert.assertEquals("Bentley",
                table.bodyElement.getChild(2).getChild(0).getText());
        Assert.assertEquals("Pittman",
                table.bodyElement.getChild(2).getChild(1).getText());
        Assert.assertEquals("86",
                table.bodyElement.getChild(2).getChild(2).getText());
        Assert.assertEquals("643-754-1623",
                table.bodyElement.getChild(2).getChild(3).getText());
    
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
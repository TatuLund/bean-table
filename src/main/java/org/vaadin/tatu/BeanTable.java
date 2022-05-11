package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.BackEndDataProvider;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataProviderWrapper;
import com.vaadin.flow.data.provider.DataViewUtils;
import com.vaadin.flow.data.provider.HasDataView;
import com.vaadin.flow.data.provider.HasLazyDataView;
import com.vaadin.flow.data.provider.HasListDataView;
import com.vaadin.flow.data.provider.IdentifierProvider;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

/**
 * This is a simple Table component backed by DataProvider. The data provider
 * populates the Table with data from the beans. The component has minimal API
 * and ultra simple design. The purpose of this component is to be a little
 * sibling to Grid. Thus there are many features intentionally left out.
 * 
 * This component does not support lazy loading of the data, thus it is purposed
 * for the small data sets only.
 * 
 * Table's cells can be populated by text, html or components.
 * 
 * Currently only minimal styling included, no scrolling, etc. provided.
 * 
 * Component has css class name "bean-table" and custom css can be applied with
 * it.
 * 
 * @author Tatu Lund
 *
 * @param <T>
 *            Bean type for the Table
 */

@CssImport("./styles/bean-table.css")
@Tag("table")
public class BeanTable<T> extends HtmlComponent
        implements HasListDataView<T, BeanTableListDataView<T>>,
        HasDataView<T, Void, BeanTableDataView<T>>,
        HasLazyDataView<T, Void, BeanTableLazyDataView<T>>, HasSize {

    private final KeyMapper<T> keyMapper = new KeyMapper<>(this::getItemId);
    private final AtomicReference<DataProvider<T, ?>> dataProvider = new AtomicReference<>(
            DataProvider.ofItems());
    private int lastNotifiedDataSize = -1;
    private volatile int lastFetchedDataSize = -1;
    private SerializableConsumer<UI> sizeRequest;
    private Registration dataProviderListenerRegistration;
    private List<Column<T>> columns = new ArrayList<>();
    private List<RowItem<T>> rows = new ArrayList<>();
    private Element headerElement;
    private Element bodyElement;
    private boolean htmlAllowed;
    private Class<T> beanType;
    private PropertySet<T> propertySet;
    private Element footerElement;
    private int pageLength = -1;
    private int currentPage = 0;
    private Object filter;
    private SerializableComparator<T> inMemorySorting;

    private final ArrayList<QuerySortOrder> backEndSorting = new ArrayList<>();
    private int dataProviderSize = -1;
    private StringProvider<T> classNameProvider;
    private BeanTableLazyDataView<T> lazyDataView;

    @FunctionalInterface
    public interface StringProvider<T> extends ValueProvider<T, String> {

        /**
         * Gets a caption for the {@code item}.
         *
         * @param item
         *            the item to get caption for
         * @return the caption of the item, not {@code null}
         */
        @Override
        String apply(T item);
    }

    @FunctionalInterface
    public interface ComponentProvider<T> extends ValueProvider<T, Component> {

        /**
         * Gets a caption for the {@code item}.
         *
         * @param item
         *            the item to get caption for
         * @return the caption of the item, not {@code null}
         */
        @Override
        Component apply(T item);
    }

    /**
     * Configuration class for the Columns.
     *
     * @param <R>
     *            Bean type
     */
    public class Column<R> {
        String header;
        ValueProvider<T, ?> valueProvider;
        ComponentProvider<T> componentProvider;
        private StringProvider<T> classNameProvider;
        private Component headerComponent;
        private String key;

        /**
         * Constructor with header and value provider
         * 
         * @param header
         *            The header as text
         * @param valueProvider
         *            The valuprovider
         */
        public Column(String header, ValueProvider<T, ?> valueProvider) {
            this.header = header;
            this.valueProvider = valueProvider;
        }

        /**
         * Constructor without parameters
         */
        public Column() {
        }

        /**
         * Set column header as String
         * 
         * @param header
         *            String for header
         * @return Column for chaining
         */
        public Column<R> setHeader(String header) {
            this.header = header;
            updateHeader();
            return this;
        }

        /**
         * Set column header as a Component
         * 
         * @param header
         *            Component used for header
         * @return Column for chaining
         */
        public Column<R> setHeader(Component header) {
            this.headerComponent = header;
            updateHeader();
            return this;
        }

        /**
         * Set component provider function for the column,
         * 
         * @param componentProvider
         *            ColumnProvider Lambda callback bean instance to Column.
         * @return Column for chaining
         */
        public Column<R> setComponentProvider(
                ComponentProvider<T> componentProvider) {
            this.componentProvider = componentProvider;
            return this;
        }

        public ComponentProvider<T> getComponentProvider() {
            return componentProvider;
        }

        public ValueProvider<T, ?> getValueProvider() {
            return valueProvider;
        }

        /**
         * Set class name provider for a table column, i.e. cells in the column.
         * 
         * @param classNameProvider
         *            StringProvider Lambda callback bean instance to String.
         * @return Column for chaining
         */
        public Column<R> setClassNameProvider(
                StringProvider<T> classNameProvider) {
            this.classNameProvider = classNameProvider;
            return this;
        }

        public StringProvider<T> getClassNameProvider() {
            return classNameProvider;
        }

        public Component getHeader() {
            if (headerComponent != null) {
                return headerComponent;
            }
            if (header != null) {
                return new Text(header);
            }
            return null;
        }

        /**
         * Set unique key for the column
         * 
         * @param key
         *            String value
         */
        public void setKey(String key) {
            assert columns.stream().noneMatch(
                    col -> col.getKey().equals(key)) : "The key must be unique";
            Objects.requireNonNull(key, "The key can't be null");
            this.key = key;
        }

        /**
         * Gets the key of the column. Keys are automatically set if columns are
         * created by property names, in that case key is the property name.
         * 
         * @return The key, can be null
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * Internal wrapper class for the rows with item data.
     * 
     * @param <R>
     *            Bean type
     */
    private class RowItem<R> {

        private R item;
        private Element rowElement;

        public RowItem(String id, R item) {
            this.item = item;
            rowElement = new Element("tr");
            if (getClassNameProvider() != null) {
                String className = getClassNameProvider().apply((T) item);
                if (className != null && !className.isEmpty()) {
                    rowElement.getClassList().add(className);
                }
            }
            createCells();
        }

        private void createCells() {
            columns.forEach(column -> {
                if (column.getComponentProvider() == null
                        && column.getValueProvider() == null) {
                    throw new IllegalStateException(
                            "Column is lacking eihercomponent or value provider.");
                }
                Element cell = new Element("td");
                Component component = null;
                Object value = null;
                if (column.getComponentProvider() != null) {
                    component = column.getComponentProvider().apply((T) item);
                } else {
                    value = column.getValueProvider().apply((T) item);
                }
                if (column.getClassNameProvider() != null) {
                    String className = column.getClassNameProvider()
                            .apply((T) item);
                    if (className != null && !className.isEmpty()) {
                        cell.getClassList().add(className);
                    }
                }
                if (value == null)
                    value = "";
                if (component != null) {
                    cell.appendChild(component.getElement());
                } else if (htmlAllowed) {
                    Html span = new Html(
                            "<span>" + value.toString() + "</span>");
                    cell.appendChild(span.getElement());
                } else {
                    cell.setText(value.toString());
                }
                rowElement.appendChild(cell);
            });
        }

        public R getItem() {
            return item;
        }

        public Element getRowElement() {
            return rowElement;
        }

        public void setItem(R item) {
            this.item = item;
            rowElement.removeAllChildren();
            createCells();
        }

    }

    /**
     * The default constructor. This creates a BeanTable without further
     * configuration. Use {@link #addColumn(String,ValueProvider)}
     * {@link #addComponentColumn(String,ComponentProvider)} to configure
     * columns.
     */
    public BeanTable() {
        setClassName("bean-table");
        headerElement = new Element("thead");
        footerElement = new Element("tfoot");
        bodyElement = new Element("tbody");
        getElement().appendChild(headerElement);
        getElement().appendChild(bodyElement);
        getElement().appendChild(footerElement);
    }

    /**
     * The default constructor with defined page length. Use this constructor
     * with large data sources, i.e. DataProvider.fromCallBacks(..). This
     * constructor enables paging controls in the footer row. Also this creates
     * a BeanTable without further configuration. Use
     * {@link #addColumn(String,ValueProvider)}
     * {@link #addComponentColumn(String,ComponentProvider)} to configure
     * columns.
     * 
     * @param pageLength
     *            Size of the page
     */
    public BeanTable(int pageLength) {
        this();
        this.pageLength = pageLength;
    }

    /**
     * Creates a new BeanTable with an initial set of columns for each of the
     * bean's properties. The property-values of the bean will be converted to
     * Strings. Full names of the properties will be used as the header
     * captions.
     * 
     * @param beanType
     *            the bean type to use, not <code>null</code>
     */
    public BeanTable(Class<T> beanType) {
        this(beanType, true);
    }

    /**
     * Creates a new BeanTable with an initial set of columns for each of the
     * bean's properties. The property-values of the bean will be converted to
     * Strings. Full names of the properties will be used as the header
     * captions.
     * <p>
     * Constructor with defined page length. Use this constructor with large
     * data sources, i.e. DataProvider.fromCallBacks(..). This constructor
     * enables paging controls in the footer row.
     * <p>
     * When autoCreateColumns is <code>false</code>. Use
     * {@link #setColumns(String...)} to define which properties to include and
     * in which order. You can also add a column for an individual property with
     * {@link #addColumn(String)}.
     *
     * @param beanType
     *            the bean type to use, not <code>null</code>
     * @param autoCreateColumns
     *            when <code>true</code>, columns are created automatically for
     *            the properties of the beanType
     */
    public BeanTable(Class<T> beanType, boolean autoCreateColumns) {
        this();
        Objects.requireNonNull(beanType, "Bean type can't be null");
        this.beanType = beanType;
        propertySet = BeanPropertySet.get(beanType);
        if (autoCreateColumns) {
            propertySet.getProperties()
                    .filter(property -> !property.isSubProperty())
                    .sorted((prop1, prop2) -> prop1.getName()
                            .compareTo(prop2.getName()))
                    .forEach(this::addColumn);
        }
    }

    /**
     * Creates a new BeanTable with an initial set of columns for each of the
     * bean's properties. The property-values of the bean will be converted to
     * Strings. Full names of the properties will be used as the header
     * captions.
     * <p>
     * When autoCreateColumns is <code>false</code>. Use
     * {@link #setColumns(String...)} to define which properties to include and
     * in which order. You can also add a column for an individual property with
     * {@link #addColumn(String)}.
     *
     * @param beanType
     *            the bean type to use, not <code>null</code>
     * @param autoCreateColumns
     *            when <code>true</code>, columns are created automatically for
     *            the properties of the beanType
     * @param pageLength
     *            Size of the page
     */
    public BeanTable(Class<T> beanType, boolean autoCreateColumns,
            int pageLength) {
        this(beanType, autoCreateColumns);
        this.pageLength = pageLength;
    }

    /**
     * Add column to Table with the given property.
     * 
     * @param property
     *            The property
     * 
     * @return A new column
     */
    private Column<T> addColumn(PropertyDefinition<T, ?> property) {
        String propertyName = property.getName();
        String name = formatName(propertyName);
        Column<T> column = addColumn(name, property.getGetter());
        column.setKey(propertyName);
        return column;
    }

    private String formatName(String propertyName) {
        if (propertyName == null || propertyName.isEmpty())
            return "";
        String name = propertyName.replaceAll(
                String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"),
                " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return name;
    }

    /**
     * Add column to Table with the given property name.
     * 
     * @param propertyName
     *            The property
     */
    public void addColumn(String propertyName) {
        Objects.requireNonNull(propertySet,
                "No property set defined, use BeanTable((Class<T> beanType) constructor");
        Objects.requireNonNull(propertyName,"propetyName cannot be null");
        propertySet.getProperties()
                .filter(property -> property.getName().equals(propertyName))
                .findFirst().ifPresent(match -> {
                    Column<T> column = addColumn(formatName(match.getName()),
                            match.getGetter());
                    column.setKey(propertyName);
                });
    }

    /**
     * Configure BeanTable to have columns with given set of property names.
     * 
     * @param propertyNames
     *            List of property names
     */
    public void setColumns(String... propertyNames) {
        for (String propertyName : propertyNames) {
            addColumn(propertyName);
        }
    }

    /**
     * Add a column with a value provider. Value provider is a function
     * reference, e.g. getter of the bean or lambda that returns the value for
     * this column.
     * 
     * @param header
     *            The header as a string, can be null
     * @param valueProvider
     *            The value provider
     * 
     * @return A column
     */
    public Column<T> addColumn(String header,
            ValueProvider<T, ?> valueProvider) {
        Objects.requireNonNull(valueProvider,"A valueProvider must not be null");
        Column<T> column = new Column<>(header, valueProvider);
        columns.add(column);
        updateHeader();
        return column;
    }

    /**
     * Add a column with component provider. Component provider is a lambda that
     * must return a new instance of a component.
     * 
     * @param header
     *            Header as string, can be null
     * @param componentProvider
     *            Component provider
     * 
     * @return A column
     */
    public Column<T> addComponentColumn(String header,
            ComponentProvider<T> componentProvider) {
        Objects.requireNonNull(componentProvider,"A componentProvider must not be null");
        Column<T> column = new Column<>();
        column.setComponentProvider(componentProvider);
        columns.add(column);
        updateHeader();
        return column;
    }

    private void updateHeader() {
        headerElement.removeAllChildren();
        Element rowElement = new Element("tr");
        columns.forEach(column -> {
            Element cell = new Element("th");
            if (column.getHeader() != null) {
                cell.appendChild(column.getHeader().getElement());
            }
            rowElement.appendChild(cell);
        });
        headerElement.appendChild(rowElement);
    }

    private void updateFooter() {
        footerElement.removeAllChildren();
        if (dataProviderSize > 0) {
            Element rowElement = new Element("tr");
            Element cell = new Element("td");
            cell.setAttribute("colspan", "" + columns.size());
            rowElement.appendChild(cell);
            Button first = new Button();
            first.setIcon(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
            first.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            first.addThemeVariants(ButtonVariant.LUMO_ICON);
            Button previous = new Button();
            previous.setIcon(VaadinIcon.ANGLE_LEFT.create());
            previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            Button next = new Button();
            next.setIcon(VaadinIcon.ANGLE_RIGHT.create());
            next.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            Button last = new Button();
            last.setIcon(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
            last.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            int lastPage = dataProviderSize % pageLength == 0
                    ? (dataProviderSize / pageLength) - 1
                    : (dataProviderSize / pageLength);
            first.addClickListener(event -> {
                if (currentPage != 0) {
                    currentPage = 0;
                    getDataProvider().refreshAll();
                }
            });
            next.addClickListener(event -> {
                if (currentPage < lastPage) {
                    currentPage++;
                    getDataProvider().refreshAll();
                }
            });
            previous.addClickListener(event -> {
                if (currentPage > 0) {
                    currentPage--;
                    getDataProvider().refreshAll();
                }
            });
            last.addClickListener(event -> {
                if (currentPage != lastPage) {
                    currentPage = lastPage;
                    getDataProvider().refreshAll();
                }
            });
            Div div = new Div();
            div.addClassName("bean-table-paging");
            Div spacer = new Div();
            spacer.addClassName("bean-table-page");
            spacer.setText((currentPage + 1) + "/" + (lastPage + 1));
            div.add(first, previous, spacer, next, last);
            cell.appendChild(div.getElement());
            footerElement.appendChild(rowElement);
        }
    }

    @Deprecated
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        this.dataProvider.set(dataProvider);
        DataViewUtils.removeComponentFilterAndSortComparator(this);
        int estimate = -1;
        if (getDataProvider() instanceof BackEndDataProvider) {
            estimate = getLazyDataView().getItemCountEstimate();
            if (estimate < 0) {
                reset(false);
            }
        } else {
            reset(false);
        }
        setupDataProviderListener(dataProvider);
    }

    private void setupDataProviderListener(DataProvider<T, ?> dataProvider) {
        if (dataProviderListenerRegistration != null) {
            dataProviderListenerRegistration.remove();
        }
        dataProviderListenerRegistration = dataProvider
                .addDataProviderListener(event -> {
                    if (event instanceof DataChangeEvent.DataRefreshEvent) {
                        doRefreshItem(event);
                    } else {
                        reset(false);
                    }
                });
    }

    private void doRefreshItem(DataChangeEvent<T> event) {
        T otherItem = ((DataChangeEvent.DataRefreshEvent<T>) event).getItem();
        getRowItems()
                .filter(rowItem -> Objects.equals(getItemId(rowItem.getItem()),
                        getItemId(otherItem)))
                .findFirst()
                .ifPresent(rowItem -> updateRow(rowItem, otherItem));
    }

    private RowItem<T> createRow(T item) {
        RowItem<T> rowItem = new RowItem<>(keyMapper.key(item), item);
        return rowItem;
    }

    private void addRow(RowItem<T> rowItem) {
        rows.add(rowItem);
        bodyElement.appendChild(rowItem.getRowElement());
    }

    void reset(boolean refresh) {
        if (!refresh) {
            bodyElement.setText("");
            rows = new ArrayList<>();
        }
        keyMapper.removeAll();
        Query query = null;
        if (pageLength < 0) {
            query = new Query();
        } else {
            int estimate = -1;
            if (getDataProvider() instanceof BackEndDataProvider) {
                estimate = getLazyDataView().getItemCountEstimate();
            }
            synchronized (dataProvider) {
                dataProviderSize = estimate < 0
                        ? getDataProvider().size(new Query(filter))
                        : estimate;
            }
            updateFooter();
            int offset = pageLength * currentPage;
            query = new Query(offset, pageLength, backEndSorting,
                    inMemorySorting, filter);
        }
        synchronized (dataProvider) {
            final AtomicInteger itemCounter = new AtomicInteger(0);
            getDataProvider().fetch(query).map(row -> createRow((T) row))
                    .forEach(rowItem -> {
                        addRow((BeanTable<T>.RowItem<T>) rowItem);
                        itemCounter.incrementAndGet();
                    });
            lastFetchedDataSize = itemCounter.get();
            if (sizeRequest == null) {
                sizeRequest = ui -> {
                    fireSizeEvent();
                    sizeRequest = null;
                };
                // Size event is fired before client response so as to avoid
                // multiple size change events during server round trips
                runBeforeClientResponse(sizeRequest);
            }
        }
    }

    protected T fetchItem(int index) {
        Query query = new Query(index, 1, backEndSorting, inMemorySorting,
                filter);
        Optional<T> result = getDataProvider().fetch(query).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    /**
     * Return the currently used data provider.
     * 
     * @return A data provider
     */
    public DataProvider<T, ?> getDataProvider() {
        return dataProvider.get();
    }

    private Object getItemId(T item) {
        if (getDataProvider() == null) {
            return item;
        }
        return getDataProvider().getId(item);
    }

    private Stream<RowItem<T>> getRowItems() {
        return rows.stream();
    }

    private void updateRow(RowItem<T> rowItem, T item) {
        rowItem.setItem(item);
    }

    @Override
    public Element getElement() {
        return super.getElement();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (getDataProvider() != null
                && dataProviderListenerRegistration == null) {
            setupDataProviderListener(getDataProvider());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (dataProviderListenerRegistration != null) {
            dataProviderListenerRegistration.remove();
            dataProviderListenerRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    /**
     * Set whether cell content should allow html content or not. If this is
     * false (default), value will be set as text content of the cell. If set to
     * true the value string will be wrapped in span element and can contain
     * html.
     * 
     * @param htmlAllowed
     *            A boolean value.
     */
    public void setHtmlAllowed(boolean htmlAllowed) {
        this.htmlAllowed = htmlAllowed;
    }

    /**
     * Set class name provider for a table row.
     * 
     * @param classNameProvider
     *            StringProvider Lambda callback bean instance to String.
     */
    public void setClassNameProvider(StringProvider<T> classNameProvider) {
        this.classNameProvider = classNameProvider;
    }

    public StringProvider<T> getClassNameProvider() {
        return classNameProvider;
    }

    @Override
    public BeanTableDataView<T> getGenericDataView() {
        return new BeanTableDataView<>(this::getDataProvider, this,
                this::identifierProviderChanged);
    }

    @Override
    public BeanTableDataView<T> setItems(DataProvider<T, Void> dataProvider) {
        setDataProvider(dataProvider);
        return getGenericDataView();
    }

    @Override
    public BeanTableDataView<T> setItems(
            InMemoryDataProvider<T> inMemoryDataProvider) {
        DataProvider<T, Void> convertedDataProvider = new DataProviderWrapper<T, Void, SerializablePredicate<T>>(
                inMemoryDataProvider) {
            @Override
            protected SerializablePredicate<T> getFilter(Query<T, Void> query) {
                // Just ignore the query filter (Void) and apply the
                // predicate only
                return Optional.ofNullable(inMemoryDataProvider.getFilter())
                        .orElse(item -> true);
            }
        };
        return setItems(convertedDataProvider);
    }

    @Override
    public BeanTableListDataView<T> getListDataView() {
        return new BeanTableListDataView<>(this::getDataProvider, this,
                this::identifierProviderChanged,
                (filter, sorting) -> reset(true));
    }

    @Override
    public BeanTableListDataView<T> setItems(ListDataProvider<T> dataProvider) {
        setDataProvider(dataProvider);
        return getListDataView();
    }

    public void setItems(Stream<T> streamOfItems) {
        setItems(DataProvider.fromStream(streamOfItems));
    }

    @SuppressWarnings("unchecked")
    private IdentifierProvider<T> getIdentifierProvider() {
        IdentifierProvider<T> identifierProviderObject = ComponentUtil
                .getData(this, IdentifierProvider.class);
        if (identifierProviderObject == null) {
            DataProvider<T, ?> dataProvider = getDataProvider();
            if (dataProvider != null) {
                return dataProvider::getId;
            } else {
                return IdentifierProvider.identity();
            }
        } else {
            return identifierProviderObject;
        }
    }

    private void identifierProviderChanged(
            IdentifierProvider<T> identifierProvider) {
        keyMapper.setIdentifierGetter(identifierProvider);
    }

    private void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui
                .beforeClientResponse(this, context -> command.accept(ui)));
    }

    private void fireSizeEvent() {
        final int newSize = lastFetchedDataSize;
        if (lastNotifiedDataSize != newSize) {
            lastNotifiedDataSize = newSize;
            fireEvent(new ItemCountChangeEvent<>(this, newSize, false));
        }
    }

    @Override
    public BeanTableLazyDataView<T> setItems(
            BackEndDataProvider<T, Void> dataProvider) {
        setDataProvider(dataProvider);
        return getLazyDataView();
    }

    @Override
    public BeanTableLazyDataView<T> getLazyDataView() {
        if (lazyDataView == null) {
            lazyDataView = new BeanTableLazyDataView<>(this::getDataProvider,
                    this);
        }
        return lazyDataView;
    }

    /**
     * Get list of the currently set columns.
     * 
     * @return List of Columns
     */
    public List<Column<T>> getColumns() {
        return columns;
    }

    /**
     * Get the column by its key.
     * 
     * @param key The key, can't be null
     * @return Optional Column
     */
    public Optional<Column<T>> getColumn(String key) {
        Objects.requireNonNull(key, "The key cannot be null");
        return columns.stream().filter(col -> col.getKey().equals(key))
                .findFirst();
    }
}

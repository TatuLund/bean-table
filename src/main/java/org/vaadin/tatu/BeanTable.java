package org.vaadin.tatu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
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
import com.vaadin.flow.function.SerializableBiFunction;
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
 * This component does support lazy loading of the data and uses paging for it.
 * 
 * Table's cells can be populated by text, html or components.
 * 
 * A11y supported.
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
    private Random rand = new Random();
    private BeanTableI18n i18n;
    private Element captionElement;

    public enum ColumnAlignment {
        CENTER, LEFT, RIGHT;
    }

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
    public class Column<R> implements Serializable {
        String header;
        ValueProvider<T, ?> valueProvider;
        ComponentProvider<T> componentProvider;
        private StringProvider<T> classNameProvider;
        private StringProvider<T> tooltipProvider;
        private Component headerComponent;
        private String key;
        private ColumnAlignment columnAlignment;
        private String width;
        private boolean rowHeader;

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
         * Set tooltip provider function for the column,
         * <p>
         * Note: Not tooltip provider applied on component columns. Add tooltip
         * to the component directly.
         * 
         * @param tooltipProvider
         *            StringProvider Lambda callback bean instance for the
         *            Column.
         * @return Column for chaining
         */
        public Column<R> setTooltipProvider(StringProvider<T> tooltipProvider) {
            this.tooltipProvider = tooltipProvider;
            return this;
        }

        public StringProvider<T> getTooltipProvider() {
            return tooltipProvider;
        }

        /**
         * Set component provider function for the column,
         * 
         * @param componentProvider
         *            ComponnetProvider Lambda callback bean instance for the
         *            Column.
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

        /**
         * Set vertical alignment of the cell content, CENTER, LEFT, RIGHT.
         * 
         * @param alignment
         *            ColumnAlignment, null to unset.
         * @return Column for chaining
         */
        public Column<R> setAlignment(ColumnAlignment alignment) {
            columnAlignment = alignment;
            return this;
        }

        public ColumnAlignment getAlignment() {
            return columnAlignment;
        }

        /**
         * Set the width of the column.
         * 
         * @param width
         *            CSS compatible width string, null to unset.
         * @return Column for chaining
         */
        public Column<R> setWidth(String width) {
            this.width = width;
            updateHeader();
            return this;
        }

        public String getWidth() {
            return width;
        }

        /**
         * Set unique key for the column
         * 
         * @param key
         *            String value
         * @return Column for chaining
         */
        public Column<R> setKey(String key) {
            assert columns.stream().noneMatch(
                    col -> col.getKey().equals(key)) : "The key must be unique";
            Objects.requireNonNull(key, "The key can't be null");
            this.key = key;
            return this;
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

        /**
         * Sets the row header nature for the column for accessibility.
         * 
         * @param rowHeader
         *            Use true if this column should act as row header.
         * @return Column for chaining
         */
        public Column<R> setRowHeader(boolean rowHeader) {
            this.rowHeader = rowHeader;
            return this;
        }

        /**
         * Returns if true if this column acts as row header.
         * 
         * @return Boolean value.
         */
        public boolean isRowHeader() {
            return rowHeader;
        }
    }

    /**
     * Internal wrapper class for the rows with item data.
     * 
     * @param <R>
     *            Bean type
     */
    private class RowItem<R> implements Serializable {

        private R item;
        private Element rowElement;

        public RowItem(String id, R item) {
            this.item = item;
            rowElement = new Element("tr");
            rowElement.setAttribute("role", "row");
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

                Element cell;
                if (column.isRowHeader()) {
                    cell = new Element("th");
                    cell.setAttribute("role", "rowheader");
                } else {
                    cell = new Element("td");
                    cell.setAttribute("role", "cell");
                }
                if (column.getAlignment() != null) {
                    cell.getStyle().set("text-align",
                            column.getAlignment().toString().toLowerCase());
                } else {
                    cell.getStyle().remove("text-align");
                }
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
                } else if (column.tooltipProvider != null) {
                    String key = randomId("tooltip", 8);
                    String tooltipText = column.getTooltipProvider()
                            .apply((T) item);
                    Html span = new Html("<span id='tooltip-" + key + "'>"
                            + value.toString() + "<vaadin-tooltip text='"
                            + tooltipText + "' for='tooltip-" + key
                            + "'></vaadin-tooltip></span>");
                    cell.appendChild(span.getElement());
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
        getElement().setAttribute("role", "table");
        getElement().setAttribute("aria-rowcount", String.valueOf(pageLength));
        headerElement = new Element("thead");
        headerElement.setAttribute("role", "rowgroup");
        footerElement = new Element("tfoot");
        bodyElement = new Element("tbody");
        bodyElement.setAttribute("role", "rowgroup");
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
        getElement().setAttribute("aria-rowcount", String.valueOf(pageLength));
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
        getElement().setAttribute("aria-rowcount", String.valueOf(pageLength));
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
        Objects.requireNonNull(propertyName, "propetyName cannot be null");
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
        Objects.requireNonNull(valueProvider,
                "A valueProvider must not be null");
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
        Objects.requireNonNull(componentProvider,
                "A componentProvider must not be null");
        Column<T> column = new Column<>();
        column.setComponentProvider(componentProvider);
        columns.add(column);
        updateHeader();
        return column;
    }

    private void updateHeader() {
        headerElement.removeAllChildren();
        Element rowElement = new Element("tr");
        rowElement.setAttribute("role", "row");
        rowElement.setAttribute("aria-rowindex", "1");
        columns.forEach(column -> {
            Element cell = new Element("th");
            cell.setAttribute("role", "columnheader");
            if (column.getHeader() != null) {
                cell.appendChild(column.getHeader().getElement());
            }
            cell.getStyle().set("width", column.getWidth());
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
            updateTooltips(first, previous, next, last);
            Div div = new Div();
            div.addClassName("bean-table-paging");
            Div spacer = new Div();
            spacer.addClassName("bean-table-page");
            spacer.setText((currentPage + 1) + "/" + (lastPage + 1));
            spacer.getElement().setAttribute("aria-label", i18n
                    .getPageProvider().apply(currentPage + 1, lastPage + 1));
            div.add(first, previous, spacer, next, last);
            cell.appendChild(div.getElement());
            footerElement.appendChild(rowElement);
        }
    }

    private void updateTooltips(Button first, Button previous, Button next,
            Button last) {
        if (i18n != null) {
            first.setTooltipText(i18n.getFirstPage());
            first.getElement().setAttribute("aria-label", i18n.getFirstPage());
            last.setTooltipText(i18n.getLastPage());
            last.getElement().setAttribute("aria-label", i18n.getLastPage());
            previous.setTooltipText(i18n.getPreviousPage());
            previous.getElement().setAttribute("aria-label",
                    i18n.getPreviousPage());
            next.setTooltipText(i18n.getNextPage());
            next.getElement().setAttribute("aria-label", i18n.getNextPage());
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

    private void addRow(RowItem<T> rowItem, int index) {
        rows.add(rowItem);
        rowItem.getRowElement().setAttribute("aria-rowindex",
                String.valueOf(index + 2));
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
                        addRow((BeanTable<T>.RowItem<T>) rowItem,
                                itemCounter.get());
                        itemCounter.incrementAndGet();
                    });
            lastFetchedDataSize = itemCounter.get();
            getElement().setAttribute("aria-rowcount",
                    String.valueOf(lastFetchedDataSize));
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
     * @param key
     *            The key, can't be null
     * @return Optional Column
     */
    public Optional<Column<T>> getColumn(String key) {
        Objects.requireNonNull(key, "The key cannot be null");
        return columns.stream().filter(col -> col.getKey().equals(key))
                .findFirst();
    }

    private String randomId(String prefix, int chars) {
        int limit = (10 * chars) - 1;
        String key = "" + rand.nextInt(limit);
        key = String.format("%" + chars + "s", key).replace(' ', '0');
        return prefix + "-" + key;
    }

    /**
     * Set the caption associated with the Table
     * 
     * @param caption
     *            A string value. Null will remove caption.
     */
    public void setCaption(String caption) {
        if (this.captionElement != null) {
            getElement().removeChild(captionElement);
        } else if (caption == null) {
            getElement().removeChild(captionElement);
        } else {
            captionElement = new Element("caption");
            String id = randomId("label", 8);
            captionElement.setAttribute("id", id);
            captionElement.setText(caption);
            getElement().setAttribute("aria-labelledby", id);
            getElement().appendChild(captionElement);
        }
    }

    /**
     * Sets the internationalization properties (texts used for button tooltips)
     * for this component.
     *
     * @param i18n
     *            the internationalized properties, null to disable all
     *            tooltips.
     */
    public void setI18n(BeanTableI18n i18n) {
        this.i18n = i18n;
    }

    /**
     * Gets the internationalization object previously set for this component.
     *
     * @return the i18n object. It will be <code>null</code>, If the i18n
     *         properties weren't set.
     */
    public BeanTableI18n getI18n() {
        return i18n;
    }

    public static class BeanTableI18n implements Serializable {
        private String lastPage;
        private String previousPage;
        private String nextPage;
        private String firstPage;
        private SerializableBiFunction<Integer, Integer, String> pageProvider;

        public String getLastPage() {
            return lastPage;
        }

        public void setLastPage(String lastPage) {
            this.lastPage = lastPage;
        }

        public String getPreviousPage() {
            return previousPage;
        }

        public void setPreviousPage(String previousPage) {
            this.previousPage = previousPage;
        }

        public String getNextPage() {
            return nextPage;
        }

        public void setNextPage(String nextPage) {
            this.nextPage = nextPage;
        }

        public String getFirstPage() {
            return firstPage;
        }

        public void setFirstPage(String firstPage) {
            this.firstPage = firstPage;
        }

        public SerializableBiFunction<Integer, Integer, String> getPageProvider() {
            return this.pageProvider;
        }

        public void setPageProvider(
                SerializableBiFunction<Integer, Integer, String> provider) {
            this.pageProvider = provider;
        }

        public static BeanTableI18n getDefault() {
            BeanTableI18n english = new BeanTableI18n();
            english.setFirstPage("First page");
            english.setNextPage("Next page");
            english.setLastPage("Last page");
            english.setPreviousPage("Previous page");
            english.setPageProvider((currentPage, lastPage) -> "Page "
                    + currentPage + " of " + lastPage);
            return english;
        }
    }

}

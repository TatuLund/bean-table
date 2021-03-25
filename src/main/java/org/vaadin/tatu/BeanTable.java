package org.vaadin.tatu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.dom.Element;
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
 * @param <T> Bean type for the Table
 */

@CssImport("./styles/bean-table.css")
@Tag("table")
public class BeanTable<T> extends HtmlContainer
        implements HasDataProvider<T>, HasSize {

    private final KeyMapper<T> keyMapper = new KeyMapper<>(this::getItemId);
    private DataProvider<T, ?> dataProvider = DataProvider.ofItems();
    private Registration dataProviderListenerRegistration;
    private List<Column<T>> columns = new ArrayList<>();
    private List<RowItem<T>> rows = new ArrayList<>();
    private Element headerElement;
    private Element bodyElement;
    private boolean htmlAllowed;
    private Class<T> beanType;
    private PropertySet<T> propertySet;

    /**
     * Configuration class for the Columns.
     *
     * @param <R> Bean type
     */
    public class Column<R> {
        String header;
        ValueProvider<T, ?> valueProvider;
        ValueProvider<T, Component> componentProvider;

        /**
         * Constructor with header and value provider
         * 
         * @param header The header as text
         * @param valueProvider The valuprovider
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

        public void setHeader(String header) {
            this.header = header;
        }

        public void setComponentProvider(
                ValueProvider<T, Component> componentProvider) {
            this.componentProvider = componentProvider;
        }

        public ValueProvider<T, ?> getValueProvider() {
            return valueProvider;
        }

        public ValueProvider<T, Component> getComponentProvider() {
            return componentProvider;
        }

        public String getHeader() {
            return header;
        }
    }

    /**
     * Internal wrapper class for the rows with item data.
     * 
     * @param <R> Bean type
     */
    private class RowItem<R> {

        private R item;
        private Element rowElement;

        public RowItem(String id, R item) {
            this.item = item;
            getElement().setProperty("value", id);
            rowElement = new Element("tr");
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
     * {@link #addComponentColumn(String,ValueProvider)} to configure columns.
     */
    public BeanTable() {
        setClassName("bean-table");
        headerElement = new Element("thead");
        bodyElement = new Element("tbody");
        getElement().appendChild(headerElement);
        getElement().appendChild(bodyElement);
    }

    /**
     * Creates a new BeanTable with an initial set of columns for each of the
     * bean's properties. The property-values of the bean will be converted to
     * Strings. Full names of the properties will be used as the header
     * captions.
     * 
     * @param beanType the bean type to use, not <code>null</code>
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
     * When autoCreateColumns is <code>false</code>. Use
     * {@link #setColumns(String...)} to define which properties to include and
     * in which order. You can also add a column for an individual property with
     * {@link #addColumn(String)}.
     *
     * @param beanType the bean type to use, not <code>null</code>
     * @param autoCreateColumns when <code>true</code>, columns are created automatically for
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
     * Add column to Table with the given property.
     * 
     * @param property The property
     * 
     * @return A new column
     */
    private Column<T> addColumn(PropertyDefinition<T, ?> property) {
        return addColumn(property.getName(), property.getGetter());
    }

    /**
     * Add column to Table with the given property name.
     * 
     * @param propertyName The property
     */
    public void addColumn(String propertyName) {
        Objects.requireNonNull(propertySet,
                "No property set defined, use BeanTable((Class<T> beanType) constructor");
        Objects.requireNonNull("propetyName cannot be null");
        propertySet.getProperties()
                .filter(property -> property.getName().equals(propertyName))
                .findFirst().ifPresent(match -> {
                    addColumn(match.getName(), match.getGetter());
                });
    }

    /**
     * Configure BeanTable to have columns with given set of property names.
     * 
     * @param propertyNames List of property names
     * 
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
     * @param header The heafers as a string, can be null
     * 
     * @param valueProvider The value provider
     * 
     * @return A column
     */
    public Column<T> addColumn(String header,
            ValueProvider<T, ?> valueProvider) {
        Column<T> column = new Column<>(header, valueProvider);
        columns.add(column);
        updateHeader();
        return column;
    }

    /**
     * Add a column with component provider. Component provider is a lambda that
     * must return a new instance of a component.
     * 
     * @param header Header as string, can be null
     * @param componentProvider Component provider
     *      
     * @return A column
     */
    public Column<T> addComponentColumn(String header,
            ValueProvider<T, Component> componentProvider) {
        Column<T> column = new Column<>();
        column.setHeader(header);
        column.setComponentProvider(componentProvider);
        columns.add(column);
        updateHeader();
        return column;
    }

    private void updateHeader() {
        headerElement.removeAllChildren();
        Element rowElement = new Element("tr");
        columns.forEach(column -> {
            Element cell = new Element("td");
            if (column.getHeader() != null) {
                cell.setText(column.getHeader());
            }
            rowElement.appendChild(cell);
        });
        headerElement.appendChild(rowElement);
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        this.dataProvider = dataProvider;
        reset(true);
        setupDataProviderListener(dataProvider);
    }

    private void setupDataProviderListener(DataProvider<T, ?> dataProvider) {
        if (dataProviderListenerRegistration != null) {
            dataProviderListenerRegistration.remove();
        }
        dataProviderListenerRegistration = dataProvider
                .addDataProviderListener(event -> {
                    if (event instanceof DataChangeEvent.DataRefreshEvent) {
                        reset(false);
                        // doRefreshItem(event);
                    } else {
                        reset(false);
                    }
                });
    }

    private void doRefreshItem(DataChangeEvent<T> event) {
        // This part of the refresh items works, but updateRow
        // is not working
        // for some reason direct manipulation of existing rows
        // with Element API does
        // not work, maybe this is a bug in Flow
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

    private void reset(boolean refresh) {
        if (!refresh) {
            bodyElement.setText("");
        }
        keyMapper.removeAll();
        getDataProvider().fetch(new Query<>()).map(this::createRow)
                .forEach(rowItem -> addRow(rowItem));
    }

    /**
     * Return the currently used data provider.
     * 
     * @return A data provider
     */
    public DataProvider<T, ?> getDataProvider() {
        return dataProvider;
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
     * @param htmlAllowed A boolean value.
     */
    public void setHthmlAllowed(boolean htmlAllowed) {
        this.htmlAllowed = htmlAllowed;
    }
}

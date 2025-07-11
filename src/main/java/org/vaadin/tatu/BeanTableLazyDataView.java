package org.vaadin.tatu;

import java.util.Optional;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.BackEndDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataProviderWrapper;
import com.vaadin.flow.data.provider.ItemIndexProvider;
import com.vaadin.flow.data.provider.LazyDataView;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.SerializableSupplier;

public class BeanTableLazyDataView<T> extends AbstractDataView<T>
        implements LazyDataView<T> {

    private BeanTable<T> table;
    private int itemCountEstimateIncrease;
    private int itemCountEstimate = -1;

    /**
     * Creates a new lazy data view for grid and verifies the passed data
     * provider is compatible with this data view implementation.
     *
     * @param dataProviderSupplier
     *            data provider supplier
     * @param component
     *            the Table
     */
    public BeanTableLazyDataView(
            SerializableSupplier<? extends DataProvider<T, ?>> dataProviderSupplier,
            BeanTable<T> component) {
        super(dataProviderSupplier, component);
        table = component;
    }

    @Override
    public void setItemCountFromDataProvider() {
        itemCountEstimate = -1;
        table.reset(false);
    }

    @Override
    public void setItemCountEstimate(int itemCountEstimate) {
        this.itemCountEstimate = itemCountEstimate;
        table.reset(false);
    }

    @Override
    public void setItemCountUnknown() {
        itemCountEstimate = -1;
    }

    @Override
    public T getItem(int index) {
        this.getItemIndex(null);
        return table.fetchItem(index);
    }

    @Override
    protected Class<?> getSupportedDataProviderType() {
        return BackEndDataProvider.class;
    }

    @Override
    public int getItemCountEstimate() {
        return itemCountEstimate;
    }

    @Override
    public void setItemCountEstimateIncrease(int itemCountEstimateIncrease) {
        this.itemCountEstimateIncrease = itemCountEstimateIncrease;
    }

    @Override
    public int getItemCountEstimateIncrease() {
        return itemCountEstimateIncrease;
    }

    @Override
    public void setItemIndexProvider(
            ItemIndexProvider<T, ?> itemIndexProvider) {
        ComponentUtil.setData(table, ItemIndexProvider.class,
                itemIndexProvider);
    }

    /**
     * Gets the item index provider for this data view's component.
     *
     * @return the item index provider. May be null.
     */
    @SuppressWarnings("unchecked")
    protected ItemIndexProvider<T, ?> getItemIndexProvider() {
        return (ItemIndexProvider<T, ?>) ComponentUtil.getData(table,
                ItemIndexProvider.class);
    }

    /**
     * Gets the index of the given item by the item index provider set with
     * {@link #setItemIndexProvider(ItemIndexProvider)}.
     *
     * @param item
     *            item to get index for
     * @return index of the item or null if the item is not found
     * @throws UnsupportedOperationException
     *             if the item index provider is not set with
     *             {@link #setItemIndexProvider(ItemIndexProvider)}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<Integer> getItemIndex(T item) {
        if (getItemIndexProvider() == null) {
            throw new UnsupportedOperationException(
                    "getItemIndex method in the LazyDataView requires a callback to fetch the index. Set it with setItemIndexProvider.");
        }
        return Optional.ofNullable(getItemIndexProvider().apply(item,
                getFilteredQueryForAllItems()));
    }

    /**
     * Gets the page index of the given item by the item index provider set with
     * {@link #setItemIndexProvider(ItemIndexProvider)}.
     *
     * @param item
     *            item to get page index for
     * @return page index of the item or null if the item is not found
     * @throws UnsupportedOperationException
     *             if the item index provider is not set with
     *             {@link #setItemIndexProvider(ItemIndexProvider)}
     */
    @SuppressWarnings("unchecked")
    public Optional<Integer> getPageIndex(T item) {
        if (getItemIndexProvider() == null) {
            throw new UnsupportedOperationException(
                    "getItemIndex method in the LazyDataView requires a callback to fetch the index. Set it with setItemIndexProvider.");
        }
        return Optional.ofNullable(getItemIndexProvider().apply(item,
                getFilteredQueryForAllItems()) / table.pageLength);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Query getFilteredQueryForAllItems() {
        return new Query(0, Integer.MAX_VALUE, table.backEndSorting,
                table.inMemorySorting, table.filter);
    }
}

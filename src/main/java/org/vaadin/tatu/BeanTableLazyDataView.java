package org.vaadin.tatu;

import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.BackEndDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.LazyDataView;
import com.vaadin.flow.function.SerializableSupplier;

public class BeanTableLazyDataView<T> extends AbstractDataView<T> implements LazyDataView<T> {

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
    public BeanTableLazyDataView(SerializableSupplier<? extends DataProvider<T, ?>> dataProviderSupplier, BeanTable<T> component) {
        super(dataProviderSupplier,component);
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
}

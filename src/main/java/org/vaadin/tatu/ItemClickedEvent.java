package org.vaadin.tatu;

import com.vaadin.flow.component.ComponentEvent;

@SuppressWarnings({ "serial", "rawtypes" })
public class ItemClickedEvent<R, C extends BeanTable>
        extends ComponentEvent<C> {
    private R item;

    public ItemClickedEvent(C source, R item,
            boolean fromClient) {
        super(source, fromClient);
        this.item = item;
    }

    public R getItem() {
        return item;
    }
}
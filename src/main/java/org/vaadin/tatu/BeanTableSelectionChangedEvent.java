package org.vaadin.tatu;

import java.util.Set;

import com.vaadin.flow.component.ComponentEvent;

@SuppressWarnings({ "serial", "rawtypes" })
public class BeanTableSelectionChangedEvent<R, C extends BeanTable>
        extends ComponentEvent<C> {
    private Set<R> selection;

    public BeanTableSelectionChangedEvent(C source, Set<R> selection,
            boolean fromClient) {
        super(source, fromClient);
        this.selection = selection;
    }

    public Set<R> getSelected() {
        return selection;
    }
}
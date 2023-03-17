package org.vaadin.tatu;

import java.util.Set;

import com.vaadin.flow.component.ComponentEvent;

@SuppressWarnings({ "serial", "rawtypes" })
public class SelectionChangedEvent<R, C extends BeanTable>
        extends ComponentEvent<C> {
    private Set<R> selection;

    public SelectionChangedEvent(C source, Set<R> selection,
            boolean fromClient) {
        super(source, fromClient);
        this.selection = selection;
    }

    public Set<R> getSelected() {
        return selection;
    }
}
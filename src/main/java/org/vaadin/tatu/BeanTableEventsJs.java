package org.vaadin.tatu;

import java.io.Serializable;

import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

@JsDefinition
interface BeanTableEventsJs extends Serializable {

    @JsExpression("return event.detail == 1;")
    DomEventFilter singleClick();

    @JsExpression("return [32, 33, 34].includes(event.keyCode);")
    DomEventFilter selectionOrPagingKey();
}

package org.vaadin.tatu;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.vaadin.tatu.BeanTable.BeanTableI18n;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("lazy")
public class LazyView extends VerticalLayout {

    public LazyView() {
        setSizeFull();
        BeanTable<Person> table = new BeanTable<>(Person.class, false, 20);
        PersonService personService = new PersonService();
        table.setColumns("firstName","lastName","age","phoneNumber","maritalStatus");
        table.addColumn("Postal Code",person -> person.getAddress() == null ? "" : person.getAddress().getPostalCode());
        table.addColumn("City",person -> person.getAddress() == null ? "" : person.getAddress().getCity());
        table.setI18n(BeanTableI18n.getDefault());
        
        BeanTableLazyDataView<Person> dataView = table.setItems(query -> personService
                        .fetch(query.getOffset(), query.getLimit(), null).stream(),query -> personService.count(null));
        table.setWidthFull();
        TextField filter = new TextField("Filter");
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(event -> {
            table.setItems(query -> personService
                    .fetch(query.getOffset(), query.getLimit(), event.getValue()).stream(),query -> personService.count(event.getValue()));
        });
        dataView.addItemCountChangeListener(event -> {
            Notification.show("Count: "+event.getItemCount());
        });
        add(filter,table);
    }

    public class PersonService {
        private PersonData personData = new PersonData();

        public List<Person> fetch(int offset, int limit, String filter) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            int end = offset + limit;
            int size = count(filter);
            if (size <= end) {
                end = size;
            }
            if (filter != null && !filter.isEmpty()) {
                return personData.getPersons().stream().filter(item -> {
                    return item.toString().toLowerCase().contains(filter.toLowerCase());
                }).collect(Collectors.toList()).subList(offset, end);
            } else {    
                return personData.getPersons().subList(offset, end);
            }
        }

        public Stream<Person> fetchPage(int page, int pageSize) {
            return personData.getPersons().stream().skip(page * pageSize)
                    .limit(pageSize);
        }

        public int count(String filter) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (filter != null && !filter.isEmpty()) {
                return personData.getPersons().stream().filter(item -> {
                    return item.toString().toLowerCase().contains(filter.toLowerCase());
                }).collect(Collectors.toList()).size();
            } else {    
                return personData.getPersons().size();
            }
        }

        public List<Person> fetchAll() {
            return personData.getPersons();
        }
    }
}

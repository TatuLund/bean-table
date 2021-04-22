package org.vaadin.tatu;

import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProviderWrapper;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;

@Route("lazy")
public class LazyView extends VerticalLayout {

    public LazyView() {
        setSizeFull();
        BeanTable<Person> table = new BeanTable<>(Person.class, false, 20);
        PersonService personService = new PersonService();
        DataProvider<Person, ?> dataProvider = DataProvider.fromCallbacks(
                query -> personService
                        .fetch(query.getOffset(), query.getLimit()).stream(),
                query -> personService.count());
        table.setColumns("firstName","lastName","age","phoneNumber","maritalStatus");
        table.addColumn("Postal Code",person -> person.getAddress() == null ? "" : person.getAddress().getPostalCode());
        table.addColumn("City",person -> person.getAddress() == null ? "" : person.getAddress().getCity());
        table.setDataProvider(dataProvider);
        table.setWidthFull();
        add(table);
    }

    public class PersonService {
        private PersonData personData = new PersonData();

        public List<Person> fetch(int offset, int limit) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            int end = offset + limit;
            int size = personData.getPersons().size();
            if (size <= end) {
                end = size;
            }
            return personData.getPersons().subList(offset, end);
        }

        public Stream<Person> fetchPage(int page, int pageSize) {
            return personData.getPersons().stream().skip(page * pageSize)
                    .limit(pageSize);
        }

        public int count() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            return personData.getPersons().size();
        }

        public List<Person> fetchAll() {
            return personData.getPersons();
        }
    }
}

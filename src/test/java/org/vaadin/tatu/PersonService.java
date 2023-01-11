package org.vaadin.tatu;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PersonService {
    private PersonData personData = new PersonData();

    public List<Person> fetch(int offset, int limit, Optional<String> filter) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        int end = offset + limit;
        int size = count(filter);
        if (size <= end) {
            end = size;
        }
        if (filter.isPresent() && !filter.get().isEmpty()) {
            return personData.getPersons().stream().filter(item -> {
                return item.toString().toLowerCase()
                        .contains(filter.get().toLowerCase());
            }).collect(Collectors.toList()).subList(offset, end);
        } else {
            return personData.getPersons().subList(offset, end);
        }
    }

    public Stream<Person> fetchPage(int page, int pageSize) {
        return personData.getPersons().stream().skip(page * pageSize)
                .limit(pageSize);
    }

    public int count(Optional<String> filter) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        if (filter.isPresent() && !filter.get().isEmpty()) {
            return personData.getPersons().stream().filter(item -> {
                return item.toString().toLowerCase()
                        .contains(filter.get().toLowerCase());
            }).collect(Collectors.toList()).size();
        } else {
            return personData.getPersons().size();
        }
    }

    public List<Person> fetchAll() {
        return personData.getPersons();
    }
}

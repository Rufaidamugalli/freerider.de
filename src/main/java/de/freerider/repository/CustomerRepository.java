package de.freerider.repository;

import de.freerider.datamodel.Customer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@Component
public class CustomerRepository implements CrudRepository<Customer, Long> {
    private HashMap<Long, Customer> customerList = new HashMap<Long, Customer>();
    Long count = 0L;

    @Override
    public <S extends Customer> S save(S entity) {
        customerList.put(entity.getId(), entity);
        count++;
        return entity;
    }

    @Override
    public <S extends Customer> Iterable<S> saveAll(Iterable<S> entities) {
        for (S entity : entities) {
            customerList.put(entity.getId(), entity);
            count++;

        }
        return entities;
    }

    @Override
    public boolean existsById(Long aLong) {
        if (customerList.containsKey(aLong)) {
            return true;
        }
        return false;
    }

    @Override
    public Optional<Customer> findById(Long aLong) {
        Customer customer = customerList.get(aLong);
        if (customer != null) {
            return Optional.of(customer);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Iterable<Customer> findAll() {
        return customerList.values();
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<Long> longs) {
        ArrayList<Customer> c_found = new ArrayList<>();
        longs.forEach(key -> {
            Customer c = customerList.get(key);
            if (c != null) {
                c_found.add(c);
            }
        });
        return c_found;
    }

    @Override
    public long count() {
        return this.count;
    }

    @Override
    public void deleteById(Long aLong) {
        customerList.remove(aLong);
        count--;
    }
    @Override
    public void delete(Customer entity) {
        customerList.remove(entity.getId(),entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {
        longs.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends Customer> entities) {
        entities.forEach(this::delete);

    }

    @Override
    public void deleteAll() {
        customerList.clear();

    }
}

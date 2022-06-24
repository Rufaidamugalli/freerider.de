package de.freerider.restapi;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;

import de.freerider.datamodel.Customer;
import de.freerider.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


@RestController
class CustomerController implements CustomersAPI {
    //
    @Autowired
    private ApplicationContext context;
    //
    private final ObjectMapper objectMapper;
    //
    private final HttpServletRequest request;

    @Autowired
    CustomerRepository repo;

    /**
     * Constructor.
     *
     * @param objectMapper entry point to JSON tree for the Jackson library
     * @param request      HTTP request object
     */
    public CustomerController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }


    /**
     * GET /customers
     * <p>
     * Return JSON Array of customers (compact).
     *
     * @return JSON Array of customers
     */
    @Override
    public ResponseEntity<List<?>> getCustomers() {
        //
        ResponseEntity<List<?>> re = null;
        System.err.println(request.getMethod() + " " + request.getRequestURI());
        try {
            ArrayNode arrayNode = customerAsJSON();
            ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ObjectNode>>() {
            });
            List<String> list = reader.readValue(arrayNode);
            //
            re = new ResponseEntity<List<?>>(list, HttpStatus.OK);

        } catch (IOException e) {
            re = new ResponseEntity<List<?>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return re;
    }

    @Override
    public ResponseEntity<?> getCustomer(long id) {
        //
        ResponseEntity<List<?>> re = null;
        System.err.println(request.getMethod() + " " + request.getRequestURI());
        try {
            ArrayNode arrayNode = customerAsJSON();
            ArrayNode foundCustomer = objectMapper.createArrayNode();
            for (int i = 0; i < arrayNode.size(); i++) {
                Long customerId = arrayNode.get(i).get("id").asLong();
                if (customerId == id) {
                    foundCustomer.add(arrayNode.get(i));
                }
            }
            ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ObjectNode>>() {
            });
            List<String> list = reader.readValue(foundCustomer);
            //
            if (list.size() == 0) {
                re = new ResponseEntity<List<?>>(HttpStatus.NOT_FOUND);
            } else {
                re = new ResponseEntity<List<?>>(list, HttpStatus.OK);
            }
        } catch (IOException e) {
            re = new ResponseEntity<List<?>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return re;
    }

    private Optional<Customer> accept(Map<String, Object> kvpairs) {
        Optional<Customer> customer = Optional.empty();
        if (kvpairs.containsKey("name") && kvpairs.containsKey("first")) {
            if (kvpairs.containsKey("id")) {
                Customer c = new Customer();
                c.setId(Long.parseLong(kvpairs.get("id").toString()));
                c.setName(kvpairs.get("first").toString(), kvpairs.get("name").toString());
                if (kvpairs.containsKey("contacts")) {
                    String[] contacts = kvpairs.get("contacts").toString().split(";");
                    for (String contact : contacts) {
                        c.addContact(contact);
                    }
                }
                customer = Optional.of(c);

            } else {
                for (long i = 0; i < repo.count() + 1; i++) {
                    if (repo.existsById(i)) {
                        continue;
                    } else {
                        Customer c = new Customer();
                        c.setId(i);
                        c.setName(kvpairs.get("first").toString(), kvpairs.get("name").toString());
                        if (kvpairs.containsKey("contacts")) {
                            String[] contacts = kvpairs.get("contacts").toString().split(";");
                            for (String contact : contacts) {
                                c.addContact(contact);
                            }
                        }
                        customer = Optional.of(c);
                    }
                }
            }
        }
        return customer;
    }

    @Override
    public ResponseEntity<List<?>> postCustomers(@RequestBody Map<String, Object>[] jsonMap) {
        System.err.println("POST /customers");
        if (jsonMap == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        //
        System.out.println("[{");
        for (Map<String, Object> kvpairs : jsonMap) {
            kvpairs.keySet().forEach(key -> {
                Object value = kvpairs.get(key);
                System.out.println("  [ " + key + ", " + value + " ]");
            });
        }
        System.out.println("}]");

        Optional<Customer> customer;
        for (Map<String, Object> kvpairs : jsonMap) {
            if (kvpairs.containsKey("id")) {
                if (repo.existsById(Long.parseLong(kvpairs.get("id").toString()))) {
                    ArrayNode arrayNode = objectMapper.createArrayNode();
                    Customer customerNode = repo.findById(Long.parseLong(kvpairs.get("id").toString())).get();
                    StringBuffer sb = new StringBuffer();
                    customerNode.getContacts().forEach(contact -> sb.append(sb.length() == 0 ? "" : "; ").append(contact));
                    arrayNode.add(objectMapper.createObjectNode().put("name", customerNode.getLastName())
                            .put("first", customerNode.getFirstName()).put("contacts", sb.toString()).put("id", customerNode.getId()));
                    ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ObjectNode>>() {
                    });
                    List<String> list = null;
                    try {
                        list = reader.readValue(arrayNode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new ResponseEntity<>(list, HttpStatus.CONFLICT);
                }
            }
            customer = accept(kvpairs);
            if (customer.isPresent()) {
                repo.save(customer.get());
            } else {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

//    @Override
//    public ResponseEntity<List<?>> postCustomers(Map<String, Object>[] jsonMap) {
//         System.err.println("POST /customers");
//        if (jsonMap == null)
//            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
////
//        System.out.println("[{");
//        for (Map<String, Object> kvpairs : jsonMap) {
//        kvpairs.keySet().forEach(key->{
//            Object value = kvpairs.get(key);
//            System.out.println("  [ " + key + ", " + value + " ]");
//        });
//    }
//        System.out.println("[{");
//        for (Map<String, Object> kvpairs : jsonMap) {
//            for (String key : kvpairs.keySet()) {
//                Optional<Customer> c = accept(kvpairs);
//                if (!c.isPresent()) {
//                    repo.save(c.get());
//                    return new ResponseEntity<>(null, HttpStatus.OK);
//                }
//                if (c.isPresent()){
//                    return new ResponseEntity<>(null, HttpStatus.CONFLICT);
//
//
//                }
//            }
//        }
//        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
//    }
//    @Override
//    public ResponseEntity<List<?>> postCustomers(Map<String, Object>[] jsonMap) {
//        System.err.println("POST /customers");
//        if (jsonMap == null)
//            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
////
//        System.out.println("[{");
//        for (Map<String, Object> kvpairs : jsonMap) {
//            kvpairs.keySet().forEach(key->{
//                    Object value = kvpairs.get(key);
//            System.out.println("  [ " + key + ", " + value + " ]");
//    });
//        }
//        System.out.println("}]");
//        return new ResponseEntity<>(null, HttpStatus.OK);
//    }

    @Override
    public ResponseEntity<List<?>> putCustomers(Map<String, Object>[] jsonMap) {
        List<Customer> cList = new ArrayList<Customer>();
        for (Map<String, Object> kvpairs_new : jsonMap) {
            for (String key : kvpairs_new.keySet()) {
                Optional<Customer> c = accept(kvpairs_new);
                if (c.isPresent()) {
                    Customer new_customer = c.get();
                    Optional<Customer> old_customer = repo.findById(new_customer.getId());
                    if(old_customer.isPresent()) {
                        Customer old_cus = old_customer.get();
                        old_cus.setName(new_customer.getName());
                        StringBuffer sb = new StringBuffer();
                        new_customer.getContacts().forEach(contact -> sb.append(sb.length() == 0 ? "" : "; ").append(contact));
                        old_cus.addContact(sb.toString());
                        repo.save(old_cus);
                        return new ResponseEntity<>(null, HttpStatus.OK);
                    }
                }
            }
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    //
    @Override
    public ResponseEntity<?> deleteCustomer(long id) {
        System.err.println("DELETE api/v1/customers/" + id);
        if (repo.existsById(id)) {
            repo.deleteById(id);
            System.out.println("customer " + id + " deleted.");
            return new ResponseEntity<>(null, HttpStatus.ACCEPTED); // status 202
        } else {
            System.err.println("customer " + id + " not found.");
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // status 404
        }
    }

    private ArrayNode customerAsJSON() {
        //
        ArrayNode arrayNode = objectMapper.createArrayNode();
        //
        Iterable<Customer> cList = repo.findAll();
        cList.forEach(c -> {
            StringBuffer sb = new StringBuffer();
            c.getContacts().forEach(contact -> sb.append(sb.length() == 0 ? "" : "; ").append(contact));
            arrayNode.add(
                    objectMapper.createObjectNode()
                            .put("name", c.getLastName())
                            .put("first", c.getFirstName())
                            .put("contacts", sb.toString())
                            .put("id", c.getId())
            );
        });
        return arrayNode;
    }

}
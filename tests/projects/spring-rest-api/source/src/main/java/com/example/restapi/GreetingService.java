package com.example.restapi;

import com.example.restapi.GreetingController.Greeting;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GreetingService {

    private final List<Greeting> greetings = new CopyOnWriteArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public List<Greeting> listAll() {
        return List.copyOf(greetings);
    }

    public Optional<Greeting> findById(int id) {
        return greetings.stream().filter(g -> g.id() == id).findFirst();
    }

    public Greeting create(String name, String message) {
        Greeting greeting = new Greeting(idCounter.getAndIncrement(), name, message);
        greetings.add(greeting);
        return greeting;
    }

    public boolean delete(int id) {
        return greetings.removeIf(g -> g.id() == id);
    }
}

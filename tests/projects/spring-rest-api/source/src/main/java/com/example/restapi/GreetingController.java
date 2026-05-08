package com.example.restapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping
    public List<Greeting> list() {
        return greetingService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Greeting> get(@PathVariable int id) {
        return greetingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Greeting> create(@Valid @RequestBody GreetingRequest request) {
        Greeting greeting = greetingService.create(request.name(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(greeting);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (greetingService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    public record Greeting(int id, String name, String message) {}

    public record GreetingRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 500) String message
    ) {}
}

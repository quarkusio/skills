package com.example.crud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void crudOperations() {
        // Create
        Todo todo = new Todo("Buy milk", "From the store");
        ResponseEntity<Todo> createResponse = restTemplate.postForEntity("/api/todos", todo, Todo.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long id = createResponse.getBody().getId();
        assertNotNull(id);
        assertEquals("Buy milk", createResponse.getBody().getTitle());

        // Read
        ResponseEntity<Todo> getResponse = restTemplate.getForEntity("/api/todos/" + id, Todo.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Buy milk", getResponse.getBody().getTitle());

        // Update
        Todo updated = new Todo("Buy oat milk", "From the organic store");
        updated.setCompleted(true);
        ResponseEntity<Todo> updateResponse = restTemplate.exchange(
                "/api/todos/" + id, HttpMethod.PUT, new HttpEntity<>(updated), Todo.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Buy oat milk", updateResponse.getBody().getTitle());
        assertTrue(updateResponse.getBody().isCompleted());

        // List
        ResponseEntity<Todo[]> listResponse = restTemplate.getForEntity("/api/todos", Todo[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertTrue(listResponse.getBody().length >= 1);

        // Filter by completed
        ResponseEntity<Todo[]> completedResponse =
                restTemplate.getForEntity("/api/todos?completed=true", Todo[].class);
        assertEquals(HttpStatus.OK, completedResponse.getStatusCode());

        // Search
        ResponseEntity<Todo[]> searchResponse =
                restTemplate.getForEntity("/api/todos/search?keyword=oat", Todo[].class);
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertTrue(searchResponse.getBody().length >= 1);

        // Delete
        restTemplate.delete("/api/todos/" + id);
        ResponseEntity<Todo> afterDelete = restTemplate.getForEntity("/api/todos/" + id, Todo.class);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }

    @Test
    void createWithInvalidInput() {
        Todo todo = new Todo("", null);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/todos", todo, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getNotFound() {
        ResponseEntity<Todo> response = restTemplate.getForEntity("/api/todos/9999", Todo.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

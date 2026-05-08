package com.example.restapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void createAndListGreetings() {
        // Create a greeting
        var request = new GreetingController.GreetingRequest("Alice", "Hello World");
        ResponseEntity<GreetingController.Greeting> createResponse =
                restTemplate.postForEntity("/api/greetings", request, GreetingController.Greeting.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals("Alice", createResponse.getBody().name());

        // List greetings
        ResponseEntity<GreetingController.Greeting[]> listResponse =
                restTemplate.getForEntity("/api/greetings", GreetingController.Greeting[].class);

        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertTrue(listResponse.getBody().length >= 1);
    }

    @Test
    void getNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/greetings/9999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createWithInvalidInput() {
        var request = new GreetingController.GreetingRequest("", "");
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/greetings", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

package com.coffeeshop.counter_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Boot an empty context: no component scan, no app class
@SpringBootTest(classes = EmptyTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CounterServiceApplicationTests {
  @Test void contextLoads() {}
}

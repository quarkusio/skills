package io.github.raeperd.realworld.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Void> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(UNPROCESSABLE_ENTITY).build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Void> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(NOT_FOUND).build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Void> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(CONFLICT).build();
    }
}

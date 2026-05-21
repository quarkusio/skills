package com.example.crud;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TodoService {

    private final TodoRepository repository;

    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    public List<Todo> findAll() {
        return repository.findAll();
    }

    public Optional<Todo> findById(Long id) {
        return repository.findById(id);
    }

    public List<Todo> findByCompleted(boolean completed) {
        return repository.findByCompleted(completed);
    }

    public List<Todo> search(String keyword) {
        return repository.searchByTitle(keyword);
    }

    public Todo create(Todo todo) {
        return repository.save(todo);
    }

    public Optional<Todo> update(Long id, Todo updated) {
        return repository.findById(id).map(todo -> {
            todo.setTitle(updated.getTitle());
            todo.setDescription(updated.getDescription());
            todo.setCompleted(updated.isCompleted());
            return repository.save(todo);
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}

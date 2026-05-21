package com.example.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCompleted(boolean completed);

    @Query("SELECT t FROM Todo t WHERE t.title LIKE %:keyword%")
    List<Todo> searchByTitle(String keyword);
}

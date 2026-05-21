package com.todo.app.service;

import com.todo.app.entity.Task;
import com.todo.app.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

  @Autowired
  private TaskRepository taskRepository;

  @Override
  public void addTask(Task task) {
    taskRepository.save(task);
  }

  @Override
  public void deleteTaskById(Long id) {
    taskRepository.deleteById(id);
  }

  @Override
  public void updateTaskById(Long id, Task task) {
    taskRepository.save(task);
  }

  @Override
  public List<Task> getAllTasks() {
    return taskRepository.findAll();
  }

  @Override
  public void deleteTask(Long taskId) {
    taskRepository.deleteById(taskId);
  }

  @Override
  public Page<Task> getAllTasksPage(int pageNo, int pageSize) {
    Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
    return taskRepository.findAll(pageable);
  }

}

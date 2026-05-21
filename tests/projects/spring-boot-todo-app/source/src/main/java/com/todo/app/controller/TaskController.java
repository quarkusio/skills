package com.todo.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.data.domain.Page;

import com.todo.app.entity.Task;
import com.todo.app.service.TaskService;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/error")
    public String viewErrorPage() {
        return "error";
    }

    @GetMapping("/")
    public String viewIndexPage() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String viewHome(Model model) {
        model.addAttribute("task", new Task());
        return findPaginated(1, model);
    }

    @GetMapping("/home/{pageNo}")
    public String findPaginated(@PathVariable(value = "pageNo") int pageNo, Model model) {

        model.addAttribute("task", new Task());

        int pageSize = 6;

        Page<Task> page = taskService.getAllTasksPage(pageNo, pageSize);
        List<Task> tasks = page.getContent();

        if (tasks.isEmpty()) {
            model.addAttribute("noTasks", true);
            if (pageNo > 1) {
                return "redirect:/home/" + (pageNo - 1);
            }
        } else {
            model.addAttribute("currentPage", pageNo);
            model.addAttribute("totalPages", page.getTotalPages());
            model.addAttribute("totalItems", page.getTotalElements());
            model.addAttribute("tasks", tasks);

            if (pageNo > page.getTotalPages()) {
                return "redirect:/home/" + page.getTotalPages();
            }
            if (pageNo < 1) {
                return "redirect:/home";
            }
        }

        return "home";
    }

    // Create task using AJAX request
    @PostMapping("/home")
    @ResponseBody
    public ResponseEntity<List<Task>> createTask(@RequestBody Task task) {
        taskService.addTask(task);
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    // Delete task using AJAX request
    @DeleteMapping("/home/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok().build();
    }

}
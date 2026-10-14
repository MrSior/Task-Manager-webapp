package com.taskmanager.service;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.User;
import com.taskmanager.dto.CreateTaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.dto.UpdateTaskRequest;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.security.AuthContext;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        Long userId = requireUserId();
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User user = requireUser();
        Task task = new Task();
        task.setUser(user);
        task.setTitle(request.getTitle().trim());
        task.setDescription(normalizeDescription(request.getDescription()));
        task.setStatus(TaskStatus.NEW);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Long userId = requireUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank");
            }
            task.setTitle(title);
        }
        if (request.getDescription() != null) {
            task.setDescription(normalizeDescription(request.getDescription()));
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        Long userId = requireUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        taskRepository.delete(task);
    }

    private User requireUser() {
        User user = AuthContext.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user;
    }

    private Long requireUserId() {
        return requireUser().getId();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }
}

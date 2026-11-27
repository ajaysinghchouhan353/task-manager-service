package service;

import event.TaskEventListener;
import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.Priority;
import model.Task;
import model.TaskRequest;
import repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private final TaskRepository repository;
    private final List<TaskEventListener> listeners = new ArrayList<>();

    public TaskService(TaskRepository repository) { this.repository = repository; }

    public void addListener(TaskEventListener listener) { listeners.add(listener); }
    public void removeListener(TaskEventListener listener) { listeners.remove(listener); }

    public void validate(TaskRequest request) {
        List<FieldIssue> issues = new ArrayList<>();
        if (request == null) {
            issues.add(new FieldIssue("request", "must not be null"));
            throw new ValidationException(issues);
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty())
            issues.add(new FieldIssue("title", "must not be empty"));
        if (request.getDescription() == null || request.getDescription().trim().isEmpty())
            issues.add(new FieldIssue("description", "must not be empty"));
        if (request.getCompleted() == null ||
                (!request.getCompleted().equals("true") && !request.getCompleted().equals("false")))
            issues.add(new FieldIssue("completed", "must be exactly true or false"));
        if (request.getDueDate() != null && !request.getDueDate().isBlank()
                && !DateHelper.isValidDate(request.getDueDate()))
            issues.add(new FieldIssue("dueDate", "must be a real date in YYYY-MM-DD format"));
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            try { Priority.valueOf(request.getPriority()); }
            catch (IllegalArgumentException e) {
                issues.add(new FieldIssue("priority", "must be LOW, MEDIUM or HIGH"));
            }
        }
        if (!issues.isEmpty()) throw new ValidationException(issues);
    }

    public Task create(TaskRequest request, Integer parentId) {
        validate(request);
        Priority priority = request.getPriority() == null || request.getPriority().isBlank()
                ? Priority.MEDIUM : Priority.valueOf(request.getPriority());
        String dueDate = request.getDueDate() == null || request.getDueDate().isBlank()
                ? null : request.getDueDate().trim();
        Task task = Task.builder().title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .completed(Boolean.parseBoolean(request.getCompleted().trim()))
                .dueDate(dueDate).priority(priority).parentId(parentId).build();
        Task saved = repository.save(task);
        notify("TASK_CREATED", saved);
        return saved;
    }

    public Task update(int id, TaskRequest request) {
        Task existing = require(id);
        validate(request);
        Priority priority = request.getPriority() == null || request.getPriority().isBlank()
                ? Priority.MEDIUM : Priority.valueOf(request.getPriority());
        String dueDate = request.getDueDate() == null || request.getDueDate().isBlank()
                ? null : request.getDueDate().trim();
        Task updated = Task.builder().id(id).title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .completed(Boolean.parseBoolean(request.getCompleted().trim()))
                .dueDate(dueDate).priority(priority).parentId(existing.getParentId())
                .createdAt(existing.getCreatedAt()).build();
        Task saved = repository.save(updated);
        notify(saved.isCompleted() ? "TASK_COMPLETED" : "TASK_UPDATED", saved);
        return saved;
    }

    public Task require(int id) {
        Task task = repository.findById(id);
        if (task == null) throw new NotFoundException("Task " + id + " not found");
        return task;
    }

    public boolean delete(int id) {
        Task task = require(id);
        if (task.getParentId() == null) {
            for (Task candidate : repository.findAll()) {
                if (Integer.valueOf(id).equals(candidate.getParentId())) {
                    repository.delete(candidate.getId());
                    notify("TASK_DELETED", candidate);
                }
            }
        }
        boolean deleted = repository.delete(id);
        notify("TASK_DELETED", task);
        return deleted;
    }

    private void notify(String eventType, Task task) {
        for (TaskEventListener listener : new ArrayList<>(listeners)) {
            listener.onEvent(eventType, task);
        }
    }
}

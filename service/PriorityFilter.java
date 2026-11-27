package service;

import model.Priority;
import model.Task;
import java.util.ArrayList;
import java.util.List;

public class PriorityFilter implements TaskFilter {
    private final Priority priority;
    public PriorityFilter(Priority priority) { this.priority = priority; }
    @Override
    public List<Task> apply(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) if (task.getPriority() == priority) result.add(task);
        return result;
    }
}

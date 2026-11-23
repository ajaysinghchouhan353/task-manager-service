package service;

import model.Task;
import java.util.ArrayList;
import java.util.List;

public class CompletedFilter implements TaskFilter {
    private final boolean completed;
    public CompletedFilter(boolean completed) { this.completed = completed; }
    @Override
    public List<Task> apply(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) if (task.isCompleted() == completed) result.add(task);
        return result;
    }
}

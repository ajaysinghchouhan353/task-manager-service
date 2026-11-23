package assignment_task_manager.service;

import assignment_task_manager.model.Task;
import java.util.List;

public interface SortStrategy {
    void sort(List<Task> tasks, String order);
}

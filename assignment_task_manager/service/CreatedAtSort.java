package assignment_task_manager.service;

import assignment_task_manager.model.Task;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CreatedAtSort implements SortStrategy {
    @Override
    public void sort(List<Task> tasks, String order) {
        Collections.sort(tasks, Comparator.comparingLong(Task::getCreatedAt));
        if ("desc".equalsIgnoreCase(order)) Collections.reverse(tasks);
    }
}

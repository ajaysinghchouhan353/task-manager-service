package assignment_task_manager.service;

import assignment_task_manager.model.Task;
import java.util.List;

public interface TaskFilter {
    List<Task> apply(List<Task> tasks);
}

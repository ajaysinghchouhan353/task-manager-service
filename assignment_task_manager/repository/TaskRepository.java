package assignment_task_manager.repository;

import assignment_task_manager.model.Task;
import java.util.List;

public interface TaskRepository {
    Task save(Task task);
    Task findById(int id);
    List<Task> findAll();
    boolean delete(int id);
    void clear();
}

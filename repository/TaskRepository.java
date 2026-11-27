package repository;

import model.Task;
import java.util.List;

public interface TaskRepository {
    Task save(Task task);
    Task findById(int id);
    List<Task> findAll();
    boolean delete(int id);
    void clear();
}

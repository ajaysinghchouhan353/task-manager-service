package service;

import model.Task;
import java.util.List;

public interface TaskFilter {
    List<Task> apply(List<Task> tasks);
}

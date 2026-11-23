package assignment_task_manager.service;

import assignment_task_manager.model.Task;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DueDateSort implements SortStrategy {
    @Override
    public void sort(List<Task> tasks, String order) {
        Collections.sort(tasks, (a, b) -> {
            String ad = a.getDueDate();
            String bd = b.getDueDate();
            if (ad == null && bd == null) return 0;
            if (ad == null) return 1;
            if (bd == null) return -1;
            return ad.compareTo(bd);
        });
        if ("desc".equalsIgnoreCase(order)) Collections.reverse(tasks);
    }
}

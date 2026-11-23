package service;

import model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFilter implements TaskFilter {
    private final String search;
    public SearchFilter(String search) { this.search = search == null ? "" : search.toLowerCase(Locale.ROOT); }
    @Override
    public List<Task> apply(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            String title = task.getTitle() == null ? "" : task.getTitle().toLowerCase(Locale.ROOT);
            String description = task.getDescription() == null ? "" : task.getDescription().toLowerCase(Locale.ROOT);
            if (title.contains(search) || description.contains(search)) result.add(task);
        }
        return result;
    }
}

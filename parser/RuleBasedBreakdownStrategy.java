package parser;

import model.ParsedSubtask;
import java.util.ArrayList;
import java.util.List;

public class RuleBasedBreakdownStrategy implements BreakdownStrategy {
    @Override
    public List<ParsedSubtask> breakDown(String taskTitle, String taskDescription) {
        List<ParsedSubtask> result = new ArrayList<>();
        String[] parts = taskTitle.split("\\s+(?:and|,|&)\\s+");
        if (parts.length <= 1) parts = new String[]{ "Plan " + taskTitle, "Complete " + taskTitle };
        for (String part : parts) {
            String title = part.trim();
            if (!title.isEmpty()) result.add(new ParsedSubtask(title, "Subtask for " + taskTitle));
        }
        return result;
    }
}

package parser;

import model.ParsedTask;
import java.time.LocalDate;
import java.util.Locale;

public class RuleBasedTaskParser implements TaskParser {
    @Override
    public ParsedTask parse(String text, String today) {
        String title = text == null ? "" : text.trim();
        if (title.length() > 80) title = title.substring(0, 80);
        String lower = title.toLowerCase(Locale.ROOT);
        String dueDate = null;
        if (lower.contains("tomorrow")) {
            dueDate = LocalDate.parse(today).plusDays(1).toString();
        }
        String recurrence = lower.contains("every month") || lower.contains("monthly") ? "monthly" : null;
        if (lower.contains("1st") && dueDate == null) {
            LocalDate now = LocalDate.parse(today);
            dueDate = now.withDayOfMonth(1).toString();
        }
        return new ParsedTask(title, title, dueDate, recurrence);
    }
}

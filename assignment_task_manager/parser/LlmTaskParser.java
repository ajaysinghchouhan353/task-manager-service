package assignment_task_manager.parser;

import assignment_task_manager.model.ParsedTask;
import java.io.IOException;
import java.util.Map;

public class LlmTaskParser implements TaskParser {
    private final LlmClient client;

    public LlmTaskParser(LlmClient client) { this.client = client; }

    @Override
    public ParsedTask parse(String text, String today) {
        String prompt = "Today's date is " + today + " (format YYYY-MM-DD).\n"
                + "Turn the user's text into ONE task.\n"
                + "Reply with ONLY a JSON object, no explanation and no code fences, with exactly "
                + "these fields: title, description, dueDate, recurrence.\n"
                + "dueDate must be YYYY-MM-DD or null. recurrence must be a short string or null.\n"
                + "User text: " + text;
        try {
            String reply = client.send(prompt);
            Map<String, String> fields = SimpleJson.parseFlat(reply);
            return new ParsedTask(fields.get("title"), fields.get("description"),
                    fields.get("dueDate"), fields.get("recurrence"));
        } catch (IOException e) {
            throw new IllegalStateException("LLM call failed", e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("LLM returned invalid JSON", e);
        }
    }
}

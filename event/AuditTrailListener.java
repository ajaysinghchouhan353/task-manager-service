package event;

import model.Task;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditTrailListener implements TaskEventListener {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    @Override
    public void onEvent(String eventType, Task task) {
        counts.put(eventType, counts.getOrDefault(eventType, 0) + 1);
    }
    public int getCount(String eventType) { return counts.getOrDefault(eventType, 0); }
}

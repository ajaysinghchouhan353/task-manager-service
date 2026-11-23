package assignment_task_manager.repository;

import assignment_task_manager.api.ApiResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IdempotencyStore {
    private static final IdempotencyStore INSTANCE = new IdempotencyStore();
    private final Map<String, Record> records = Collections.synchronizedMap(new LinkedHashMap<>());

    private IdempotencyStore() {}
    public static IdempotencyStore getInstance() { return INSTANCE; }

    public synchronized Record find(String key) { return records.get(key); }

    public synchronized void save(String key, String fingerprint, ApiResponse response) {
        records.put(key, new Record(fingerprint, response));
    }

    public synchronized ReplayResult replayOrReserve(String key, String fingerprint) {
        Record seen = records.get(key);
        if (seen == null) {
            return ReplayResult.newRequest();
        }
        if (seen.getFingerprint().equals(fingerprint)) {
            return ReplayResult.replay(seen.getResponse());
        }
        return ReplayResult.conflict();
    }

    public synchronized void clear() { records.clear(); }

    public static final class ReplayResult {
        private final ApiResponse response;
        private final boolean conflict;

        private ReplayResult(ApiResponse response, boolean conflict) {
            this.response = response;
            this.conflict = conflict;
        }

        public static ReplayResult newRequest() { return new ReplayResult(null, false); }
        public static ReplayResult replay(ApiResponse response) { return new ReplayResult(response, false); }
        public static ReplayResult conflict() { return new ReplayResult(null, true); }

        public boolean isNewRequest() { return response == null && !conflict; }
        public boolean isConflict() { return conflict; }
        public ApiResponse getResponse() { return response; }
    }

    public static final class Record {
        private final String fingerprint;
        private final ApiResponse response;
        public Record(String fingerprint, ApiResponse response) {
            this.fingerprint = fingerprint;
            this.response = response;
        }
        public String getFingerprint() { return fingerprint; }
        public ApiResponse getResponse() { return response; }
    }
}

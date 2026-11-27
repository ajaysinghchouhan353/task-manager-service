package api;

import exception.ApiException;
import exception.BadGatewayException;
import exception.FieldIssue;
import exception.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * INSTRUCTOR-PROVIDED SIGNATURES ONLY (Step 3) - students write the bodies.
 *
 * The one place where the error shape is built. Target shape:
 *
 * {
 *   "error": {
 *     "code": "VALIDATION_ERROR",
 *     "message": "Validation failed",
 *     "details": [ { "field": "title", "issue": "must not be empty" } ]
 *   }
 * }
 *
 * Hints:
 *  - Build the body with LinkedHashMap / ArrayList (see ListMapEnumPrimer).
 *  - "code" and "message" are ALWAYS Strings.
 *  - "details" is only filled for ValidationException (use instanceof);
 *    for every other exception send an empty list.
 *  - The ApiResponse status comes from e.getStatus().
 *
 * When you copy this into your own project, change the package line to: package api;
 */
public class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    /** Known failure (400 / 404 / 422 / 502 / 503). */
    public static ApiResponse from(ApiException e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", e.getCode());
        error.put("message", e.getMessage());
        List<Map<String, String>> details = new ArrayList<>();
        if (e instanceof ValidationException validation) {
            for (FieldIssue issue : validation.getIssues()) details.add(detail(issue));
        } else if (e instanceof BadGatewayException gateway) {
            for (FieldIssue issue : gateway.getIssues()) details.add(detail(issue));
        }
        error.put("details", details);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        return new ApiResponse(e.getStatus(), body);
    }

    /** Anything unexpected: 500, code "INTERNAL_ERROR", same shape. Never leak a stack trace. */
    public static ApiResponse fromUnexpected(Exception e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "INTERNAL_ERROR");
        error.put("message", "Internal server error");
        error.put("details", new ArrayList<>());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        return new ApiResponse(500, body);
    }

    private static Map<String, String> detail(FieldIssue issue) {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("field", issue.getField());
        detail.put("issue", issue.getIssue());
        return detail;
    }
}

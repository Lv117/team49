package cn.edu.sdu.java.server.exception;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    public static final String STUDENT_NUM_REQUIRED = "STUDENT_NUM_REQUIRED";
    public static final String STUDENT_NUM_CONFLICT = "STUDENT_NUM_CONFLICT";
    public static final String STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";
    public static final String STUDENT_VALIDATION_ERROR = "STUDENT_VALIDATION_ERROR";
    public static final String STUDENT_SAVE_TIMEOUT = "STUDENT_SAVE_TIMEOUT";
    public static final String STUDENT_SAVE_DB_ERROR = "STUDENT_SAVE_DB_ERROR";
    public static final String STUDENT_USER_TYPE_MISSING = "STUDENT_USER_TYPE_MISSING";
}

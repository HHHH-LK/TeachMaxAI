package com.aiproject.smartcampus.pojo.enums;

public enum NotificationType implements BaseEnum<String> {
    SYSTEM("system", "系统通知"),
    COURSE("course", "课程通知"),
    EXAM("exam", "考试通知"),
    GENERAL("general", "普通通知");

    private final String code;
    private final String description;

    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

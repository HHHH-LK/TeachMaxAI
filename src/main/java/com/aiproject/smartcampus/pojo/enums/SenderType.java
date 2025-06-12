package com.aiproject.smartcampus.pojo.enums;

public enum SenderType implements BaseEnum<String> {
    ADMIN("admin", "管理员"),
    TEACHER("teacher", "教师");

    private final String code;
    private final String description;

    SenderType(String code, String description) {
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

package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.pojo.enums.NotificationType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import java.sql.*;


@MappedTypes(NotificationType.class)
public class NotificationTypeHandler extends BaseTypeHandler<NotificationType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, NotificationType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public NotificationType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return codeOf(rs.getString(columnName));
    }

    @Override
    public NotificationType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return codeOf(rs.getString(columnIndex));
    }

    @Override
    public NotificationType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return codeOf(cs.getString(columnIndex));
    }

    private NotificationType codeOf(String code) {
        if (code == null) return null;
        for (NotificationType value : NotificationType.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown code for NotificationType: " + code);
    }
}

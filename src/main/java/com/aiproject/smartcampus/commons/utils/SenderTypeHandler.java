package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.pojo.enums.SenderType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;


@MappedTypes(SenderType.class)
public class SenderTypeHandler extends BaseTypeHandler<SenderType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, SenderType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public SenderType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return codeOf(rs.getString(columnName));
    }

    @Override
    public SenderType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return codeOf(rs.getString(columnIndex));
    }

    @Override
    public SenderType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return codeOf(cs.getString(columnIndex));
    }

    private SenderType codeOf(String code) {
        if (code == null) {
            return null;
        }
        for (SenderType value : SenderType.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown code for SenderType: " + code);
    }
}

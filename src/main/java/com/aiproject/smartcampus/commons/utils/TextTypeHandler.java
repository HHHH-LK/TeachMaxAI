package com.aiproject.smartcampus.commons.utils; /**
 * 最终解决方案：专门处理TEXT字段的TypeHandler
 */

import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.io.Reader;
import java.io.StringWriter;
import java.io.IOException;

/**
 * 专门处理TEXT/BLOB字段的TypeHandler
 * 解决MyBatis显示<<BLOB>>的问题
 */
@MappedJdbcTypes({JdbcType.BLOB, JdbcType.LONGVARCHAR, JdbcType.CLOB})
@MappedTypes(String.class)
public class TextTypeHandler implements TypeHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(TextTypeHandler.class);

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.LONGVARCHAR);
        } else {
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        return readAsString(rs, columnName);
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        return readAsString(rs, columnIndex);
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readAsString(cs, columnIndex);
    }

    /**
     * 从ResultSet读取字符串（按列名）
     */
    private String readAsString(ResultSet rs, String columnName) throws SQLException {
        try {
            // 方法1：直接尝试getString
            String directResult = rs.getString(columnName);
            if (directResult != null && !directResult.isEmpty()) {
                log.debug("Successfully read {} using getString(): {} chars",
                        columnName, directResult.length());
                return directResult;
            }

            // 方法2：处理CLOB
            Clob clob = rs.getClob(columnName);
            if (clob != null) {
                String result = readClob(clob);
                log.debug("Successfully read {} using getClob(): {} chars",
                        columnName, result != null ? result.length() : 0);
                return result;
            }

            // 方法3：处理BLOB
            Blob blob = rs.getBlob(columnName);
            if (blob != null) {
                String result = readBlob(blob);
                log.debug("Successfully read {} using getBlob(): {} chars",
                        columnName, result != null ? result.length() : 0);
                return result;
            }

            // 方法4：作为二进制数据读取
            byte[] bytes = rs.getBytes(columnName);
            if (bytes != null && bytes.length > 0) {
                String result = new String(bytes, StandardCharsets.UTF_8);
                log.debug("Successfully read {} using getBytes(): {} chars",
                        columnName, result.length());
                return result;
            }

            log.debug("Column {} is null or empty", columnName);
            return null;

        } catch (Exception e) {
            log.error("Error reading column {}: {}", columnName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从ResultSet读取字符串（按索引）
     */
    private String readAsString(ResultSet rs, int columnIndex) throws SQLException {
        try {
            String directResult = rs.getString(columnIndex);
            if (directResult != null && !directResult.isEmpty()) {
                return directResult;
            }

            Clob clob = rs.getClob(columnIndex);
            if (clob != null) {
                return readClob(clob);
            }

            Blob blob = rs.getBlob(columnIndex);
            if (blob != null) {
                return readBlob(blob);
            }

            byte[] bytes = rs.getBytes(columnIndex);
            if (bytes != null && bytes.length > 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }

            return null;
        } catch (Exception e) {
            log.error("Error reading column index {}: {}", columnIndex, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从CallableStatement读取字符串
     */
    private String readAsString(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            String directResult = cs.getString(columnIndex);
            if (directResult != null && !directResult.isEmpty()) {
                return directResult;
            }

            Clob clob = cs.getClob(columnIndex);
            if (clob != null) {
                return readClob(clob);
            }

            Blob blob = cs.getBlob(columnIndex);
            if (blob != null) {
                return readBlob(blob);
            }

            return null;
        } catch (Exception e) {
            log.error("Error reading callable statement parameter {}: {}", columnIndex, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 读取CLOB数据
     */
    private String readClob(Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }

        try {
            long length = clob.length();
            if (length == 0) {
                return "";
            }

            // 对于大型CLOB，限制读取长度
            if (length > Integer.MAX_VALUE) {
                log.warn("CLOB too large ({}), reading first {} characters", length, Integer.MAX_VALUE);
                length = Integer.MAX_VALUE;
            }

            return clob.getSubString(1, (int) length);

        } catch (Exception e) {
            // 备用方法：使用Reader
            try (Reader reader = clob.getCharacterStream();
                 StringWriter writer = new StringWriter()) {

                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = reader.read(buffer)) > 0) {
                    writer.write(buffer, 0, charsRead);
                }
                return writer.toString();

            } catch (IOException ioException) {
                log.error("Failed to read CLOB using both methods", e);
                throw new SQLException("Failed to read CLOB", e);
            }
        }
    }

    /**
     * 读取BLOB数据
     */
    private String readBlob(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }

        try {
            long length = blob.length();
            if (length == 0) {
                return "";
            }

            if (length > Integer.MAX_VALUE) {
                log.warn("BLOB too large ({}), reading first {} bytes", length, Integer.MAX_VALUE);
                length = Integer.MAX_VALUE;
            }

            byte[] bytes = blob.getBytes(1, (int) length);
            return new String(bytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to read BLOB", e);
            throw new SQLException("Failed to read BLOB", e);
        }
    }
}
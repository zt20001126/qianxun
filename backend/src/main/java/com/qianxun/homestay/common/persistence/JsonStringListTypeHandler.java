package com.qianxun.homestay.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeException;

/** 将 MySQL JSON 数组映射为 Java 字符串列表。 */
@MappedTypes(List.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.OTHER})
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType jdbcType)
            throws SQLException {
        try {
            statement.setString(index, OBJECT_MAPPER.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            throw new TypeException("无法将字符串列表写入 JSON 字段", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private List<String> parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new TypeException("数据库 JSON 字段不是字符串数组", exception);
        }
    }
}

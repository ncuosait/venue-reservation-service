package tw.edu.ncu.osa.venue_reservation_service.util;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 逗號分隔字符串轉換類型處理器
 * 用於在資料庫（VARCHAR 存儲逗號分隔設備名稱）與 Java 物件（List<String> 存儲設備清單）之間進行轉換
 */
public class CommaSeparatedListTypeHandler extends BaseTypeHandler<List<String>> {

    /**
     * 設置 PreparedStatement 參數（Java → SQL）
     * 將 List<String> 轉換為逗號分隔的字符串
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, null);
        } else {
            String commaSeparated = String.join(",", parameter);
            ps.setString(i, commaSeparated);
        }
    }

    /**
     * 從 ResultSet 中獲取值（SQL → Java）
     * 將逗號分隔的字符串轉換為 List<String>
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parseCommaSeparatedString(value);
    }

    /**
     * 從 ResultSet 中按索引獲取值（SQL → Java）
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parseCommaSeparatedString(value);
    }

    /**
     * 從 CallableStatement 中獲取值（SQL → Java）
     */
    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parseCommaSeparatedString(value);
    }

    /**
     * 解析逗號分隔字符串為 List<String>
     * @param value 逗號分隔的字符串，例如 "麥克風,投影機,音響"
     * @return 設備清單，若輸入為空則返回空 List
     */
    private List<String> parseCommaSeparatedString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.split(","));
    }
}


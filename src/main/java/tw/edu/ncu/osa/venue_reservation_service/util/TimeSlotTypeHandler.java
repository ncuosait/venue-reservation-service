package tw.edu.ncu.osa.venue_reservation_service.util;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 時段位元遮罩類型處理器
 * 用於在資料庫（Integer 存儲時段位元遮罩）與 Java 物件（List<Integer> 存儲時段號列表）之間進行轉換
 */
public class TimeSlotTypeHandler extends BaseTypeHandler<List<Integer>> {

    /**
     * 設置 PreparedStatement 參數（Java → SQL）
     * 將 List<Integer> 轉換為位元遮罩整數
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) throws SQLException {
        int mask = BookingUtils.convertToMask(parameter);
        ps.setInt(i, mask);
    }

    /**
     * 從 ResultSet 中獲取值（SQL → Java）
     * 將位元遮罩整數轉換為 List<Integer>
     */
    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int mask = rs.getInt(columnName);
        return BookingUtils.parseMaskToList(mask);
    }

    /**
     * 從 ResultSet 中按索引獲取值（SQL → Java）
     */
    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int mask = rs.getInt(columnIndex);
        return BookingUtils.parseMaskToList(mask);
    }

    /**
     * 從 CallableStatement 中獲取值（SQL → Java）
     */
    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int mask = cs.getInt(columnIndex);
        return BookingUtils.parseMaskToList(mask);
    }
}


package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String userId;   // 學號或工號
    private String name;     // 姓名
    private String role;     // STUDENT, STAFF, ADMIN
    private Long unitId;     // 所屬單位 ID
}

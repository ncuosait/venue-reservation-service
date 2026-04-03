package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BookingRequestDTO {

    @NotNull(message = "場地 ID 不可為空")
    private Long venueId;

    @NotNull(message = "預約日期不可為空")
    @FutureOrPresent(message = "預約日期不能是過去的時間")
    private LocalDate bookingDate;

    @NotEmpty(message = "請至少選擇一個預約時段")
    private List<@Min(0) @Max(23) Integer> slots; // 24 小時制，對應 0-23 的索引

    @NotBlank(message = "請填寫使用用途")
    @Size(max = 255, message = "用途描述過長")
    private String purpose;

    @Min(value = 1, message = "預估人數至少需為 1 人")
    private Integer participantCount;

    @Valid
    @NotNull(message = "聯絡資訊不可為空")
    private ContactDTO contactInfo;

    // 可借 可不借
    private List<Long> equipmentIds; // 儲存設備 ID 列表

    @Data
    public static class ContactDTO {
        @NotBlank(message = "聯絡人姓名不可為空")
        private String name;

        @Email(message = "電子郵件格式不正確")
        @NotBlank(message = "電子郵件不可為空")
        private String email;

        @NotBlank(message = "聯絡電話不可為空")
        private String phone;
    }
}

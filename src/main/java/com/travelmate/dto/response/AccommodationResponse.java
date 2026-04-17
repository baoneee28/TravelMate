package com.travelmate.dto.response;

import com.travelmate.enums.PropertyType;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) dùng để truyền thông tin accommodation
 * từ Service → Controller → Thymeleaf template.
 *
 * Tại sao dùng DTO thay vì truyền thẳng entity?
 * - Tách biệt layer: template chỉ nhận đúng data cần, không expose toàn bộ entity
 * - An toàn hơn: không vô tình expose field nhạy cảm (vd: partner password)
 * - Thêm field computed: priceFormatted (string đã format) để template không cần tính
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationResponse {

    /** ID dùng để build URL: /accommodations/{id} */
    private Long id;

    /** Tên nơi lưu trú. Ví dụ: "Khách Sạn Bình Minh Đà Nẵng" */
    private String name;

    /** Enum loại lưu trú: HOTEL | HOMESTAY | VILLA | APARTMENT */
    private PropertyType propertyType;

    /**
     * Nhãn hiển thị theo ngôn ngữ Việt.
     * Ví dụ: HOTEL → "Khách sạn", APARTMENT → "Căn hộ"
     * Được tính trong Service, template chỉ việc dùng th:text="${acc.propertyTypeLabel}"
     */
    private String propertyTypeLabel;

    /** Mô tả chi tiết nơi lưu trú */
    private String description;

    /** Địa chỉ cụ thể. Ví dụ: "123 Trần Phú, Hải Châu" */
    private String address;

    /** Thành phố. Ví dụ: "Đà Nẵng" */
    private String city;

    /** Giá mỗi đêm dạng số (để tính toán nếu cần) */
    private BigDecimal pricePerNight;

    /**
     * Giá đã format theo định dạng Việt Nam.
     * Ví dụ: 1200000 → "1.200.000"
     * Template dùng: th:text="${acc.priceFormatted + ' VNĐ'}"
     */
    private String priceFormatted;

    /** Sức chứa tối đa (số khách) */
    private Integer maxGuests;

    /**
     * URL ảnh đại diện.
     * Có thể là path local hoặc URL bên ngoài.
     * Null-safe ở template bằng Thymeleaf conditional.
     */
    private String thumbnailUrl;
}

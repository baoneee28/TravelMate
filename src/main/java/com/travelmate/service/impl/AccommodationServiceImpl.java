package com.travelmate.service.impl;

import com.travelmate.dto.response.AccommodationResponse;
import com.travelmate.entity.Accommodation;
import com.travelmate.enums.ApprovalStatus;
import com.travelmate.enums.PropertyType;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.service.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Implementation của AccommodationService.
 *
 * @Transactional(readOnly = true): toàn bộ method trong class này
 * chỉ đọc DB (không ghi), giúp Hibernate tối ưu performance.
 * Khi cần ghi, override lại annotation ở method cụ thể.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationServiceImpl implements AccommodationService {

    private final AccommodationRepository accommodationRepository;

    /**
     * Tìm kiếm danh sách accommodation APPROVED với filter tuỳ chọn.
     *
     * Xử lý chuỗi rỗng: nếu city = "" (user không nhập gì) → coi như null
     * để repository query không filter theo city.
     */
    @Override
    public List<AccommodationResponse> searchApproved(String city, PropertyType propertyType) {
        // Chuẩn hoá city: rỗng hoặc chỉ khoảng trắng → null để query bỏ filter
        String cityParam = (city != null && !city.isBlank()) ? city.trim() : null;

        List<Accommodation> list = accommodationRepository.searchApproved(cityParam, propertyType);

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy chi tiết 1 accommodation đã APPROVED.
     *
     * Kết hợp 2 điều kiện trong 1 query: id + approvalStatus = APPROVED
     * Nếu không thoả → ném ResourceNotFoundException → Controller bắt và xử lý.
     */
    @Override
    public AccommodationResponse findApprovedById(Long id) {
        Accommodation accommodation = accommodationRepository
                .findByIdAndApprovalStatus(id, ApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nơi lưu trú hoặc nơi lưu trú chưa được duyệt"));

        return toResponse(accommodation);
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Chuyển đổi entity Accommodation → DTO AccommodationResponse.
     *
     * Tập trung logic mapping ở đây để tránh trùng lặp giữa các method.
     * Đây là pattern "Mapper trong Service" — phù hợp với đồ án cơ sở,
     * không cần thư viện MapStruct hay ModelMapper.
     */
    private AccommodationResponse toResponse(Accommodation acc) {
        return AccommodationResponse.builder()
                .id(acc.getId())
                .name(acc.getName())
                .propertyType(acc.getPropertyType())
                .propertyTypeLabel(getPropertyTypeLabel(acc.getPropertyType()))
                .description(acc.getDescription())
                .address(acc.getAddress())
                .city(acc.getCity())
                .pricePerNight(acc.getPricePerNight())
                .priceFormatted(formatPrice(acc.getPricePerNight()))
                .maxGuests(acc.getMaxGuests())
                .thumbnailUrl(acc.getThumbnailUrl())
                .build();
    }

    /**
     * Trả về nhãn hiển thị tiếng Việt của loại lưu trú.
     * Dùng Java 14+ switch expression để code ngắn gọn.
     */
    private String getPropertyTypeLabel(PropertyType type) {
        if (type == null) return "";
        return switch (type) {
            case HOTEL     -> "Khách sạn";
            case HOMESTAY  -> "Homestay";
            case VILLA     -> "Villa";
            case APARTMENT -> "Căn hộ";
        };
    }

    /**
     * Format số tiền theo định dạng Việt Nam: dấu chấm phân cách hàng nghìn.
     * Ví dụ: 1200000.00 → "1.200.000"
     *
     * Dùng NumberFormat với Locale("vi", "VN") để đảm bảo format đúng.
     * KHÔNG dùng String.format("%,d") vì kết quả phụ thuộc vào locale của server.
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0); // Bỏ phần thập phân (.00) cho gọn
        return nf.format(price);
    }
}

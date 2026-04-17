package com.travelmate.service;

import com.travelmate.dto.response.AccommodationResponse;
import com.travelmate.enums.PropertyType;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Accommodation.
 *
 * Dùng Interface + Implementation (AccommodationServiceImpl) là pattern chuẩn
 * trong Spring Boot vì:
 * - Dễ test (có thể mock interface)
 * - Dễ thay implementation sau này
 * - Tách biệt contract và logic
 */
public interface AccommodationService {

    /**
     * Tìm kiếm danh sách accommodation đã APPROVED.
     * Hỗ trợ filter theo thành phố và loại lưu trú (tuỳ chọn).
     *
     * @param city         Tên thành phố (null hoặc rỗng = không filter)
     * @param propertyType Loại lưu trú (null = không filter)
     * @return Danh sách DTO, sắp xếp theo ngày tạo mới nhất
     */
    List<AccommodationResponse> searchApproved(String city, PropertyType propertyType);

    /**
     * Lấy chi tiết 1 accommodation đã APPROVED theo id.
     *
     * Ném ResourceNotFoundException nếu:
     * - Không tìm thấy id
     * - Tìm thấy nhưng chưa được duyệt (PENDING/REJECTED)
     * → Tránh USER truy cập listing chưa được ADMIN duyệt
     *
     * @param id ID của accommodation
     * @return DTO chi tiết
     */
    AccommodationResponse findApprovedById(Long id);
}

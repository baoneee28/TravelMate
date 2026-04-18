package com.travelmate.service.impl;

import com.travelmate.dto.response.*;
import com.travelmate.entity.*;
import com.travelmate.enums.*;
import com.travelmate.exception.BadRequestException;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.*;
import com.travelmate.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getDashboardStats() {
        return AdminStatsResponse.builder()
                .totalAccommodations(accommodationRepository.count())
                .pendingAccommodations(accommodationRepository.countByApprovalStatus(ApprovalStatus.PENDING))
                .approvedAccommodations(accommodationRepository.countByApprovalStatus(ApprovalStatus.APPROVED))
                .totalBookings(bookingRepository.count())
                .pendingBookings(bookingRepository.countByBookingStatus(BookingStatus.PENDING))
                .confirmedBookings(bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED))
                .completedBookings(bookingRepository.countByBookingStatus(BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByBookingStatus(BookingStatus.CANCELLED))
                .totalUsers(userRepository.countByRole(Role.USER))
                .totalPartners(userRepository.countByRole(Role.PARTNER))
                .totalReviews(reviewRepository.count())
                .build();
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookings() {
        return bookingRepository.findAllWithUserAndAccommodation()
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    public void updateBookingStatus(Long id, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking #" + id));

        BookingStatus newStatus;
        try {
            newStatus = BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ: " + status);
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Không thể thay đổi trạng thái booking đã kết thúc");
        }

        booking.setBookingStatus(newStatus);
        bookingRepository.save(booking);
    }

    // ── Accommodation ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminAccommodationResponse> getAllAccommodations() {
        return accommodationRepository.findAllWithPartner()
                .stream()
                .map(this::toAccommodationResponse)
                .toList();
    }

    @Override
    public void approveAccommodation(Long id) {
        Accommodation acc = accommodationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nơi lưu trú #" + id));
        acc.setApprovalStatus(ApprovalStatus.APPROVED);
        accommodationRepository.save(acc);
    }

    @Override
    public void rejectAccommodation(Long id) {
        Accommodation acc = accommodationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nơi lưu trú #" + id));
        acc.setApprovalStatus(ApprovalStatus.REJECTED);
        accommodationRepository.save(acc);
    }

    // ── User ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng #" + id));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Không thể khóa tài khoản Admin");
        }
        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.BLOCKED : UserStatus.ACTIVE);
        userRepository.save(user);
    }

    // ── Review ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminReviewResponse> getAllReviews() {
        return reviewRepository.findAllWithUserAndAccommodation()
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá #" + id);
        }
        reviewRepository.deleteById(id);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private AdminBookingResponse toBookingResponse(Booking b) {
        long nights = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
        User u = b.getUser();
        Accommodation a = b.getAccommodation();
        return AdminBookingResponse.builder()
                .id(b.getId())
                .userName(u.getFullName())
                .userEmail(u.getEmail())
                .userPhone(u.getPhone())
                .accommodationId(a.getId())
                .accommodationName(a.getName())
                .accommodationCity(a.getCity())
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .numNights(nights)
                .numAdults(b.getNumAdults())
                .numChildren(b.getNumChildren())
                .totalPrice(b.getTotalPrice())
                .totalPriceFormatted(formatPrice(b.getTotalPrice()))
                .bookingStatus(b.getBookingStatus())
                .statusLabel(getBookingStatusLabel(b.getBookingStatus()))
                .statusBadgeClass(getBookingBadgeClass(b.getBookingStatus()))
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private AdminAccommodationResponse toAccommodationResponse(Accommodation a) {
        User partner = a.getPartner();
        return AdminAccommodationResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .propertyType(a.getPropertyType())
                .propertyTypeLabel(getPropertyTypeLabel(a.getPropertyType()))
                .city(a.getCity())
                .address(a.getAddress())
                .pricePerNight(a.getPricePerNight())
                .priceFormatted(formatPrice(a.getPricePerNight()))
                .maxGuests(a.getMaxGuests())
                .thumbnailUrl(a.getThumbnailUrl())
                .approvalStatus(a.getApprovalStatus())
                .approvalStatusLabel(getApprovalStatusLabel(a.getApprovalStatus()))
                .approvalStatusBadgeClass(getApprovalBadgeClass(a.getApprovalStatus()))
                .partnerName(partner.getFullName())
                .partnerEmail(partner.getEmail())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AdminUserResponse toUserResponse(User u) {
        long bookingCount = u.getRole() == Role.USER
                ? bookingRepository.countByUserId(u.getId())
                : 0L;
        return AdminUserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .roleLabel(getRoleLabel(u.getRole()))
                .roleBadgeClass(getRoleBadgeClass(u.getRole()))
                .status(u.getStatus())
                .statusLabel(u.getStatus() == UserStatus.ACTIVE ? "Hoạt động" : "Bị khóa")
                .active(u.getStatus() == UserStatus.ACTIVE)
                .bookingCount(bookingCount)
                .createdAt(u.getCreatedAt())
                .avatarInitials(getInitials(u.getFullName()))
                .avatarColor(getAvatarColor(u.getRole()))
                .build();
    }

    private AdminReviewResponse toReviewResponse(Review r) {
        return AdminReviewResponse.builder()
                .id(r.getId())
                .userName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .accommodationId(r.getAccommodation().getId())
                .accommodationName(r.getAccommodation().getName())
                .bookingId(r.getBooking().getId())
                .rating(r.getRating())
                .starsDisplay("★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating()))
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(price);
    }

    private String getBookingStatusLabel(BookingStatus s) {
        return switch (s) {
            case PENDING   -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private String getBookingBadgeClass(BookingStatus s) {
        return switch (s) {
            case PENDING   -> "badge--warning";
            case CONFIRMED -> "badge--primary";
            case CANCELLED -> "badge--danger";
            case COMPLETED -> "badge--success";
        };
    }

    private String getApprovalStatusLabel(ApprovalStatus s) {
        return switch (s) {
            case PENDING  -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
        };
    }

    private String getApprovalBadgeClass(ApprovalStatus s) {
        return switch (s) {
            case PENDING  -> "badge--warning";
            case APPROVED -> "badge--success";
            case REJECTED -> "badge--danger";
        };
    }

    private String getPropertyTypeLabel(PropertyType t) {
        if (t == null) return "";
        return switch (t) {
            case HOTEL     -> "Khách sạn";
            case HOMESTAY  -> "Homestay";
            case VILLA     -> "Villa";
            case APARTMENT -> "Căn hộ";
        };
    }

    private String getRoleLabel(Role r) {
        return switch (r) {
            case USER    -> "Khách";
            case ADMIN   -> "Admin";
            case PARTNER -> "Đối tác";
        };
    }

    private String getRoleBadgeClass(Role r) {
        return switch (r) {
            case USER    -> "badge--muted";
            case ADMIN   -> "badge--primary";
            case PARTNER -> "badge--warning";
        };
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String getAvatarColor(Role role) {
        return switch (role) {
            case USER    -> "var(--color-primary)";
            case ADMIN   -> "var(--color-success)";
            case PARTNER -> "var(--color-accent)";
        };
    }
}

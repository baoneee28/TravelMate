package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Notification;
import com.travelmate.entity.User;
import com.travelmate.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (Objects.equals(n.getUser().getId(), user.getId())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllReadByUser(user);
    }

    @Transactional
    public Notification createReviewReminder(User user, Accommodation accommodation, Long bookingId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(Notification.Type.REVIEW_REMINDER);
        n.setTitle("Hãy đánh giá kỳ nghỉ tại " + accommodation.getName());
        n.setMessage("Kỳ lưu trú tại " + accommodation.getName()
                + " của bạn đã kết thúc. Hãy chia sẻ trải nghiệm để giúp cộng đồng TravelMate!");
        // Gắn bookingId vào URL để FE auto-scroll và mở form đánh giá đúng booking
        n.setTargetUrl("/my-bookings?tab=COMPLETED&reviewBookingId=" + bookingId);
        n.setIsRead(false);
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification createBookingConfirmed(User user, String bookingCode, Accommodation accommodation) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(Notification.Type.BOOKING_CONFIRMED);
        n.setTitle("Đặt phòng của bạn đã được xác nhận");
        n.setMessage("Booking " + bookingCode + " tại " + accommodation.getName()
                + " đã được xác nhận. Chúc bạn có chuyến đi vui vẻ!");
        n.setTargetUrl("/my-bookings");
        n.setIsRead(false);
        return notificationRepository.save(n);
    }

    /**
     * Notification khi admin phản hồi support ticket.
     * Gửi cho user đã đăng nhập (USER role) hoặc partner.
     */
    @Transactional
    public Notification createTicketResponded(User recipient, Long ticketId, String subject) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setType(Notification.Type.BOOKING_CONFIRMED); // reuse type — hoặc thêm SUPPORT nếu có
        n.setTitle("Yêu cầu hỗ trợ của bạn đã được phản hồi");
        n.setMessage("Admin đã phản hồi ticket \"" + subject + "\". Vào mục Hỗ trợ để xem chi tiết.");
        n.setTargetUrl("/contact");
        n.setIsRead(false);
        return notificationRepository.save(n);
    }
}

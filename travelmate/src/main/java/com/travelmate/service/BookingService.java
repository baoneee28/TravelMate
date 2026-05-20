package com.travelmate.service;

import com.travelmate.entity.*;
import com.travelmate.entity.enums.*;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.RemainingPaymentStatus;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * BookingService — Xử lý nghiệp vụ đặt phòng.
 *
 * Chức năng:
 *   1. Tính tiền booking (số đêm × giá × số phòng)
 *   2. Tạo booking + payment demo
 *   3. Sinh booking code: BK-<roomCode>-<sequence>
 *   4. Lấy danh sách booking của user
 */
@SuppressWarnings("null")
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final AccommodationRepository accommodationRepository;
    private final VoucherService voucherService;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          RoomRepository roomRepository,
                          AccommodationRepository accommodationRepository,
                          VoucherService voucherService,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.accommodationRepository = accommodationRepository;
        this.voucherService = voucherService;
        this.notificationService = notificationService;
    }

    /**
     * Tính số đêm giữa ngày check-in và check-out.
     * Tối thiểu 1 đêm.
     */
    public long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return Math.max(nights, 1); // ít nhất 1 đêm
    }

    /**
     * Tính tổng tiền booking.
     * Công thức: pricePerNight × numberOfNights × roomQuantity
     */
    public BigDecimal calculateTotalAmount(Room room, long numberOfNights, int roomQuantity) {
        return room.getPricePerNight()
                .multiply(BigDecimal.valueOf(numberOfNights))
                .multiply(BigDecimal.valueOf(roomQuantity));
    }

    /**
     * Tính số tiền cần thanh toán dựa trên paymentOption.
     *
     * - FULL_PAYMENT: trả 100%
     * - DEPOSIT_30:   trả 30%
     */
    public BigDecimal calculatePaidAmount(BigDecimal totalAmount, PaymentOption paymentOption) {
        if (paymentOption == PaymentOption.DEPOSIT_30) {
            // 30% tổng tiền, làm tròn xuống
            return totalAmount.multiply(BigDecimal.valueOf(0.3))
                    .setScale(0, RoundingMode.FLOOR);
        }
        // FULL_PAYMENT → trả hết
        return totalAmount;
    }

    /**
     * Sinh mã booking theo format: BK-<roomCode>-<sequence>.
     *
     * Sequence = số booking hiện có của room đó + 1, pad 4 số.
     * VD: BK-R101-0001, BK-R101-0002
     *
     * ⚠️ Đơn giản hóa cho đồ án. Production cần cơ chế
     * chống trùng phức tạp hơn (lock DB, UUID, v.v.)
     */
    public String generateBookingCode(Room room) {
        long count = bookingRepository.countByRoom(room);
        String sequence = String.format("%04d", count + 1);
        return "BK-" + room.getRoomCode() + "-" + sequence;
    }

    /**
     * Tạo booking + payment demo.
     *
     * Luồng:
     *   1. Validate room còn đủ phòng
     *   2. Tính tiền
     *   3. Sinh booking code
     *   4. Tạo Booking (status = PENDING_ADMIN_APPROVAL)
     *   5. Tạo Payment demo (status = SUBMITTED)
     *   6. Giảm availableQuantity của room
     *
     * @return Booking vừa tạo
     * @throws RuntimeException nếu hết phòng hoặc dữ liệu không hợp lệ
     */
    @Transactional
    public Booking createBooking(User user, Room room, Accommodation accommodation,
                                  String customerName, String customerPhone, String customerEmail,
                                  LocalDate checkIn, LocalDate checkOut,
                                  int adults, int children, int roomQuantity,
                                  PaymentOption paymentOption) {
        return createBooking(user, room, accommodation, customerName, customerPhone, customerEmail,
                checkIn, checkOut, adults, children, roomQuantity, paymentOption, null);
    }

    /**
     * Tạo booking + payment demo (hỗ trợ voucher).
     *
     * Luồng:
     *   1. Validate ngày nhận/trả phòng
     *   2. Validate voucher (nếu có) và tính giảm giá
     *   3. Tính tiền (sau giảm)
     *   4. Sinh booking code
     *   5. Tạo Booking (status = PENDING_ADMIN_APPROVAL)
     *   6. Tạo Payment demo
     *   7. Giảm availableQuantity của room
     *
     * @param voucherCode Mã voucher user nhập (có thể null)
     * @return Booking vừa tạo
     */
    @Transactional
    public Booking createBooking(User user, Room room, Accommodation accommodation,
                                  String customerName, String customerPhone, String customerEmail,
                                  LocalDate checkIn, LocalDate checkOut,
                                  int adults, int children, int roomQuantity,
                                  PaymentOption paymentOption, String voucherCode) {

        // === 0. Validate ngày nhận/trả phòng ===
        // checkIn và checkOut không được null
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("Vui lòng chọn ngày nhận và trả phòng!");
        }
        // checkOut phải sau checkIn
        if (!checkOut.isAfter(checkIn)) {
            throw new RuntimeException("Ngày trả phòng phải sau ngày nhận phòng!");
        }
        // checkIn không được là ngày trong quá khứ
        if (checkIn.isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày nhận phòng không được nhỏ hơn ngày hiện tại!");
        }

        // === 0.0a. Validate số phòng ===
        if (roomQuantity < 1) {
            throw new RuntimeException("Số phòng đặt phải ít nhất là 1!");
        }
        if (roomQuantity > 20) {
            throw new RuntimeException("Số phòng đặt không vượt quá 20 phòng mỗi lần!");
        }

        // === 0.0b. Validate số người ===
        if (adults < 1) {
            throw new RuntimeException("Cần ít nhất 1 người lớn khi đặt phòng!");
        }
        if (children < 0) {
            throw new RuntimeException("Số trẻ em không được âm!");
        }

        // === 0.0c. Validate thông tin khách hàng ===
        if (customerName == null || customerName.isBlank()) {
            throw new RuntimeException("Tên khách hàng không được để trống!");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        // Chuẩn hóa phone: bỏ khoảng trắng, dấu gạch ngang
        String cleanPhone = customerPhone.replaceAll("[\\s\\-\\.]", "");
        if (!cleanPhone.matches("^(0|\\+84)[0-9]{8,10}$")) {
            throw new RuntimeException("Số điện thoại không đúng định dạng (VD: 0901234567 hoặc +84901234567)!");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (!customerEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Email không đúng định dạng!");
        }

        // === 0.1. Validate accommodation phải được duyệt (APPROVED) ===
        // Tránh trường hợp user hack URL đặt phòng ở listing chưa duyệt
        if (accommodation.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Nơi lưu trú này chưa được duyệt để đặt phòng!");
        }

        // === 0.1b. Validate room phải được duyệt (APPROVED) ===
        if (room.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Phòng này chưa được Admin duyệt. Vui lòng chọn phòng khác!");
        }

        // === 0.1c. Validate partner (owner) phải đang ACTIVE ===
        // Ngăn đặt phòng khi partner bị khóa/vô hiệu hóa
        User owner = accommodation.getOwner();
        if (owner != null && !"ACTIVE".equals(owner.getStatus())) {
            throw new RuntimeException("Nơi lưu trú này hiện tạm ngưng hoạt động. Vui lòng thử lại sau hoặc chọn cơ sở khác!");
        }

        // === 0.1c. Validate room PHẢI thuộc accommodation đã chỉ định ===
        // Ngăn user can thiệp URL để đặt phòng của accommodation khác
        if (room.getAccommodation() == null ||
            !room.getAccommodation().getId().equals(accommodation.getId())) {
            throw new RuntimeException("Phòng này không thuộc nơi lưu trú đã chọn. Vui lòng đặt lại!");
        }

        // === 0.2. Validate sức chứa phòng ===
        // totalGuests = adults + children phải <= capacity * roomQuantity
        int totalGuests = adults + children;
        int maxGuests = room.getCapacity() * roomQuantity;
        if (totalGuests > maxGuests) {
            throw new RuntimeException(
                "Số khách vượt quá sức chứa của phòng! " +
                "Tối đa " + maxGuests + " khách cho " + roomQuantity + " phòng."
            );
        }

        // === 1. Validate phòng còn đủ theo khoảng ngày yêu cầu (anti-overbooking) ===
        // Dùng PESSIMISTIC_WRITE lock để chặn 2 request cùng lúc vào cùng 1 phòng
        room = roomRepository.findByIdForUpdate(room.getId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        List<BookingStatus> occupyStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,        // đang giữ phòng tạm khi user thanh toán VNPAY
                BookingStatus.PENDING_ADMIN_APPROVAL,
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN
        );
        int sumAllActive = bookingRepository.sumActiveQtyForRoom(room.getId(), occupyStatuses);
        int totalRooms   = room.getAvailableQuantity() + sumAllActive;
        int occupiedInRange = bookingRepository.sumOverlappingQtyForRoom(
                room.getId(), checkIn, checkOut, occupyStatuses);
        int availableForRange = Math.max(0, totalRooms - occupiedInRange);

        if (availableForRange < roomQuantity) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String ciStr = checkIn.format(fmt);
            String coStr = checkOut.format(fmt);
            if (availableForRange <= 0) {
                throw new RuntimeException(
                    "⚠️ Rất tiếc! Phòng \"" + room.getRoomName() + "\" vừa hết chỗ cho khoảng ngày "
                    + ciStr + " → " + coStr
                    + ". Vui lòng chọn ngày khác hoặc chọn phòng khác!");
            } else {
                throw new RuntimeException(
                    "⚠️ Phòng \"" + room.getRoomName() + "\" chỉ còn " + availableForRange
                    + " phòng trống cho khoảng ngày " + ciStr + " → " + coStr
                    + ". Bạn đang yêu cầu " + roomQuantity + " phòng — vui lòng giảm số phòng hoặc chọn ngày khác!");
            }
        }

        // === 2. Tính tiền (trước khi giảm) ===
        long numberOfNights = calculateNights(checkIn, checkOut);
        BigDecimal totalBeforeDiscount = calculateTotalAmount(room, numberOfNights, roomQuantity);

        // === 2.1. Apply voucher (nếu có) ===
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        com.travelmate.entity.enums.VoucherCostBearer costBearer = null;

        if (voucherCode != null && !voucherCode.isBlank()) {
            // Voucher sai → ném lỗi ra ngoài, không tạo booking
            com.travelmate.entity.Voucher voucher =
                    voucherService.validateVoucher(voucherCode, totalBeforeDiscount, room);
            if (voucher != null) {
                discountAmount = voucherService.calculateDiscount(voucher, totalBeforeDiscount);
                appliedVoucherCode = voucher.getCode();
                costBearer = voucher.getCostBearer();
            }
        }

        BigDecimal totalAmount = totalBeforeDiscount.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        BigDecimal paidAmount = calculatePaidAmount(totalAmount, paymentOption);
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

        // Số tiền gửi sang VNPAY phải tối thiểu 5.000₫
        BigDecimal minVnpay = new BigDecimal("5000");
        if (paidAmount.compareTo(minVnpay) < 0) {
            throw new RuntimeException(
                "Số tiền thanh toán qua VNPAY phải tối thiểu 5.000₫. " +
                "Mã giảm giá đã áp dụng vượt quá giới hạn cho phép.");
        }

        // === 3. Sinh booking code — placeholder, sẽ cập nhật sau khi save lấy ID ===
        String bookingCode = "BK-PENDING";

        // === 4. Tạo Booking ===
        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setRoom(room);
        booking.setCustomerName(customerName);
        booking.setCustomerPhone(customerPhone);
        booking.setCustomerEmail(customerEmail);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setAdults(adults);
        booking.setChildren(children);
        booking.setRoomQuantity(roomQuantity);
        booking.setTotalAmount(totalAmount);
        booking.setPaymentOption(paymentOption);
        booking.setPaidAmount(paidAmount);
        booking.setRemainingAmount(remainingAmount);
        // === VOUCHER FIELDS ===
        booking.setTotalBeforeDiscount(totalBeforeDiscount);
        booking.setDiscountAmount(discountAmount);
        booking.setVoucherCode(appliedVoucherCode);
        booking.setVoucherCostBearer(costBearer);
        // Trạng thái ban đầu: chờ user hoàn tất thanh toán VNPAY
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        booking.setBookingSource(BookingSource.ONLINE);
        // Phòng được giữ tạm 15 phút — Scheduler sẽ hủy nếu user không thanh toán
        booking.setExpireAt(LocalDateTime.now().plusMinutes(15));
        // Với cọc 30%: phần còn lại chưa thu → UNPAID
        // Với thanh toán 100%: không cần thu thêm → NOT_REQUIRED
        booking.setRemainingPaymentStatus(
            paymentOption == PaymentOption.DEPOSIT_30
                ? RemainingPaymentStatus.UNPAID
                : RemainingPaymentStatus.NOT_REQUIRED
        );

        booking = bookingRepository.save(booking);

        // === 3b. Cập nhật booking code dùng ID thực — chống trùng tuyệt đối ===
        // Format: BK-<roomCode>-<bookingId padded 6 digits>
        String finalCode = "BK-" + room.getRoomCode() + "-" + String.format("%06d", booking.getId());
        booking.setBookingCode(finalCode);
        booking = bookingRepository.save(booking);

        // === 5. Tạo Payment — chờ user thanh toán qua VNPAY ===
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setPaymentOption(paymentOption);
        payment.setAmount(paidAmount);
        payment.setGateway("VNPAY");
        // vnpTxnRef dùng để VNPAY định danh giao dịch: TM<bookingId><timestamp>
        payment.setVnpTxnRef("TM" + booking.getId() + System.currentTimeMillis());
        // transactionCode nội bộ TravelMate
        payment.setTransactionCode("TXN-" + String.format("%08d", booking.getId()));
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        // expireAt đồng bộ với booking.expireAt
        payment.setExpireAt(booking.getExpireAt());
        payment.setNote(paymentOption == PaymentOption.DEPOSIT_30
                ? "Cọc 30% qua VNPAY — đang chờ xác nhận giao dịch"
                : "Thanh toán 100% qua VNPAY — đang chờ xác nhận giao dịch");
        // paidAt chưa set — sẽ được set khi VNPAY xác nhận thành công

        paymentRepository.save(payment);

        // === 6. Giảm số phòng trống ===
        // ⚠️ ĐƠN GIẢN HÓA CHO ĐỒ ÁN:
        // Giảm availableQuantity ngay sau khi user xác nhận thanh toán demo.
        // Trong production thực tế cần cơ chế reserve/lock phức tạp hơn,
        // ví dụ: chỉ giảm sau khi admin duyệt, hoặc dùng "hold" tạm 15 phút.
        room.setAvailableQuantity(room.getAvailableQuantity() - roomQuantity);
        roomRepository.save(room);

        return booking;
    }

    /**
     * Lấy danh sách booking của user, sắp xếp mới nhất trước.
     */
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Hủy đặt phòng — chỉ user sở hữu booking mới được hủy.
     *
     * Rule:
     *   - Chỉ hủy được nếu status đang PENDING_ADMIN_APPROVAL
     *   - A1: Trả lại availableQuantity cho room
     *   - A2: Set PaymentStatus = CANCELLED (cả booking lẫn payment entity)
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này!");
        }
        // Chặn hủy booking sau khi đã check-in (khách đang lưu trú)
        if (booking.getBookingStatus() == BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Không thể hủy đơn khi khách đã check-in và đang lưu trú! Vui lòng liên hệ cơ sở lưu trú.");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Đơn đã hoàn tất, không thể hủy!");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_ADMIN_APPROVAL
                && booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể hủy đơn đang chờ thanh toán, chờ TravelMate xác nhận, hoặc đã xác nhận (chưa check-in)!");
        }

        // Capture trạng thái gốc trước khi hủy
        boolean alreadyPaid = booking.getBookingStatus() == BookingStatus.PENDING_ADMIN_APPROVAL;

        // A1: Trả lại số phòng trống
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.CANCELLED);

        if (alreadyPaid) {
            // Đã thanh toán/cọc qua VNPAY → cần Admin xử lý hoàn tiền
            booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            paymentRepository.findByBooking(booking).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                payment.setNote("User hủy sau khi đã thanh toán qua VNPAY — Admin cần xử lý hoàn tiền.");
                paymentRepository.save(payment);
            });
        } else {
            // Chưa thanh toán qua VNPAY (PENDING_PAYMENT) → hủy thẳng
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
            paymentRepository.findByBooking(booking).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
                payment.setNote("User hủy khi chưa hoàn tất thanh toán VNPAY.");
                paymentRepository.save(payment);
            });
        }

        return bookingRepository.save(booking);
    }

    // ─── Admin methods ──────────────────────────────────────────────────────

    /** Lấy tất cả booking (dùng cho admin), mới nhất trước — dùng JOIN FETCH để tránh NPE và N+1. */
    public List<Booking> getAllBookingsForAdmin() {
        return bookingRepository.findAllForAdminPage();
    }

    /**
     * Admin duyệt booking.
     * BookingStatus → CONFIRMED, PaymentStatus → APPROVED.
     */
    @Transactional
    public Booking approveBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));
        if (booking.getBookingStatus() != BookingStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("Chỉ có thể duyệt đơn đang chờ duyệt!");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.APPROVED);

        // Set partnerStatus để partner biết cần xác nhận giữ phòng
        // Nếu accommodation có owner (partner), partner sẽ thấy booking này
        if (booking.getAccommodation() != null && booking.getAccommodation().getOwner() != null) {
            booking.setPartnerStatus(PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION);
        }

        bookingRepository.save(booking);

        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            payment.setPaymentStatus(PaymentStatus.APPROVED);
            payment.setApprovedAt(LocalDateTime.now()); // ghi lại thời điểm admin duyệt
            paymentRepository.save(payment);
        });

        // Gửi notification xác nhận cho user sau khi admin duyệt
        notificationService.createBookingConfirmed(
                booking.getUser(),
                booking.getBookingCode(),
                booking.getAccommodation());

        return booking;
    }

    /**
     * Admin từ chối booking (có lý do).
     * BookingStatus → CANCELLED, PaymentStatus → REJECTED.
     * Trả lại availableQuantity cho room.
     */
    @Transactional
    public Booking rejectBookingByAdmin(Long bookingId) {
        return rejectBookingByAdmin(bookingId, null);
    }

    @Transactional
    public Booking rejectBookingByAdmin(Long bookingId, String rejectReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));
        if (booking.getBookingStatus() != BookingStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("Chỉ có thể từ chối đơn đang chờ duyệt!");
        }

        // Booking ở PENDING_ADMIN_APPROVAL nghĩa là user đã thanh toán/cọc qua VNPAY thành công.
        // Admin từ chối → cần hoàn tiền, không phải REJECTED "không thu phí".
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        String baseNote = (rejectReason != null && !rejectReason.isBlank())
                ? "Admin từ chối: " + rejectReason.trim()
                : "Admin từ chối xác nhận đơn đặt phòng.";
        booking.setNote(baseNote + " — Đang tiến hành xử lý hoàn tiền cho khách.");
        bookingRepository.save(booking);

        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            payment.setNote("Admin từ chối xác nhận thanh toán — chờ hoàn tiền cho khách.");
            paymentRepository.save(payment);
        });

        // Trả lại số phòng trống
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        return booking;
    }

    /** Tìm booking theo ID (dùng cho admin detail page) */
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng #" + id));
    }

    /**
     * [ADMIN OVERRIDE] Admin ghi đè check-in — dùng khi Partner không thao tác được hoặc can thiệp khẩn.
     *
     * Thao tác check-in bình thường nên do PARTNER thực hiện (checkInByPartner).
     * Admin override chỉ dùng khi có sự cố hoặc tranh chấp.
     *
     * Chỉ áp dụng với booking đang CONFIRMED và paymentStatus = APPROVED.
     */
    @Transactional
    public Booking checkInBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                "Chỉ có thể check-in đơn đang CONFIRMED! " +
                "Hiện tại: " + booking.getBookingStatus().name()
            );
        }
        if (booking.getPaymentStatus() != PaymentStatus.APPROVED
                && booking.getPaymentStatus() != PaymentStatus.NOT_REQUIRED) {
            throw new RuntimeException("Chỉ check-in được khi thanh toán đã được xác nhận!");
        }
        if (booking.getPartnerStatus() == PartnerBookingStatus.PARTNER_CANCELLED) {
            throw new RuntimeException(
                "❌ Partner đã hủy giữ phòng! Vui lòng xử lý hủy đơn trước khi check-in.");
        }

        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        appendNote(booking, "[Admin Override] Check-in được ghi đè bởi Admin.");
        return bookingRepository.save(booking);
    }

    /**
     * Admin đánh dấu booking hoàn tất sau khi khách checkout.
     * Áp dụng với booking đang CONFIRMED hoặc CHECKED_IN.
     *
     * Khi hoàn tất:
     *   - BookingStatus → COMPLETED
     *   - room.availableQuantity += booking.roomQuantity  (mở lại phòng cho khách khác)
     *   - Tránh cộng phòng nhiều lần: guard check status trước khi xử lý
     */
    @Transactional
    public Booking completeBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException(
                "Chỉ có thể hoàn tất đơn đang CHECKED_IN! " +
                "Hiện tại: " + booking.getBookingStatus().name()
            );
        }

        // Mở lại phòng khi khách đã checkout — tránh cộng 2 lần nhờ guard status ở trên
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.COMPLETED);
        appendNote(booking, "[Admin Override] Khách đã checkout. Đơn đặt phòng hoàn tất. Phòng đã được mở lại.");
        Booking saved = bookingRepository.save(booking);

        // Chỉ tạo review reminder cho booking ONLINE của USER thực sự
        if ((saved.getBookingSource() == null || saved.getBookingSource() == BookingSource.ONLINE)
                && saved.getUser() != null
                && saved.getUser().getRole() == User.Role.USER) {
            notificationService.createReviewReminder(saved.getUser(), saved.getAccommodation(), saved.getId());
        }
        return saved;
    }

    // ─── FIX 6: Admin mô phỏng no-show / quá hạn check-in ──────────────────

    /**
     * Admin đánh dấu khách không đến (mô phỏng no-show/quá hạn check-in).
     *
     * Chỉ áp dụng với booking đang CONFIRMED (đã admin duyệt).
     *
     * Nghiệp vụ theo paymentOption:
     *
     * --- DEPOSIT_30 (cọc 30%) ---
     *   Khách không đến → mất toàn bộ cọc.
     *   BookingStatus  → NO_SHOW
     *   PaymentStatus  → DEPOSIT_FORFEITED
     *   Note: "Khách không đến check-in, cọc 30% bị giữ lại."
     *   Phòng được mở lại (trả availableQuantity).
     *
     * --- FULL_PAYMENT (100%) ---
     *   Theo rule nhóm: đã thanh toán 100% → vẫn xử lý check-in.
     *   BookingStatus  → CHECKED_IN
     *   PaymentStatus  → APPROVED (giữ nguyên)
     *   Note: "Khách đã thanh toán 100%, hệ thống xử lý check-in theo chính sách."
     *   Phòng KHÔNG mở lại (booking vẫn được tính là đã dùng phòng).
     */
    @Transactional
    public Booking markNoShow(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        // Chỉ cho phép no-show với booking đã được admin duyệt (CONFIRMED)
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                "Chỉ có thể đánh dấu no-show với đơn đã được duyệt (CONFIRMED)! " +
                "Đơn hiện tại: " + booking.getBookingStatus().name()
            );
        }

        if (booking.getPaymentOption() == PaymentOption.DEPOSIT_30) {
            // === Case 1: Cọc 30% — khách mất cọc ===
            booking.setBookingStatus(BookingStatus.NO_SHOW);
            booking.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
            booking.setNote("Khách không đến check-in. Cọc 30% bị giữ lại theo chính sách.");

            // Cập nhật payment entity → DEPOSIT_FORFEITED
            paymentRepository.findByBooking(booking).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
                payment.setNote("Cọc 30% bị giữ lại do khách không đến check-in.");
                payment.setApprovedAt(java.time.LocalDateTime.now());
                paymentRepository.save(payment);
            });

            // Mở lại phòng cho khách khác đặt
            Room room = booking.getRoom();
            room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
            roomRepository.save(room);

        } else {
            // === Case 2: Thanh toán 100% — theo rule nhóm: vẫn xử lý check-in ===
            booking.setBookingStatus(BookingStatus.CHECKED_IN);
            // PaymentStatus giữ APPROVED — không mất tiền
            booking.setNote("Khách đã thanh toán 100%. Hệ thống xử lý check-in theo chính sách.");

            // Cập nhật note cho payment (giữ APPROVED)
            paymentRepository.findByBooking(booking).ifPresent(payment -> {
                payment.setNote("Thanh toán 100% — check-in xử lý theo chính sách dù khách không đến.");
                paymentRepository.save(payment);
            });

            // Không mở lại phòng — booking 100% vẫn tính là đã sử dụng
        }

        return bookingRepository.save(booking);
    }

    // ─── Partner methods ─────────────────────────────────────────────────────

    /**
     * Lấy booking thuộc accommodation do partner sở hữu.
     *
     * Business rule:
     *   - Ownership: chỉ booking của accommodation có owner = partner
     *   - Status filter: KHÔNG hiển thị PENDING_ADMIN_APPROVAL
     *   - Partner chỉ thấy booking SAU KHI admin đã duyệt/xử lý
     *
     * Trạng thái partner được thấy:
     *   - CONFIRMED:  admin đã duyệt
     *   - CHECKED_IN:  khách đã check-in
     *   - COMPLETED:   đã hoàn tất
     *   - NO_SHOW:     khách không đến (mất cọc)
     */
    public List<Booking> getBookingsForPartner(User partner) {
        List<BookingStatus> visibleStatuses = List.of(
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN,
                BookingStatus.COMPLETED,
                BookingStatus.NO_SHOW
        );
        return bookingRepository
                .findByAccommodationOwnerAndBookingStatusInOrderByCreatedAtDesc(partner, visibleStatuses);
    }

    // ─── Admin Dashboard methods ─────────────────────────────────────────────

    /** Đếm booking theo trạng thái */
    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByBookingStatus(status);
    }

    /** Đếm tổng accommodation đã duyệt */
    public long countApprovedAccommodations() {
        return accommodationRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
    }

    /** Tính tổng phòng trống (đơn giản: sum availableQuantity) */
    public int countTotalAvailableRooms() {
        return roomRepository.findAll().stream()
                .mapToInt(Room::getAvailableQuantity)
                .sum();
    }

    /**
     * Tính tổng doanh thu demo từ payment đã xác nhận.
     *
     * Tính cả:
     *   - APPROVED: thanh toán đã admin duyệt
     *   - DEPOSIT_FORFEITED: tiền cọc bị giữ lại do no-show
     *
     * Không tính: PENDING_ADMIN_APPROVAL, REJECTED, CANCELLED, SUBMITTED
     */
    public BigDecimal calculateDemoRevenue() {
        List<PaymentStatus> revenueStatuses = List.of(
                PaymentStatus.APPROVED,
                PaymentStatus.DEPOSIT_FORFEITED
        );
        return paymentRepository.findByPaymentStatusIn(revenueStatuses).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Partner confirm hold ────────────────────────────────────────────────

    /**
     * Partner xác nhận giữ phòng cho khách.
     *
     * Rule:
     *   1. Booking phải tồn tại
     *   2. Booking phải thuộc accommodation owner = partner
     *   3. BookingStatus phải là CONFIRMED
     *   4. partnerStatus phải là PENDING_PARTNER_CONFIRMATION hoặc null
     *   5. Không cho partner xác nhận booking của partner khác
     *   6. Không cho xác nhận booking PENDING_ADMIN_APPROVAL/CANCELLED/NO_SHOW/COMPLETED
     */
    @Transactional
    public Booking confirmBookingHoldByPartner(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        // Rule 2: Kiểm tra ownership
        if (booking.getAccommodation().getOwner() == null ||
                !booking.getAccommodation().getOwner().getId().equals(partner.getId())) {
            throw new RuntimeException("Bạn không có quyền xác nhận đơn này! Đơn không thuộc cơ sở của bạn.");
        }

        // Rule 3: BookingStatus phải là CONFIRMED
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Chỉ xác nhận được đơn đã admin duyệt (CONFIRMED)! " +
                    "Hiện tại: " + booking.getBookingStatus().name());
        }

        // Rule 3b: PaymentStatus phải là APPROVED — đảm bảo admin đã xác nhận thanh toán
        if (booking.getPaymentStatus() != PaymentStatus.APPROVED) {
            throw new RuntimeException("Chỉ xác nhận giữ phòng khi thanh toán đã được admin duyệt (APPROVED)!");
        }

        // Rule 4: partnerStatus phải là PENDING hoặc null
        if (booking.getPartnerStatus() != null &&
                booking.getPartnerStatus() != PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION) {
            throw new RuntimeException("Đơn này đã được xác nhận hoặc không ở trạng thái chờ!");
        }

        // Xác nhận giữ phòng
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);

        // Ghi note
        String currentNote = booking.getNote();
        String partnerNote = "Partner đã xác nhận giữ phòng cho khách.";
        booking.setNote(currentNote != null && !currentNote.isEmpty()
                ? currentNote + " | " + partnerNote
                : partnerNote);

        return bookingRepository.save(booking);
    }

    /**
     * Partner check-in khi khách đến nhận phòng.
     *
     * Rule:
     *   - Booking phải thuộc accommodation.owner = partner
     *   - BookingStatus phải là CONFIRMED
     *   - PaymentStatus phải là APPROVED
     *   - partnerStatus phải là PARTNER_CONFIRMED
     */
    @Transactional
    public Booking checkInByPartner(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        validatePartnerOwnership(booking, partner);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Chỉ có thể check-in đơn đang CONFIRMED! Hiện tại: " + booking.getBookingStatus().name());
        }
        // Cho phép APPROVED (online booking) và NOT_REQUIRED (direct booking — thu tại cơ sở)
        if (booking.getPaymentStatus() != PaymentStatus.APPROVED
                && booking.getPaymentStatus() != PaymentStatus.NOT_REQUIRED) {
            throw new RuntimeException("Chỉ check-in được khi thanh toán đã được xác nhận!");
        }
        if (booking.getPartnerStatus() != PartnerBookingStatus.PARTNER_CONFIRMED) {
            throw new RuntimeException("Cần xác nhận giữ phòng trước khi thực hiện check-in!");
        }

        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        String checkinNote = "[Partner] Khách đã đến nhận phòng. Check-in lúc "
                + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        appendNote(booking, checkinNote);
        return bookingRepository.save(booking);
    }

    /**
     * Partner check-in kèm xác nhận thu 70% cùng lúc (cho DEPOSIT_30).
     */
    @Transactional
    public Booking checkInWithRemainingConfirmByPartner(Long bookingId, User partner,
                                                         boolean remainingConfirmed, String remainingNote) {
        // DEPOSIT_30 bắt buộc xác nhận đã thu 70% trước khi check-in
        if (!remainingConfirmed) {
            Booking peek = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));
            if (peek.getPaymentOption() == PaymentOption.DEPOSIT_30) {
                throw new RuntimeException(
                    "⚠️ Đơn cọc 30%: Vui lòng xác nhận đã thu đủ khoản còn lại tại cơ sở trước khi check-in cho khách.");
            }
        }
        Booking booking = checkInByPartner(bookingId, partner);
        if (remainingConfirmed && booking.getPaymentOption() == PaymentOption.DEPOSIT_30) {
            booking.setRemainingPaymentStatus(RemainingPaymentStatus.PAID_AT_PROPERTY);
            booking.setRemainingPaidAt(LocalDateTime.now());
            booking.setRemainingPaymentNote(remainingNote);
            String noteText = "[Partner] Đã thu đủ khoản còn lại tại cơ sở lúc check-in.";
            if (remainingNote != null && !remainingNote.isBlank()) noteText += " (" + remainingNote.trim() + ")";
            appendNote(booking, noteText);
            booking = bookingRepository.save(booking);
        }
        return booking;
    }

    /**
     * Partner báo no-show khi khách không đến.
     *
     * Rule:
     *   - Booking phải thuộc accommodation.owner = partner
     *   - BookingStatus phải là CONFIRMED
     *   - partnerStatus phải là PARTNER_CONFIRMED
     *
     * Nghiệp vụ DEPOSIT_30: khách mất cọc, mở lại phòng.
     * Nghiệp vụ FULL_PAYMENT: xử lý check-in theo chính sách.
     */
    @Transactional
    public Booking markNoShowByPartner(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        validatePartnerOwnership(booking, partner);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Chỉ báo no-show được khi booking đang CONFIRMED! Hiện tại: " + booking.getBookingStatus().name());
        }
        if (booking.getPartnerStatus() != PartnerBookingStatus.PARTNER_CONFIRMED) {
            throw new RuntimeException("Cần xác nhận giữ phòng trước khi báo no-show!");
        }
        // Guard: chỉ báo no-show cho booking ONLINE (có thanh toán qua TravelMate)
        if (booking.getBookingSource() == BookingSource.DIRECT
                || booking.getBookingSource() == BookingSource.MANUAL_BLOCK) {
            throw new RuntimeException("⚠️ Không thể báo no-show cho booking trực tiếp (DIRECT) hoặc chặn phòng (MANUAL_BLOCK)! Chức năng này chỉ dành cho booking online qua TravelMate.");
        }

        if (booking.getPaymentOption() != PaymentOption.DEPOSIT_30) {
            throw new RuntimeException(
                "⚠️ No-show & mở lại phòng chỉ áp dụng cho đơn cọc 30% (DEPOSIT_30). " +
                "Với đơn đã thanh toán 100%, vui lòng liên hệ Admin để xử lý theo chính sách hoàn trả.");
        }

        booking.setBookingStatus(BookingStatus.NO_SHOW);
        booking.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
        appendNote(booking, "[Partner] Khách không đến nhận phòng. Cọc 30% bị giữ lại theo chính sách.");

        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            payment.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
            payment.setNote("Cọc 30% bị giữ lại do khách không đến check-in (Partner xác nhận no-show).");
            payment.setApprovedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
        });

        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    /**
     * Partner check-out / hoàn tất khi khách trả phòng.
     *
     * Rule:
     *   - Booking phải thuộc accommodation.owner = partner
     *   - BookingStatus phải là CHECKED_IN
     *   - partnerStatus phải là PARTNER_CONFIRMED
     *   - Mở lại phòng (restore availableQuantity) sau checkout
     */
    @Transactional
    public Booking markCompletedByPartner(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        validatePartnerOwnership(booking, partner);

        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException(
                    "Chỉ check-out được đơn đang CHECKED_IN! Hiện tại: " + booking.getBookingStatus().name());
        }
        if (booking.getPartnerStatus() != PartnerBookingStatus.PARTNER_CONFIRMED) {
            throw new RuntimeException("Đơn chưa được xác nhận giữ phòng!");
        }

        // Chặn check-out nếu đơn cọc 30% chưa xác nhận thu phần còn lại tại cơ sở
        if ((booking.getBookingSource() == null || booking.getBookingSource() == BookingSource.ONLINE)
                && booking.getPaymentOption() == PaymentOption.DEPOSIT_30
                && booking.getRemainingPaymentStatus() != RemainingPaymentStatus.PAID_AT_PROPERTY) {
            throw new RuntimeException(
                "⚠️ Đơn cọc 30% — vui lòng xác nhận đã thu đủ 70% còn lại tại cơ sở trước khi check-out!"
                + " Nhấn 'Xác nhận thu phần còn lại' để tiếp tục.");
        }

        // Mở lại phòng sau khi khách trả phòng
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_COMPLETED);
        appendNote(booking, "[Partner] Khách đã trả phòng. Check-out hoàn tất lúc "
                + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        Booking saved = bookingRepository.save(booking);
        // Chỉ tạo review reminder cho booking ONLINE của USER thực sự
        if ((saved.getBookingSource() == null || saved.getBookingSource() == BookingSource.ONLINE)
                && saved.getUser() != null
                && saved.getUser().getRole() == User.Role.USER) {
            notificationService.createReviewReminder(saved.getUser(), saved.getAccommodation(), saved.getId());
        }
        return saved;
    }

    /**
     * Partner hủy booking phía partner.
     *
     * Rule:
     *   - Booking phải thuộc accommodation.owner = partner
     *   - BookingStatus phải là CONFIRMED (chưa check-in)
     *   - Không cho hủy nếu đã CHECKED_IN/COMPLETED/NO_SHOW
     *
     * Lưu ý: hủy phía partner KHÔNG thay đổi bookingStatus (admin quản lý).
     * Chỉ set partnerStatus = PARTNER_CANCELLED để admin biết partner từ chối.
     */
    @Transactional
    public Booking cancelByPartner(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        // Ownership check
        validatePartnerOwnership(booking, partner);

        // Chỉ cho hủy khi booking đang CONFIRMED
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Chỉ hủy được đơn đang CONFIRMED! " +
                    "Hiện tại: " + booking.getBookingStatus().name());
        }

        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CANCELLED);
        appendNote(booking, "⚠️ Partner đã hủy xác nhận. Admin cần xem xét.");

        return bookingRepository.save(booking);
    }

    // ─── Direct Booking & Block Room (Partner) ───────────────────────────────

    /**
     * Partner tạo booking trực tiếp cho khách đến tại cơ sở.
     *
     * Nghiệp vụ:
     *   - Booking source = DIRECT
     *   - Không tạo payment (tiền thu trực tiếp tại cơ sở)
     *   - Không tính commission, không đưa vào settlement
     *   - Ảnh hưởng availability quota TravelMate (giảm phòng trống)
     *   - Trạng thái: CONFIRMED ngay lập tức (partner đã xác nhận)
     */
    @Transactional
    public Booking createDirectBooking(User partner, Room room, Accommodation accommodation,
                                       String customerName, String customerPhone, String customerEmail,
                                       LocalDate checkIn, LocalDate checkOut,
                                       int adults, int children, int roomQuantity,
                                       String note) {
        // Validate ownership
        if (accommodation.getOwner() == null || !accommodation.getOwner().getId().equals(partner.getId())) {
            throw new RuntimeException("Cơ sở này không thuộc quyền quản lý của bạn!");
        }
        if (room.getAccommodation() == null || !room.getAccommodation().getId().equals(accommodation.getId())) {
            throw new RuntimeException("Phòng này không thuộc cơ sở đã chọn!");
        }
        if (accommodation.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Cơ sở chưa được Admin duyệt!");
        }
        if (room.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Phòng chưa được Admin duyệt!");
        }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new RuntimeException("Ngày trả phòng phải sau ngày nhận phòng!");
        }
        if (roomQuantity < 1) {
            throw new RuntimeException("Số phòng đặt phải ít nhất là 1!");
        }
        if (adults < 1) {
            throw new RuntimeException("Số người lớn phải ít nhất là 1!");
        }
        // Validate sức chứa: tổng khách không vượt sức chứa tối đa * số phòng
        int maxGuests = room.getCapacity() * roomQuantity;
        if ((adults + children) > maxGuests) {
            throw new RuntimeException(
                "⚠️ Tổng số khách (" + (adults + children) + " người) vượt quá sức chứa tối đa của "
                + roomQuantity + " phòng (" + maxGuests + " người). Vui lòng tăng số phòng hoặc giảm số khách.");
        }

        // Kiểm tra availability (chống overbooking)
        room = roomRepository.findByIdForUpdate(room.getId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        List<BookingStatus> occupyStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PENDING_ADMIN_APPROVAL,
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN
        );
        int sumAllActive = bookingRepository.sumActiveQtyForRoom(room.getId(), occupyStatuses);
        int totalRooms = room.getAvailableQuantity() + sumAllActive;
        int occupiedInRange = bookingRepository.sumOverlappingQtyForRoom(
                room.getId(), checkIn, checkOut, occupyStatuses);
        int availableForRange = Math.max(0, totalRooms - occupiedInRange);

        if (availableForRange < roomQuantity) {
            throw new RuntimeException(
                "⚠️ Không đủ phòng trong khoảng ngày này! Còn trống: " + availableForRange
                + " phòng, bạn cần " + roomQuantity + " phòng.");
        }

        // Tạo booking trực tiếp
        Booking booking = new Booking();
        booking.setBookingCode("BK-PENDING");
        booking.setUser(partner); // Partner là người tạo
        booking.setAccommodation(accommodation);
        booking.setRoom(room);
        booking.setCustomerName(customerName != null && !customerName.isBlank() ? customerName : "Khách trực tiếp");
        booking.setCustomerPhone(customerPhone != null ? customerPhone : "");
        booking.setCustomerEmail(customerEmail != null ? customerEmail : "");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setAdults(adults > 0 ? adults : 1);
        booking.setChildren(children >= 0 ? children : 0);
        booking.setRoomQuantity(roomQuantity);

        // Tài chính: partner thu trực tiếp, không qua TravelMate
        long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
        BigDecimal total = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(roomQuantity));
        booking.setTotalAmount(total);
        booking.setTotalBeforeDiscount(total);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setPaidAmount(total); // Thu trực tiếp tại cơ sở
        booking.setRemainingAmount(BigDecimal.ZERO);
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);

        // Trạng thái
        booking.setBookingStatus(BookingStatus.CONFIRMED);      // Xác nhận ngay
        booking.setPaymentStatus(PaymentStatus.NOT_REQUIRED);   // Thu trực tiếp tại cơ sở, không qua TravelMate
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.DIRECT);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.NOT_REQUIRED);
        booking.setNote(note != null && !note.isBlank()
                ? "[Direct] " + note.trim()
                : "[Direct] Booking trực tiếp tại cơ sở — partner tạo thủ công.");

        booking = bookingRepository.save(booking);
        String code = "BK-" + room.getRoomCode() + "-D" + String.format("%06d", booking.getId());
        booking.setBookingCode(code);
        booking = bookingRepository.save(booking);

        // Giảm phòng trống
        room.setAvailableQuantity(room.getAvailableQuantity() - roomQuantity);
        roomRepository.save(room);

        return booking;
    }

    /**
     * Partner chặn phòng/căn (bảo trì, giữ nội bộ).
     *
     * Nghiệp vụ:
     *   - Booking source = MANUAL_BLOCK
     *   - Không tạo payment
     *   - Không tính commission, không settlement
     *   - Ảnh hưởng availability (giảm quota TravelMate)
     */
    @Transactional
    public Booking blockRoom(User partner, Room room, Accommodation accommodation,
                              LocalDate checkIn, LocalDate checkOut,
                              int roomQuantity, String blockReason) {
        // Validate ownership
        if (accommodation.getOwner() == null || !accommodation.getOwner().getId().equals(partner.getId())) {
            throw new RuntimeException("Cơ sở này không thuộc quyền quản lý của bạn!");
        }
        if (room.getAccommodation() == null || !room.getAccommodation().getId().equals(accommodation.getId())) {
            throw new RuntimeException("Phòng này không thuộc cơ sở đã chọn!");
        }
        // Guard: cơ sở và phòng phải được Admin duyệt mới cho chặn phòng
        if (accommodation.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("⚠️ Cơ sở chưa được Admin duyệt! Chỉ cơ sở APPROVED mới có thể chặn phòng.");
        }
        if (room.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("⚠️ Phòng chưa được Admin duyệt! Chỉ phòng APPROVED mới có thể chặn.");
        }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu!");
        }
        if (roomQuantity < 1) {
            throw new RuntimeException("Số phòng chặn phải ít nhất là 1!");
        }

        room = roomRepository.findByIdForUpdate(room.getId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        List<BookingStatus> occupyStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PENDING_ADMIN_APPROVAL,
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN
        );
        int sumAllActive = bookingRepository.sumActiveQtyForRoom(room.getId(), occupyStatuses);
        int totalRooms = room.getAvailableQuantity() + sumAllActive;
        int occupiedInRange = bookingRepository.sumOverlappingQtyForRoom(
                room.getId(), checkIn, checkOut, occupyStatuses);
        int availableForRange = Math.max(0, totalRooms - occupiedInRange);

        if (availableForRange < roomQuantity) {
            throw new RuntimeException(
                "⚠️ Không đủ phòng để chặn trong khoảng ngày này! Còn có thể chặn: "
                + availableForRange + " phòng.");
        }

        Booking block = new Booking();
        block.setBookingCode("BK-PENDING");
        block.setUser(partner);
        block.setAccommodation(accommodation);
        block.setRoom(room);
        block.setCustomerName("CHẶN PHÒNG");
        block.setCustomerPhone("");
        block.setCustomerEmail("");
        block.setCheckIn(checkIn);
        block.setCheckOut(checkOut);
        block.setAdults(0);
        block.setChildren(0);
        block.setRoomQuantity(roomQuantity);
        block.setTotalAmount(BigDecimal.ZERO);
        block.setTotalBeforeDiscount(BigDecimal.ZERO);
        block.setDiscountAmount(BigDecimal.ZERO);
        block.setPaidAmount(BigDecimal.ZERO);
        block.setRemainingAmount(BigDecimal.ZERO);
        block.setPaymentOption(PaymentOption.FULL_PAYMENT);
        block.setBookingStatus(BookingStatus.CONFIRMED);
        block.setPaymentStatus(PaymentStatus.NOT_REQUIRED);  // Không phát sinh thanh toán TravelMate
        block.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        block.setBookingSource(BookingSource.MANUAL_BLOCK);
        block.setRemainingPaymentStatus(RemainingPaymentStatus.NOT_REQUIRED);
        block.setBlockReason(blockReason != null ? blockReason.trim() : "Chặn phòng nội bộ");
        block.setNote("[Chặn phòng] " + (blockReason != null ? blockReason.trim() : "Chặn phòng nội bộ"));

        block = bookingRepository.save(block);
        String code = "BK-" + room.getRoomCode() + "-B" + String.format("%06d", block.getId());
        block.setBookingCode(code);
        block = bookingRepository.save(block);

        room.setAvailableQuantity(room.getAvailableQuantity() - roomQuantity);
        roomRepository.save(room);

        return block;
    }

    /**
     * Partner xác nhận đã thu 70% còn lại tại cơ sở (cho booking DEPOSIT_30 đang CHECKED_IN).
     *
     * Nghiệp vụ:
     *   - Chỉ áp dụng khi booking DEPOSIT_30 + CHECKED_IN
     *   - remainingPaymentStatus → PAID_AT_PROPERTY
     *   - remainingPaidAt = now()
     */
    @Transactional
    public Booking confirmRemainingPaymentByPartner(Long bookingId, User partner, String paymentNote) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        validatePartnerOwnership(booking, partner);

        if (booking.getPaymentOption() != PaymentOption.DEPOSIT_30) {
            throw new RuntimeException("Chỉ xác nhận thu tiền còn lại cho đơn cọc 30%!");
        }
        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Chỉ xác nhận thu tiền khi khách đang ở (CHECKED_IN)!");
        }
        if (booking.getRemainingPaymentStatus() == RemainingPaymentStatus.PAID_AT_PROPERTY) {
            throw new RuntimeException("Đơn này đã được xác nhận thu tiền rồi!");
        }

        booking.setRemainingPaymentStatus(RemainingPaymentStatus.PAID_AT_PROPERTY);
        booking.setRemainingPaidAt(LocalDateTime.now());
        String noteText = "[Partner] Đã thu đủ " +
                (booking.getRemainingAmount() != null
                        ? String.format("%,.0f₫", booking.getRemainingAmount().doubleValue())
                        : "khoản còn lại")
                + " tại cơ sở.";
        if (paymentNote != null && !paymentNote.isBlank()) {
            noteText += " Ghi chú: " + paymentNote.trim();
        }
        booking.setRemainingPaymentNote(paymentNote);
        appendNote(booking, noteText);

        return bookingRepository.save(booking);
    }

    /**
     * Hủy chặn phòng (MANUAL_BLOCK) — chỉ partner sở hữu mới được hủy.
     */
    @Transactional
    public Booking cancelBlock(Long blockId, User partner) {
        Booking block = bookingRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch chặn phòng!"));

        validatePartnerOwnership(block, partner);

        if (block.getBookingSource() != BookingSource.MANUAL_BLOCK) {
            throw new RuntimeException("Chỉ có thể hủy lịch chặn phòng!");
        }
        if (block.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Lịch chặn phòng này đã được hủy rồi!");
        }

        // Hoàn lại số phòng
        Room room = block.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + block.getRoomQuantity());
        roomRepository.save(room);

        block.setBookingStatus(BookingStatus.CANCELLED);
        block.setPaymentStatus(PaymentStatus.CANCELLED);
        appendNote(block, "[Partner] Đã hủy chặn phòng.");

        return bookingRepository.save(block);
    }

    /**
     * Hủy booking trực tiếp (DIRECT) — chỉ partner sở hữu mới được hủy.
     */
    @Transactional
    public Booking cancelDirectBooking(Long bookingId, User partner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking trực tiếp!"));

        validatePartnerOwnership(booking, partner);

        if (booking.getBookingSource() != BookingSource.DIRECT) {
            throw new RuntimeException("Chỉ có thể hủy booking trực tiếp qua chức năng này!");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy booking đã hoàn tất hoặc đã hủy!");
        }
        if (booking.getBookingStatus() == BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Không thể hủy booking khi khách đã check-in. Vui lòng thực hiện check-out trước!");
        }

        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.CANCELLED);
        appendNote(booking, "[Partner] Đã hủy booking trực tiếp.");

        return bookingRepository.save(booking);
    }

    /** Lấy tất cả booking thuộc partner (bao gồm DIRECT, MANUAL_BLOCK) */
    public List<Booking> getAllBookingsForPartner(User partner) {
        return bookingRepository.findAllByAccommodationOwnerOrderByCreatedAtDesc(partner);
    }

    /**
     * Partner ghi chú thêm cho booking (check-in thực tế, lưu ý, v.v.).
     */
    @Transactional
    public Booking addPartnerNote(Long bookingId, User partner, String note) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        // Ownership check
        validatePartnerOwnership(booking, partner);

        if (note == null || note.trim().isEmpty()) {
            throw new RuntimeException("Ghi chú không được để trống!");
        }

        appendNote(booking, "[Partner] " + note.trim());
        return bookingRepository.save(booking);
    }

    /**
     * Admin xử lý khi Partner hủy giữ phòng (partnerStatus = PARTNER_CANCELLED).
     * Hành động: hủy booking, hoàn lại phòng.
     */
    @Transactional
    public Booking handlePartnerCancelledByAdmin(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        if (booking.getPartnerStatus() != PartnerBookingStatus.PARTNER_CANCELLED) {
            throw new RuntimeException("Booking này chưa bị partner hủy!");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking này đã được hủy rồi!");
        }

        // Hủy booking — payment chuyển sang REFUND_PENDING (chờ hoàn tiền cho user)
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        String note = "Admin hủy đơn do partner từ chối giữ phòng. Đang tiến hành hoàn tiền cho khách";
        if (reason != null && !reason.isBlank()) note += ": " + reason.trim();
        appendNote(booking, note);
        bookingRepository.save(booking);

        // Cập nhật payment → REFUND_PENDING (hoàn tiền đang xử lý)
        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            payment.setNote("Hoàn tiền đang xử lý — Partner hủy giữ phòng, Admin đã xác nhận hủy đơn.");
            paymentRepository.save(payment);
        });

        // Trả lại phòng
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        return booking;
    }

    // ─── Mark Refunded ────────────────────────────────────────────────────────

    /**
     * Admin xác nhận đã chuyển khoản hoàn tiền cho khách.
     * Chuyển paymentStatus: REFUND_PENDING → REFUNDED.
     */
    @Transactional
    public Booking markRefundedByAdmin(Long bookingId, String note) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        if (booking.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new RuntimeException("Đơn không ở trạng thái chờ hoàn tiền (REFUND_PENDING)!");
        }

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        String logNote = "Admin đã xác nhận hoàn tiền cho khách";
        if (note != null && !note.isBlank()) logNote += ": " + note.trim();
        appendNote(booking, logNote);
        bookingRepository.save(booking);

        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            if (payment.getNote() == null) {
                payment.setNote("Đã hoàn tiền — Admin xác nhận.");
            } else {
                payment.setNote(payment.getNote() + " | Đã hoàn tiền — Admin xác nhận.");
            }
            paymentRepository.save(payment);
        });

        return booking;
    }

    // ─── Private helper ──────────────────────────────────────────────────────

    /** Validate: booking phải thuộc accommodation do partner sở hữu */
    private void validatePartnerOwnership(Booking booking, User partner) {
        if (booking.getAccommodation().getOwner() == null ||
                !booking.getAccommodation().getOwner().getId().equals(partner.getId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn này! Đơn không thuộc cơ sở của bạn.");
        }
    }

    /** Nối ghi chú vào note hiện tại (pipe-separated) */
    private void appendNote(Booking booking, String newNote) {
        String current = booking.getNote();
        booking.setNote(current != null && !current.isEmpty()
                ? current + " | " + newNote
                : newNote);
    }
}

/**
 * booking-store.js
 * Lưu trữ và quản lý đơn đặt phòng vào localStorage.
 * Pattern tương đồng với room-store.js.
 */
(function () {
  const STORAGE_KEY = 'travelmateBookings';

  const BOOKING_STATUSES = ['pending', 'confirmed', 'completed', 'cancelled'];

  const STATUS_LABELS = {
    pending:   'Chờ duyệt',
    confirmed: 'Xác nhận',
    completed: 'Hoàn thành',
    cancelled: 'Đã hủy',
  };

  const STATUS_BADGE_CLASSES = {
    pending:   'badge--warning',
    confirmed: 'badge--primary',
    completed: 'badge--success',
    cancelled: 'badge--danger',
  };

  /** Dữ liệu mẫu mặc định */
  const DEFAULT_BOOKINGS = [
    {
      id: 'BK-2026-0001',
      customerName: 'Nguyễn Văn An',
      phone: '0901 234 567',
      email: 'an.nguyen@email.com',
      hotel: 'Sunrise Sapa Lodge',
      roomName: 'Vip 1',
      roomId: 'room-default-1',
      checkIn: '2026-04-20',
      checkOut: '2026-04-23',
      nights: 3,
      totalPrice: 3600000,
      status: 'confirmed',
      note: '',
      createdAt: '17/04/2026 08:10:00',
    },
    {
      id: 'BK-2026-0002',
      customerName: 'Trần Thị Bình',
      phone: '0912 345 678',
      email: 'binh.tran@email.com',
      hotel: 'Mây Homestay Sa Pa',
      roomName: 'Garden Bungalow',
      roomId: 'room-default-3',
      checkIn: '2026-04-25',
      checkOut: '2026-04-28',
      nights: 3,
      totalPrice: 4260000,
      status: 'pending',
      note: 'Khách yêu cầu phòng tầng cao',
      createdAt: '17/04/2026 09:15:00',
    },
    {
      id: 'BK-2026-0003',
      customerName: 'Lê Minh Cường',
      phone: '0923 456 789',
      email: 'cuong.le@email.com',
      hotel: 'Sora Hotel Hanoi',
      roomName: 'Premium City View',
      roomId: 'room-default-2',
      checkIn: '2026-04-15',
      checkOut: '2026-04-17',
      nights: 2,
      totalPrice: 3560000,
      status: 'completed',
      note: '',
      createdAt: '14/04/2026 14:30:00',
    },
    {
      id: 'BK-2026-0004',
      customerName: 'Phạm Lan Anh',
      phone: '0934 567 890',
      email: 'anh.pham@email.com',
      hotel: 'Blue Coast Resort',
      roomName: 'Sunset Ocean Suite',
      roomId: 'room-default-4',
      checkIn: '2026-04-10',
      checkOut: '2026-04-12',
      nights: 2,
      totalPrice: 5720000,
      status: 'cancelled',
      note: 'Khách đổi kế hoạch',
      createdAt: '10/04/2026 10:00:00',
    },
    {
      id: 'BK-2026-0005',
      customerName: 'Hoàng Đức Thành',
      phone: '0945 678 901',
      email: 'thanh.hoang@email.com',
      hotel: 'Ocean Pearl Retreat',
      roomName: 'Cliff Pool Escape',
      roomId: 'room-default-1',
      checkIn: '2026-05-01',
      checkOut: '2026-05-04',
      nights: 3,
      totalPrice: 15600000,
      status: 'pending',
      note: 'Đặt cho tuần trăng mật',
      createdAt: '17/04/2026 11:00:00',
    },
    {
      id: 'BK-2026-0006',
      customerName: 'Vũ Thị Mai',
      phone: '0956 789 012',
      email: 'mai.vu@email.com',
      hotel: 'Sunrise Sapa Lodge',
      roomName: 'Deluxe Mountain View',
      roomId: 'room-default-2',
      checkIn: '2026-04-22',
      checkOut: '2026-04-24',
      nights: 2,
      totalPrice: 3300000,
      status: 'confirmed',
      note: '',
      createdAt: '16/04/2026 16:00:00',
    },
  ];

  // ── Helpers ────────────────────────────────────────────────────────────────
  function readJSON(key, fallback) {
    try {
      const raw = window.localStorage.getItem(key);
      if (!raw) return fallback;
      return JSON.parse(raw);
    } catch {
      return fallback;
    }
  }

  function writeJSON(key, value) {
    window.localStorage.setItem(key, JSON.stringify(value));
  }

  function createId() {
    const now = new Date();
    const seq = String(Date.now()).slice(-4);
    return `BK-${now.getFullYear()}-${seq}`;
  }

  function normalizeBooking(b) {
    const status = BOOKING_STATUSES.includes(b.status) ? b.status : 'pending';
    return {
      id: b.id || createId(),
      customerName: b.customerName || 'Khách hàng',
      phone: b.phone || '--',
      email: b.email || '',
      hotel: b.hotel || 'TravelMate',
      roomName: b.roomName || 'Phòng',
      roomId: b.roomId || '',
      checkIn: b.checkIn || '',
      checkOut: b.checkOut || '',
      nights: Number(b.nights) || 1,
      totalPrice: Number(b.totalPrice) || 0,
      status,
      note: b.note || '',
      createdAt: b.createdAt || new Date().toLocaleString('vi-VN'),
    };
  }

  function ensureBookings() {
    const stored = readJSON(STORAGE_KEY, null);
    if (Array.isArray(stored) && stored.length) {
      return stored.map(normalizeBooking);
    }
    const defaults = DEFAULT_BOOKINGS.map(normalizeBooking);
    writeJSON(STORAGE_KEY, defaults);
    return defaults;
  }

  // ── Public API ─────────────────────────────────────────────────────────────
  function getBookings() {
    return ensureBookings();
  }

  function getBookingById(id) {
    return getBookings().find((b) => b.id === id) || null;
  }

  function getBookingsByRoomId(roomId) {
    return getBookings().filter((b) => b.roomId === roomId);
  }

  function addBooking(payload) {
    const next = normalizeBooking({ ...payload, id: createId() });
    const bookings = getBookings();
    bookings.unshift(next);
    writeJSON(STORAGE_KEY, bookings);
    return next;
  }

  function updateBooking(id, updates) {
    const bookings = getBookings().map((b) =>
      b.id === id ? normalizeBooking({ ...b, ...updates }) : b
    );
    writeJSON(STORAGE_KEY, bookings);
    return bookings.find((b) => b.id === id) || null;
  }

  function deleteBooking(id) {
    const bookings = getBookings().filter((b) => b.id !== id);
    writeJSON(STORAGE_KEY, bookings);
    return bookings;
  }

  function getStats() {
    const bookings = getBookings();
    return {
      total: bookings.length,
      pending:   bookings.filter((b) => b.status === 'pending').length,
      confirmed: bookings.filter((b) => b.status === 'confirmed').length,
      completed: bookings.filter((b) => b.status === 'completed').length,
      cancelled: bookings.filter((b) => b.status === 'cancelled').length,
      revenue: bookings
        .filter((b) => b.status === 'completed' || b.status === 'confirmed')
        .reduce((s, b) => s + b.totalPrice, 0),
    };
  }

  function formatDate(dateStr) {
    if (!dateStr) return '--';
    const parts = dateStr.split('-');
    if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
    return dateStr;
  }

  function formatCurrency(value) {
    return `${Number(value || 0).toLocaleString('vi-VN')}đ`;
  }

  function getStatusLabel(status) {
    return STATUS_LABELS[status] || status;
  }

  function getStatusBadgeClass(status) {
    return STATUS_BADGE_CLASSES[status] || 'badge--muted';
  }

  window.TravelMateBookingStore = {
    BOOKING_STATUSES,
    STATUS_LABELS,
    getBookings,
    getBookingById,
    getBookingsByRoomId,
    addBooking,
    updateBooking,
    deleteBooking,
    getStats,
    formatDate,
    formatCurrency,
    getStatusLabel,
    getStatusBadgeClass,
  };
})();

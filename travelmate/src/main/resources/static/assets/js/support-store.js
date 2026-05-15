/**
 * support-store.js
 * Quản lý yêu cầu hỗ trợ từ Partner gửi lên Admin.
 */
(function () {
  const STORAGE_KEY = 'travelmateSupportTickets';

  const CATEGORIES = [
    { id: 'customer',   label: 'Hỗ trợ về khách hàng',      icon: '👥', color: '#0ea5e9', bg: '#e0f2fe' },
    { id: 'room-reg',   label: 'Hỗ trợ đăng ký phòng',      icon: '🏠', color: '#16a34a', bg: '#dcfce7' },
    { id: 'room-rent',  label: 'Hỗ trợ phòng cho thuê',     icon: '🛏',  color: '#7c3aed', bg: '#ede9fe' },
    { id: 'voucher',    label: 'Hỗ trợ voucher',             icon: '🎟',  color: '#d97706', bg: '#fef3c7' },
    { id: 'policy',     label: 'Chính sách của TravelMate',  icon: '📋', color: '#dc2626', bg: '#fee2e2' },
  ];

  const PRIORITIES = ['Thấp', 'Trung bình', 'Cao', 'Khẩn cấp'];

  const STATUSES = {
    open:        { label: 'Chờ xử lý',   badge: 'badge--warning' },
    in_progress: { label: 'Đang xử lý',  badge: 'badge--primary' },
    resolved:    { label: 'Đã giải quyết', badge: 'badge--success' },
    closed:      { label: 'Đã đóng',     badge: 'badge--muted'   },
  };

  const DEFAULT_TICKETS = [
    {
      id: 'TK-2026-0001',
      category: 'room-reg',
      subject: 'Phòng gửi không hiện trong danh sách chờ duyệt',
      description: 'Tôi đã gửi phòng từ 2 ngày trước nhưng vẫn chưa thấy phòng trong danh sách chờ duyệt của admin. Nhờ admin kiểm tra giúp.',
      priority: 'Cao',
      status: 'open',
      partnerName: 'Sunrise Sapa Lodge',
      partnerId: 'partner-001',
      createdAt: '16/04/2026 09:00:00',
      updatedAt: '16/04/2026 09:00:00',
      replies: [],
    },
    {
      id: 'TK-2026-0002',
      category: 'voucher',
      subject: 'Voucher giảm giá không áp dụng được cho phòng',
      description: 'Khi khách hàng nhập mã voucher SUMMER20, hệ thống báo lỗi "Voucher không hợp lệ" dù voucher vẫn còn hạn.',
      priority: 'Trung bình',
      status: 'in_progress',
      partnerName: 'Ocean Pearl Retreat',
      partnerId: 'partner-002',
      createdAt: '15/04/2026 14:30:00',
      updatedAt: '16/04/2026 10:15:00',
      replies: [
        { from: 'admin', name: 'Admin TravelMate', content: 'Chúng tôi đang kiểm tra lỗi voucher, sẽ phản hồi trong vòng 24h.', time: '16/04/2026 10:15:00' }
      ],
    },
  ];

  // ── Helpers ────────────────────────────────────────────────────────────────
  function read() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  function write(data) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }

  function ensure() {
    const stored = read();
    if (Array.isArray(stored) && stored.length) return stored;
    write(DEFAULT_TICKETS);
    return DEFAULT_TICKETS;
  }

  function makeId() {
    return `TK-${new Date().getFullYear()}-${String(Date.now()).slice(-4)}`;
  }

  function now() {
    return new Date().toLocaleString('vi-VN');
  }

  // ── Public API ─────────────────────────────────────────────────────────────
  function getTickets()           { return ensure(); }
  function getTicketById(id)      { return getTickets().find(t => t.id === id) || null; }
  function getCategoryById(id)    { return CATEGORIES.find(c => c.id === id) || null; }
  function getStatusInfo(status)  { return STATUSES[status] || STATUSES.open; }

  function addTicket(payload) {
    const ticket = {
      id: makeId(),
      category: payload.category || 'customer',
      subject: payload.subject || '',
      description: payload.description || '',
      priority: PRIORITIES.includes(payload.priority) ? payload.priority : 'Trung bình',
      status: 'open',
      partnerName: payload.partnerName || 'Partner',
      partnerId: payload.partnerId || '',
      createdAt: now(),
      updatedAt: now(),
      replies: [],
    };
    const tickets = getTickets();
    tickets.unshift(ticket);
    write(tickets);
    return ticket;
  }

  function updateTicketStatus(id, status) {
    const tickets = getTickets().map(t =>
      t.id === id ? { ...t, status, updatedAt: now() } : t
    );
    write(tickets);
  }

  function addReply(ticketId, { from, name, content }) {
    const tickets = getTickets().map(t => {
      if (t.id !== ticketId) return t;
      return {
        ...t,
        updatedAt: now(),
        status: from === 'admin' && t.status === 'open' ? 'in_progress' : t.status,
        replies: [...(t.replies || []), { from, name, content, time: now() }],
      };
    });
    write(tickets);
  }

  function getStats() {
    const all = getTickets();
    return {
      total: all.length,
      open:        all.filter(t => t.status === 'open').length,
      in_progress: all.filter(t => t.status === 'in_progress').length,
      resolved:    all.filter(t => t.status === 'resolved').length,
      closed:      all.filter(t => t.status === 'closed').length,
    };
  }

  window.TravelMateSupportStore = {
    CATEGORIES,
    PRIORITIES,
    STATUSES,
    getTickets,
    getTicketById,
    getCategoryById,
    getStatusInfo,
    addTicket,
    updateTicketStatus,
    addReply,
    getStats,
  };
})();

/**
 * admin-dashboard.js
 * Kéo số liệu thực từ room-store và voucher-store để cập nhật Dashboard Admin.
 */
document.addEventListener('DOMContentLoaded', () => {
  const roomStore    = window.TravelMateRoomStore;
  const bookingStore = window.TravelMateBookingStore;
  if (!roomStore) return;

  // ── 1. Lấy dữ liệu từ stores ─────────────────────────────────────────────
  const rooms = roomStore.getRooms();
  const totalRooms = rooms.length;
  const pendingRooms = rooms.filter((r) => r.approvalStatus === 'Chờ duyệt').length;
  const approvedRooms = rooms.filter((r) => r.approvalStatus === 'Đã duyệt').length;
  const avgPrice = totalRooms
    ? Math.round(rooms.reduce((s, r) => s + (Number(r.price) || 0), 0) / totalRooms)
    : 0;

  // Thống kê roomStatus
  const roomStatusCount = {};
  rooms.forEach((r) => {
    const key = r.roomStatus || 'Phòng trống';
    roomStatusCount[key] = (roomStatusCount[key] || 0) + 1;
  });

  // ── 2. Cập nhật stats cards ───────────────────────────────────────────────
  setCardValue('[data-dash="totalRooms"]', totalRooms);
  setCardValue('[data-dash="pendingRooms"]', pendingRooms);
  setCardValue('[data-dash="approvedRooms"]', approvedRooms);
  setCardValue('[data-dash="avgPrice"]', formatPrice(avgPrice));

  // Cập nhật trend text
  setCardTrend('[data-dash-trend="rooms"]', `${totalRooms} phòng đang quản lý`);
  setCardTrend('[data-dash-trend="pending"]', pendingRooms > 0 ? `↑ ${pendingRooms} cần duyệt` : '✓ Không có hồ sơ mới');
  setCardTrend('[data-dash-trend="approved"]', `${approvedRooms} / ${totalRooms} đã mở bán`);

  // Booking stats (nếu có booking-store)
  if (bookingStore) {
    const bs = bookingStore.getStats();
    setCardValue('[data-dash="totalBookings"]',   bs.total);
    setCardValue('[data-dash="pendingBookings"]', bs.pending);
    setCardValue('[data-dash="totalRevenue"]',    formatPrice(bs.revenue));
    setCardTrend('[data-dash-trend="bookings"]', `${bs.confirmed} đã xác nhận`);
    setCardTrend('[data-dash-trend="revenue"]',  `${bs.completed} đơn hoàn thành`);
  }


  // ── 3. Cập nhật bảng phòng gần đây ───────────────────────────────────────
  const recentBody = document.getElementById('dash-recent-rooms');
  if (recentBody) {
    const recent = rooms.slice(0, 5);
    if (!recent.length) {
      recentBody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--color-text-muted);padding:16px;">Chưa có phòng nào</td></tr>`;
    } else {
      recentBody.innerHTML = recent.map((room) => {
        const approvalClass = room.approvalStatus === 'Đã duyệt' ? 'badge--success'
          : room.approvalStatus === 'Từ chối' ? 'badge--danger' : 'badge--warning';
        const statusBadge = getRoomStatusBadge(room.roomStatus || room.status);
        return `
          <tr>
            <td style="font-weight:600">${escapeHtml(room.roomName)}</td>
            <td style="font-size:12px;color:var(--color-text-muted)">${escapeHtml(room.propertyName)}</td>
            <td>${escapeHtml(roomStore.getRoomTypeLabel(room))}</td>
            <td>${statusBadge}</td>
            <td><span class="badge ${approvalClass}">${escapeHtml(room.approvalStatus)}</span></td>
          </tr>
        `;
      }).join('');
    }
  }

  // ── 4. Cập nhật thống kê trạng thái phòng (mini bar) ─────────────────────
  const statsWrap = document.getElementById('dash-room-status-stats');
  if (statsWrap) {
    const ROOM_STATUS_CONFIG = {
      'Phòng trống':    { dot: '#22c55e' },
      'Đang được thuê': { dot: '#f97316' },
      'Đã đặt trước':   { dot: '#eab308' },
      'Hủy đặt phòng': { dot: '#ef4444' },
      'Gia hạn thuê':   { dot: '#ec4899' },
    };

    statsWrap.innerHTML = Object.entries(ROOM_STATUS_CONFIG).map(([label, cfg]) => {
      const count = roomStatusCount[label] || 0;
      const pct = totalRooms ? Math.round((count / totalRooms) * 100) : 0;
      return `
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px;">
          <span style="width:10px;height:10px;border-radius:50%;background:${cfg.dot};flex-shrink:0;"></span>
          <span style="flex:1;font-size:13px;">${label}</span>
          <span style="font-weight:600;font-size:13px;min-width:24px;text-align:right;">${count}</span>
          <div style="width:80px;height:6px;border-radius:999px;background:#e5e7eb;overflow:hidden;">
            <div style="width:${pct}%;height:100%;background:${cfg.dot};border-radius:999px;transition:width .4s;"></div>
          </div>
          <span style="font-size:11px;color:var(--color-text-muted);min-width:28px;text-align:right;">${pct}%</span>
        </div>
      `;
    }).join('');
  }

  // ── 5. Cập nhật ngày giờ header ──────────────────────────────────────────
  const dateEl = document.getElementById('dash-date');
  if (dateEl) {
    dateEl.textContent = `📅 ${new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })}`;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  function setCardValue(selector, value) {
    const el = document.querySelector(selector);
    if (el) el.textContent = String(value);
  }

  function setCardTrend(selector, text) {
    const el = document.querySelector(selector);
    if (el) el.textContent = text;
  }

  function formatPrice(value) {
    if (!value) return '0đ';
    if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
    if (value >= 1_000) return `${Math.round(value / 1_000)}K`;
    return `${value}đ`;
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function getRoomStatusBadge(status) {
    const MAP = {
      'Phòng trống':    { color: '#16a34a', bg: '#dcfce7', dot: '#22c55e' },
      'Đang được thuê': { color: '#c2410c', bg: '#ffedd5', dot: '#f97316' },
      'Đã đặt trước':   { color: '#b45309', bg: '#fef9c3', dot: '#eab308' },
      'Hủy đặt phòng': { color: '#b91c1c', bg: '#fee2e2', dot: '#ef4444' },
      'Gia hạn thuê':   { color: '#9d174d', bg: '#fce7f3', dot: '#ec4899' },
    };
    const cfg = MAP[status];
    if (!cfg) return `<span style="color:#6b7280;font-size:12px;">${escapeHtml(status || '--')}</span>`;
    return `<span style="display:inline-flex;align-items:center;gap:5px;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:600;background:${cfg.bg};color:${cfg.color};">
      <span style="width:6px;height:6px;border-radius:50%;background:${cfg.dot};"></span>${escapeHtml(status)}</span>`;
  }
});

/**
 * partner-bookings.js
 * Partner xem danh sách đặt phòng cho tất cả phòng của họ.
 * Hiện tại lấy toàn bộ booking từ booking-store (production sẽ filter theo partnerId).
 */
document.addEventListener('DOMContentLoaded', () => {
  const bookingStore = window.TravelMateBookingStore;
  const roomStore    = window.TravelMateRoomStore;
  if (!bookingStore) return;

  const tbody       = document.getElementById('partner-bookings-table');
  const searchInput = document.getElementById('partner-booking-search');
  const filterTabs  = document.querySelectorAll('[data-partner-booking-filter]');

  let currentFilter = 'all';

  // ── Stats ──────────────────────────────────────────────────────────────────
  function updateStats(bookings) {
    const set = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v; };
    const total     = bookings.length;
    const pending   = bookings.filter((b) => b.status === 'pending').length;
    const completed = bookings.filter((b) => b.status === 'completed').length;
    const revenue   = bookings
      .filter((b) => b.status === 'confirmed' || b.status === 'completed')
      .reduce((s, b) => s + b.totalPrice, 0);

    set('pstat-total',     total);
    set('pstat-pending',   pending);
    set('pstat-completed', completed);
    const rev = revenue >= 1_000_000
      ? `${(revenue / 1_000_000).toFixed(1)}M`
      : bookingStore.formatCurrency(revenue);
    set('pstat-revenue', rev);
  }

  // ── Tab counts ─────────────────────────────────────────────────────────────
  function updateTabCounts(bookings) {
    const map = {
      all: bookings.length,
      pending:   bookings.filter((b) => b.status === 'pending').length,
      confirmed: bookings.filter((b) => b.status === 'confirmed').length,
      completed: bookings.filter((b) => b.status === 'completed').length,
      cancelled: bookings.filter((b) => b.status === 'cancelled').length,
    };
    filterTabs.forEach((tab) => {
      const key = tab.getAttribute('data-partner-booking-filter');
      const el  = tab.querySelector('[data-tab-count]');
      if (el) el.textContent = `(${map[key] ?? 0})`;
    });
  }

  // ── Get partnered bookings ─────────────────────────────────────────────────
  function getPartnerBookings() {
    /**
     * Trong production: lọc bookings có roomId thuộc partner hiện tại.
     * Hiện tại demo: trả về toàn bộ vì không có auth system.
     */
    return bookingStore.getBookings();
  }

  // ── Filtered list ──────────────────────────────────────────────────────────
  function getFiltered(allBookings) {
    const query = (searchInput?.value || '').trim().toLowerCase();
    return allBookings.filter((b) => {
      if (currentFilter !== 'all' && b.status !== currentFilter) return false;
      if (query) {
        const hay = [b.id, b.customerName, b.phone, b.hotel, b.roomName].join(' ').toLowerCase();
        if (!hay.includes(query)) return false;
      }
      return true;
    });
  }

  // ── Render ─────────────────────────────────────────────────────────────────
  function render() {
    if (!tbody) return;

    const allBookings = getPartnerBookings();
    updateStats(allBookings);
    updateTabCounts(allBookings);

    const filtered = getFiltered(allBookings);

    if (!filtered.length) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align:center;padding:32px;color:var(--color-text-muted);">
            <div style="font-size:32px;margin-bottom:8px;">📭</div>
            Chưa có đơn đặt phòng nào
          </td>
        </tr>`;
      return;
    }

    tbody.innerHTML = filtered.map((b) => {
      const badge = bookingStore.getStatusBadgeClass(b.status);
      const label = bookingStore.getStatusLabel(b.status);

      let actions = `<button class="btn btn--outline btn--sm btn-partner-view" data-id="${esc(b.id)}">Chi tiết</button>`;

      if (b.status === 'confirmed') {
        actions += `
          <button class="btn btn--sm btn-checkin" data-id="${esc(b.id)}"
            style="background:#0ea5e9;color:#fff;border:none;border-radius:6px;padding:5px 10px;cursor:pointer;font-size:12px;">
            Check-in
          </button>`;
      }
      if (b.status === 'pending') {
        actions += `
          <button class="btn btn--sm btn-confirm-booking" data-id="${esc(b.id)}"
            style="background:var(--color-success);color:#fff;border:none;border-radius:6px;padding:5px 10px;cursor:pointer;font-size:12px;">
            Xác nhận
          </button>`;
      }

      const strikeStyle = b.status === 'cancelled' ? 'text-decoration:line-through;color:var(--color-text-muted);' : '';

      return `
        <tr>
          <td style="font-family:monospace;font-size:12px;color:var(--color-primary);font-weight:600;">${esc(b.id)}</td>
          <td>
            <div style="font-weight:600;">${esc(b.customerName)}</div>
            <div style="font-size:12px;color:var(--color-text-muted);">${esc(b.phone)}</div>
          </td>
          <td>
            <div style="font-weight:500;">${esc(b.roomName)}</div>
            <div style="font-size:12px;color:var(--color-text-muted);">${esc(b.hotel)}</div>
          </td>
          <td>${bookingStore.formatDate(b.checkIn)}</td>
          <td>${bookingStore.formatDate(b.checkOut)}</td>
          <td style="text-align:center;">${b.nights}</td>
          <td style="font-weight:600;color:var(--color-primary);${strikeStyle}">
            ${bookingStore.formatCurrency(b.totalPrice)}
          </td>
          <td><span class="badge ${badge}">${esc(label)}</span></td>
          <td>
            <div class="table__actions">${actions}</div>
          </td>
        </tr>`;
    }).join('');
  }

  // ── Detail modal ───────────────────────────────────────────────────────────
  function showDetail(booking) {
    let modal = document.getElementById('pb-detail-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'pb-detail-modal';
      modal.style.cssText =
        'position:fixed;inset:0;z-index:9999;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.45);';
      document.body.appendChild(modal);
    }

    const badge = bookingStore.getStatusBadgeClass(booking.status);
    const label = bookingStore.getStatusLabel(booking.status);

    modal.innerHTML = `
      <div style="background:#fff;border-radius:16px;width:min(500px,95vw);padding:28px;box-shadow:0 20px 60px rgba(0,0,0,.25);">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
          <div>
            <div style="font-size:18px;font-weight:700;margin-bottom:4px;">📋 ${esc(booking.id)}</div>
            <span class="badge ${badge}">${esc(label)}</span>
          </div>
          <button id="pb-close" style="background:none;border:none;font-size:22px;cursor:pointer;color:#6b7280;">✕</button>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:14px;">
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">KHÁCH HÀNG</div>
            <div style="font-weight:600;">${esc(booking.customerName)}</div>
            <div style="color:#6b7280;">${esc(booking.phone)}</div>
            <div style="color:#6b7280;">${esc(booking.email)}</div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">PHÒNG</div>
            <div style="font-weight:600;">${esc(booking.roomName)}</div>
            <div style="color:#6b7280;">${esc(booking.hotel)}</div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">THỜI GIAN LƯU TRÚ</div>
            <div>Nhận: <strong>${bookingStore.formatDate(booking.checkIn)}</strong></div>
            <div>Trả: <strong>${bookingStore.formatDate(booking.checkOut)}</strong></div>
            <div>${booking.nights} đêm</div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">DOANH THU</div>
            <div style="font-size:20px;font-weight:700;color:var(--color-primary);">${bookingStore.formatCurrency(booking.totalPrice)}</div>
          </div>
        </div>
        ${booking.note ? `<div style="margin-top:12px;background:#fef9c3;border-radius:8px;padding:12px;font-size:13px;">📝 ${esc(booking.note)}</div>` : ''}
        <div style="margin-top:20px;text-align:right;">
          <button id="pb-close-2" class="btn btn--ghost btn--sm">Đóng</button>
        </div>
      </div>`;

    const close = () => modal.remove();
    modal.querySelector('#pb-close').addEventListener('click', close);
    modal.querySelector('#pb-close-2').addEventListener('click', close);
    modal.addEventListener('click', (e) => { if (e.target === modal) close(); });
  }

  // ── Events ─────────────────────────────────────────────────────────────────
  filterTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      filterTabs.forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      currentFilter = tab.getAttribute('data-partner-booking-filter');
      render();
    });
  });

  if (searchInput) searchInput.addEventListener('input', render);

  if (tbody) {
    tbody.addEventListener('click', (e) => {
      const viewBtn = e.target.closest('.btn-partner-view');
      if (viewBtn) {
        const b = bookingStore.getBookingById(viewBtn.getAttribute('data-id'));
        if (b) showDetail(b);
        return;
      }
      const confirmBtn = e.target.closest('.btn-confirm-booking');
      if (confirmBtn) {
        const id = confirmBtn.getAttribute('data-id');
        bookingStore.updateBooking(id, { status: 'confirmed' });
        render();
        if (typeof showToast === 'function') showToast('Đã xác nhận đặt phòng', 'success');
        return;
      }
      const checkinBtn = e.target.closest('.btn-checkin');
      if (checkinBtn) {
        const id = checkinBtn.getAttribute('data-id');
        bookingStore.updateBooking(id, { status: 'completed' });
        render();
        if (typeof showToast === 'function') showToast('Đã xác nhận check-in thành công', 'success');
      }
    });
  }

  function esc(v) {
    return String(v ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  render();
});

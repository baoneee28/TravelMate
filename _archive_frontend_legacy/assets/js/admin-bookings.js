/**
 * admin-bookings.js
 * Quản lý đặt phòng: render bảng, filter tabs, tìm kiếm, duyệt/hủy.
 */
document.addEventListener('DOMContentLoaded', () => {
  const store = window.TravelMateBookingStore;
  if (!store) return;

  const tbody       = document.getElementById('admin-bookings-table');
  const searchInput = document.getElementById('booking-search');
  const filterTabs  = document.querySelectorAll('[data-booking-filter]');
  const fromDate    = document.getElementById('booking-from-date');
  const toDate      = document.getElementById('booking-to-date');

  let currentFilter = 'all';

  // ── Render stats tabs ──────────────────────────────────────────────────────
  function updateTabCounts() {
    const stats = store.getStats();
    const map = {
      all: stats.total,
      pending: stats.pending,
      confirmed: stats.confirmed,
      completed: stats.completed,
      cancelled: stats.cancelled,
    };
    filterTabs.forEach((tab) => {
      const key = tab.getAttribute('data-booking-filter');
      const countEl = tab.querySelector('[data-tab-count]');
      if (countEl) countEl.textContent = `(${map[key] ?? 0})`;
    });
  }

  // ── Filter + search ────────────────────────────────────────────────────────
  function getFilteredBookings() {
    const query = (searchInput?.value || '').trim().toLowerCase();
    const from  = fromDate?.value || '';
    const to    = toDate?.value   || '';

    return store.getBookings().filter((b) => {
      if (currentFilter !== 'all' && b.status !== currentFilter) return false;

      if (query) {
        const haystack = [b.id, b.customerName, b.phone, b.hotel, b.roomName]
          .join(' ').toLowerCase();
        if (!haystack.includes(query)) return false;
      }

      if (from && b.checkIn && b.checkIn < from) return false;
      if (to   && b.checkIn && b.checkIn > to)   return false;

      return true;
    });
  }

  // ── Render table ───────────────────────────────────────────────────────────
  function renderTable() {
    if (!tbody) return;

    updateTabCounts();
    const bookings = getFilteredBookings();

    if (!bookings.length) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align:center;padding:32px;color:var(--color-text-muted);">
            <div style="font-size:32px;margin-bottom:8px;">📭</div>
            Không có đơn đặt phòng nào phù hợp
          </td>
        </tr>`;
      return;
    }

    tbody.innerHTML = bookings.map((b) => {
      const badgeClass = store.getStatusBadgeClass(b.status);
      const statusLabel = store.getStatusLabel(b.status);
      const isPending = b.status === 'pending';
      const isDone = b.status === 'completed' || b.status === 'cancelled';

      const actions = isDone
        ? `<button class="btn btn--outline btn--sm btn-view-booking" data-id="${esc(b.id)}">Chi tiết</button>`
        : `
          ${isPending ? `
            <button class="btn btn--sm btn-approve-booking" data-id="${esc(b.id)}"
              style="background:var(--color-success);color:#fff;border:none;border-radius:6px;padding:5px 12px;cursor:pointer;">
              ✓ Duyệt
            </button>` : ''}
          <button class="btn btn--danger btn--sm btn-cancel-booking" data-id="${esc(b.id)}" data-code="${esc(b.id)}">
            Hủy
          </button>`;

      return `
        <tr data-booking-status="${esc(b.status)}">
          <td style="font-family:monospace;font-size:12px;color:var(--color-primary);font-weight:600;">${esc(b.id)}</td>
          <td>
            <div style="font-weight:600;">${esc(b.customerName)}</div>
            <div style="font-size:12px;color:var(--color-text-muted);">${esc(b.phone)}</div>
          </td>
          <td>
            <div style="font-weight:500;">${esc(b.hotel)}</div>
            <div style="font-size:12px;color:var(--color-text-muted);">${esc(b.roomName)}</div>
          </td>
          <td>${store.formatDate(b.checkIn)}</td>
          <td>${store.formatDate(b.checkOut)}</td>
          <td style="text-align:center;">${b.nights}</td>
          <td style="font-weight:600;color:var(--color-primary);${b.status === 'cancelled' ? 'text-decoration:line-through;color:var(--color-text-muted);' : ''}">
            ${store.formatCurrency(b.totalPrice)}
          </td>
          <td><span class="badge ${badgeClass}">${esc(statusLabel)}</span></td>
          <td>
            <div class="table__actions">${actions}</div>
            ${b.note ? `<div style="font-size:11px;color:var(--color-text-muted);margin-top:4px;">📝 ${esc(b.note)}</div>` : ''}
          </td>
        </tr>`;
    }).join('');

    // Update result count label
    const countEl = document.getElementById('booking-result-count');
    if (countEl) countEl.textContent = `${bookings.length} đơn`;
  }

  // ── Modal chi tiết booking ─────────────────────────────────────────────────
  function showDetailModal(booking) {
    let modal = document.getElementById('booking-detail-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'booking-detail-modal';
      modal.style.cssText = `
        position:fixed;inset:0;z-index:9999;display:flex;align-items:center;justify-content:center;
        background:rgba(0,0,0,0.45);animation:fadeIn .15s ease;`;
      document.body.appendChild(modal);
    }

    const badge = store.getStatusBadgeClass(booking.status);
    const label = store.getStatusLabel(booking.status);

    modal.innerHTML = `
      <div style="background:#fff;border-radius:16px;width:min(520px,95vw);padding:28px;box-shadow:0 20px 60px rgba(0,0,0,.25);animation:slideUp .2s ease;">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
          <div>
            <div style="font-size:18px;font-weight:700;margin-bottom:4px;">📋 ${esc(booking.id)}</div>
            <span class="badge ${badge}">${esc(label)}</span>
          </div>
          <button id="close-booking-modal" style="background:none;border:none;font-size:22px;cursor:pointer;color:#6b7280;">✕</button>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:14px;">
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">KHÁCH HÀNG</div>
            <div style="font-weight:600;">${esc(booking.customerName)}</div>
            <div style="color:#6b7280;">${esc(booking.phone)}</div>
            <div style="color:#6b7280;">${esc(booking.email)}</div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">PHÒNG / KHÁCH SẠN</div>
            <div style="font-weight:600;">${esc(booking.roomName)}</div>
            <div style="color:#6b7280;">${esc(booking.hotel)}</div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">THỜI GIAN</div>
            <div><span style="color:#6b7280;">Nhận:</span> <strong>${store.formatDate(booking.checkIn)}</strong></div>
            <div><span style="color:#6b7280;">Trả:</span> <strong>${store.formatDate(booking.checkOut)}</strong></div>
            <div><span style="color:#6b7280;">Số đêm:</span> <strong>${booking.nights}</strong></div>
          </div>
          <div style="background:#f9fafb;border-radius:8px;padding:12px;">
            <div style="font-size:11px;color:#6b7280;margin-bottom:4px;">THANH TOÁN</div>
            <div style="font-size:20px;font-weight:700;color:var(--color-primary);">${store.formatCurrency(booking.totalPrice)}</div>
            <div style="font-size:12px;color:#6b7280;">Tạo lúc: ${esc(booking.createdAt)}</div>
          </div>
        </div>
        ${booking.note ? `<div style="margin-top:12px;background:#fef9c3;border-radius:8px;padding:12px;font-size:13px;">📝 <strong>Ghi chú:</strong> ${esc(booking.note)}</div>` : ''}
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
          ${booking.status === 'pending' ? `
            <button class="btn btn--sm modal-approve-btn" data-id="${esc(booking.id)}"
              style="background:var(--color-success);color:#fff;border:none;border-radius:6px;padding:8px 16px;cursor:pointer;font-weight:600;">
              ✓ Duyệt ngay
            </button>
            <button class="btn btn--danger btn--sm modal-cancel-btn" data-id="${esc(booking.id)}">Hủy booking</button>
          ` : ''}
          <button id="close-booking-modal-2" class="btn btn--ghost btn--sm">Đóng</button>
        </div>
      </div>`;

    const closeModal = () => modal.remove();
    modal.querySelector('#close-booking-modal')?.addEventListener('click', closeModal);
    modal.querySelector('#close-booking-modal-2')?.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

    modal.querySelector('.modal-approve-btn')?.addEventListener('click', () => {
      store.updateBooking(booking.id, { status: 'confirmed' });
      closeModal();
      renderTable();
      if (typeof showToast === 'function') showToast(`Đã duyệt booking ${booking.id}`, 'success');
    });

    modal.querySelector('.modal-cancel-btn')?.addEventListener('click', () => {
      if (!confirm(`Hủy booking ${booking.id}?`)) return;
      store.updateBooking(booking.id, { status: 'cancelled' });
      closeModal();
      renderTable();
      if (typeof showToast === 'function') showToast(`Đã hủy booking ${booking.id}`, 'info');
    });
  }

  // ── Event listeners ────────────────────────────────────────────────────────
  filterTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      filterTabs.forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      currentFilter = tab.getAttribute('data-booking-filter');
      renderTable();
    });
  });

  if (searchInput) searchInput.addEventListener('input', renderTable);
  if (fromDate)    fromDate.addEventListener('change', renderTable);
  if (toDate)      toDate.addEventListener('change', renderTable);

  if (tbody) {
    tbody.addEventListener('click', (e) => {
      const approveBtn = e.target.closest('.btn-approve-booking');
      if (approveBtn) {
        const id = approveBtn.getAttribute('data-id');
        store.updateBooking(id, { status: 'confirmed' });
        renderTable();
        if (typeof showToast === 'function') showToast(`Đã duyệt booking ${id}`, 'success');
        return;
      }

      const cancelBtn = e.target.closest('.btn-cancel-booking');
      if (cancelBtn) {
        const id = cancelBtn.getAttribute('data-id');
        if (!confirm(`Hủy booking ${id}?`)) return;
        store.updateBooking(id, { status: 'cancelled' });
        renderTable();
        if (typeof showToast === 'function') showToast(`Đã hủy booking ${id}`, 'info');
        return;
      }

      const viewBtn = e.target.closest('.btn-view-booking');
      if (viewBtn) {
        const id = viewBtn.getAttribute('data-id');
        const booking = store.getBookingById(id);
        if (booking) showDetailModal(booking);
      }
    });
  }

  // ── Helper ─────────────────────────────────────────────────────────────────
  function esc(v) {
    return String(v ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── Init ───────────────────────────────────────────────────────────────────
  renderTable();
});

/**
 * admin-support.js
 * Admin xem, phân loại và phản hồi yêu cầu hỗ trợ từ Partner.
 */
document.addEventListener('DOMContentLoaded', () => {
  const store = window.TravelMateSupportStore;
  if (!store) return;

  const tbody          = document.getElementById('admin-support-table');
  const searchInput    = document.getElementById('support-search');
  const priorityFilter = document.getElementById('priority-filter');
  const catFilter      = document.getElementById('admin-cat-filter');
  const filterTabs     = document.querySelectorAll('[data-support-filter]');

  let currentFilter = 'all';
  let activeTicketId = null;

  // ── Populate category filter select ─────────────────────────────────────
  if (catFilter) {
    store.CATEGORIES.forEach(cat => {
      const opt = document.createElement('option');
      opt.value = cat.id;
      opt.textContent = `${cat.icon} ${cat.label}`;
      catFilter.appendChild(opt);
    });
    catFilter.addEventListener('change', render);
  }

  // ── Stats ────────────────────────────────────────────────────────────────
  function updateStats() {
    const s = store.getStats();
    const set = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v; };
    set('stat-total',      s.total);
    set('stat-open',       s.open);
    set('stat-inprogress', s.in_progress);
    set('stat-resolved',   s.resolved);
  }

  // ── Tab counts ────────────────────────────────────────────────────────────
  function updateTabCounts(tickets) {
    const map = {
      all:         tickets.length,
      open:        tickets.filter(t => t.status === 'open').length,
      in_progress: tickets.filter(t => t.status === 'in_progress').length,
      resolved:    tickets.filter(t => t.status === 'resolved').length,
      closed:      tickets.filter(t => t.status === 'closed').length,
    };
    filterTabs.forEach(tab => {
      const key = tab.getAttribute('data-support-filter');
      const el  = tab.querySelector('[data-tab-count]');
      if (el) el.textContent = `(${map[key] ?? 0})`;
    });
  }

  // ── Filter ────────────────────────────────────────────────────────────────
  function getFiltered() {
    const query    = (searchInput?.value || '').toLowerCase().trim();
    const priority = priorityFilter?.value || 'all';
    const cat      = catFilter?.value || 'all';

    return store.getTickets().filter(t => {
      if (currentFilter !== 'all' && t.status !== currentFilter) return false;
      if (priority !== 'all' && t.priority !== priority) return false;
      if (cat !== 'all' && t.category !== cat) return false;
      if (query) {
        const hay = [t.id, t.subject, t.partnerName, t.description].join(' ').toLowerCase();
        if (!hay.includes(query)) return false;
      }
      return true;
    });
  }

  // ── Priority badge ────────────────────────────────────────────────────────
  const PRIORITY_CFG = {
    'Khẩn cấp':   { color: '#dc2626', dot: '#ef4444' },
    'Cao':         { color: '#c2410c', dot: '#f97316' },
    'Trung bình':  { color: '#b45309', dot: '#f59e0b' },
    'Thấp':        { color: '#15803d', dot: '#22c55e' },
  };

  function priorityBadge(p) {
    const cfg = PRIORITY_CFG[p] || { color: '#6b7280', dot: '#9ca3af' };
    return `<span style="display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;color:${cfg.color};">
      <span style="width:7px;height:7px;border-radius:50%;background:${cfg.dot};"></span>${esc(p)}
    </span>`;
  }

  // ── Render table ──────────────────────────────────────────────────────────
  function render() {
    if (!tbody) return;
    updateStats();
    const all      = store.getTickets();
    const filtered = getFiltered();
    updateTabCounts(all);

    if (!filtered.length) {
      tbody.innerHTML = `
        <tr><td colspan="8" style="text-align:center;padding:40px;color:var(--color-text-muted);">
          <div style="font-size:32px;margin-bottom:8px;">📭</div>
          Không có yêu cầu hỗ trợ nào
        </td></tr>`;
      return;
    }

    tbody.innerHTML = filtered.map(t => {
      const cat    = store.getCategoryById(t.category);
      const status = store.getStatusInfo(t.status);
      const replyCount = (t.replies || []).length;
      const hasAdminReply = (t.replies || []).some(r => r.from === 'admin');

      return `
        <tr>
          <td style="font-family:monospace;font-size:12px;color:var(--color-primary);font-weight:600;">${esc(t.id)}</td>
          <td>
            <span class="ticket-row-cat"
              style="background:${cat?.bg || '#f3f4f6'};color:${cat?.color || '#374151'};">
              ${cat?.icon || '📋'} ${esc(cat?.label || t.category)}
            </span>
          </td>
          <td>
            <div style="font-weight:600;font-size:13px;max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${esc(t.subject)}
            </div>
            ${replyCount > 0 ? `<div style="font-size:11px;color:${hasAdminReply ? '#6b7280' : 'var(--color-primary)'};margin-top:2px;">
              💬 ${replyCount} phản hồi${!hasAdminReply ? ' • <strong>Chưa Admin trả lời</strong>' : ''}
            </div>` : '<div style="font-size:11px;color:#9ca3af;margin-top:2px;">Chưa có phản hồi</div>'}
          </td>
          <td style="font-size:13px;font-weight:500;">${esc(t.partnerName)}</td>
          <td>${priorityBadge(t.priority)}</td>
          <td><span class="badge ${status.badge}">${esc(status.label)}</span></td>
          <td style="font-size:12px;color:var(--color-text-muted);">${esc(t.createdAt)}</td>
          <td>
            <div class="table__actions">
              <button class="btn btn--primary btn--sm btn-open-ticket" data-id="${esc(t.id)}">
                Phản hồi →
              </button>
            </div>
          </td>
        </tr>`;
    }).join('');

    tbody.querySelectorAll('.btn-open-ticket').forEach(btn => {
      btn.addEventListener('click', () => openModal(btn.getAttribute('data-id')));
    });
  }

  // ── Filter events ─────────────────────────────────────────────────────────
  filterTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      filterTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentFilter = tab.getAttribute('data-support-filter');
      render();
    });
  });

  if (searchInput)    searchInput.addEventListener('input', render);
  if (priorityFilter) priorityFilter.addEventListener('change', render);

  // ── Modal ─────────────────────────────────────────────────────────────────
  const modal     = document.getElementById('admin-ticket-modal');
  const atmClose  = document.getElementById('atm-close');
  const atmStatus = document.getElementById('atm-status-select');
  const atmUpdate = document.getElementById('atm-update-status');
  const atmChat   = document.getElementById('atm-chat');
  const atmReply  = document.getElementById('atm-reply-input');
  const atmSend   = document.getElementById('atm-reply-send');

  function openModal(id) {
    const ticket = store.getTicketById(id);
    if (!ticket) return;
    activeTicketId = id;

    const cat    = store.getCategoryById(ticket.category);
    const status = store.getStatusInfo(ticket.status);

    document.getElementById('atm-id').textContent = ticket.id;
    document.getElementById('atm-subject').textContent = ticket.subject;

    const meta = document.getElementById('atm-meta');
    meta.innerHTML = `
      <span class="badge ${status.badge}">${esc(status.label)}</span>
      <span style="color:#6b7280;">${cat?.icon || '📋'} ${esc(cat?.label || ticket.category)}</span>
      <span>${priorityBadge(ticket.priority)}</span>
      <span style="color:#6b7280;">👤 ${esc(ticket.partnerName)}</span>
      <span style="color:#6b7280;">🕐 ${esc(ticket.createdAt)}</span>`;

    if (atmStatus) atmStatus.value = ticket.status;

    renderModalChat(ticket);
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';

    // auto-move to in_progress if it's open
    if (ticket.status === 'open') {
      store.updateTicketStatus(id, 'in_progress');
      if (atmStatus) atmStatus.value = 'in_progress';
      render();
    }
  }

  function renderModalChat(ticket) {
    if (!atmChat) return;
    let html = `
      <!-- Description as first partner message -->
      <div class="chat-bubble-wrap partner">
        <div class="chat-meta">👤 ${esc(ticket.partnerName)} • ${esc(ticket.createdAt)}</div>
        <div class="chat-bubble partner-msg">${esc(ticket.description)}</div>
      </div>`;

    (ticket.replies || []).forEach(r => {
      const isAdmin = r.from === 'admin';
      html += `
        <div class="chat-bubble-wrap ${isAdmin ? 'admin' : 'partner'}">
          <div class="chat-meta">${isAdmin ? '🛡 ' : '👤 '}${esc(r.name)} • ${esc(r.time)}</div>
          <div class="chat-bubble ${isAdmin ? 'admin-msg' : 'partner-msg'}">${esc(r.content)}</div>
        </div>`;
    });

    atmChat.innerHTML = html;
    atmChat.scrollTop = atmChat.scrollHeight;
  }

  // Close modal
  function closeModal() {
    if (modal) modal.style.display = 'none';
    activeTicketId = null;
  }

  atmClose?.addEventListener('click', closeModal);
  modal?.addEventListener('click', e => {
    if (e.target === modal) closeModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal?.style.display === 'flex') closeModal();
  });

  // Update status
  atmUpdate?.addEventListener('click', () => {
    if (!activeTicketId || !atmStatus) return;
    store.updateTicketStatus(activeTicketId, atmStatus.value);
    render();
    if (typeof showToast === 'function') showToast('Đã cập nhật trạng thái ticket', 'success');
    // Refresh meta badge in modal
    const ticket = store.getTicketById(activeTicketId);
    if (ticket) {
      const s = store.getStatusInfo(ticket.status);
      const statusSpan = document.querySelector('#atm-meta .badge');
      if (statusSpan) { statusSpan.className = `badge ${s.badge}`; statusSpan.textContent = s.label; }
    }
  });

  // Send reply
  atmSend?.addEventListener('click', sendAdminReply);
  atmReply?.addEventListener('keydown', e => {
    if (e.key === 'Enter' && e.ctrlKey) sendAdminReply();
  });

  function sendAdminReply() {
    const content = atmReply?.value.trim();
    if (!content || !activeTicketId) return;
    store.addReply(activeTicketId, {
      from: 'admin',
      name: 'Admin TravelMate',
      content,
    });
    atmReply.value = '';
    const ticket = store.getTicketById(activeTicketId);
    if (ticket) renderModalChat(ticket);
    render();
    if (typeof showToast === 'function') showToast('Đã gửi phản hồi đến Partner', 'success');
  }

  // Quick reply buttons
  document.querySelectorAll('.quick-reply-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      if (atmReply) atmReply.value = btn.getAttribute('data-text');
      atmReply?.focus();
    });
  });

  // ── Helper ────────────────────────────────────────────────────────────────
  function esc(v) {
    return String(v ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── Init ──────────────────────────────────────────────────────────────────
  render();
});

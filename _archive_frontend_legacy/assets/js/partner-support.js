/**
 * partner-support.js
 * Logic cho trang Partner — Trung tâm hỗ trợ.
 */
document.addEventListener('DOMContentLoaded', () => {
  const store = window.TravelMateSupportStore;
  if (!store) return;

  // ── State ───────────────────────────────────────────────────────────────
  let currentTicketFilter = 'all';
  let currentFaqCat = store.CATEGORIES[0].id;
  let activeTicketId = null;

  // ── FAQ data theo 5 danh mục ───────────────────────────────────────────
  const FAQ_DATA = {
    'customer': [
      { q: 'Khách hàng đánh giá xấu phòng của tôi, tôi có thể khiếu nại không?', a: 'Có. Bạn có thể gửi yêu cầu qua mục "Hỗ trợ về khách hàng". Admin sẽ xem xét nội dung đánh giá và xử lý trong vòng 3 ngày làm việc.' },
      { q: 'Làm thế nào để liên hệ trực tiếp với khách hàng đã đặt phòng?', a: 'Hiện tại hệ thống chưa hỗ trợ liên hệ trực tiếp. Mọi thông tin khách hàng được bảo mật. Admin sẽ làm trung gian khi cần thiết.' },
      { q: 'Khách hàng đặt phòng rồi không đến, tôi phải làm gì?', a: 'Trường hợp khách no-show, bạn hãy gửi yêu cầu hỗ trợ kèm mã booking. Admin sẽ hỗ trợ xử lý theo chính sách hủy phòng đang áp dụng.' },
      { q: 'Tôi có thể từ chối phục vụ khách hàng không?', a: 'Bạn không thể từ chối khách hàng sau khi đã xác nhận booking. Trường hợp đặc biệt (khách vi phạm nội quy), hãy liên hệ Admin ngay lập tức.' },
    ],
    'room-reg': [
      { q: 'Tại sao phòng của tôi chưa được duyệt sau nhiều ngày?', a: 'Thời gian duyệt phòng thông thường là 1-3 ngày làm việc. Nếu quá 3 ngày, hãy gửi yêu cầu hỗ trợ kèm tên phòng để chúng tôi kiểm tra.' },
      { q: 'Phòng bị từ chối, tôi cần làm gì để đăng lại?', a: 'Admin sẽ ghi rõ lý do từ chối. Bạn vào trang Sửa phòng, chỉnh sửa theo yêu cầu rồi gửi lại. Phòng sẽ tự động quay về trạng thái Chờ duyệt.' },
      { q: 'Tôi có thể đăng bao nhiêu phòng trên TravelMate?', a: 'Hiện tại không giới hạn số phòng. Tuy nhiên mỗi phòng đều phải qua quy trình duyệt để đảm bảo chất lượng cho khách hàng.' },
      { q: 'Thông tin nào bắt buộc khi đăng ký phòng?', a: 'Thông tin bắt buộc: Tên phòng, Tên cơ sở, Loại phòng, Giá/đêm, Sức chứa. Các thông tin thêm về tiện nghi giúp phòng được duyệt nhanh hơn.' },
    ],
    'room-rent': [
      { q: 'Tôi muốn tạm ngưng cho thuê phòng trong một thời gian, phải làm gì?', a: 'Bạn vào trang Danh sách phòng, đổi Trạng thái phòng thành "Hủy đặt phòng". Phòng sẽ không nhận đặt mới nhưng vẫn hiển thị trong hệ thống.' },
      { q: 'Giá phòng của tôi có thể thay đổi theo mùa không?', a: 'Có. Bạn vào trang Sửa phòng để cập nhật giá. Lưu ý: sau khi sửa giá, phòng sẽ quay về Chờ duyệt và cần Admin duyệt lại.' },
      { q: 'Khách đang ở phòng nhưng muốn gia hạn thêm ngày, tôi xử lý thế nào?', a: 'Đổi trạng thái phòng thành "Gia hạn thuê" trong bảng danh sách phòng. Hệ thống sẽ ghi nhận trạng thái mới để tránh nhận đặt trùng lịch.' },
      { q: 'Làm sao để tăng tỷ lệ đặt phòng cho phòng của tôi?', a: 'Bạn nên: (1) Thêm đầy đủ thông tin tiện nghi, (2) Bổ sung hình ảnh chất lượng cao, (3) Gán voucher giảm giá hấp dẫn, (4) Giữ giá cạnh tranh theo khu vực.' },
    ],
    'voucher': [
      { q: 'Tôi có thể tự tạo voucher cho phòng của mình không?', a: 'Hiện tại Partner không tự tạo voucher. Chỉ Admin mới tạo được voucher. Bạn vào trang "Add voucher" để chọn và gán voucher sẵn có của Admin cho phòng.' },
      { q: 'Voucher tôi gán cho phòng bao lâu thì hết hạn?', a: 'Thời hạn voucher do Admin quy định khi tạo. Bạn có thể xem ngày hết hạn trong danh sách voucher ở trang Add voucher.' },
      { q: 'Khách hàng báo lỗi khi nhập mã voucher, tôi phải làm gì?', a: 'Hãy gửi yêu cầu hỗ trợ với mục "Hỗ trợ voucher", đính kèm: mã voucher, tên phòng, thông báo lỗi khách nhận được. Admin sẽ kiểm tra trong vòng 8h.' },
      { q: 'Một phòng có thể gán nhiều voucher không?', a: 'Có, một phòng có thể được gán nhiều voucher khác nhau. Tuy nhiên mỗi booking chỉ áp dụng được 1 voucher.' },
    ],
    'policy': [
      { q: 'TravelMate thu phí hoa hồng bao nhiêu % trên mỗi đặt phòng?', a: 'Mức phí hoa hồng hiện tại là 10% trên mỗi booking thành công. Chi tiết xem tại trang Điều khoản đối tác trong hợp đồng đã ký.' },
      { q: 'Chính sách hủy phòng của TravelMate là gì?', a: 'Mặc định: Hủy trước 48h — hoàn 100%. Hủy trong 24-48h — hoàn 50%. Hủy trong 24h — không hoàn tiền. Partner có thể đề xuất chính sách riêng qua yêu cầu hỗ trợ.' },
      { q: 'TravelMate bảo vệ quyền lợi của Partner như thế nào khi có tranh chấp?', a: 'Khi có tranh chấp với khách hàng, Admin sẽ làm trung gian hòa giải trong vòng 5 ngày làm việc. Partner và khách hàng đều được bảo vệ theo Điều khoản sử dụng.' },
      { q: 'Tôi cần làm gì để trở thành Partner đối tác cao cấp (Premium)?', a: 'Đối tác đạt: (1) Trên 20 phòng đã duyệt, (2) Điểm đánh giá trung bình ≥ 4.5 sao, (3) Tỷ lệ hủy < 5% sẽ được nâng cấp lên Premium. Liên hệ Admin để biết thêm chi tiết.' },
    ],
  };

  // ── Helper ─────────────────────────────────────────────────────────────
  function esc(v) {
    return String(v ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── Tabs ────────────────────────────────────────────────────────────────
  const mainTabs = document.querySelectorAll('[data-tab]');
  mainTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      mainTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const id = tab.getAttribute('data-tab');
      ['new-ticket','my-tickets','faq'].forEach(t => {
        const el = document.getElementById(`tab-${t}`);
        if (el) el.hidden = t !== id;
      });
      if (id === 'my-tickets') renderTicketList();
      if (id === 'faq') renderFaqCatTabs();
    });
  });

  // ── Category Picker ────────────────────────────────────────────────────
  const catPicker = document.getElementById('category-picker');
  const selectedCatInput = document.getElementById('selected-category');
  const panelSelectedCat = document.getElementById('panel-selected-cat');
  const selectedCatInfo = document.getElementById('selected-cat-info');

  if (catPicker) {
    store.CATEGORIES.forEach(cat => {
      const card = document.createElement('div');
      card.className = 'support-cat-card';
      card.setAttribute('data-cat-id', cat.id);
      card.innerHTML = `
        <span class="check-mark">✓</span>
        <span class="support-cat-card__icon">${cat.icon}</span>
        <div class="support-cat-card__label">${esc(cat.label)}</div>`;
      card.addEventListener('click', () => {
        catPicker.querySelectorAll('.support-cat-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        selectedCatInput.value = cat.id;
        // Hiện panel info
        if (panelSelectedCat) panelSelectedCat.hidden = false;
        if (selectedCatInfo) {
          selectedCatInfo.innerHTML = `
            <div class="partner-note__title" style="color:${cat.color}">${cat.icon} ${esc(cat.label)}</div>
            <div class="partner-note__list">
              ${getCategGuidance(cat.id)}
            </div>`;
        }
      });
      catPicker.appendChild(card);
    });
  }

  function getCategGuidance(id) {
    const guides = {
      'customer':  'Gửi yêu cầu liên quan đến: tranh chấp với khách, đánh giá tiêu cực, no-show, vi phạm nội quy.',
      'room-reg':  'Gửi yêu cầu liên quan đến: phòng chờ duyệt quá lâu, phòng bị từ chối, thông tin phòng bị lỗi.',
      'room-rent': 'Gửi yêu cầu liên quan đến: trạng thái phòng, lịch đặt phòng, gia hạn thuê, giá theo mùa.',
      'voucher':   'Gửi yêu cầu liên quan đến: lỗi áp dụng voucher, voucher không hiển thị, yêu cầu tạo voucher mới.',
      'policy':    'Gửi yêu cầu liên quan đến: hoa hồng, chính sách hủy, quyền lợi đối tác, tranh chấp pháp lý.',
    };
    return guides[id] || '';
  }

  // ── Support Form ───────────────────────────────────────────────────────
  const supportForm = document.getElementById('support-form');
  if (supportForm) {
    supportForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const catId = selectedCatInput.value;
      if (!catId) {
        alert('Vui lòng chọn danh mục hỗ trợ!');
        return;
      }
      const subject     = document.getElementById('support-subject').value.trim();
      const description = document.getElementById('support-description').value.trim();
      const priority    = document.getElementById('support-priority').value;

      if (!subject || !description) {
        alert('Vui lòng điền đầy đủ tiêu đề và mô tả!');
        return;
      }

      store.addTicket({
        category: catId,
        subject,
        description,
        priority,
        partnerName: 'Sunrise Sapa Lodge',
        partnerId: 'partner-001',
      });

      resetSupportForm();
      updateBadgeCount();

      if (typeof showToast === 'function') {
        showToast('✅ Yêu cầu đã gửi thành công! Chúng tôi sẽ phản hồi sớm nhất.', 'success');
      }

      // Chuyển sang tab "Yêu cầu của tôi"
      setTimeout(() => {
        document.querySelector('[data-tab="my-tickets"]')?.click();
      }, 1200);
    });
  }

  window.resetSupportForm = function () {
    if (supportForm) supportForm.reset();
    selectedCatInput.value = '';
    catPicker?.querySelectorAll('.support-cat-card').forEach(c => c.classList.remove('selected'));
    if (panelSelectedCat) panelSelectedCat.hidden = true;
  };

  // ── Badge count (open tickets) ─────────────────────────────────────────
  function updateBadgeCount() {
    const badge = document.getElementById('badge-count');
    if (!badge) return;
    const open = store.getTickets().filter(t => t.status === 'open').length;
    badge.textContent = open > 0 ? open : '';
    badge.style.display = open > 0 ? '' : 'none';
  }

  // ── Ticket List ────────────────────────────────────────────────────────
  const ticketFilterTabs = document.querySelectorAll('[data-ticket-filter]');
  ticketFilterTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      ticketFilterTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentTicketFilter = tab.getAttribute('data-ticket-filter');
      renderTicketList();
    });
  });

  function renderTicketList() {
    const list = document.getElementById('ticket-list');
    if (!list) return;

    let tickets = store.getTickets();
    if (currentTicketFilter !== 'all') {
      tickets = tickets.filter(t => t.status === currentTicketFilter);
    }

    if (!tickets.length) {
      list.innerHTML = `
        <div style="text-align:center;padding:48px;color:var(--color-text-muted);">
          <div style="font-size:40px;margin-bottom:12px;">📭</div>
          <div style="font-size:15px;">Không có yêu cầu hỗ trợ nào</div>
          <button class="btn btn--primary btn--sm" style="margin-top:16px;"
            onclick="document.querySelector('[data-tab=new-ticket]').click()">
            ✏️ Gửi yêu cầu mới
          </button>
        </div>`;
      return;
    }

    list.innerHTML = tickets.map(t => {
      const cat    = store.getCategoryById(t.category);
      const status = store.getStatusInfo(t.status);
      const replyCount = (t.replies || []).length;
      const hasNewAdmin = (t.replies || []).some(r => r.from === 'admin');

      return `
        <div class="ticket-card" data-ticket-id="${esc(t.id)}">
          <div class="ticket-cat-dot" style="background:${cat?.color || '#6b7280'}"></div>
          <div style="flex:1;min-width:0;">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;margin-bottom:6px;">
              <div style="font-weight:600;font-size:14px;color:var(--color-text);">${esc(t.subject)}</div>
              <span class="badge ${status.badge}" style="flex-shrink:0;">${esc(status.label)}</span>
            </div>
            <div style="font-size:12px;color:var(--color-text-muted);margin-bottom:8px;display:flex;gap:12px;flex-wrap:wrap;">
              <span>${cat?.icon || '📋'} ${esc(cat?.label || t.category)}</span>
              <span>🔖 ${esc(t.id)}</span>
              <span>⚡ ${esc(t.priority)}</span>
              <span>🕐 ${esc(t.createdAt)}</span>
            </div>
            <div style="font-size:13px;color:var(--color-text-muted);overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;">
              ${esc(t.description)}
            </div>
          </div>
          <div style="display:flex;flex-direction:column;align-items:flex-end;gap:6px;flex-shrink:0;">
            ${replyCount > 0 ? `<span style="font-size:12px;color:${hasNewAdmin ? 'var(--color-primary)' : '#6b7280'};">
              💬 ${replyCount} phản hồi${hasNewAdmin ? ' 🔵' : ''}
            </span>` : ''}
            <button class="btn btn--outline btn--sm">Xem →</button>
          </div>
        </div>`;
    }).join('');

    // Click handler
    list.querySelectorAll('.ticket-card').forEach(card => {
      card.addEventListener('click', () => {
        const id = card.getAttribute('data-ticket-id');
        openTicketModal(id);
      });
    });
  }

  // ── Ticket Detail Modal ────────────────────────────────────────────────
  const modal = document.getElementById('ticket-detail-modal');

  function closeModal() {
    if (modal) modal.style.display = 'none';
    activeTicketId = null;
  }

  document.getElementById('close-ticket-modal')?.addEventListener('click', closeModal);
  modal?.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal?.style.display === 'flex') closeModal();
  });

  function openTicketModal(id) {
    const ticket = store.getTicketById(id);
    if (!ticket) return;
    activeTicketId = id;

    const cat    = store.getCategoryById(ticket.category);
    const status = store.getStatusInfo(ticket.status);

    document.getElementById('modal-ticket-id').textContent    = ticket.id;
    document.getElementById('modal-ticket-subject').textContent = ticket.subject;

    const meta = document.getElementById('modal-ticket-meta');
    meta.innerHTML = `
      <span>${cat?.icon || '📋'} ${esc(cat?.label || ticket.category)}</span>
      <span class="badge ${status.badge}">${esc(status.label)}</span>
      <span>⚡ ${esc(ticket.priority)}</span>
      <span style="color:#6b7280;">🕐 ${esc(ticket.createdAt)}</span>`;

    renderChatArea(ticket);
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';
  }

  function renderChatArea(ticket) {
    const area = document.getElementById('modal-chat-area');
    if (!area) return;

    let html = `
      <!-- Mô tả ban đầu = bubble của partner -->
      <div style="display:flex;flex-direction:column;align-items:flex-end;margin-bottom:12px;">
        <div class="chat-meta" style="text-align:right;">Bạn • ${esc(ticket.createdAt)}</div>
        <div class="chat-bubble chat-bubble--partner">${esc(ticket.description)}</div>
      </div>`;

    (ticket.replies || []).forEach(r => {
      const isAdmin = r.from === 'admin';
      html += `
        <div style="display:flex;flex-direction:column;align-items:${isAdmin ? 'flex-start' : 'flex-end'};margin-bottom:12px;">
          <div class="chat-meta" style="text-align:${isAdmin ? 'left' : 'right'};">
            ${isAdmin ? '🛡 ' : ''}${esc(r.name)} • ${esc(r.time)}
          </div>
          <div class="chat-bubble chat-bubble--${isAdmin ? 'admin' : 'partner'}">${esc(r.content)}</div>
        </div>`;
    });

    if (ticket.status === 'resolved' || ticket.status === 'closed') {
      html += `<div style="text-align:center;font-size:13px;color:var(--color-text-muted);padding:12px;background:#f0fdf4;border-radius:8px;">
        ✅ Yêu cầu này đã được ${ticket.status === 'resolved' ? 'giải quyết' : 'đóng'}.
      </div>`;
    }

    area.innerHTML = html;
    area.scrollTop = area.scrollHeight;
  }

  // Send reply
  document.getElementById('modal-reply-send')?.addEventListener('click', () => {
    const input = document.getElementById('modal-reply-input');
    const content = input.value.trim();
    if (!content || !activeTicketId) return;

    store.addReply(activeTicketId, {
      from: 'partner',
      name: 'Sunrise Sapa Lodge',
      content,
    });
    input.value = '';
    const ticket = store.getTicketById(activeTicketId);
    if (ticket) renderChatArea(ticket);
    renderTicketList();
  });

  // ── FAQ ────────────────────────────────────────────────────────────────
  function renderFaqCatTabs() {
    const tabsEl   = document.getElementById('faq-cat-tabs');
    const contentEl = document.getElementById('faq-content');
    if (!tabsEl || !contentEl) return;

    if (!tabsEl.hasChildNodes()) {
      store.CATEGORIES.forEach((cat, i) => {
        const btn = document.createElement('button');
        btn.className = 'filter-tab' + (i === 0 ? ' active' : '');
        btn.setAttribute('data-faq-cat', cat.id);
        btn.innerHTML = `${cat.icon} ${esc(cat.label)}`;
        btn.addEventListener('click', () => {
          tabsEl.querySelectorAll('[data-faq-cat]').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          currentFaqCat = cat.id;
          renderFaqItems(currentFaqCat);
        });
        tabsEl.appendChild(btn);
      });
    }

    renderFaqItems(currentFaqCat);
  }

  function renderFaqItems(catId) {
    const el = document.getElementById('faq-content');
    if (!el) return;
    const items = FAQ_DATA[catId] || [];
    el.innerHTML = items.map((item, i) => `
      <div class="faq-item" id="faq-${catId}-${i}">
        <div class="faq-item__header" onclick="toggleFaq('faq-${catId}-${i}')">
          <span>${esc(item.q)}</span>
          <span class="faq-item__arrow">▾</span>
        </div>
        <div class="faq-item__body">${esc(item.a)}</div>
      </div>`).join('');
  }

  window.toggleFaq = function(id) {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('open');
  };

  // ── Init ───────────────────────────────────────────────────────────────
  updateBadgeCount();
});

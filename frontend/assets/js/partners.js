document.addEventListener('DOMContentLoaded', () => {
  const roomStore = window.TravelMateRoomStore;
  const voucherStore = window.TravelMateVoucherStore;
  if (!roomStore) return;

  const STORAGE_KEYS = {
    furniture: 'travelmateFurnitureUpdates',
  };

  function readList(key, fallback) {
    try {
      const raw = window.localStorage.getItem(key);
      if (!raw) return fallback;
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : fallback;
    } catch (error) {
      return fallback;
    }
  }

  function writeList(key, value) {
    window.localStorage.setItem(key, JSON.stringify(value));
  }

  function getFurnitureUpdates() {
    return readList(STORAGE_KEYS.furniture, []);
  }

  function saveFurnitureUpdates(items) {
    writeList(STORAGE_KEYS.furniture, items);
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function getAmenityLabels() {
    return [...document.querySelectorAll('#partner-add-room-form .partner-checklist input:checked')]
      .map((input) => input.closest('.form-check')?.innerText.trim())
      .filter(Boolean);
  }

  function populateRoomOptions(selector, includeAllOption, onlyApproved = false) {
    const select = document.querySelector(selector);
    if (!select) return;

    let rooms = roomStore.getRooms();
    if (onlyApproved) {
      rooms = rooms.filter((room) => room.approvalStatus === 'Đã duyệt');
    }

    if (!rooms.length) {
      select.innerHTML = `<option value="">Chưa có phòng nào${onlyApproved ? ' đã được duyệt' : ''}</option>`;
      select.disabled = true;
      return;
    }

    const options = rooms
      .map((room) => `<option value="${escapeHtml(room.id)}">${escapeHtml(room.roomName)}</option>`)
      .join('');

    select.disabled = false;
    select.innerHTML = includeAllOption
      ? `<option value="all">Toàn bộ cơ sở</option>${options}`
      : options;

    const latestRoomId = roomStore.getLatestRoomId();
    if (!includeAllOption && latestRoomId && rooms.some((room) => room.id === latestRoomId)) {
      select.value = latestRoomId;
    }
  }

  function getApprovedRooms() {
    return roomStore.getRooms().filter((room) => room.approvalStatus === 'Đã duyệt');
  }

  function getRequestedVoucherRoomId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('roomId') || voucherStore?.getLatestRoomId() || roomStore.getLatestRoomId();
  }

  function populateApprovedRoomOptions(selector) {
    const select = document.querySelector(selector);
    if (!select) return [];

    const rooms = getApprovedRooms();
    if (!rooms.length) {
      select.innerHTML = '<option value="">Chưa có phòng đã duyệt</option>';
      select.disabled = true;
      return rooms;
    }

    select.disabled = false;
    select.innerHTML = rooms
      .map((room) => `<option value="${escapeHtml(room.id)}">${escapeHtml(room.roomName)}</option>`)
      .join('');

    const requestedRoomId = getRequestedVoucherRoomId();
    const selectedRoom = rooms.find((room) => room.id === requestedRoomId) || rooms[0];
    select.value = selectedRoom.id;
    if (voucherStore) voucherStore.setLatestRoomId(selectedRoom.id);
    return rooms;
  }

  function renderRoomsTable() {
    const tbody = document.getElementById('partner-rooms-table');
    if (!tbody) return;

    const rooms = roomStore.getRooms();

    if (!rooms.length) {
      const totalEl = document.querySelector('[data-summary="totalRooms"]');
      const pendingEl = document.querySelector('[data-summary="pendingRooms"]');
      const activeEl = document.querySelector('[data-summary="activeRooms"]');
      const avgPriceEl = document.querySelector('[data-summary="avgPrice"]');

      if (totalEl) totalEl.textContent = '0';
      if (pendingEl) pendingEl.textContent = '0';
      if (activeEl) activeEl.textContent = '0';
      if (avgPriceEl) avgPriceEl.textContent = '0';

      tbody.innerHTML = `
        <tr>
          <td colspan="9">
            <div class="partner-empty-inline">Chưa có phòng nào được lưu. Hãy gửi một hồ sơ mới để bắt đầu.</div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = rooms
      .map((room, index) => {
        const roomTypeLabel = roomStore.getRoomTypeLabel(room);
        const roomTypeClass = room.roomType ? roomStore.getBadgeClass(room.roomType) : 'badge--muted';
        const isApproved = room.approvalStatus === 'Đã duyệt';
        const voucherLabels = voucherStore ? voucherStore.getRoomVoucherLabels(room.id) : [];
        const voucherNote = voucherLabels.length ? voucherLabels.slice(0, 2).join(', ') : 'Chưa gán voucher';
        const actionButtons = isApproved
          ? `
                <button class="btn btn--outline btn--sm btn-manage-room" data-room-id="${escapeHtml(room.id)}">
                  Nội thất
                </button>
                <button class="btn btn--primary btn--sm btn-add-voucher" data-room-id="${escapeHtml(room.id)}">
                  Add voucher
                </button>
              `
          : `
                <button class="btn btn--outline btn--sm btn-track-room" data-room-id="${escapeHtml(room.id)}">
                  Theo dõi
                </button>
              `;

        return `
          <tr>
            <td>${index + 1}</td>
            <td>
              <strong>${escapeHtml(room.roomName)}</strong>
              <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 4px;">
                ${escapeHtml(room.propertyName)}
              </div>
            </td>
            <td><span class="badge ${roomTypeClass}">${escapeHtml(roomTypeLabel)}</span></td>
            <td>${escapeHtml(room.capacity)}</td>
            <td>${escapeHtml(room.size || '--')}</td>
            <td style="font-weight: 600; color: var(--color-primary)">${escapeHtml(roomStore.formatCurrency(room.price))}</td>
            <td><span class="status-dot ${roomStore.getStatusClass(room.status)}">${escapeHtml(room.status)}</span></td>
            <td><span class="status-dot ${roomStore.getStatusClass(room.approvalStatus)}">${escapeHtml(room.approvalStatus)}</span></td>
            <td>
              <div class="table__actions">
                ${actionButtons}
                <button class="btn btn--danger btn--sm btn-delete-room" data-room-id="${escapeHtml(room.id)}" data-name="${escapeHtml(room.roomName)}">
                  Xóa
                </button>
              </div>
              ${
                isApproved
                  ? `<div class="partner-action-note">Voucher: ${escapeHtml(voucherNote)}</div>`
                  : ''
              }
            </td>
          </tr>
        `;
      })
      .join('');

    const totalRooms = rooms.length;
    const pendingRooms = rooms.filter((room) => room.approvalStatus === 'Chờ duyệt').length;
    const activeRooms = rooms.filter((room) => room.status === 'Đang bán').length;
    const avgPrice = rooms.length
      ? Math.round(rooms.reduce((sum, room) => sum + (Number(room.price) || 0), 0) / rooms.length)
      : 0;

    const totalEl = document.querySelector('[data-summary="totalRooms"]');
    const pendingEl = document.querySelector('[data-summary="pendingRooms"]');
    const activeEl = document.querySelector('[data-summary="activeRooms"]');
    const avgPriceEl = document.querySelector('[data-summary="avgPrice"]');

    if (totalEl) totalEl.textContent = String(totalRooms);
    if (pendingEl) pendingEl.textContent = String(pendingRooms);
    if (activeEl) activeEl.textContent = String(activeRooms);
    if (avgPriceEl) avgPriceEl.textContent = roomStore.formatCurrency(avgPrice).replace('đ', '');
  }

  function renderPendingRoom() {
    const pendingRoomName = document.querySelector('[data-pending-room="roomName"]');
    if (!pendingRoomName) return;

    const room = roomStore.getLatestRoom();
    if (!room) return;

    const roomTypeLabel = roomStore.getRoomTypeLabel(room);
    const payload = {
      ...room,
      roomTypeLabel,
      priceLabel: roomStore.formatCurrency(room.price),
      adminNoteLabel: room.adminNote || 'Admin chưa để lại ghi chú.',
      noteLabel: room.note || 'Không có ghi chú từ đối tác.',
      reviewedAtLabel: room.reviewedAt || 'Chưa có',
    };

    const statusConfig = {
      'Chờ duyệt': {
        alertClass: 'alert--info',
        icon: '⏳',
        message: 'Hồ sơ phòng đã được gửi thành công. Admin sẽ rà soát và có thể chỉnh lại thông tin trước khi mở bán.',
        statusText: 'Đang chờ admin duyệt hồ sơ và xác nhận loại phòng.',
      },
      'Đã duyệt': {
        alertClass: 'alert--success',
        icon: '✅',
        message: `Phòng đã được duyệt và phân loại vào nhóm ${roomTypeLabel}.`,
        statusText: `Đã duyệt, hiện thuộc nhóm ${roomTypeLabel}.`,
      },
      'Từ chối': {
        alertClass: 'alert--danger',
        icon: '⚠',
        message: 'Hồ sơ hiện cần bổ sung thông tin trước khi được mở bán lại.',
        statusText: 'Hồ sơ đã bị từ chối tạm thời và chờ bạn cập nhật thêm.',
      },
    };

    const currentState = statusConfig[room.approvalStatus] || statusConfig['Chờ duyệt'];
    const alertEl = document.getElementById('pending-room-alert');
    const alertTextEl = document.getElementById('pending-room-alert-text');
    const iconEl = document.getElementById('pending-room-icon');
    const statusTextEl = document.getElementById('pending-room-status');

    if (alertEl) alertEl.className = `alert ${currentState.alertClass}`;
    if (alertTextEl) alertTextEl.textContent = currentState.message;
    if (iconEl) iconEl.textContent = currentState.icon;
    if (statusTextEl) statusTextEl.textContent = currentState.statusText;

    document.querySelectorAll('[data-pending-room]').forEach((node) => {
      const key = node.getAttribute('data-pending-room');
      const value = payload[key];
      if (value !== undefined && value !== null && value !== '') {
        node.textContent = value;
      }
    });
  }

  function renderFurnitureUpdates() {
    const list = document.getElementById('partner-furniture-list');
    if (!list) return;

    const items = getFurnitureUpdates();
    if (!items.length) {
      list.innerHTML = '<div class="partner-empty-inline">Chưa có lần cập nhật nội thất nào được lưu.</div>';
      return;
    }

    list.innerHTML = items
      .slice(0, 6)
      .map((item) => {
        const selectedItems = Array.isArray(item.selectedItems) ? item.selectedItems.slice(0, 2).join(', ') : 'Chưa chọn';
        return `<span class="partner-chip">${escapeHtml(item.roomName)}: ${escapeHtml(selectedItems || 'Chưa chọn')}</span>`;
      })
      .join('');
  }

  function renderVoucherRoomSummary(room) {
    const summary = document.getElementById('partner-voucher-room-summary');
    if (!summary) return;

    if (!room) {
      summary.innerHTML = `
        <div class="partner-note__title">Thông tin phòng</div>
        <div class="partner-note__list">Chưa có phòng đã được admin duyệt để gán voucher.</div>
      `;
      return;
    }

    summary.innerHTML = `
      <div class="partner-note__title">${escapeHtml(room.roomName)}</div>
      <div class="partner-note__list">
        <span>Cơ sở: ${escapeHtml(room.propertyName)}</span>
        <span>Loại phòng: ${escapeHtml(roomStore.getRoomTypeLabel(room))}</span>
        <span>Giá/đêm: ${escapeHtml(roomStore.formatCurrency(room.price))}</span>
      </div>
    `;
  }

  function renderVoucherOptions(roomId) {
    const optionsWrap = document.getElementById('partner-voucher-options');
    if (!optionsWrap) return;

    if (!voucherStore) {
      optionsWrap.innerHTML = '<div class="partner-empty-inline">Chưa tải được kho voucher từ admin.</div>';
      return;
    }

    const vouchers = voucherStore.getActiveVouchers();
    const selectedIds = voucherStore.getRoomVoucherIds(roomId);

    if (!vouchers.length) {
      optionsWrap.innerHTML =
        '<div class="partner-empty-inline">Admin chưa bật voucher nào. Hãy chờ admin tạo voucher trước.</div>';
      return;
    }

    optionsWrap.innerHTML = vouchers
      .map((voucher) => {
        const isChecked = selectedIds.includes(voucher.id) ? 'checked' : '';
        return `
          <label class="partner-voucher-card">
            <input type="checkbox" name="voucherIds" value="${escapeHtml(voucher.id)}" ${isChecked} />
            <span class="partner-voucher-card__body">
              <span class="partner-voucher-card__code">${escapeHtml(voucher.code)}</span>
              <span class="partner-voucher-card__title">${escapeHtml(voucher.title)}</span>
              <span class="partner-voucher-card__meta">
                ${escapeHtml(voucherStore.formatDiscount(voucher))} • ${escapeHtml(voucher.startDate || '--')} - ${escapeHtml(voucher.endDate || '--')}
              </span>
              <span class="partner-voucher-card__condition">${escapeHtml(voucher.condition || 'Không có điều kiện bổ sung.')}</span>
            </span>
          </label>
        `;
      })
      .join('');
  }

  function renderAssignedVouchers(roomId) {
    const list = document.getElementById('partner-voucher-list');
    if (!list) return;

    if (!voucherStore || !roomId) {
      list.innerHTML = '<div class="partner-empty-inline">Chưa chọn phòng.</div>';
      return;
    }

    const vouchers = voucherStore.getRoomVouchers(roomId);
    if (!vouchers.length) {
      list.innerHTML = '<div class="partner-empty-inline">Phòng này chưa được gán voucher.</div>';
      return;
    }

    list.innerHTML = vouchers
      .map(
        (voucher) =>
          `<span class="partner-chip partner-chip--removable">
            <span>${escapeHtml(voucher.code)} - ${escapeHtml(voucherStore.formatDiscount(voucher))}</span>
            <button
              class="partner-chip__remove btn-remove-room-voucher"
              type="button"
              data-voucher-id="${escapeHtml(voucher.id)}"
              data-code="${escapeHtml(voucher.code)}"
              aria-label="Xóa voucher ${escapeHtml(voucher.code)} khỏi phòng"
            >
              ×
            </button>
          </span>`,
      )
      .join('');
  }

  function renderVouchers() {
    const roomSelect = document.getElementById('partner-voucher-room');
    if (!roomSelect) return;

    const room = roomStore.getRoomById(roomSelect.value);
    if (!room) {
      const optionsWrap = document.getElementById('partner-voucher-options');
      renderVoucherRoomSummary(null);
      if (optionsWrap) {
        optionsWrap.innerHTML =
          '<div class="partner-empty-inline">Chưa có phòng đã duyệt nên chưa thể chọn voucher.</div>';
      }
      renderAssignedVouchers('');
      return;
    }

    renderVoucherRoomSummary(room);
    renderVoucherOptions(room.id);
    renderAssignedVouchers(room.id);
  }

  const addRoomForm = document.getElementById('partner-add-room-form');
  if (addRoomForm) {
    addRoomForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(addRoomForm);
      const timestamp = new Date().toLocaleString('vi-VN');
      const room = roomStore.addRoom({
        roomName: (formData.get('roomName') || 'Phòng mới').toString().trim(),
        propertyName: (formData.get('propertyName') || 'TravelMate Partner').toString().trim(),
        roomType: (formData.get('roomType') || 'Hotel').toString().trim(),
        capacity: (formData.get('capacity') || '2 khách').toString().trim(),
        price: (formData.get('price') || '0').toString(),
        size: (formData.get('size') || '--').toString().trim() || '--',
        note: (formData.get('note') || '').toString().trim(),
        amenities: getAmenityLabels(),
        furniture: [],
        status: 'Tạm khóa bán',
        approvalStatus: 'Chờ duyệt',
        adminNote: '',
        submittedAt: timestamp,
        reviewedAt: '',
        createdAt: timestamp,
      });

      roomStore.setLatestRoomId(room.id);
      window.location.href = 'pending-room.html';
    });
  }

  const furnitureForm = document.getElementById('partner-furniture-form');
  if (furnitureForm) {
    const approvedRooms = populateApprovedRoomOptions('#partner-furniture-room');
    const submitButton = furnitureForm.querySelector('button[type="submit"]');
    if (submitButton) submitButton.disabled = !approvedRooms.length;

    furnitureForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(furnitureForm);
      const roomId = (formData.get('roomId') || '').toString();
      const selectedItems = [...furnitureForm.querySelectorAll('input[name="furnitureItems"]:checked')].map(
        (input) => input.value,
      );

      const room = roomStore.getRoomById(roomId);
      const update = {
        id: roomStore.createId('furniture'),
        roomId,
        roomName: room?.roomName || 'Phòng chưa xác định',
        furnitureGroup: (formData.get('furnitureGroup') || 'Phòng ngủ').toString(),
        selectedItems,
        note: (formData.get('note') || '').toString().trim(),
        updatedAt: new Date().toLocaleString('vi-VN'),
      };

      if (room) {
        roomStore.updateRoom(roomId, {
          furniture: selectedItems,
          note: update.note || room.note,
        });
      }

      const updates = getFurnitureUpdates();
      updates.unshift(update);
      saveFurnitureUpdates(updates);
      renderFurnitureUpdates();

      if (typeof showToast === 'function') {
        showToast(`Đã lưu cập nhật nội thất cho "${update.roomName}"`, 'success');
      }
    });
  }

  const voucherForm = document.getElementById('partner-voucher-form');
  if (voucherForm) {
    const roomSelect = document.getElementById('partner-voucher-room');
    const approvedRooms = populateApprovedRoomOptions('#partner-voucher-room');
    const submitButton = voucherForm.querySelector('button[type="submit"]');
    if (submitButton) submitButton.disabled = !approvedRooms.length || !voucherStore;
    renderVouchers();

    if (roomSelect) {
      roomSelect.addEventListener('change', () => {
        if (voucherStore) voucherStore.setLatestRoomId(roomSelect.value);
        renderVouchers();
      });
    }

    voucherForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(voucherForm);
      const roomId = (formData.get('roomId') || '').toString();
      const room = roomStore.getRoomById(roomId);

      if (!voucherStore) {
        if (typeof showToast === 'function') showToast('Chưa tải được kho voucher từ admin.', 'error');
        return;
      }

      if (!room || room.approvalStatus !== 'Đã duyệt') {
        if (typeof showToast === 'function') {
          showToast('Chỉ có thể gán voucher cho phòng đã được admin duyệt.', 'error');
        }
        return;
      }

      const selectedVoucherIds = [...voucherForm.querySelectorAll('input[name="voucherIds"]:checked')].map(
        (input) => input.value,
      );

      voucherStore.assignVouchersToRoom(roomId, selectedVoucherIds);
      voucherStore.setLatestRoomId(roomId);
      renderVouchers();
      renderRoomsTable();

      if (typeof showToast === 'function') {
        const message = selectedVoucherIds.length
          ? `Đã gán ${selectedVoucherIds.length} voucher cho "${room.roomName}"`
          : `Đã bỏ toàn bộ voucher khỏi "${room.roomName}"`;
        showToast(message, 'success');
      }
    });
  }

  const assignedVoucherList = document.getElementById('partner-voucher-list');
  if (assignedVoucherList) {
    assignedVoucherList.addEventListener('click', (event) => {
      const removeButton = event.target.closest('.btn-remove-room-voucher');
      if (!removeButton || !voucherStore) return;

      const roomSelect = document.getElementById('partner-voucher-room');
      const roomId = roomSelect?.value || '';
      const voucherId = removeButton.getAttribute('data-voucher-id') || '';
      const voucherCode = removeButton.getAttribute('data-code') || 'voucher';
      const room = roomStore.getRoomById(roomId);
      if (!roomId || !voucherId || !room) return;

      if (!window.confirm(`Xóa voucher "${voucherCode}" khỏi "${room.roomName}"?`)) return;

      const nextVoucherIds = voucherStore.getRoomVoucherIds(roomId).filter((id) => id !== voucherId);
      voucherStore.assignVouchersToRoom(roomId, nextVoucherIds);
      renderVouchers();
      renderRoomsTable();

      if (typeof showToast === 'function') {
        showToast(`Đã xóa voucher "${voucherCode}" khỏi "${room.roomName}"`, 'success');
      }
    });
  }

  const roomsTable = document.getElementById('partner-rooms-table');
  if (roomsTable) {
    roomsTable.addEventListener('click', (event) => {
      const deleteButton = event.target.closest('.btn-delete-room');
      if (deleteButton) {
        const roomId = deleteButton.getAttribute('data-room-id');
        const roomName = deleteButton.getAttribute('data-name') || 'phòng';
        if (!window.confirm(`Bạn có chắc muốn xóa "${roomName}"?`)) return;

        roomStore.deleteRoom(roomId);
        renderRoomsTable();

        if (typeof showToast === 'function') {
          showToast(`Đã xóa "${roomName}"`, 'success');
        }
        return;
      }

      const trackButton = event.target.closest('.btn-track-room');
      if (trackButton) {
        roomStore.setLatestRoomId(trackButton.getAttribute('data-room-id'));
        window.location.href = 'pending-room.html';
        return;
      }

      const addVoucherButton = event.target.closest('.btn-add-voucher');
      if (addVoucherButton) {
        const roomId = addVoucherButton.getAttribute('data-room-id');
        if (voucherStore) voucherStore.setLatestRoomId(roomId);
        roomStore.setLatestRoomId(roomId);
        window.location.href = `add-voucher.html?roomId=${encodeURIComponent(roomId)}`;
        return;
      }

      const manageButton = event.target.closest('.btn-manage-room');
      if (manageButton) {
        roomStore.setLatestRoomId(manageButton.getAttribute('data-room-id'));
        window.location.href = 'add-furniture.html';
      }
    });
  }

  renderRoomsTable();
  renderPendingRoom();
  renderFurnitureUpdates();
  renderVouchers();
});

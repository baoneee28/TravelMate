document.addEventListener('DOMContentLoaded', () => {
  const roomStore = window.TravelMateRoomStore;
  const tableBody = document.getElementById('admin-room-review-table');

  if (!roomStore || !tableBody) return;

  const searchInput = document.getElementById('admin-room-search');
  const approvalFilter = document.getElementById('admin-room-approval-filter');
  const typeFilter = document.getElementById('admin-room-type-filter');
  const resultCount = document.getElementById('admin-room-result-count');
  const tableWrap = document.getElementById('admin-room-review-wrap');
  const emptyState = document.getElementById('admin-room-review-empty');

  const reviewForm = document.getElementById('admin-room-review-form');
  const reviewIdInput = document.getElementById('admin-room-id');
  const roomNameInput = document.getElementById('admin-room-name-input');
  const propertyNameInput = document.getElementById('admin-room-property-input');
  const addressInput = document.getElementById('admin-room-address-input');
  const typeSelect = document.getElementById('admin-room-type');
  const capacityInput = document.getElementById('admin-room-capacity-input');
  const sizeInput = document.getElementById('admin-room-size-input');
  const priceInput = document.getElementById('admin-room-price-input');
  const saleStatusInput = document.getElementById('admin-room-sale-status-input');
  const roomStatusInput = document.getElementById('admin-room-status-input');
  const amenitiesCheckboxes = document.querySelectorAll('#admin-room-amenities-checklist input[type="checkbox"]');
  
  function getCheckedAmenities() {
    const checked = document.querySelectorAll('#admin-room-amenities-checklist input[type="checkbox"]:checked');
    return Array.from(checked).map(cb => cb.value);
  }
  const imagesPreview = document.getElementById('admin-room-images-preview');
  const partnerNoteInput = document.getElementById('admin-room-partner-note-input');
  const adminNoteInput = document.getElementById('admin-room-note');
  const rejectButton = document.getElementById('admin-room-reject-btn');
  const approveButton = document.getElementById('admin-room-approve-btn');

  const selectedTitle = document.getElementById('admin-room-selected-title');
  const selectedProperty = document.getElementById('admin-room-selected-property');
  const selectedApproval = document.getElementById('admin-room-selected-approval');
  const submittedAt = document.getElementById('admin-room-submitted-at');
  const reviewedAt = document.getElementById('admin-room-reviewed-at');
  const capacity = document.getElementById('admin-room-capacity');
  const size = document.getElementById('admin-room-size');
  const price = document.getElementById('admin-room-price');
  const saleStatus = document.getElementById('admin-room-sale-status');
  const adminHistory = document.getElementById('admin-room-admin-history');

  let selectedRoomId = '';

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  const ROOM_STATUS_CONFIG = {
    'Phòng trống':    { color: '#16a34a', bg: '#dcfce7', dot: '#22c55e' },
    'Đang được thuê': { color: '#c2410c', bg: '#ffedd5', dot: '#f97316' },
    'Đã đặt trước':   { color: '#b45309', bg: '#fef9c3', dot: '#eab308' },
    'Hủy đặt phòng': { color: '#b91c1c', bg: '#fee2e2', dot: '#ef4444' },
    'Gia hạn thuê':   { color: '#9d174d', bg: '#fce7f3', dot: '#ec4899' },
  };

  function getRoomStatusBadge(status) {
    const cfg = ROOM_STATUS_CONFIG[status];
    if (!cfg) return `<span style="font-size:13px;color:#6b7280;">${escapeHtml(status || '--')}</span>`;
    return `<span style="display:inline-flex;align-items:center;gap:5px;padding:2px 9px;border-radius:999px;font-size:12px;font-weight:600;background:${cfg.bg};color:${cfg.color};white-space:nowrap;"><span style="width:7px;height:7px;border-radius:50%;background:${cfg.dot};flex-shrink:0;"></span>${escapeHtml(status)}</span>`;
  }

  function getApprovalBadgeClass(status) {
    if (status === 'Đã duyệt') return 'badge--success';
    if (status === 'Từ chối') return 'badge--danger';
    return 'badge--warning';
  }

  function parseAmenities(value) {
    return String(value || '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  function syncHeaderWithInputs() {
    if (selectedTitle && roomNameInput) {
      selectedTitle.textContent = roomNameInput.value.trim() || 'Chọn một hồ sơ để duyệt';
    }

    if (selectedProperty && propertyNameInput) {
      selectedProperty.textContent = propertyNameInput.value.trim() || 'Thông tin chi tiết sẽ hiển thị tại đây.';
    }
  }

  function getFilteredRooms() {
    const query = String(searchInput?.value || '')
      .trim()
      .toLowerCase();
    const typeValue = typeFilter?.value || 'all';

    return roomStore.getRooms().filter((room) => {
      const matchesQuery =
        !query ||
        [room.roomName, room.propertyName, room.capacity, room.size, roomStore.getRoomTypeLabel(room)]
          .join(' ')
          .toLowerCase()
          .includes(query);

      const matchesApproval = room.approvalStatus === 'Chờ duyệt';
      const matchesType =
        typeValue === 'all' ||
        (typeValue === '__pending__' ? !room.roomType : room.roomType === typeValue);

      return matchesQuery && matchesApproval && matchesType;
    });
  }

  function updateSummary() {
    const rooms = roomStore.getRooms();
    const summary = {
      pending: rooms.filter((room) => room.approvalStatus === 'Chờ duyệt').length,
      Villa: rooms.filter((room) => room.roomType === 'Villa').length,
      Hotel: rooms.filter((room) => room.roomType === 'Hotel').length,
      Homestay: rooms.filter((room) => room.roomType === 'Homestay').length,
      Resort: rooms.filter((room) => room.roomType === 'Resort').length,
    };

    Object.entries(summary).forEach(([key, value]) => {
      const element = document.querySelector(`[data-review-summary="${key}"]`);
      if (element) element.textContent = String(value);
    });
  }

  function getPreferredRoom(filteredRooms) {
    return (
      filteredRooms.find((room) => room.id === selectedRoomId) ||
      filteredRooms.find((room) => room.approvalStatus === 'Chờ duyệt') ||
      filteredRooms[0] ||
      null
    );
  }

  function renderTable(rooms, selectedId) {
    if (resultCount) {
      resultCount.textContent = `${rooms.length} hồ sơ hiển thị`;
    }

    if (!rooms.length) {
      tableBody.innerHTML = '';
      if (tableWrap) tableWrap.hidden = true;
      if (emptyState) emptyState.hidden = false;
      return;
    }

    if (tableWrap) tableWrap.hidden = false;
    if (emptyState) emptyState.hidden = true;

    tableBody.innerHTML = rooms
      .map((room, index) => {
        const roomTypeLabel = roomStore.getRoomTypeLabel(room);
        const roomTypeClass = room.roomType ? roomStore.getBadgeClass(room.roomType) : 'badge--muted';
        const rowClass = room.id === selectedId ? 'room-review-row is-selected' : 'room-review-row';

        return `
          <tr class="${rowClass}" data-room-id="${escapeHtml(room.id)}">
            <td>${index + 1}</td>
            <td>
              <strong>${escapeHtml(room.roomName)}</strong>
              <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 4px;">
                ${escapeHtml(room.capacity)} • ${escapeHtml(room.size || '--')}
              </div>
            </td>
            <td>${escapeHtml(room.propertyName)}</td>
            <td><span class="badge ${roomTypeClass}">${escapeHtml(roomTypeLabel)}</span></td>
            <td style="font-weight: 600; color: var(--color-primary)">${escapeHtml(roomStore.formatCurrency(room.price))}</td>
            <td>${getRoomStatusBadge(room.roomStatus || room.status)}</td>
            <td><span class="status-dot ${roomStore.getStatusClass(room.approvalStatus)}">${escapeHtml(room.approvalStatus)}</span></td>
            <td>${escapeHtml(room.submittedAt)}</td>
            <td>
              <button class="btn btn--outline btn--sm btn-review-room" data-room-id="${escapeHtml(room.id)}">
                ${room.approvalStatus === 'Chờ duyệt' ? 'Duyệt' : 'Xem'}
              </button>
            </td>
          </tr>
        `;
      })
      .join('');
  }

  function setPanelState(room) {
    if (!room) {
      selectedRoomId = '';
      reviewIdInput.value = '';
      roomNameInput.value = '';
      propertyNameInput.value = '';
      if (addressInput) addressInput.value = '';
      typeSelect.value = '';
      capacityInput.value = '';
      sizeInput.value = '';
      priceInput.value = '';
      saleStatusInput.value = 'Đang bán';
      if (amenitiesCheckboxes) amenitiesCheckboxes.forEach(cb => cb.checked = false);
      if (imagesPreview) imagesPreview.innerHTML = '<div style="padding: 1rem; border: 1px dashed var(--color-border); text-align: center; color: var(--color-text-muted); grid-column: span 3; border-radius: var(--radius-md);">Chưa có bảng dữ liệu hồ sơ nào.</div>';
      partnerNoteInput.value = '';
      adminNoteInput.value = '';

      if (selectedTitle) selectedTitle.textContent = 'Chưa có hồ sơ nào để duyệt';
      if (selectedProperty) selectedProperty.textContent = 'Bộ lọc hiện tại không có dữ liệu.';
      if (selectedApproval) selectedApproval.className = 'badge badge--muted';
      if (selectedApproval) selectedApproval.textContent = 'Không có dữ liệu';
      if (submittedAt) submittedAt.textContent = '--';
      if (reviewedAt) reviewedAt.textContent = '--';
      if (capacity) capacity.textContent = '--';
      if (size) size.textContent = '--';
      if (price) price.textContent = '--';
      if (saleStatus) saleStatus.textContent = '--';
      if (adminHistory) adminHistory.textContent = 'Chưa có lịch sử duyệt.';

      if (approveButton) approveButton.disabled = true;
      if (rejectButton) rejectButton.disabled = true;
      if (roomNameInput) roomNameInput.disabled = true;
      if (propertyNameInput) propertyNameInput.disabled = true;
      if (typeSelect) typeSelect.disabled = true;
      if (capacityInput) capacityInput.disabled = true;
      if (sizeInput) sizeInput.disabled = true;
      if (priceInput) priceInput.disabled = true;
      if (saleStatusInput) saleStatusInput.disabled = true;
      if (roomStatusInput) roomStatusInput.disabled = true;
      if (amenitiesCheckboxes) amenitiesCheckboxes.forEach(cb => cb.disabled = true);
      if (adminNoteInput) adminNoteInput.disabled = true;
      return;
    }

    selectedRoomId = room.id;
    roomStore.setLatestRoomId(room.id);

    reviewIdInput.value = room.id;
    roomNameInput.value = room.roomName || '';
    propertyNameInput.value = room.propertyName || '';
    if (addressInput) addressInput.value = room.address || '';
    typeSelect.value = room.roomType || '';
    capacityInput.value = room.capacity || '';
    sizeInput.value = room.size || '';
    priceInput.value = room.price || '';
    saleStatusInput.value = room.status || 'Đang bán';
    if (roomStatusInput) roomStatusInput.value = room.roomStatus || 'Phòng trống';
    if (amenitiesCheckboxes) {
      amenitiesCheckboxes.forEach(cb => {
        cb.checked = room.amenities && Array.isArray(room.amenities) && room.amenities.some(a => a.trim().toLowerCase() === cb.value.trim().toLowerCase());
      });
    }
    
    if (imagesPreview) {
      const seed = Array.from(String(room.roomName || 'A')).reduce((sum, c) => sum + c.charCodeAt(0), 0);
      const img1 = `https://images.unsplash.com/photo-${1500000000000 + (seed * 1000)}?w=320&q=70`;
      const img2 = `https://images.unsplash.com/photo-${1500000000000 + (seed * 1001)}?w=320&q=70`;
      const img3 = `https://images.unsplash.com/photo-${1500000000000 + (seed * 1002)}?w=320&q=70`;
      const imgStyle = "width: 100%; aspect-ratio: 16/9; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--color-border-light);";
      
      imagesPreview.innerHTML = `
        <img src="${img1}" style="${imgStyle}" alt="Ảnh đại diện phòng" title="Ảnh đại diện" />
        <img src="${img2}" style="${imgStyle}" alt="Ảnh chi tiết 1" title="Ảnh chi tiết 1" />
        <img src="${img3}" style="${imgStyle}" alt="Ảnh chi tiết 2" title="Ảnh chi tiết 2" />
      `;
    }

    partnerNoteInput.value = room.note || '';
    adminNoteInput.value = room.adminNote || '';

    if (selectedTitle) selectedTitle.textContent = room.roomName;
    if (selectedProperty) selectedProperty.textContent = room.propertyName;
    if (selectedApproval) selectedApproval.className = `badge ${getApprovalBadgeClass(room.approvalStatus)}`;
    if (selectedApproval) selectedApproval.textContent = room.approvalStatus;
    if (submittedAt) submittedAt.textContent = room.submittedAt || '--';
    if (reviewedAt) reviewedAt.textContent = room.reviewedAt || 'Chưa duyệt';
    if (capacity) capacity.textContent = room.capacity || '--';
    if (size) size.textContent = room.size || '--';
    if (price) price.textContent = roomStore.formatCurrency(room.price);
    if (saleStatus) saleStatus.textContent = room.roomStatus || room.status || '--';
    if (adminHistory) adminHistory.textContent = room.adminNote || 'Chưa có lịch sử duyệt.';

    if (approveButton) approveButton.disabled = false;
    if (rejectButton) rejectButton.disabled = false;
    if (roomNameInput) roomNameInput.disabled = false;
    if (propertyNameInput) propertyNameInput.disabled = false;
    if (typeSelect) typeSelect.disabled = false;
    if (capacityInput) capacityInput.disabled = false;
    if (sizeInput) sizeInput.disabled = false;
    if (priceInput) priceInput.disabled = false;
    if (saleStatusInput) saleStatusInput.disabled = false;
    if (roomStatusInput) roomStatusInput.disabled = false;
    if (adminNoteInput) adminNoteInput.disabled = false;
  }

  function renderPage() {
    const filteredRooms = getFilteredRooms();
    const preferredRoom = getPreferredRoom(filteredRooms);
    updateSummary();
    renderTable(filteredRooms, preferredRoom?.id || '');
    setPanelState(preferredRoom);
  }

  function approveRoom(event) {
    event.preventDefault();

    const roomId = reviewIdInput.value;
    const roomNameValue = roomNameInput.value.trim();
    const propertyNameValue = propertyNameInput.value.trim();
    const roomType = typeSelect.value;
    const capacityValue = capacityInput.value.trim();
    const sizeValue = sizeInput.value.trim();
    const priceValue = priceInput.value.trim();
    if (!roomId) return;

    if (!roomNameValue) {
      if (typeof showToast === 'function') {
        showToast('Hãy nhập tên phòng trước khi duyệt.', 'error');
      }
      roomNameInput.focus();
      return;
    }

    if (!propertyNameValue) {
      if (typeof showToast === 'function') {
        showToast('Hãy nhập tên cơ sở trước khi duyệt.', 'error');
      }
      propertyNameInput.focus();
      return;
    }

    if (!roomType) {
      if (typeof showToast === 'function') {
        showToast('Hãy chọn loại phòng trước khi duyệt.', 'error');
      }
      roomTypeInput.focus();
      return;
    }

    if (!priceValue) {
      if (typeof showToast === 'function') {
        showToast('Hãy nhập giá phòng trước khi duyệt.', 'error');
      }
      priceInput.focus();
      return;
    }

    const existingRoom = roomStore.getRooms().find(r => r.id === roomId);
    const roomCode = existingRoom?.roomCode || ('RM-' + Math.floor(1000 + Math.random() * 9000));

    const updatedRoom = roomStore.updateRoom(roomId, {
      roomCode,
      roomName: roomNameValue,
      propertyName: propertyNameValue,
      address: addressInput ? addressInput.value.trim() : '',
      roomType,
      capacity: capacityValue || '2 khách',
      size: sizeValue || '--',
      price: priceValue,
      amenities: getCheckedAmenities(),
      note: partnerNoteInput.value.trim(),
      status: saleStatusInput.value || 'Đang bán',
      roomStatus: roomStatusInput ? roomStatusInput.value : 'Phòng trống',
      approvalStatus: 'Đã duyệt',
      adminNote:
        adminNoteInput.value.trim() || `Admin đã duyệt và phân loại phòng vào nhóm ${roomType}.`,
      reviewedAt: new Date().toLocaleString('vi-VN'),
    });

    if (typeof showToast === 'function') {
      showToast(`Đã duyệt "${updatedRoom?.roomName || roomNameValue}"`, 'success');
    }

    renderPage();
  }

  function rejectRoom() {
    const roomId = reviewIdInput.value;
    if (!roomId) return;

    const updatedRoom = roomStore.updateRoom(roomId, {
      roomName: roomNameInput.value.trim() || 'Phòng chưa đặt tên',
      propertyName: propertyNameInput.value.trim() || 'TravelMate Partner',
      roomType: roomTypeInput.value,
      capacity: capacityInput.value.trim() || '2 khách',
      size: sizeInput.value.trim() || '--',
      price: priceInput.value.trim() || '0',
      amenities: getCheckedAmenities(),
      note: partnerNoteInput.value.trim(),
      status: 'Tạm ẩn',
      roomStatus: 'Hủy đặt phòng',
      approvalStatus: 'Từ chối',
      adminNote:
        adminNoteInput.value.trim() ||
        'Cần bổ sung thêm ảnh thực tế hoặc mô tả rõ hơn trước khi duyệt lại.',
      reviewedAt: new Date().toLocaleString('vi-VN'),
    });

    if (typeof showToast === 'function') {
      showToast(`Đã từ chối "${updatedRoom?.roomName || 'hồ sơ phòng'}"`, 'info');
    }

    renderPage();
  }

  if (searchInput) searchInput.addEventListener('input', renderPage);
  if (approvalFilter) approvalFilter.addEventListener('change', renderPage);
  if (typeFilter) typeFilter.addEventListener('change', renderPage);

  tableBody.addEventListener('click', (event) => {
    const button = event.target.closest('.btn-review-room');
    const row = event.target.closest('tr[data-room-id]');
    const roomId = button?.getAttribute('data-room-id') || row?.getAttribute('data-room-id');
    if (!roomId) return;

    selectedRoomId = roomId;
    renderPage();
  });

  if (reviewForm) {
    reviewForm.addEventListener('submit', approveRoom);
  }

  if (rejectButton) {
    rejectButton.addEventListener('click', rejectRoom);
  }

  [roomNameInput, propertyNameInput].forEach((input) => {
    if (!input) return;
    input.addEventListener('input', syncHeaderWithInputs);
  });

  renderPage();
});

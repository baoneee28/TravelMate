document.addEventListener('DOMContentLoaded', () => {
  const STORAGE_KEYS = {
    rooms: 'travelmatePartnerRooms',
    latestPendingRoom: 'travelmatePendingRoom',
    furniture: 'travelmateFurnitureUpdates',
    vouchers: 'travelmatePartnerVouchers',
  };

  const defaultRooms = [
    {
      id: 'room-default-1',
      roomName: 'Deluxe Mountain View',
      propertyName: 'Sunrise Sapa Lodge',
      roomType: 'Deluxe',
      capacity: '2 khách',
      size: '32m²',
      price: '1650000',
      note: '',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      furniture: [],
      amenities: [],
      createdAt: '14/04/2026 08:00:00',
    },
    {
      id: 'room-default-2',
      roomName: 'Family Balcony Suite',
      propertyName: 'Sunrise Sapa Lodge',
      roomType: 'Suite',
      capacity: '4 khách',
      size: '52m²',
      price: '2450000',
      note: '',
      status: 'Tạm khóa bán',
      approvalStatus: 'Chờ duyệt',
      furniture: [],
      amenities: [],
      createdAt: '14/04/2026 08:15:00',
    },
    {
      id: 'room-default-3',
      roomName: 'Standard Garden',
      propertyName: 'Sunrise Sapa Lodge',
      roomType: 'Standard',
      capacity: '2 khách',
      size: '26m²',
      price: '1150000',
      note: '',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      furniture: ['Giường 1m8', 'Rèm cản sáng', 'Minibar', 'Két sắt', 'Máy sấy tóc'],
      amenities: [],
      createdAt: '14/04/2026 08:30:00',
    },
    {
      id: 'room-default-4',
      roomName: 'Studio Corner Room',
      propertyName: 'Sunrise Sapa Lodge',
      roomType: 'Studio',
      capacity: '3 khách',
      size: '40m²',
      price: '1920000',
      note: '',
      status: 'Tạm ẩn',
      approvalStatus: 'Đã duyệt',
      furniture: [],
      amenities: [],
      createdAt: '14/04/2026 08:45:00',
    },
  ];

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

  function ensureRooms() {
    const rooms = readList(STORAGE_KEYS.rooms, []);
    if (rooms.length) return rooms;
    writeList(STORAGE_KEYS.rooms, defaultRooms);
    return defaultRooms;
  }

  function getRooms() {
    return ensureRooms();
  }

  function saveRooms(rooms) {
    writeList(STORAGE_KEYS.rooms, rooms);
  }

  function getFurnitureUpdates() {
    return readList(STORAGE_KEYS.furniture, []);
  }

  function saveFurnitureUpdates(items) {
    writeList(STORAGE_KEYS.furniture, items);
  }

  function getVouchers() {
    return readList(STORAGE_KEYS.vouchers, []);
  }

  function saveVouchers(items) {
    writeList(STORAGE_KEYS.vouchers, items);
  }

  function createId(prefix) {
    return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
  }

  function formatCurrency(value) {
    const amount = Number(value) || 0;
    return `${amount.toLocaleString('vi-VN')}đ`;
  }

  function getStatusClass(status) {
    if (status === 'Đang bán' || status === 'Đã duyệt') return 'status-dot--active';
    if (status === 'Chờ duyệt' || status === 'Tạm khóa bán') return 'status-dot--pending';
    return 'status-dot--inactive';
  }

  function getBadgeClass(type) {
    const map = {
      Deluxe: 'badge--primary',
      Suite: 'badge--accent',
      Standard: 'badge--muted',
      Studio: 'badge--success',
      'Family Room': 'badge--accent',
    };
    return map[type] || 'badge--primary';
  }

  function getAmenityLabels() {
    return [...document.querySelectorAll('#partner-add-room-form .partner-checklist input:checked')]
      .map((input) => input.closest('.form-check')?.innerText.trim())
      .filter(Boolean);
  }

  function populateRoomOptions(selector, includeAllOption) {
    const select = document.querySelector(selector);
    if (!select) return;

    const rooms = getRooms();
    const options = rooms.map((room) => `<option value="${room.id}">${room.roomName}</option>`).join('');
    select.innerHTML = includeAllOption
      ? `<option value="all">Toàn bộ cơ sở</option>${options}`
      : options;
  }

  function renderRoomsTable() {
    const tbody = document.getElementById('partner-rooms-table');
    if (!tbody) return;

    const rooms = getRooms();
    tbody.innerHTML = rooms
      .map((room, index) => `
        <tr>
          <td>${index + 1}</td>
          <td><strong>${room.roomName}</strong></td>
          <td><span class="badge ${getBadgeClass(room.roomType)}">${room.roomType}</span></td>
          <td>${room.capacity}</td>
          <td>${room.size || '--'}</td>
          <td style="font-weight: 600; color: var(--color-primary)">${formatCurrency(room.price)}</td>
          <td><span class="status-dot ${getStatusClass(room.status)}">${room.status}</span></td>
          <td><span class="status-dot ${getStatusClass(room.approvalStatus)}">${room.approvalStatus}</span></td>
          <td>
            <div class="table__actions">
              <a href="add-furniture.html" class="btn btn--outline btn--sm">Nội thất</a>
              <button class="btn btn--danger btn--sm btn-delete-room" data-room-id="${room.id}" data-name="${room.roomName}">Xóa</button>
            </div>
          </td>
        </tr>
      `)
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
    if (avgPriceEl) avgPriceEl.textContent = formatCurrency(avgPrice).replace('đ', '');
  }

  function renderPendingRoom() {
    const pendingRoomName = document.querySelector('[data-pending-room="roomName"]');
    if (!pendingRoomName) return;

    const rooms = getRooms();
    const latestPendingRoomRaw = window.localStorage.getItem(STORAGE_KEYS.latestPendingRoom);
    const fallbackRoom = rooms.find((room) => room.approvalStatus === 'Chờ duyệt') || rooms[0];
    let payload = fallbackRoom;

    if (latestPendingRoomRaw) {
      try {
        payload = JSON.parse(latestPendingRoomRaw);
      } catch (error) {
        window.localStorage.removeItem(STORAGE_KEYS.latestPendingRoom);
      }
    }

    if (!payload) return;

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
      .map((item) => `<span class="partner-chip">${item.roomName}: ${item.selectedItems.slice(0, 2).join(', ') || 'Chưa chọn'}</span>`)
      .join('');
  }

  function renderVouchers() {
    const list = document.getElementById('partner-voucher-list');
    if (!list) return;

    const vouchers = getVouchers();
    if (!vouchers.length) {
      list.innerHTML = '<div class="partner-empty-inline">Chưa có voucher nào được tạo.</div>';
      return;
    }

    list.innerHTML = vouchers
      .slice(0, 6)
      .map((voucher) => `<span class="partner-chip">${voucher.code} - ${voucher.discountLabel}</span>`)
      .join('');
  }

  const addRoomForm = document.getElementById('partner-add-room-form');
  if (addRoomForm) {
    addRoomForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(addRoomForm);
      const payload = {
        id: createId('room'),
        roomName: formData.get('roomName') || 'Phòng mới',
        propertyName: formData.get('propertyName') || 'TravelMate Partner',
        roomType: formData.get('roomType') || 'Deluxe',
        capacity: formData.get('capacity') || '2 khách',
        price: formData.get('price') || '0',
        size: formData.get('size') || '--',
        note: formData.get('note') || '',
        amenities: getAmenityLabels(),
        furniture: [],
        status: 'Tạm khóa bán',
        approvalStatus: 'Chờ duyệt',
        submittedAt: new Date().toLocaleString('vi-VN'),
        createdAt: new Date().toLocaleString('vi-VN'),
      };

      const rooms = getRooms();
      rooms.unshift(payload);
      saveRooms(rooms);
      window.localStorage.setItem(STORAGE_KEYS.latestPendingRoom, JSON.stringify(payload));
      window.location.href = 'pending-room.html';
    });
  }

  const furnitureForm = document.getElementById('partner-furniture-form');
  if (furnitureForm) {
    populateRoomOptions('#partner-furniture-room', false);

    furnitureForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(furnitureForm);
      const roomId = formData.get('roomId');
      const selectedItems = [...furnitureForm.querySelectorAll('input[name="furnitureItems"]:checked')]
        .map((input) => input.value);
      const update = {
        id: createId('furniture'),
        roomId,
        roomName: getRooms().find((room) => room.id === roomId)?.roomName || 'Phòng chưa xác định',
        furnitureGroup: formData.get('furnitureGroup') || 'Phòng ngủ',
        selectedItems,
        note: formData.get('note') || '',
        updatedAt: new Date().toLocaleString('vi-VN'),
      };

      const rooms = getRooms().map((room) => {
        if (room.id !== roomId) return room;
        return {
          ...room,
          furniture: selectedItems,
          note: update.note || room.note,
        };
      });
      saveRooms(rooms);

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
    populateRoomOptions('#partner-voucher-room', true);

    voucherForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const formData = new FormData(voucherForm);
      const scope = formData.get('scope');
      const discountType = formData.get('discountType');
      const discountValue = formData.get('discountValue') || '0';
      const scopeLabel = scope === 'all'
        ? 'Toàn bộ cơ sở'
        : getRooms().find((room) => room.id === scope)?.roomName || 'Toàn bộ cơ sở';
      const discountLabel = discountType === 'Giảm theo %'
        ? `${discountValue}%`
        : `${Number(discountValue || 0).toLocaleString('vi-VN')}đ`;

      const voucher = {
        id: createId('voucher'),
        code: (formData.get('code') || 'VOUCHER').toString().trim().toUpperCase(),
        scope,
        scopeLabel,
        discountType,
        discountValue,
        discountLabel,
        startDate: formData.get('startDate') || '',
        endDate: formData.get('endDate') || '',
        condition: formData.get('condition') || '',
        createdAt: new Date().toLocaleString('vi-VN'),
      };

      const vouchers = getVouchers();
      vouchers.unshift(voucher);
      saveVouchers(vouchers);
      renderVouchers();
      voucherForm.reset();
      const roomSelect = document.getElementById('partner-voucher-room');
      if (roomSelect) roomSelect.value = 'all';

      if (typeof showToast === 'function') {
        showToast(`Đã tạo voucher "${voucher.code}"`, 'success');
      }
    });
  }

  const roomsTable = document.getElementById('partner-rooms-table');
  if (roomsTable) {
    roomsTable.addEventListener('click', (event) => {
      const button = event.target.closest('.btn-delete-room');
      if (!button) return;

      const roomId = button.getAttribute('data-room-id');
      const roomName = button.getAttribute('data-name') || 'phòng';
      if (!window.confirm(`Bạn có chắc muốn xóa "${roomName}"?`)) return;

      const rooms = getRooms().filter((room) => room.id !== roomId);
      saveRooms(rooms);
      renderRoomsTable();

      if (typeof showToast === 'function') {
        showToast(`Đã xóa "${roomName}"`, 'success');
      }
    });
  }

  ensureRooms();
  renderRoomsTable();
  renderPendingRoom();
  renderFurnitureUpdates();
  renderVouchers();
});

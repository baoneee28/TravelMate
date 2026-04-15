(function () {
  const STORAGE_KEYS = {
    rooms: 'travelmatePartnerRooms',
    latestRoomId: 'travelmateLatestRoomId',
    legacyLatestRoom: 'travelmatePendingRoom',
  };

  const ROOM_CATEGORIES = ['Villa', 'Hotel', 'Homestay', 'Resort'];
  const APPROVAL_STATUSES = ['Chờ duyệt', 'Đã duyệt', 'Từ chối'];
  const SALE_STATUSES = ['Đang bán', 'Tạm khóa bán', 'Tạm ẩn'];

  const DEFAULT_ROOMS = [
    {
      id: 'room-default-1',
      roomName: 'Cliff Pool Escape',
      propertyName: 'Ocean Pearl Retreat',
      roomType: 'Villa',
      capacity: '6 khách',
      size: '120m²',
      price: '5200000',
      note: 'Biệt lập, hồ bơi riêng và sân hiên hướng biển.',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      adminNote: 'Hồ sơ hợp lệ, xếp vào nhóm Villa.',
      furniture: ['Giường king size', 'Bàn trà ngoài trời'],
      amenities: ['Hồ bơi riêng', 'Bồn tắm', 'Wi-Fi tốc độ cao'],
      submittedAt: '14/04/2026 08:10:00',
      reviewedAt: '14/04/2026 09:30:00',
      createdAt: '14/04/2026 08:10:00',
    },
    {
      id: 'room-default-2',
      roomName: 'Premium City View',
      propertyName: 'Sora Hotel Hanoi',
      roomType: 'Hotel',
      capacity: '2 khách',
      size: '34m²',
      price: '1780000',
      note: 'Phòng trung tâm, phù hợp khách công tác.',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      adminNote: 'Đã duyệt và xếp vào nhóm Hotel.',
      furniture: ['Bàn làm việc', 'Tủ lạnh mini'],
      amenities: ['Máy lạnh', 'Smart TV', 'Két sắt'],
      submittedAt: '14/04/2026 08:30:00',
      reviewedAt: '14/04/2026 09:10:00',
      createdAt: '14/04/2026 08:30:00',
    },
    {
      id: 'room-default-3',
      roomName: 'Garden Bungalow',
      propertyName: 'Mây Homestay Sa Pa',
      roomType: 'Homestay',
      capacity: '3 khách',
      size: '45m²',
      price: '1420000',
      note: 'Có hiên riêng, ưu tiên khách thích không gian yên tĩnh.',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      adminNote: 'Đã duyệt và xếp vào nhóm Homestay.',
      furniture: ['Sofa thư giãn', 'Bàn trà ngoài trời'],
      amenities: ['Ban công riêng', 'Máy sấy tóc', 'Wi-Fi tốc độ cao'],
      submittedAt: '14/04/2026 08:45:00',
      reviewedAt: '14/04/2026 09:20:00',
      createdAt: '14/04/2026 08:45:00',
    },
    {
      id: 'room-default-4',
      roomName: 'Sunset Ocean Suite',
      propertyName: 'Blue Coast Resort',
      roomType: 'Resort',
      capacity: '4 khách',
      size: '62m²',
      price: '2860000',
      note: 'Phòng view biển, kết nối trực tiếp khu tiện ích nghỉ dưỡng.',
      status: 'Đang bán',
      approvalStatus: 'Đã duyệt',
      adminNote: 'Đã duyệt và xếp vào nhóm Resort.',
      furniture: ['Bồn tắm nằm', 'Tủ quần áo'],
      amenities: ['Bồn tắm', 'Smart TV', 'Mini bar'],
      submittedAt: '14/04/2026 08:55:00',
      reviewedAt: '14/04/2026 09:40:00',
      createdAt: '14/04/2026 08:55:00',
    },
    {
      id: 'room-default-5',
      roomName: 'Forest Balcony Retreat',
      propertyName: 'Cloud Valley Lodge',
      roomType: '',
      capacity: '2 khách',
      size: '38m²',
      price: '1960000',
      note: 'Đang bổ sung thêm ảnh thật khu vực ban công và nội thất mới.',
      status: 'Tạm khóa bán',
      approvalStatus: 'Chờ duyệt',
      adminNote: '',
      furniture: [],
      amenities: ['Ban công riêng', 'Máy lạnh'],
      submittedAt: '15/04/2026 09:15:00',
      reviewedAt: '',
      createdAt: '15/04/2026 09:15:00',
    },
  ];

  function readJSON(key, fallback) {
    try {
      const raw = window.localStorage.getItem(key);
      if (!raw) return fallback;
      return JSON.parse(raw);
    } catch (error) {
      return fallback;
    }
  }

  function writeJSON(key, value) {
    window.localStorage.setItem(key, JSON.stringify(value));
  }

  function createId(prefix) {
    return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
  }

  function normalizeDate(value, fallback) {
    return value ? String(value) : fallback;
  }

  function toList(value) {
    return Array.isArray(value) ? value.filter(Boolean).map((item) => String(item)) : [];
  }

  function isKnownCategory(value) {
    const normalized = String(value || '').trim().toLowerCase();
    return ROOM_CATEGORIES.some((item) => item.toLowerCase() === normalized);
  }

  function inferCategory(room) {
    const rawValues = [room.roomType, room.category, room.propertyName, room.roomName]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    if (rawValues.includes('villa')) return 'Villa';
    if (rawValues.includes('resort')) return 'Resort';
    if (rawValues.includes('homestay') || rawValues.includes('lodge') || rawValues.includes('bungalow')) {
      return 'Homestay';
    }

    if (
      rawValues.includes('hotel') ||
      rawValues.includes('deluxe') ||
      rawValues.includes('suite') ||
      rawValues.includes('standard') ||
      rawValues.includes('studio') ||
      rawValues.includes('family')
    ) {
      return 'Hotel';
    }

    return '';
  }

  function normalizeCategory(value, room) {
    const normalized = String(value || '').trim().toLowerCase();
    const directMatch = ROOM_CATEGORIES.find((item) => item.toLowerCase() === normalized);
    return directMatch || inferCategory(room);
  }

  function normalizeApprovalStatus(room, category) {
    const rawStatus = String(room.approvalStatus || '').trim();

    if (rawStatus === 'Đã duyệt' || rawStatus === 'Chờ duyệt' || rawStatus === 'Từ chối') {
      return rawStatus;
    }

    if (rawStatus === 'Đã từ chối') {
      return 'Từ chối';
    }

    if (!category) {
      return 'Chờ duyệt';
    }

    return 'Đã duyệt';
  }

  function normalizeSaleStatus(status, approvalStatus) {
    const safeStatus = SALE_STATUSES.includes(status) ? status : 'Đang bán';

    if (approvalStatus === 'Đã duyệt') return safeStatus;
    if (approvalStatus === 'Từ chối') return 'Tạm ẩn';
    return 'Tạm khóa bán';
  }

  function normalizeRoom(room, index) {
    const resolvedCategory = normalizeCategory(room.roomType || room.category, room);
    const approvalStatus = normalizeApprovalStatus(room, resolvedCategory);
    const fallbackDate = new Date().toLocaleString('vi-VN');

    return {
      id: room.id || `room-legacy-${index + 1}`,
      roomName: room.roomName || `Phòng ${index + 1}`,
      propertyName: room.propertyName || 'TravelMate Partner',
      roomType: resolvedCategory || '',
      capacity: room.capacity || '2 khách',
      size: room.size || '--',
      price: String(room.price ?? '0'),
      note: room.note || '',
      adminNote: room.adminNote || '',
      amenities: toList(room.amenities),
      furniture: toList(room.furniture),
      status: normalizeSaleStatus(room.status, approvalStatus),
      approvalStatus,
      submittedAt: normalizeDate(room.submittedAt || room.createdAt, fallbackDate),
      reviewedAt: normalizeDate(room.reviewedAt, ''),
      createdAt: normalizeDate(room.createdAt || room.submittedAt, fallbackDate),
    };
  }

  function ensureRooms() {
    const storedRooms = readJSON(STORAGE_KEYS.rooms, null);
    const source = Array.isArray(storedRooms) ? storedRooms : DEFAULT_ROOMS;
    const normalizedRooms = source.map(normalizeRoom);

    if (!storedRooms || JSON.stringify(storedRooms) !== JSON.stringify(normalizedRooms)) {
      writeJSON(STORAGE_KEYS.rooms, normalizedRooms);
    }

    return normalizedRooms;
  }

  function getRooms() {
    return ensureRooms();
  }

  function saveRooms(rooms) {
    const normalizedRooms = Array.isArray(rooms) ? rooms.map((room, index) => normalizeRoom(room, index)) : [];
    writeJSON(STORAGE_KEYS.rooms, normalizedRooms);
    return normalizedRooms;
  }

  function getRoomById(roomId) {
    return getRooms().find((room) => room.id === roomId) || null;
  }

  function addRoom(payload) {
    const nextRoom = normalizeRoom(
      {
        ...payload,
        id: payload.id || createId('room'),
      },
      0,
    );
    const rooms = getRooms();
    rooms.unshift(nextRoom);
    saveRooms(rooms);
    setLatestRoomId(nextRoom.id);
    return nextRoom;
  }

  function updateRoom(roomId, updates) {
    let updatedRoom = null;

    const rooms = getRooms().map((room, index) => {
      if (room.id !== roomId) return room;
      updatedRoom = normalizeRoom({ ...room, ...updates }, index);
      return updatedRoom;
    });

    saveRooms(rooms);
    return updatedRoom;
  }

  function deleteRoom(roomId) {
    const rooms = getRooms().filter((room) => room.id !== roomId);
    saveRooms(rooms);

    if (getLatestRoomId() === roomId) {
      const fallbackRoom = rooms.find((room) => room.approvalStatus === 'Chờ duyệt') || rooms[0];
      if (fallbackRoom) {
        setLatestRoomId(fallbackRoom.id);
      } else {
        window.localStorage.removeItem(STORAGE_KEYS.latestRoomId);
      }
    }

    return rooms;
  }

  function setLatestRoomId(roomId) {
    if (!roomId) return;
    window.localStorage.setItem(STORAGE_KEYS.latestRoomId, String(roomId));
  }

  function getLatestRoomId() {
    const currentId = window.localStorage.getItem(STORAGE_KEYS.latestRoomId);
    if (currentId) return currentId;

    const legacyRaw = window.localStorage.getItem(STORAGE_KEYS.legacyLatestRoom);
    if (!legacyRaw) return '';

    try {
      const parsed = JSON.parse(legacyRaw);
      if (parsed && parsed.id) {
        setLatestRoomId(parsed.id);
        window.localStorage.removeItem(STORAGE_KEYS.legacyLatestRoom);
        return parsed.id;
      }
    } catch (error) {
      return legacyRaw;
    }

    return '';
  }

  function getLatestRoom() {
    const rooms = getRooms();
    const latestRoomId = getLatestRoomId();

    if (latestRoomId) {
      const matchedRoom = rooms.find((room) => room.id === latestRoomId);
      if (matchedRoom) return matchedRoom;
    }

    return rooms.find((room) => room.approvalStatus === 'Chờ duyệt') || rooms[0] || null;
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

  function getBadgeClass(roomType) {
    const map = {
      Villa: 'badge--success',
      Hotel: 'badge--primary',
      Homestay: 'badge--accent',
      Resort: 'badge--warning',
    };

    return map[roomType] || 'badge--muted';
  }

  function getRoomTypeLabel(room) {
    return room && room.roomType ? room.roomType : 'Chờ admin phân loại';
  }

  window.TravelMateRoomStore = {
    STORAGE_KEYS,
    ROOM_CATEGORIES,
    APPROVAL_STATUSES,
    SALE_STATUSES,
    getRooms,
    saveRooms,
    getRoomById,
    addRoom,
    updateRoom,
    deleteRoom,
    setLatestRoomId,
    getLatestRoomId,
    getLatestRoom,
    formatCurrency,
    getStatusClass,
    getBadgeClass,
    getRoomTypeLabel,
    createId,
  };
})();

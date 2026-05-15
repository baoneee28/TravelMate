(function () {
  const STORAGE_KEYS = {
    vouchers: 'travelmateAdminVouchers',
    roomAssignments: 'travelmateRoomVoucherAssignments',
    latestRoomId: 'travelmateVoucherRoomId',
  };

  const VOUCHER_STATUSES = ['Đang hoạt động', 'Tạm ẩn', 'Hết hạn'];
  const DISCOUNT_TYPES = {
    percent: 'Giảm theo %',
    amount: 'Giảm theo số tiền',
  };

  const DEFAULT_VOUCHERS = [
    {
      id: 'voucher-default-1',
      code: 'TRAVEL20',
      title: 'Ưu đãi mùa hè',
      discountType: 'percent',
      discountValue: '20',
      startDate: '2026-04-01',
      endDate: '2026-06-30',
      usageLimit: '200',
      condition: 'Áp dụng cho phòng đã được admin duyệt, đặt tối thiểu 2 đêm.',
      status: 'Đang hoạt động',
      target: 'user',
      createdAt: '15/04/2026 08:00:00',
    },
    {
      id: 'voucher-default-2',
      code: 'FAMILY500',
      title: 'Giảm cho gia đình',
      discountType: 'amount',
      discountValue: '500000',
      startDate: '2026-04-10',
      endDate: '2026-05-31',
      usageLimit: '120',
      condition: 'Áp dụng cho đơn từ 3.000.000đ.',
      status: 'Đang hoạt động',
      target: 'user',
      createdAt: '15/04/2026 08:05:00',
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

  function normalizeDiscountType(value) {
    const raw = String(value || '').trim();
    if (raw === 'amount' || raw === DISCOUNT_TYPES.amount) return 'amount';
    return 'percent';
  }

  function normalizeVoucher(voucher, index) {
    const discountType = normalizeDiscountType(voucher.discountType);
    const status = VOUCHER_STATUSES.includes(voucher.status) ? voucher.status : 'Đang hoạt động';

    return {
      id: voucher.id || createId(`voucher-${index + 1}`),
      code: String(voucher.code || `VOUCHER${index + 1}`).trim().toUpperCase(),
      title: String(voucher.title || 'Voucher ưu đãi').trim(),
      discountType,
      discountValue: String(voucher.discountValue ?? '0').trim(),
      target: String(voucher.target || 'user'),
      startDate: String(voucher.startDate || '').trim(),
      endDate: String(voucher.endDate || '').trim(),
      usageLimit: String(voucher.usageLimit || '').trim(),
      condition: String(voucher.condition || '').trim(),
      status,
      createdAt: String(voucher.createdAt || new Date().toLocaleString('vi-VN')),
    };
  }

  function ensureVouchers() {
    const storedVouchers = readJSON(STORAGE_KEYS.vouchers, null);
    const source = Array.isArray(storedVouchers) ? storedVouchers : DEFAULT_VOUCHERS;
    const normalized = source.map(normalizeVoucher);

    if (!storedVouchers || JSON.stringify(storedVouchers) !== JSON.stringify(normalized)) {
      writeJSON(STORAGE_KEYS.vouchers, normalized);
    }

    return normalized;
  }

  function getVouchers() {
    return ensureVouchers();
  }

  function saveVouchers(vouchers) {
    const normalized = Array.isArray(vouchers) ? vouchers.map(normalizeVoucher) : [];
    writeJSON(STORAGE_KEYS.vouchers, normalized);
    return normalized;
  }

  function addVoucher(payload) {
    const voucher = normalizeVoucher(
      {
        ...payload,
        id: payload.id || createId('voucher'),
        createdAt: payload.createdAt || new Date().toLocaleString('vi-VN'),
      },
      0,
    );
    const vouchers = getVouchers();
    vouchers.unshift(voucher);
    saveVouchers(vouchers);
    return voucher;
  }

  function updateVoucher(voucherId, updates) {
    let updatedVoucher = null;
    const vouchers = getVouchers().map((voucher, index) => {
      if (voucher.id !== voucherId) return voucher;
      updatedVoucher = normalizeVoucher({ ...voucher, ...updates, id: voucher.id }, index);
      return updatedVoucher;
    });

    saveVouchers(vouchers);
    return updatedVoucher;
  }

  function getAssignments() {
    const assignments = readJSON(STORAGE_KEYS.roomAssignments, {});
    return assignments && typeof assignments === 'object' && !Array.isArray(assignments) ? assignments : {};
  }

  function saveAssignments(assignments) {
    writeJSON(STORAGE_KEYS.roomAssignments, assignments);
    return assignments;
  }

  function deleteVoucher(voucherId) {
    const vouchers = getVouchers().filter((voucher) => voucher.id !== voucherId);
    const assignments = getAssignments();

    Object.keys(assignments).forEach((roomId) => {
      assignments[roomId] = (assignments[roomId] || []).filter((id) => id !== voucherId);
      if (!assignments[roomId].length) delete assignments[roomId];
    });

    saveVouchers(vouchers);
    saveAssignments(assignments);
    return vouchers;
  }

  function getVoucherById(voucherId) {
    return getVouchers().find((voucher) => voucher.id === voucherId) || null;
  }

  function getActiveVouchers() {
    return getVouchers().filter((voucher) => voucher.status === 'Đang hoạt động');
  }

  function assignVouchersToRoom(roomId, voucherIds) {
    if (!roomId) return {};
    const validIds = getVouchers().map((voucher) => voucher.id);
    const uniqueVoucherIds = [...new Set((voucherIds || []).filter((id) => validIds.includes(id)))];
    const assignments = getAssignments();

    if (uniqueVoucherIds.length) {
      assignments[roomId] = uniqueVoucherIds;
    } else {
      delete assignments[roomId];
    }

    return saveAssignments(assignments);
  }

  function getRoomVoucherIds(roomId) {
    const assignments = getAssignments();
    return Array.isArray(assignments[roomId]) ? assignments[roomId] : [];
  }

  function getRoomVouchers(roomId) {
    const voucherMap = new Map(getVouchers().map((voucher) => [voucher.id, voucher]));
    return getRoomVoucherIds(roomId)
      .map((voucherId) => voucherMap.get(voucherId))
      .filter(Boolean);
  }

  function getRoomVoucherLabels(roomId) {
    return getRoomVouchers(roomId).map((voucher) => `${voucher.code} (${formatDiscount(voucher)})`);
  }

  function setLatestRoomId(roomId) {
    if (!roomId) return;
    window.localStorage.setItem(STORAGE_KEYS.latestRoomId, String(roomId));
  }

  function getLatestRoomId() {
    return window.localStorage.getItem(STORAGE_KEYS.latestRoomId) || '';
  }

  function formatDiscount(voucher) {
    if (!voucher) return '--';
    const amount = Number(voucher.discountValue) || 0;
    return voucher.discountType === 'amount' ? `${amount.toLocaleString('vi-VN')}đ` : `${amount}%`;
  }

  function getDiscountTypeLabel(voucher) {
    return DISCOUNT_TYPES[voucher?.discountType] || DISCOUNT_TYPES.percent;
  }

  function getStatusBadgeClass(status) {
    if (status === 'Đang hoạt động') return 'badge--success';
    if (status === 'Hết hạn') return 'badge--danger';
    return 'badge--warning';
  }

  window.TravelMateVoucherStore = {
    STORAGE_KEYS,
    VOUCHER_STATUSES,
    DISCOUNT_TYPES,
    getVouchers,
    saveVouchers,
    addVoucher,
    updateVoucher,
    deleteVoucher,
    getVoucherById,
    getActiveVouchers,
    getAssignments,
    assignVouchersToRoom,
    getRoomVoucherIds,
    getRoomVouchers,
    getRoomVoucherLabels,
    setLatestRoomId,
    getLatestRoomId,
    formatDiscount,
    getDiscountTypeLabel,
    getStatusBadgeClass,
    createId,
  };
})();

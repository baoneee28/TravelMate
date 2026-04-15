document.addEventListener('DOMContentLoaded', () => {
  const voucherStore = window.TravelMateVoucherStore;
  const form = document.getElementById('admin-voucher-form');
  const tableBody = document.getElementById('admin-vouchers-table');

  if (!voucherStore || !form || !tableBody) return;

  const searchInput = document.getElementById('admin-voucher-search');
  const statusFilter = document.getElementById('admin-voucher-status-filter');
  const formTitle = document.getElementById('admin-voucher-form-title');
  const submitButton = document.getElementById('admin-voucher-submit');
  const cancelButton = document.getElementById('admin-voucher-cancel');
  const editingInput = document.getElementById('admin-voucher-editing-id');
  const countEl = document.getElementById('admin-voucher-result-count');

  const tabs = document.querySelectorAll('.admin-tab');
  let currentTargetFilter = 'user';

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function getFilteredVouchers() {
    const query = String(searchInput?.value || '').trim().toLowerCase();
    const status = statusFilter?.value || 'all';

    return voucherStore.getVouchers().filter((voucher) => {
      const voucherTarget = voucher.target || 'user';
      if (voucherTarget !== currentTargetFilter) return false;

      const matchesQuery =
        !query ||
        [voucher.code, voucher.title, voucher.condition, voucherStore.formatDiscount(voucher)]
          .join(' ')
          .toLowerCase()
          .includes(query);
      const matchesStatus = status === 'all' || voucher.status === status;
      return matchesQuery && matchesStatus;
    });
  }

  function updateSummary() {
    const allVouchers = voucherStore.getVouchers();
    const vouchers = allVouchers.filter((v) => (v.target || 'user') === currentTargetFilter);
    const assignedRoomIds = Object.keys(voucherStore.getAssignments());
    
    const summary = {
      total: vouchers.length,
      active: vouchers.filter((voucher) => voucher.status === 'Đang hoạt động').length,
      hidden: vouchers.filter((voucher) => voucher.status === 'Tạm ẩn').length,
      assigned: currentTargetFilter === 'partner' ? assignedRoomIds.length : 0,
    };

    Object.entries(summary).forEach(([key, value]) => {
      const el = document.querySelector(`[data-voucher-summary="${key}"]`);
      if (el) el.textContent = String(value);
    });
  }

  function renderTable() {
    const vouchers = getFilteredVouchers();

    if (countEl) countEl.textContent = `${vouchers.length} voucher hiển thị`;

    if (!vouchers.length) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="8">
            <div class="partner-empty-inline">Không có voucher phù hợp với bộ lọc hiện tại.</div>
          </td>
        </tr>
      `;
      updateSummary();
      return;
    }

    tableBody.innerHTML = vouchers
      .map(
        (voucher, index) => `
          <tr>
            <td>${index + 1}</td>
            <td>
              <strong>${escapeHtml(voucher.code)}</strong>
              <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 4px;">
                ${escapeHtml(voucher.title)}
              </div>
            </td>
            <td>${escapeHtml(voucher.target === 'partner' ? 'Đối tác' : 'Người dùng')}</td>
            <td>${escapeHtml(voucherStore.getDiscountTypeLabel(voucher))}</td>
            <td style="font-weight: 600; color: var(--color-primary)">
              ${escapeHtml(voucherStore.formatDiscount(voucher))}
            </td>
            <td>${escapeHtml(voucher.startDate || '--')}</td>
            <td>${escapeHtml(voucher.endDate || '--')}</td>
            <td><span class="badge ${voucherStore.getStatusBadgeClass(voucher.status)}">${escapeHtml(voucher.status)}</span></td>
            <td>
              <div class="table__actions">
                <button class="btn btn--outline btn--sm btn-edit-voucher" data-voucher-id="${escapeHtml(voucher.id)}">
                  Sửa
                </button>
                <button class="btn btn--danger btn--sm btn-delete-voucher" data-voucher-id="${escapeHtml(voucher.id)}" data-code="${escapeHtml(voucher.code)}">
                  Xóa
                </button>
              </div>
            </td>
          </tr>
        `,
      )
      .join('');

    updateSummary();
  }

  function setFormMode(voucher = null) {
    if (voucher) {
      editingInput.value = voucher.id;
      form.elements.code.value = voucher.code;
      form.elements.title.value = voucher.title;
      form.elements.target.value = voucher.target || 'user';
      form.elements.discountType.value = voucher.discountType;
      form.elements.discountValue.value = voucher.discountValue;
      form.elements.startDate.value = voucher.startDate;
      form.elements.endDate.value = voucher.endDate;
      form.elements.usageLimit.value = voucher.usageLimit;
      form.elements.status.value = voucher.status;
      form.elements.condition.value = voucher.condition;
      if (formTitle) formTitle.textContent = 'Cập nhật voucher';
      if (submitButton) submitButton.textContent = 'Lưu thay đổi';
      if (cancelButton) cancelButton.hidden = false;
      return;
    }

    editingInput.value = '';
    form.reset();
    if(form.elements.target) form.elements.target.value = 'user';
    form.elements.discountType.value = 'percent';
    form.elements.status.value = 'Đang hoạt động';
    if (formTitle) formTitle.textContent = 'Tạo voucher mới';
    if (submitButton) submitButton.textContent = 'Tạo voucher';
    if (cancelButton) cancelButton.hidden = true;
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault();

    const formData = new FormData(form);
    const code = String(formData.get('code') || '').trim().toUpperCase();
    const discountValue = String(formData.get('discountValue') || '').trim();

    if (!code) {
      if (typeof showToast === 'function') showToast('Hãy nhập mã voucher.', 'error');
      form.elements.code.focus();
      return;
    }

    if (!discountValue) {
      if (typeof showToast === 'function') showToast('Hãy nhập giá trị ưu đãi.', 'error');
      form.elements.discountValue.focus();
      return;
    }

    const payload = {
      code,
      title: String(formData.get('title') || 'Voucher ưu đãi').trim(),
      target: String(formData.get('target') || 'user'),
      discountType: String(formData.get('discountType') || 'percent'),
      discountValue,
      startDate: String(formData.get('startDate') || ''),
      endDate: String(formData.get('endDate') || ''),
      usageLimit: String(formData.get('usageLimit') || ''),
      status: String(formData.get('status') || 'Đang hoạt động'),
      condition: String(formData.get('condition') || '').trim(),
    };

    const editingId = editingInput.value;
    const savedVoucher = editingId
      ? voucherStore.updateVoucher(editingId, payload)
      : voucherStore.addVoucher(payload);

    if (typeof showToast === 'function') {
      showToast(
        editingId ? `Đã cập nhật voucher "${savedVoucher?.code || code}"` : `Đã tạo voucher "${code}"`,
        'success',
      );
    }

    setFormMode();
    renderTable();
  });

  tableBody.addEventListener('click', (event) => {
    const editButton = event.target.closest('.btn-edit-voucher');
    if (editButton) {
      const voucher = voucherStore.getVoucherById(editButton.getAttribute('data-voucher-id'));
      if (voucher) setFormMode(voucher);
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    const deleteButton = event.target.closest('.btn-delete-voucher');
    if (deleteButton) {
      const code = deleteButton.getAttribute('data-code') || 'voucher';
      if (!window.confirm(`Bạn có chắc muốn xóa voucher "${code}"?`)) return;
      voucherStore.deleteVoucher(deleteButton.getAttribute('data-voucher-id'));
      renderTable();
      if (typeof showToast === 'function') showToast(`Đã xóa voucher "${code}"`, 'success');
    }
  });

  if (searchInput) searchInput.addEventListener('input', renderTable);
  if (statusFilter) statusFilter.addEventListener('change', renderTable);
  if (cancelButton) cancelButton.addEventListener('click', () => setFormMode());

  tabs.forEach(tab => {
    tab.addEventListener('click', (e) => {
      e.preventDefault();
      tabs.forEach(t => {
        t.classList.remove('active');
        t.style.fontWeight = '500';
        t.style.color = 'var(--color-text-muted)';
        t.style.borderBottom = 'none';
      });
      tab.classList.add('active');
      tab.style.fontWeight = '600';
      tab.style.color = 'var(--color-primary)';
      tab.style.borderBottom = '2px solid var(--color-primary)';
      
      currentTargetFilter = tab.getAttribute('data-target') || 'user';
      renderTable();
    });
  });

  setFormMode();
  renderTable();
});

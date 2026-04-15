/**
 * admin-places.js - Region filters and local image preview for places page.
 */

document.addEventListener('DOMContentLoaded', () => {
  const tableBody = document.getElementById('places-table-body');
  if (!tableBody) return;

  const regionTabs = document.querySelectorAll('.place-region-tab');
  const searchInput = document.getElementById('place-table-search');
  const typeFilter = document.getElementById('place-type-filter');
  const statusFilter = document.getElementById('place-status-filter');
  const countEl = document.querySelector('.table-result-count');
  const imageFileInput = document.getElementById('place-image-file');
  const imagePreview = document.getElementById('place-image-preview');
  const totalPlaces = 100;
  let activeRegion = document.querySelector('.place-region-tab.active')?.dataset.region || 'north';
  let activeEditRow = null;

  const normalizeText = (value = '') =>
    value
      .toString()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D')
      .toLowerCase()
      .trim();

  const getRows = () => [...tableBody.querySelectorAll('tr')];

  const renumberRows = (visibleRows) => {
    visibleRows.forEach((row, index) => {
      const indexCell = row.querySelector('[data-row-index]');
      if (indexCell) indexCell.textContent = index + 1;
    });
  };

  const applyPlaceFilters = () => {
    const query = normalizeText(searchInput?.value || '');
    const typeValue = typeFilter?.value || 'all';
    const statusValue = statusFilter?.value || 'all';
    const visibleRows = [];

    getRows().forEach((row) => {
      const matchesRegion = row.dataset.region === activeRegion;
      const matchesSearch = !query || normalizeText(row.textContent).includes(query);
      const matchesType = typeValue === 'all' || row.dataset.placeType === typeValue;
      const matchesStatus = statusValue === 'all' || row.dataset.placeStatus === statusValue;
      const shouldShow = matchesRegion && matchesSearch && matchesType && matchesStatus;

      row.hidden = !shouldShow;
      row.style.display = shouldShow ? '' : 'none';
      if (shouldShow) visibleRows.push(row);
    });

    renumberRows(visibleRows);
    if (countEl) {
      countEl.textContent = `Hiển thị ${visibleRows.length} / ${totalPlaces} địa điểm du lịch`;
    }
  };

  regionTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      regionTabs.forEach((item) => item.classList.remove('active'));
      tab.classList.add('active');
      activeRegion = tab.dataset.region;
      applyPlaceFilters();
    });
  });

  if (searchInput) searchInput.addEventListener('input', applyPlaceFilters);
  if (typeFilter) typeFilter.addEventListener('change', applyPlaceFilters);
  if (statusFilter) statusFilter.addEventListener('change', applyPlaceFilters);

  const renderImagePreview = (src = '') => {
    if (!imagePreview) return;
    imagePreview.textContent = '';

    if (!src) {
      const emptyText = document.createElement('span');
      emptyText.textContent = 'Chưa chọn ảnh';
      imagePreview.appendChild(emptyText);
      return;
    }

    const img = document.createElement('img');
    img.src = src;
    img.alt = 'Ảnh địa điểm được chọn từ máy';
    imagePreview.appendChild(img);
  };

  document.querySelectorAll('[data-modal="modal-add-place"]').forEach((trigger) => {
    trigger.addEventListener('click', () => {
      activeEditRow = trigger.classList.contains('btn-edit-place') ? trigger.closest('tr') : null;
      if (imageFileInput) imageFileInput.value = '';
      renderImagePreview();
    });
  });

  if (imageFileInput) {
    imageFileInput.addEventListener('change', () => {
      const file = imageFileInput.files?.[0];
      if (!file) {
        renderImagePreview();
        return;
      }

      const reader = new FileReader();
      reader.addEventListener('load', () => {
        const imageUrl = reader.result;
        if (typeof imageUrl !== 'string') return;

        renderImagePreview(imageUrl);

        const rowImage = activeEditRow?.querySelector('.place-table-image');
        if (rowImage) {
          rowImage.src = imageUrl;
          rowImage.alt = file.name;
        }
      });
      reader.readAsDataURL(file);
    });
  }

  document.querySelectorAll('.btn-delete[data-name]').forEach((button) => {
    button.addEventListener('click', () => {
      window.setTimeout(applyPlaceFilters, 350);
    });
  });

  applyPlaceFilters();
});

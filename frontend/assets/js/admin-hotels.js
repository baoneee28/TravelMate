document.addEventListener('DOMContentLoaded', () => {
  const tableBody = document.getElementById('hotels-table-body');
  if (!tableBody) return;

  const rows = [...tableBody.querySelectorAll('tr')];
  const searchInput = document.getElementById('hotel-table-search');
  const statusSelect = document.getElementById('hotel-status-filter');
  const starSelect = document.getElementById('hotel-star-filter');
  const typeTabs = [...document.querySelectorAll('.hotel-type-tab[data-room-type]')];
  const countEl = document.querySelector('.table-result-count');

  let activeRoomType = typeTabs.find((tab) => tab.classList.contains('active'))?.dataset.roomType || 'hotel';

  function applyFilters() {
    const query = (searchInput?.value || '').toLowerCase().trim();
    const statusValue = statusSelect?.value || 'all';
    const starValue = starSelect?.value || 'all';
    let visibleCount = 0;

    rows.forEach((row) => {
      const rowText = row.textContent.toLowerCase();
      const matchesSearch = !query || rowText.includes(query);
      const matchesRoomType = row.dataset.roomType === activeRoomType;
      const matchesStatus = statusValue === 'all' || row.dataset.hotelStatus === statusValue;
      const matchesStar = starValue === 'all' || row.dataset.star === starValue;
      const isVisible = matchesSearch && matchesRoomType && matchesStatus && matchesStar;

      row.style.display = isVisible ? '' : 'none';

      if (isVisible) {
        visibleCount += 1;
        const rowIndex = row.querySelector('[data-row-index]');
        if (rowIndex) rowIndex.textContent = String(visibleCount);
      }
    });

    if (countEl) {
      countEl.textContent = `Hiển thị ${visibleCount} / 48 cơ sở lưu trú`;
    }
  }

  typeTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      activeRoomType = tab.dataset.roomType || 'hotel';
      typeTabs.forEach((item) => item.classList.remove('active'));
      tab.classList.add('active');
      applyFilters();
    });
  });

  if (searchInput) searchInput.addEventListener('input', applyFilters);
  if (statusSelect) statusSelect.addEventListener('change', applyFilters);
  if (starSelect) starSelect.addEventListener('change', applyFilters);

  tableBody.addEventListener('click', (event) => {
    if (event.target.closest('.btn-delete')) {
      window.setTimeout(applyFilters, 320);
    }
  });

  applyFilters();
});

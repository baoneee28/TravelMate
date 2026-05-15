document.addEventListener('DOMContentLoaded', () => {
  const tableBody = document.getElementById('hotels-table-body');
  if (!tableBody) return;

  const searchInput = document.getElementById('hotel-table-search');
  const statusSelect = document.getElementById('hotel-status-filter');
  const starSelect = document.getElementById('hotel-star-filter');
  const typeTabs = [...document.querySelectorAll('.hotel-type-tab[data-room-type]')];
  const countEl = document.querySelector('.table-result-count');

  let activeRoomType = typeTabs.find((tab) => tab.classList.contains('active'))?.dataset.roomType || 'all';

  function applyFilters() {
    const query = (searchInput?.value || '').toLowerCase().trim();
    const statusValue = statusSelect?.value || 'all';
    const starValue = starSelect?.value || 'all'; // Currently no star rating in roomStore, so we'll mock it or ignore it

    // Get only approved rooms
    let rooms = roomStore.getRooms().filter(r => r.approvalStatus === 'Đã duyệt');

    // Apply exact type tab filter (e.g. hotel vs villa), using lowercase for match safety
    if (activeRoomType !== 'all') {
      rooms = rooms.filter(r => r.roomType && r.roomType.toLowerCase() === activeRoomType.toLowerCase());
    }

    // Filter by text search
    if (query) {
      rooms = rooms.filter(r => 
        (r.roomName || '').toLowerCase().includes(query) || 
        (r.propertyName || '').toLowerCase().includes(query)
      );
    }

    // Filter by status (Hoạt động / Tạm ngưng) vs 'Đang bán' / 'Tạm ẩn' in roomStore
    if (statusValue !== 'all') {
       // Mock translation from roomStore sale status to hotel UI status
       rooms = rooms.filter(r => {
          if (statusValue === 'active') return r.status === 'Đang bán';
          if (statusValue === 'inactive') return r.status !== 'Đang bán';
          return true;
       });
    }

    // Render logic
    tableBody.innerHTML = '';
    
    rooms.forEach((room, index) => {
      const tr = document.createElement('tr');
      
      const statusClass = room.status === 'Đang bán' ? 'status-dot--active' : 'status-dot--inactive';
      const statusText = room.status === 'Đang bán' ? 'Hoạt động' : 'Tạm ngưng';
      // Mock random image for demo
      const imgUrl = `https://images.unsplash.com/photo-${1500000000000 + index * 1000}?w=160&q=60`;
      
      // Generate mock fallback code if old room
      const fallbackCode = 'RM-' + (1000 + index * 10);
      
      tr.innerHTML = `
        <td><strong>${room.roomCode || fallbackCode}</strong></td>
        <td>
            <img src="${imgUrl}" class="table-hotel-image" alt="Hình ảnh phòng" style="object-fit: cover;" />
        </td>
        <td>
            <strong>${room.propertyName || 'Chưa cập nhật tên cơ sở'}</strong><br>
            <span style="color:var(--color-text-muted); font-size:12px;">(${room.roomName || 'Chưa cập nhật tên phòng'})</span>
        </td>
        <td><div style="max-width: 180px; white-space: normal; font-size: 13px;">${room.address || 'Chưa cập nhật địa chỉ'}</div></td>
        <td><span class="status-dot ${statusClass}">${statusText}</span></td>
        <td><strong style="color: var(--color-primary)">${typeof roomStore !== 'undefined' ? roomStore.formatCurrency(room.price) : room.price + 'đ'}</strong></td>
        <td>
            <div class="table__actions">
                <button class="btn btn--outline btn--sm" data-modal="modal-add-hotel" title="Sửa">✏</button>
                <button class="btn btn--danger btn--sm btn-delete" data-name="${room.roomName}" title="Xóa">🗑</button>
            </div>
        </td>
      `;
      tableBody.appendChild(tr);
    });

    if (countEl) {
      countEl.textContent = `Hiển thị ${rooms.length} / ${rooms.length} cơ sở lưu trú`;
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
      // Typically we'd delete from store, but here just visually refresh
      window.setTimeout(applyFilters, 320);
    }
  });

  applyFilters();
});

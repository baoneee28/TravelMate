/**
 * admin.js - TravelMate Admin JavaScript
 * Sidebar toggle, table search, modals, CRUD actions
 */

document.addEventListener('DOMContentLoaded', () => {

  // ── Admin Sidebar Toggle (Mobile) ────────────
  const sidebarHamburger = document.querySelector('.admin-topbar__hamburger');
  const adminSidebar     = document.querySelector('.admin-sidebar');
  const sidebarOverlay   = document.querySelector('.admin-sidebar-overlay');

  if (sidebarHamburger && adminSidebar) {
    sidebarHamburger.addEventListener('click', () => {
      adminSidebar.classList.toggle('open');
      if (sidebarOverlay) sidebarOverlay.classList.toggle('open');
    });
  }

  if (sidebarOverlay) {
    sidebarOverlay.addEventListener('click', () => {
      adminSidebar.classList.remove('open');
      sidebarOverlay.classList.remove('open');
    });
  }

  // ── Table Search/Filter ──────────────────────
  const tableSearch = document.getElementById('table-search');
  if (tableSearch) {
    tableSearch.addEventListener('input', () => {
      const query = tableSearch.value.toLowerCase().trim();
      const rows  = document.querySelectorAll('.data-table tbody tr');

      rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(query) ? '' : 'none';
      });

      const visible = [...rows].filter(r => r.style.display !== 'none').length;
      const countEl = document.querySelector('.table-result-count');
      if (countEl) countEl.textContent = `${visible} kết quả`;
    });
  }

  // ── Status Filter Tabs ───────────────────────
  document.querySelectorAll('.filter-tab[data-filter]').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.filter-tab[data-filter]').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const filter = tab.dataset.filter;
      const rows   = document.querySelectorAll('.data-table tbody tr[data-status]');

      rows.forEach(row => {
        if (filter === 'all' || row.dataset.status === filter) {
          row.style.display = '';
        } else {
          row.style.display = 'none';
        }
      });
    });
  });

  // ── Add/Edit Modal ───────────────────────────
  // Handled in main.js via [data-modal] triggers

  // ── Delete Confirmation ──────────────────────
  document.querySelectorAll('.btn-delete[data-name]').forEach(btn => {
    btn.addEventListener('click', () => {
      const name    = btn.dataset.name;
      const confirm = window.confirm(`Bạn có chắc muốn xóa "${name}"?`);
      if (confirm) {
        // In static demo: just remove the row
        const row = btn.closest('tr');
        if (row) {
          row.style.opacity = '0';
          row.style.transition = 'opacity 0.3s';
          setTimeout(() => row.remove(), 300);
          if (typeof showToast === 'function') {
            showToast(`Đã xóa "${name}" thành công`, 'success');
          }
        }
      }
    });
  });

  // ── Active Sidebar Link ──────────────────────
  const currentPath = window.location.pathname.split('/').pop();
  document.querySelectorAll('.admin-sidebar__link').forEach(link => {
    const href = link.getAttribute('href');
    if (href && href.includes(currentPath)) {
      link.classList.add('active');
    }
  });

  // ── Bar Chart Animation ──────────────────────
  document.querySelectorAll('.chart-bar').forEach(bar => {
    const height = bar.getAttribute('data-height') || '50%';
    bar.style.height = '0';
    setTimeout(() => {
      bar.style.height = height;
      bar.style.transition = 'height 0.8s ease';
    }, 200);
  });

  // ── Pagination ───────────────────────────────
  const paginationItems = document.querySelectorAll('.pagination__item[data-page]');
  paginationItems.forEach(item => {
    if (item.classList.contains('disabled')) return;
    item.addEventListener('click', () => {
      paginationItems.forEach(i => i.classList.remove('active'));
      item.classList.add('active');
    });
  });

  // ── Row Actions Dropdown ─────────────────────
  document.querySelectorAll('.row-actions-btn').forEach(btn => {
    const dropdown = btn.nextElementSibling;
    if (!dropdown) return;

    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      // Close all other dropdowns
      document.querySelectorAll('.row-actions-dropdown.open').forEach(d => {
        if (d !== dropdown) d.classList.remove('open');
      });
      dropdown.classList.toggle('open');
    });
  });

  document.addEventListener('click', () => {
    document.querySelectorAll('.row-actions-dropdown.open').forEach(d => d.classList.remove('open'));
  });

});

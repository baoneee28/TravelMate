/* ==============================================
   main.js - TravelMate Shared JavaScript
   Mobile nav, dropdowns, general interactions
   ============================================== */

document.addEventListener('DOMContentLoaded', () => {

  // ── Mobile Navbar Toggle ─────────────────────
  const hamburger = document.querySelector('.navbar__hamburger');
  const mobileNav = document.querySelector('.navbar__mobile');

  if (hamburger && mobileNav) {
    hamburger.addEventListener('click', () => {
      const isOpen = mobileNav.classList.toggle('open');
      hamburger.setAttribute('aria-expanded', isOpen);
    });
  }

  // ── Shared User Sidebar ──────────────────────
  const hamburgerBtn = document.getElementById('hamburgerBtn');
  const sidebarMenu = document.getElementById('sidebarMenu');
  const sidebarOverlay = document.getElementById('sidebarOverlay');
  const sidebarCloseBtn = document.getElementById('sidebarCloseBtn');

  if (hamburgerBtn && sidebarMenu && sidebarOverlay) {
    const openSidebar = () => {
      sidebarMenu.classList.add('active');
      sidebarOverlay.classList.add('active');
      hamburgerBtn.setAttribute('aria-expanded', 'true');
      document.body.style.overflow = 'hidden';
    };

    const closeSidebar = () => {
      sidebarMenu.classList.remove('active');
      sidebarOverlay.classList.remove('active');
      hamburgerBtn.setAttribute('aria-expanded', 'false');
      document.body.style.overflow = '';
    };

    hamburgerBtn.addEventListener('click', (event) => {
      event.stopPropagation();
      openSidebar();
    });
    sidebarCloseBtn?.addEventListener('click', closeSidebar);
    sidebarOverlay.addEventListener('click', closeSidebar);
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') closeSidebar();
    });
  }

  // ── User Dropdown Menu ───────────────────────
  const userMenuTriggers = document.querySelectorAll('.user-menu__trigger');

  userMenuTriggers.forEach(trigger => {
    const dropdown = trigger.closest('.user-menu')?.querySelector('.user-menu__dropdown');
    if (!dropdown) return;

    trigger.addEventListener('click', (e) => {
      e.stopPropagation();
      dropdown.classList.toggle('open');
    });
  });

  // Close dropdowns on outside click
  document.addEventListener('click', () => {
    document.querySelectorAll('.user-menu__dropdown.open').forEach(d => {
      d.classList.remove('open');
    });
  });

  // ── Smooth Scroll for Anchor Links ──────────
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', (e) => {
      const target = document.querySelector(anchor.getAttribute('href'));
      if (target) {
        e.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  });

  // ── Active Navbar Link ───────────────────────
  const currentPath = window.location.pathname;
  document.querySelectorAll('.navbar__link').forEach(link => {
    if (link.getAttribute('href') === currentPath ||
        currentPath.includes(link.getAttribute('href'))) {
      link.classList.add('active');
    }
  });

  // ── Toast Notification Helper ────────────────
  window.showToast = (message, type = 'success') => {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.innerHTML = `
      <span class="toast__icon">${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span>
      <span class="toast__message">${message}</span>
    `;

    // Inline toast styles
    Object.assign(toast.style, {
      position: 'fixed',
      bottom: '24px',
      right: '24px',
      background: type === 'success' ? '#27ae60' : type === 'error' ? '#e74c3c' : '#3498db',
      color: '#fff',
      padding: '12px 20px',
      borderRadius: '8px',
      display: 'flex',
      alignItems: 'center',
      gap: '10px',
      fontSize: '14px',
      fontWeight: '500',
      boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
      zIndex: '9999',
      animation: 'fadeInUp 0.3s ease',
      maxWidth: '360px',
    });

    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  };

  // ── Modal Helper ─────────────────────────────
  // Open modal: <button data-modal="modal-id">
  document.querySelectorAll('[data-modal]').forEach(trigger => {
    trigger.addEventListener('click', () => {
      const modal = document.getElementById(trigger.dataset.modal);
      if (modal) {
        modal.classList.add('open');
        document.body.style.overflow = 'hidden';
      }
    });
  });

  // Close modal on overlay click or close button
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) closeModal(overlay);
    });
  });

  document.querySelectorAll('.modal__close').forEach(btn => {
    btn.addEventListener('click', () => {
      const overlay = btn.closest('.modal-overlay');
      if (overlay) closeModal(overlay);
    });
  });

  function closeModal(overlay) {
    overlay.classList.remove('open');
    document.body.style.overflow = '';
  }

  // Close modal on ESC
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.open').forEach(closeModal);
    }
  });

  // ── Reset body overflow khi navigate đi ──────
  // Đảm bảo overflow:hidden từ modal không bị giữ lại khi back/forward
  window.addEventListener('pagehide', () => {
    document.body.style.overflow = '';
  });
  window.addEventListener('beforeunload', () => {
    document.body.style.overflow = '';
  });

  // ── Image Fallback ───────────────────────────
  document.querySelectorAll('img').forEach(img => {
    img.addEventListener('error', () => {
      img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%23e8f4fb" width="400" height="300"/%3E%3Ctext x="50%25" y="50%25" fill="%231A6B9A" font-size="18" text-anchor="middle" dy="0.3em" font-family="sans-serif"%3ETravelMate%3C/text%3E%3C/svg%3E';
    });
  });
  // SCROLL TO TOP
const scrollBtn = document.getElementById("scrollTopBtn");

if (scrollBtn) {
    window.addEventListener("scroll", () => {
        if (document.body.classList.contains("chatbot-open")) {
            scrollBtn.classList.remove("show");
            return;
        }

        if (window.scrollY > 400) {
            scrollBtn.classList.add("show");
        } else {
            scrollBtn.classList.remove("show");
        }
    });

    scrollBtn.addEventListener("click", () => {
        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    });
}

  // ── Header User Dropdown (sau khi đăng nhập) ─────────
  const headerUserBtn = document.getElementById('headerUserBtn');
  const headerUser    = document.getElementById('headerUser');

  if (headerUserBtn && headerUser) {
    // Toggle dropdown khi click vào button
    headerUserBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      headerUser.classList.toggle('open');
    });

    // Đóng dropdown khi click ra ngoài
    document.addEventListener('click', (e) => {
      if (!headerUser.contains(e.target)) {
        headerUser.classList.remove('open');
      }
    });

    // Đóng dropdown khi nhấn ESC
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') headerUser.classList.remove('open');
    });
  }

  // ── Notification Bell ────────────────────────
  const notifBtn = document.getElementById('notifBtn');
  const notifWrap = document.getElementById('notifWrap');
  const notifDropdown = document.getElementById('notifDropdown');
  const notifBadge = document.getElementById('notifBadge');
  const notifList = document.getElementById('notifList');
  const notifReadAll = document.getElementById('notifReadAll');

  if (notifBtn && notifWrap && notifDropdown && notifBadge && notifList) {
    let notifOpen = false;
    let notifLoaded = false;
    const icons = {
      REVIEW_REMINDER:       { cls: 'notif-icon-review', icon: 'fa-star' },
      BOOKING_CONFIRMED:     { cls: 'notif-icon-confirm', icon: 'fa-circle-check' },
      BOOKING_CHECKIN_READY: { cls: 'notif-icon-checkin', icon: 'fa-bed' },
      SYSTEM:                { cls: 'notif-icon-system', icon: 'fa-bell' }
    };

    const escapeHtml = (value) => String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');

    const updateBadge = () => {
      fetch('/api/notifications/count')
        .then(response => response.json())
        .then(data => {
          const count = data.count || 0;
          if (count > 0) {
            notifBadge.textContent = count > 99 ? '99+' : count;
            notifBadge.style.display = 'flex';
          } else {
            notifBadge.style.display = 'none';
          }
        })
        .catch(() => {});
    };

    const renderNotifications = (items) => {
      if (!items || items.length === 0) {
        notifList.innerHTML = '<p class="notif-empty">Không có thông báo</p>';
        return;
      }

      notifList.innerHTML = items.map(item => {
        const icon = icons[item.type] || icons.SYSTEM;
        return `<a class="notif-item ${item.isRead ? '' : 'unread'}" href="${escapeHtml(item.targetUrl || '/my-bookings')}" data-id="${item.id}" data-read="${item.isRead}">
          <div class="notif-item-icon ${icon.cls}"><i class="fa-solid ${icon.icon}"></i></div>
          <div class="notif-item-body">
            <div class="notif-item-title">${escapeHtml(item.title)}</div>
            <div class="notif-item-msg">${escapeHtml(item.message)}</div>
            <div class="notif-item-time">${escapeHtml(item.createdAt)}</div>
          </div>
        </a>`;
      }).join('');

      notifList.querySelectorAll('.notif-item').forEach(item => {
        item.addEventListener('click', function () {
          if (this.dataset.read === 'false') {
            fetch('/api/notifications/' + this.dataset.id + '/read', { method: 'POST' })
              .then(() => {
                this.classList.remove('unread');
                this.dataset.read = 'true';
                updateBadge();
              })
              .catch(() => {});
          }
        });
      });
    };

    const loadNotifications = () => {
      notifList.innerHTML = '<p class="notif-empty">Đang tải...</p>';
      fetch('/api/notifications')
        .then(response => response.json())
        .then(renderNotifications)
        .catch(() => {
          notifList.innerHTML = '<p class="notif-empty">Không thể tải thông báo</p>';
        });
    };

    notifBtn.addEventListener('click', (event) => {
      event.stopPropagation();
      notifOpen = !notifOpen;
      notifDropdown.style.display = notifOpen ? 'block' : 'none';
      if (notifOpen && !notifLoaded) {
        loadNotifications();
        notifLoaded = true;
      }
    });

    notifReadAll?.addEventListener('click', (event) => {
      event.stopPropagation();
      fetch('/api/notifications/read-all', { method: 'POST' })
        .then(() => {
          notifBadge.style.display = 'none';
          notifLoaded = false;
          loadNotifications();
        })
        .catch(() => {});
    });

    document.addEventListener('click', (event) => {
      if (notifOpen && !notifWrap.contains(event.target)) {
        notifOpen = false;
        notifDropdown.style.display = 'none';
      }
    });

    updateBadge();
  }
});


/**
 * user.js - TravelMate User-side JavaScript
 * Hotel search, date pickers, booking form validation
 */

document.addEventListener('DOMContentLoaded', () => {

  // ── Hotel Search Form ────────────────────────
  const searchForm = document.getElementById('hotel-search-form');
  if (searchForm) {
    searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const destination = searchForm.querySelector('#search-destination')?.value;
      const checkin     = searchForm.querySelector('#search-checkin')?.value;
      const checkout    = searchForm.querySelector('#search-checkout')?.value;
      const guests      = searchForm.querySelector('#search-guests')?.value;

      if (!destination) {
        showFieldError('search-destination', 'Vui lòng nhập điểm đến');
        return;
      }

      const params = new URLSearchParams({ destination, checkin, checkout, guests });
      window.location.href = `hotels.html?${params.toString()}`;
    });
  }

  // ── Date Range Validation ────────────────────
  const checkinInput  = document.getElementById('checkin');
  const checkoutInput = document.getElementById('checkout');

  if (checkinInput) {
    // Set minimum date to today
    const today = new Date().toISOString().split('T')[0];
    checkinInput.min = today;

    checkinInput.addEventListener('change', () => {
      if (checkoutInput) {
        checkoutInput.min = checkinInput.value;
        if (checkoutInput.value && checkoutInput.value <= checkinInput.value) {
          // Auto set checkout to next day
          const nextDay = new Date(checkinInput.value);
          nextDay.setDate(nextDay.getDate() + 1);
          checkoutInput.value = nextDay.toISOString().split('T')[0];
        }
      }
      updateBookingSummary();
    });
  }

  if (checkoutInput) {
    checkoutInput.addEventListener('change', updateBookingSummary);
  }

  // ── Booking Summary Updater ──────────────────
  function updateBookingSummary() {
    const checkin  = document.getElementById('checkin')?.value;
    const checkout = document.getElementById('checkout')?.value;

    if (!checkin || !checkout) return;

    const d1 = new Date(checkin);
    const d2 = new Date(checkout);
    const nights = Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24));

    if (nights <= 0) return;

    // Update nights display
    const nightsEl = document.querySelector('[data-binding="nights"]');
    if (nightsEl) nightsEl.textContent = nights;

    // Update total price
    const pricePerNight = parseFloat(
      document.querySelector('[data-price-per-night]')?.dataset.pricePerNight || 0
    );
    const totalEl = document.querySelector('[data-binding="total"]');
    if (totalEl && pricePerNight) {
      totalEl.textContent = formatCurrency(pricePerNight * nights);
    }
  }

  // ── Booking Form Validation ──────────────────
  const bookingForm = document.getElementById('booking-form');
  if (bookingForm) {
    bookingForm.addEventListener('submit', (e) => {
      e.preventDefault();
      let valid = true;

      // Validate required fields
      bookingForm.querySelectorAll('[required]').forEach(field => {
        if (!field.value.trim()) {
          showFieldError(field.id, 'Trường này là bắt buộc');
          valid = false;
        } else {
          clearFieldError(field.id);
        }
      });

      // Validate phone
      const phone = document.getElementById('customer-phone');
      if (phone && phone.value) {
        const phoneRegex = /^(0|\+84)[0-9]{8,10}$/;
        if (!phoneRegex.test(phone.value)) {
          showFieldError('customer-phone', 'Số điện thoại không hợp lệ');
          valid = false;
        }
      }

      if (valid) {
        // Simulate booking submission
        const submitBtn = bookingForm.querySelector('[type="submit"]');
        if (submitBtn) {
          submitBtn.textContent = 'Đang xử lý...';
          submitBtn.disabled = true;
        }

        setTimeout(() => {
          if (typeof showToast === 'function') {
            showToast('Đặt phòng thành công! Chúng tôi sẽ xác nhận qua email.', 'success');
          }
          if (submitBtn) {
            submitBtn.textContent = 'Xác nhận đặt phòng';
            submitBtn.disabled = false;
          }
        }, 1500);
      }
    });
  }

  // ── Hotel Filters (Sidebar) ──────────────────
  // Price range inputs sync
  const priceMin = document.getElementById('price-min');
  const priceMax = document.getElementById('price-max');

  if (priceMin && priceMax) {
    [priceMin, priceMax].forEach(input => {
      input.addEventListener('input', applyFilters);
    });
  }

  document.querySelectorAll('.filter-checkbox').forEach(cb => {
    cb.addEventListener('change', applyFilters);
  });

  function applyFilters() {
    // In static UI, just show toast for demo
    if (typeof showToast === 'function') {
      showToast('Bộ lọc đã được áp dụng', 'success');
    }
  }

  // ── Mobile Filter Sidebar Toggle ─────────────
  const filterToggleBtn = document.querySelector('.filter-toggle-btn');
  const filterSidebar   = document.querySelector('.filter-sidebar');

  if (filterToggleBtn && filterSidebar) {
    filterToggleBtn.addEventListener('click', () => {
      filterSidebar.classList.toggle('open');
    });

    // Add close button to sidebar on mobile
    const closeBtn = document.createElement('button');
    closeBtn.className = 'btn btn--ghost w-full mt-4';
    closeBtn.textContent = 'Đóng bộ lọc';
    closeBtn.addEventListener('click', () => filterSidebar.classList.remove('open'));
    filterSidebar.appendChild(closeBtn);
  }

  // ── Place Category Filter ────────────────────
  document.querySelectorAll('.place-category-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.place-category-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });

  // ── Star Rating Hover Effect ─────────────────
  document.querySelectorAll('.star-rating-input').forEach(ratingEl => {
    const stars = ratingEl.querySelectorAll('.star-btn');
    stars.forEach((star, i) => {
      star.addEventListener('mouseenter', () => {
        stars.forEach((s, j) => {
          s.style.color = j <= i ? 'var(--color-accent)' : 'var(--color-border)';
        });
      });

      star.addEventListener('mouseleave', () => {
        const selected = parseInt(ratingEl.dataset.rating || 0);
        stars.forEach((s, j) => {
          s.style.color = j < selected ? 'var(--color-accent)' : 'var(--color-border)';
        });
      });

      star.addEventListener('click', () => {
        ratingEl.dataset.rating = i + 1;
        const input = ratingEl.querySelector('input[type="hidden"]');
        if (input) input.value = i + 1;
      });
    });
  });

  // ── Helpers ──────────────────────────────────
  function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    if (!field) return;
    field.classList.add('form-control--error');

    let errMsg = field.parentElement.querySelector('.form-error-msg');
    if (!errMsg) {
      errMsg = document.createElement('p');
      errMsg.className = 'form-error-msg';
      field.after(errMsg);
    }
    errMsg.textContent = message;
  }

  function clearFieldError(fieldId) {
    const field = document.getElementById(fieldId);
    if (!field) return;
    field.classList.remove('form-control--error');
    field.parentElement.querySelector('.form-error-msg')?.remove();
  }

  function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      minimumFractionDigits: 0,
    }).format(amount);
  }

});

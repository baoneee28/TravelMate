/**
 * admin-revenue.js
 * Trang quản lý doanh thu admin:
 *  – Hiển thị bảng chiết khấu (Hotel 15%, Villa 12%, Homestay 10%, Resort 18%)
 *  – Thống kê doanh thu tuần (thu từ user, chiết khấu admin, hoàn trả partner)
 *  – Bảng chiết khấu đã nộp của tất cả partner
 *  – Lịch sử hoàn tiền Thứ Ba hàng tuần
 *
 * Chính sách:
 *  • Admin giữ 100% thanh toán từ user
 *  • Voucher admin cấp cho user → admin chịu chi phí
 *  • Voucher partner tự thêm → partner chịu chi phí
 *  • Hoàn tiền cho partner vào Thứ Ba hàng tuần (đã trừ chiết khấu + voucher partner)
 */
document.addEventListener('DOMContentLoaded', () => {

  /* ── Constants ──────────────────────────────────────────────────────────── */
  const DISCOUNT_RATES = { hotel: 0.15, villa: 0.12, homestay: 0.10, resort: 0.18 };

  const TYPE_LABELS = { hotel: 'Hotel', villa: 'Villa', homestay: 'Homestay', resort: 'Resort' };

  const DAY_SHORT = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

  /* ── Mock Partners ──────────────────────────────────────────────────────── */
  const PARTNERS = [
    { id: 1, name: 'Sunrise Sapa Lodge',       type: 'hotel',    city: 'Sapa' },
    { id: 2, name: 'Villa Đà Lạt Garden',       type: 'villa',    city: 'Đà Lạt' },
    { id: 3, name: 'Hội An Ancient Homestay',   type: 'homestay', city: 'Hội An' },
    { id: 4, name: 'Phú Quốc Ocean Resort',     type: 'resort',   city: 'Phú Quốc' },
    { id: 5, name: 'Hanoi Heritage Hotel',       type: 'hotel',    city: 'Hà Nội' },
    { id: 6, name: 'Nha Trang Beach Villa',      type: 'villa',    city: 'Nha Trang' },
    { id: 7, name: 'Ba Vì Mountain Homestay',    type: 'homestay', city: 'Ba Vì' },
    { id: 8, name: 'Mũi Né Sand Dunes Resort',   type: 'resort',   city: 'Mũi Né' },
    { id: 9, name: 'Hạ Long Bay Boutique Hotel', type: 'hotel',    city: 'Hạ Long' },
    { id: 10,name: 'Côn Đảo Luxury Resort',      type: 'resort',   city: 'Côn Đảo' },
  ];

  /* ── Generate Weekly Data ────────────────────────────────────────────────── */
  function generateWeeksData() {
    const weeks = [];
    const today = new Date();
    const dayOfWeek = today.getDay(); // 0=Sun
    const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const currentMonday = new Date(today);
    currentMonday.setDate(today.getDate() + diffToMonday);
    currentMonday.setHours(0, 0, 0, 0);

    for (let w = -3; w <= 2; w++) {
      const monday = new Date(currentMonday);
      monday.setDate(currentMonday.getDate() + w * 7);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);

      // Per-partner weekly data
      const partners = PARTNERS.map(p => {
        const rate = DISCOUNT_RATES[p.type];
        const days = [];
        let weekGross = 0, weekCommission = 0, weekVoucher = 0;

        for (let d = 0; d < 7; d++) {
          const date = new Date(monday);
          date.setDate(monday.getDate() + d);
          const isPast = date <= today && w <= 0;

          let gross = 0, bookings = 0, adminVoucher = 0, partnerVoucher = 0;
          if (isPast) {
            const hasBooking = Math.random() > 0.3;
            if (hasBooking) {
              bookings = Math.floor(Math.random() * 3) + 1;
              const basePrice = {
                hotel: 1000000, villa: 3000000, homestay: 800000, resort: 2500000
              }[p.type];
              gross = bookings * basePrice * (0.8 + Math.random() * 0.6);
              gross = Math.round(gross / 10000) * 10000;

              // Admin voucher (random chance): admin absorbs this cost
              adminVoucher = Math.random() > 0.7
                ? Math.round(gross * (Math.random() * 0.05) / 1000) * 1000 : 0;

              // Partner voucher (random chance): partner absorbs this cost
              partnerVoucher = Math.random() > 0.75
                ? Math.round(gross * (Math.random() * 0.04) / 1000) * 1000 : 0;
            }
          }

          const commission = Math.round(gross * rate);
          weekGross      += gross;
          weekCommission += commission;
          weekVoucher    += partnerVoucher;

          days.push({ date: new Date(date), gross, bookings, commission, adminVoucher, partnerVoucher });
        }

        // Net payout = gross - commission - partnerVoucher
        const weekPayout = weekGross - weekCommission - weekVoucher;

        return {
          ...p,
          days,
          weekGross,
          weekCommission,
          weekVoucher,
          weekPayout: Math.max(0, weekPayout),
          rate,
          payoutStatus: w < 0 ? 'paid' : w === 0 ? 'pending' : 'future',
        };
      });

      // Aggregate daily totals across all partners
      const dailyTotals = Array.from({ length: 7 }, (_, d) => ({
        gross:      partners.reduce((s, p) => s + p.days[d].gross, 0),
        commission: partners.reduce((s, p) => s + p.days[d].commission, 0),
        payout:     partners.reduce((s, p) => s + Math.max(0, p.days[d].gross - p.days[d].commission - p.days[d].partnerVoucher), 0),
        bookings:   partners.reduce((s, p) => s + p.days[d].bookings, 0),
      }));

      weeks.push({ monday: new Date(monday), sunday: new Date(sunday), partners, dailyTotals });
    }

    return weeks;
  }

  const WEEKS_DATA = generateWeeksData();
  let currentWeekIndex = 3;

  /* ── DOM References ──────────────────────────────────────────────────────── */
  const weekSelectorEl     = document.getElementById('adrev-week-selector');
  const totalCollectedEl   = document.getElementById('adrev-total-collected');
  const totalCommissionEl  = document.getElementById('adrev-total-commission');
  const totalPayoutEl      = document.getElementById('adrev-total-payout');
  const totalBookingsEl    = document.getElementById('adrev-total-bookings');
  const chartEl            = document.getElementById('adrev-chart');
  const partnerTbodyEl     = document.getElementById('adrev-partner-tbody');
  const partnerSearchEl    = document.getElementById('adrev-partner-search');
  const typeFilterEl       = document.getElementById('adrev-type-filter');
  const sumGrossEl         = document.getElementById('adrev-sum-gross');
  const sumCommissionEl    = document.getElementById('adrev-sum-commission');
  const sumVoucherEl       = document.getElementById('adrev-sum-voucher');
  const sumPayoutEl        = document.getElementById('adrev-sum-payout');
  const payoutListEl       = document.getElementById('adrev-payout-list');
  const nextPayoutEl       = document.getElementById('adrev-next-payout');
  const dateEl             = document.getElementById('adrev-date');

  /* ── Helpers ─────────────────────────────────────────────────────────────── */
  function fmt(n) {
    if (n >= 1e9) return (n / 1e9).toFixed(1).replace('.0','') + ' tỷ';
    if (n >= 1e6) return (n / 1e6).toFixed(1).replace('.0','') + 'M';
    if (n >= 1e3) return (n / 1e3).toFixed(0) + 'K';
    return n.toLocaleString('vi-VN');
  }

  function fmtFull(n) { return n.toLocaleString('vi-VN') + 'đ'; }

  function fmtDate(d) {
    return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
  }

  function fmtWeekLabel(mon, sun) {
    const d1 = `${String(mon.getDate()).padStart(2,'0')}/${String(mon.getMonth()+1).padStart(2,'0')}`;
    const d2 = `${String(sun.getDate()).padStart(2,'0')}/${String(sun.getMonth()+1).padStart(2,'0')}`;
    return `${d1} – ${d2}`;
  }

  /* ── Init Date ───────────────────────────────────────────────────────────── */
  function initDate() {
    const today = new Date();
    if (dateEl) dateEl.textContent = '📅 ' + fmtDate(today);

    // Find next Tuesday
    const dayOfWeek = today.getDay(); // 0=Sun,2=Tue
    const daysToTue = dayOfWeek <= 2 ? 2 - dayOfWeek : 9 - dayOfWeek;
    const nextTue = new Date(today);
    nextTue.setDate(today.getDate() + (daysToTue === 0 ? 7 : daysToTue));
    if (nextPayoutEl) nextPayoutEl.textContent = 'Thứ Ba, ' + fmtDate(nextTue);
  }

  /* ── Week Selector ─────────────────────────────────────────────────────── */
  function renderWeekSelector() {
    if (!weekSelectorEl) return;
    weekSelectorEl.innerHTML = '';
    WEEKS_DATA.forEach((week, i) => {
      const btn = document.createElement('button');
      btn.className = `adrev-week-btn${i === currentWeekIndex ? ' active' : ''}`;
      const label = fmtWeekLabel(week.monday, week.sunday);
      btn.textContent = i === 3 ? `Tuần này (${label})` : label;
      btn.addEventListener('click', () => {
        currentWeekIndex = i;
        renderWeekSelector();
        renderKPIs();
        renderChart();
        renderPartnerTable();
      });
      weekSelectorEl.appendChild(btn);
    });

    const activeBtn = weekSelectorEl.querySelector('.active');
    if (activeBtn) activeBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
  }

  /* ── KPI Cards ───────────────────────────────────────────────────────────── */
  function renderKPIs() {
    const week = WEEKS_DATA[currentWeekIndex];
    const totalGross      = week.partners.reduce((s, p) => s + p.weekGross, 0);
    const totalCommission = week.partners.reduce((s, p) => s + p.weekCommission, 0);
    const totalPayout     = week.partners.reduce((s, p) => s + p.weekPayout, 0);
    const totalBookings   = week.partners.reduce((s, p) => p.days.reduce((s2, d) => s2 + d.bookings, 0) + s, 0);

    if (totalCollectedEl)  totalCollectedEl.textContent  = fmtFull(totalGross);
    if (totalCommissionEl) totalCommissionEl.textContent = fmtFull(totalCommission);
    if (totalPayoutEl)     totalPayoutEl.textContent     = fmtFull(totalPayout);
    if (totalBookingsEl)   totalBookingsEl.textContent   = totalBookings + ' booking';
  }

  /* ── Chart ────────────────────────────────────────────────────────────────── */
  function renderChart() {
    if (!chartEl) return;
    const week = WEEKS_DATA[currentWeekIndex];
    const maxGross = Math.max(...week.dailyTotals.map(d => d.gross), 1);

    chartEl.innerHTML = '';

    week.dailyTotals.forEach((day, i) => {
      const pctGross      = (day.gross / maxGross) * 100;
      const pctCommission = (day.commission / maxGross) * 100;
      const pctPayout     = (day.payout / maxGross) * 100;

      const group = document.createElement('div');
      group.className = 'adrev-bar-group';

      const amountLabel = day.gross > 0
        ? `<div class="adrev-bar-amount">${fmt(day.gross)}</div>` : '';

      group.innerHTML = `
        <div class="adrev-bar-cols">
          <div class="adrev-bar-track">
            <div class="adrev-bar-fill adrev-bar-fill--collected" style="height:${pctGross}%"></div>
          </div>
          <div class="adrev-bar-track">
            <div class="adrev-bar-fill adrev-bar-fill--commission" style="height:${pctCommission}%"></div>
          </div>
          <div class="adrev-bar-track">
            <div class="adrev-bar-fill adrev-bar-fill--payout" style="height:${pctPayout}%"></div>
          </div>
        </div>
        ${amountLabel}
        <span class="adrev-bar-day">${DAY_SHORT[i]}</span>
      `;

      chartEl.appendChild(group);
    });
  }

  /* ── Partner Table ──────────────────────────────────────────────────────── */
  function renderPartnerTable() {
    if (!partnerTbodyEl) return;

    const week        = WEEKS_DATA[currentWeekIndex];
    const searchVal   = (partnerSearchEl?.value || '').toLowerCase();
    const typeFilter  = typeFilterEl?.value || '';

    let filtered = week.partners.filter(p => {
      const matchName = p.name.toLowerCase().includes(searchVal);
      const matchType = !typeFilter || p.type === typeFilter;
      return matchName && matchType;
    });

    // Sort by weekCommission desc
    filtered.sort((a, b) => b.weekCommission - a.weekCommission);

    partnerTbodyEl.innerHTML = '';

    if (!filtered.length) {
      partnerTbodyEl.innerHTML = `
        <tr><td colspan="9" class="adrev-empty">
          <i class="fa-solid fa-search"></i> Không tìm thấy partner nào.
        </td></tr>`;
      updateTotals([]);
      return;
    }

    filtered.forEach((p, idx) => {
      const typeClass = `adrev-type-badge--${p.type}`;
      const statusClass = {
        paid: 'adrev-status-badge--paid',
        pending: 'adrev-status-badge--pending',
        future: 'adrev-status-badge--processing',
      }[p.payoutStatus];
      const statusLabel = {
        paid: '<i class="fa-solid fa-check"></i> Đã hoàn',
        pending: '<i class="fa-solid fa-clock"></i> Chờ T3',
        future: '<i class="fa-solid fa-hourglass"></i> Chưa đến',
      }[p.payoutStatus];

      const tr = document.createElement('tr');
      tr.setAttribute('data-partner-id', p.id);
      tr.innerHTML = `
        <td style="color:var(--adrev-text-muted);font-size:12px;">${idx + 1}</td>
        <td>
          <div style="font-weight:600;color:var(--adrev-text);">${p.name}</div>
          <div style="font-size:11px;color:var(--adrev-text-muted);">📍 ${p.city}</div>
        </td>
        <td><span class="adrev-type-badge ${typeClass}">${TYPE_LABELS[p.type]}</span></td>
        <td><span class="adrev-rate-pill">${(p.rate * 100).toFixed(0)}%</span></td>
        <td style="font-weight:600;">${fmtFull(p.weekGross)}</td>
        <td style="font-weight:700;color:var(--adrev-purple);">${fmtFull(p.weekCommission)}</td>
        <td style="color:var(--adrev-rose);">${fmtFull(p.weekVoucher)}</td>
        <td style="font-weight:700;color:var(--adrev-success);">${fmtFull(p.weekPayout)}</td>
        <td><span class="adrev-status-badge ${statusClass}">${statusLabel}</span></td>
      `;
      partnerTbodyEl.appendChild(tr);
    });

    updateTotals(filtered);
  }

  function updateTotals(partners) {
    const sumGross      = partners.reduce((s, p) => s + p.weekGross, 0);
    const sumCommission = partners.reduce((s, p) => s + p.weekCommission, 0);
    const sumVoucher    = partners.reduce((s, p) => s + p.weekVoucher, 0);
    const sumPayout     = partners.reduce((s, p) => s + p.weekPayout, 0);

    if (sumGrossEl)      sumGrossEl.textContent      = fmtFull(sumGross);
    if (sumCommissionEl) sumCommissionEl.textContent = fmtFull(sumCommission);
    if (sumVoucherEl)    sumVoucherEl.textContent    = fmtFull(sumVoucher);
    if (sumPayoutEl)     sumPayoutEl.textContent     = fmtFull(sumPayout);
  }

  /* ── Payout History List ─────────────────────────────────────────────────── */
  function renderPayoutHistory() {
    if (!payoutListEl) return;

    const pastWeeks = WEEKS_DATA.filter((_, i) => i < 3); // weeks 0,1,2 are past
    const today = new Date();

    payoutListEl.innerHTML = '';

    // Build payout records: each past week → Thứ Ba of the NEXT week
    const payoutRecords = pastWeeks.map((week, i) => {
      const payout = new Date(week.sunday);
      // Next Tuesday after sunday
      const diff = (2 - payout.getDay() + 7) % 7 || 7;
      payout.setDate(payout.getDate() + diff);

      const totalPayout     = week.partners.reduce((s, p) => s + p.weekPayout, 0);
      const totalCommission = week.partners.reduce((s, p) => s + p.weekCommission, 0);
      const isPast = payout < today;

      return {
        date: payout,
        totalPayout,
        totalCommission,
        partnerCount: week.partners.filter(p => p.weekGross > 0).length,
        isPast,
        weekLabel: fmtWeekLabel(week.monday, week.sunday),
      };
    }).reverse(); // most recent first

    // Also add the upcoming payout (current week → next Tuesday)
    const currentWeek = WEEKS_DATA[3];
    const nextTuePayout = new Date(currentWeek.sunday);
    const diff2 = (2 - nextTuePayout.getDay() + 7) % 7 || 7;
    nextTuePayout.setDate(nextTuePayout.getDate() + diff2);
    payoutRecords.unshift({
      date: nextTuePayout,
      totalPayout: currentWeek.partners.reduce((s, p) => s + p.weekPayout, 0),
      totalCommission: currentWeek.partners.reduce((s, p) => s + p.weekCommission, 0),
      partnerCount: currentWeek.partners.filter(p => p.weekGross > 0).length,
      isPast: false,
      upcoming: true,
      weekLabel: fmtWeekLabel(currentWeek.monday, currentWeek.sunday),
    });

    payoutRecords.forEach(record => {
      const badgeClass = record.upcoming
        ? 'adrev-payout-item__date-badge--amber'
        : record.isPast
          ? 'adrev-payout-item__date-badge--success'
          : '';

      const item = document.createElement('div');
      item.className = 'adrev-payout-item';
      item.innerHTML = `
        <div class="adrev-payout-item__date-badge ${badgeClass}">
          <span class="adrev-payout-item__day">${String(record.date.getDate()).padStart(2,'0')}</span>
          <span class="adrev-payout-item__month">T${record.date.getMonth()+1}</span>
        </div>
        <div class="adrev-payout-item__info">
          <div class="adrev-payout-item__title">
            ${record.upcoming ? '🔔 ' : record.isPast ? '✅ ' : '⏳ '}
            ${record.upcoming ? 'Hoàn tiền sắp tới' : record.isPast ? 'Đã hoàn thành' : 'Đang xử lý'}
            – Thứ Ba, ${fmtDate(record.date)}
          </div>
          <div class="adrev-payout-item__partners">
            Tuần ${record.weekLabel} · ${record.partnerCount} partner nhận tiền
            · Chiết khấu admin thu: <strong>${fmtFull(record.totalCommission)}</strong>
          </div>
        </div>
        <div class="adrev-payout-item__amount">
          <div class="adrev-payout-item__amount-value">${fmtFull(record.totalPayout)}</div>
          <div class="adrev-payout-item__amount-label">Hoàn cho partner</div>
        </div>
      `;
      payoutListEl.appendChild(item);
    });
  }

  /* ── Filters ─────────────────────────────────────────────────────────────── */
  if (partnerSearchEl) {
    partnerSearchEl.addEventListener('input', renderPartnerTable);
  }
  if (typeFilterEl) {
    typeFilterEl.addEventListener('change', renderPartnerTable);
  }

  /* ── Init ────────────────────────────────────────────────────────────────── */
  initDate();
  renderWeekSelector();
  renderKPIs();
  renderChart();
  renderPartnerTable();
  renderPayoutHistory();
});

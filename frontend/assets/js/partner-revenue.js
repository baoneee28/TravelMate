/**
 * partner-revenue.js
 * Logic doanh thu theo tuần cho Partner: biểu đồ cột, overlay chi tiết ngày.
 */
document.addEventListener('DOMContentLoaded', () => {

  // ── Mock Revenue Data ─────────────────────────────────────────────────────
  // Each week has 7 days (Mon–Sun), each day has: amount, rooms, hours
  const WEEKS_DATA = generateWeeksData();

  function generateWeeksData() {
    const weeks = [];
    const today = new Date();
    // Find Monday of the current week
    const dayOfWeek = today.getDay(); // 0=Sun
    const diffToMonday = (dayOfWeek === 0 ? -6 : 1 - dayOfWeek);
    const currentMonday = new Date(today);
    currentMonday.setDate(today.getDate() + diffToMonday);
    currentMonday.setHours(0, 0, 0, 0);

    // Generate 6 weeks (3 past, current, 2 future with zeros)
    for (let w = -3; w <= 2; w++) {
      const monday = new Date(currentMonday);
      monday.setDate(currentMonday.getDate() + w * 7);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);

      const days = [];
      for (let d = 0; d < 7; d++) {
        const date = new Date(monday);
        date.setDate(monday.getDate() + d);
        const isPast = date <= today;
        const isFuture = w > 0;

        if (isFuture || !isPast) {
          days.push({ date: new Date(date), amount: 0, rooms: 0, hours: 0 });
        } else {
          // Random realistic revenue data
          const hasBooking = Math.random() > 0.25;
          const rooms = hasBooking ? Math.floor(Math.random() * 4) + 1 : 0;
          const hours = rooms * (Math.floor(Math.random() * 20) + 4);
          const pricePerHour = Math.floor(Math.random() * 80000) + 40000;
          const amount = rooms > 0 ? hours * pricePerHour : 0;

          days.push({
            date: new Date(date),
            amount: Math.round(amount / 1000) * 1000,
            rooms,
            hours,
          });
        }
      }

      weeks.push({
        monday: new Date(monday),
        sunday: new Date(sunday),
        days,
      });
    }

    return weeks;
  }

  const DAY_NAMES = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
  const DAY_SHORT = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

  let currentWeekIndex = 3; // index of current week in WEEKS_DATA
  let selectedDayIndex = null;

  // ── DOM Elements ──────────────────────────────────────────────────────────
  const weekSelector    = document.getElementById('rv-week-selector');
  const totalAmountEl   = document.getElementById('rv-total-amount');
  const totalRoomsEl    = document.getElementById('rv-total-rooms');
  const totalHoursEl    = document.getElementById('rv-total-hours');
  const chartEl         = document.getElementById('rv-chart');
  const summaryView     = document.getElementById('rv-summary-view');
  const dayDetail       = document.getElementById('rv-day-detail');
  const dayTitleEl      = document.getElementById('rv-day-title');
  const dayAmountEl     = document.getElementById('rv-day-amount');
  const dayRoomsEl      = document.getElementById('rv-day-rooms');
  const dayHoursEl      = document.getElementById('rv-day-hours');
  const dayCloseBtn     = document.getElementById('rv-day-close');
  const miniChartEl     = document.getElementById('rv-mini-chart');

  // ── Helpers ───────────────────────────────────────────────────────────────
  function formatCurrency(n) {
    if (n >= 1000000) return (n / 1000000).toFixed(1).replace('.0', '') + 'M đ';
    if (n >= 1000) return (n / 1000).toFixed(0) + 'K đ';
    return n.toLocaleString('vi-VN') + 'đ';
  }

  function formatCurrencyFull(n) {
    return n.toLocaleString('vi-VN') + 'đ';
  }

  function formatDate(d) {
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }

  function formatWeekLabel(mon, sun) {
    const d1 = `${String(mon.getDate()).padStart(2, '0')}/${String(mon.getMonth() + 1).padStart(2, '0')}`;
    const d2 = `${String(sun.getDate()).padStart(2, '0')}/${String(sun.getMonth() + 1).padStart(2, '0')}`;
    return `${d1} – ${d2}`;
  }

  // ── Render Week Selector ──────────────────────────────────────────────────
  function renderWeekSelector() {
    weekSelector.innerHTML = '';
    WEEKS_DATA.forEach((week, i) => {
      const btn = document.createElement('button');
      btn.className = `rv-week-btn${i === currentWeekIndex ? ' active' : ''}`;
      
      // Highlight tuần hiện tại (index 3)
      const dateLabel = formatWeekLabel(week.monday, week.sunday);
      btn.textContent = i === 3 ? `Tuần này (${dateLabel})` : dateLabel;

      btn.addEventListener('click', () => {
        currentWeekIndex = i;
        selectedDayIndex = null;
        closeDayDetail();
        renderWeekSelector();
        renderSummary();
        renderChart();
      });
      weekSelector.appendChild(btn);
    });

    // Scroll active button into view
    const activeBtn = weekSelector.querySelector('.active');
    if (activeBtn) {
      activeBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  }

  // ── Render Summary ────────────────────────────────────────────────────────
  function renderSummary() {
    const week = WEEKS_DATA[currentWeekIndex];
    const totalAmount = week.days.reduce((s, d) => s + d.amount, 0);
    const totalRooms  = week.days.reduce((s, d) => s + d.rooms, 0);
    const totalHours  = week.days.reduce((s, d) => s + d.hours, 0);

    totalAmountEl.textContent = formatCurrencyFull(totalAmount);
    totalRoomsEl.textContent  = totalRooms + ' phòng';
    totalHoursEl.textContent  = totalHours + ' giờ';
  }

  // ── Render Chart ──────────────────────────────────────────────────────────
  function renderChart() {
    const week = WEEKS_DATA[currentWeekIndex];
    const maxAmount = Math.max(...week.days.map(d => d.amount), 1);
    const peakIndex = week.days.findIndex(d => d.amount === maxAmount && d.amount > 0);

    chartEl.innerHTML = '';

    // Add peak line
    if (maxAmount > 0) {
      const peakLine = document.createElement('div');
      peakLine.className = 'rv-chart-peak-line';
      peakLine.style.top = '0px'; // top of chart = max height bar
      peakLine.innerHTML = `<span class="rv-chart-peak-label">Cao nhất: ${formatCurrency(maxAmount)}</span>`;
      chartEl.appendChild(peakLine);
    }

    week.days.forEach((day, i) => {
      const pct = maxAmount > 0 ? (day.amount / maxAmount) * 100 : 0;
      const isPeak = i === peakIndex && day.amount > 0;

      const col = document.createElement('div');
      col.className = `rv-bar-col${selectedDayIndex === i ? ' is-selected' : ''}`;
      col.setAttribute('data-day-index', i);

      col.innerHTML = `
        <div class="rv-bar-track">
          <div class="rv-bar-fill ${isPeak ? 'is-peak' : ''}" style="height: ${pct}%;">
            <span class="rv-bar-amount">${day.amount > 0 ? formatCurrency(day.amount) : ''}</span>
          </div>
        </div>
        <span class="rv-bar-day">${DAY_SHORT[i]}</span>
      `;

      col.addEventListener('click', () => openDayDetail(i));
      chartEl.appendChild(col);
    });
  }

  // ── Day Detail ────────────────────────────────────────────────────────────
  function openDayDetail(dayIndex) {
    selectedDayIndex = dayIndex;
    const week = WEEKS_DATA[currentWeekIndex];
    const day = week.days[dayIndex];

    dayTitleEl.innerHTML = `<span>${DAY_NAMES[dayIndex]}</span> – ${formatDate(day.date)}`;
    dayAmountEl.textContent = formatCurrencyFull(day.amount);
    dayRoomsEl.textContent  = day.rooms + ' phòng';
    dayHoursEl.textContent  = day.hours + ' giờ';

    // Render mini chart with highlight
    renderMiniChart(dayIndex);

    // Highlight selected bar in main chart
    highlightBar(dayIndex);

    // Show overlay and hide summary to prevent cut-offs
    summaryView.classList.add('hidden');
    dayDetail.classList.add('open');
  }

  function closeDayDetail() {
    selectedDayIndex = null;
    dayDetail.classList.remove('open');
    summaryView.classList.remove('hidden');
    // Remove all highlights
    chartEl.querySelectorAll('.rv-bar-fill').forEach(b => {
      b.classList.remove('is-highlighted', 'is-dimmed');
    });
    chartEl.querySelectorAll('.rv-bar-col').forEach(c => c.classList.remove('is-selected'));
  }

  function highlightBar(dayIndex) {
    chartEl.querySelectorAll('.rv-bar-col').forEach((col, i) => {
      const fill = col.querySelector('.rv-bar-fill');
      col.classList.toggle('is-selected', i === dayIndex);
      if (i === dayIndex) {
        fill.classList.add('is-highlighted');
        fill.classList.remove('is-dimmed');
      } else {
        fill.classList.add('is-dimmed');
        fill.classList.remove('is-highlighted');
      }
    });
  }

  function renderMiniChart(highlightIndex) {
    const week = WEEKS_DATA[currentWeekIndex];
    const maxAmount = Math.max(...week.days.map(d => d.amount), 1);

    miniChartEl.innerHTML = '';
    week.days.forEach((day, i) => {
      const pct = maxAmount > 0 ? (day.amount / maxAmount) * 100 : 0;
      const isCurrent = i === highlightIndex;

      const col = document.createElement('div');
      col.className = 'rv-mini-bar-col';
      col.innerHTML = `
        <div class="rv-mini-bar-track">
          <div class="rv-mini-bar-fill${isCurrent ? ' is-current' : ''}" style="height: ${pct}%;"></div>
        </div>
        <span class="rv-mini-bar-day${isCurrent ? ' is-current' : ''}">${DAY_SHORT[i]}</span>
      `;
      miniChartEl.appendChild(col);
    });
  }

  // ── Events ────────────────────────────────────────────────────────────────
  dayCloseBtn.addEventListener('click', closeDayDetail);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && dayDetail.classList.contains('open')) closeDayDetail();
  });

  // ── Init ──────────────────────────────────────────────────────────────────
  renderWeekSelector();
  renderSummary();
  renderChart();
});

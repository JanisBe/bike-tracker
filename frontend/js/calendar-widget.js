/**
 * Calendar Widget for displaying cycling activities per day.
 * Displays a monthly calendar grid with activity dots, popover on hover/click with rides list,
 * and allows navigating to ride details or filtering.
 */
import {formatDuration} from './stats.js';

export class CalendarWidget {
  /**
   * @param {HTMLElement} containerEl 
   * @param {Object} options
   * @param {Function} options.onSelectRide - Callback when a ride is clicked: (rideId) => void
   * @param {Function} options.onDateFilter - Optional callback when day is clicked
   */
  constructor(containerEl, { onSelectRide, onDateFilter } = {}) {
    this.container = containerEl;
    this.onSelectRide = onSelectRide;
    this.onDateFilter = onDateFilter;

    this.currentDate = new Date(); // Month currently displayed
    this.rides = [];
    this.ridesByDate = new Map(); // key: 'YYYY-MM-DD', val: Array of rides

    this.popoverEl = null;
    this.activeHoverDate = null;
    this.popoverTimeout = null;

    this.init();
  }

  init() {
    this.container.classList.add("calendar-widget");
    this.renderSkeleton();
    this.createPopover();
    this.setupGlobalEvents();
  }

  createPopover() {
    this.popoverEl = document.createElement("div");
    this.popoverEl.className = "calendar-popover";
    this.popoverEl.setAttribute("role", "tooltip");
    document.body.appendChild(this.popoverEl);

    // Keep popover alive when mouse moves over it
    this.popoverEl.addEventListener("mouseenter", () => {
      if (this.popoverTimeout) clearTimeout(this.popoverTimeout);
    });
    this.popoverEl.addEventListener("mouseleave", () => {
      this.popoverTimeout = setTimeout(() => {
        this.hidePopover();
      }, 200);
    });
  }

  setupGlobalEvents() {
    // Hide popover on outside click or escape
    document.addEventListener("click", (e) => {
      if (!e.target.closest(".calendar-widget") && !e.target.closest(".calendar-popover")) {
        this.hidePopover();
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        this.hidePopover();
      }
    });

    window.addEventListener("resize", () => this.hidePopover());
    window.addEventListener("scroll", (e) => {
      // Don't hide if the scroll event occurred inside the popover itself
      if (this.popoverEl && (this.popoverEl === e.target || this.popoverEl.contains(e.target))) {
        return;
      }
      this.hidePopover();
    }, true);
  }

  renderSkeleton() {
    this.container.innerHTML = `
      <div class="calendar-header">
        <div class="calendar-title-group">
          <span class="calendar-icon">📅</span>
          <span class="calendar-month-label" id="cal-month-label">Miesiąc Rok</span>
        </div>
        <div class="calendar-nav-buttons">
          <button class="cal-nav-btn" id="cal-prev-month" title="Poprzedni miesiąc" aria-label="Poprzedni miesiąc">‹</button>
          <button class="cal-today-btn" id="cal-today-btn" title="Dziś">Dziś</button>
          <button class="cal-nav-btn" id="cal-next-month" title="Następny miesiąc" aria-label="Następny miesiąc">›</button>
        </div>
      </div>
      <div class="calendar-weekdays">
        <span>Pn</span><span>Wt</span><span>Śr</span><span>Cz</span><span>Pt</span><span>So</span><span>Nd</span>
      </div>
      <div class="calendar-grid" id="cal-days-grid"></div>
    `;

    this.monthLabelEl = this.container.querySelector("#cal-month-label");
    this.gridEl = this.container.querySelector("#cal-days-grid");
    this.prevBtn = this.container.querySelector("#cal-prev-month");
    this.todayBtn = this.container.querySelector("#cal-today-btn");
    this.nextBtn = this.container.querySelector("#cal-next-month");

    this.prevBtn.addEventListener("click", () => this.changeMonth(-1));
    this.nextBtn.addEventListener("click", () => this.changeMonth(1));
    this.todayBtn.addEventListener("click", () => {
      this.currentDate = new Date();
      this.render();
    });
  }

  changeMonth(delta) {
    this.hidePopover();
    this.currentDate.setMonth(this.currentDate.getMonth() + delta);
    this.render();
  }

  /**
   * Updates rides dataset and updates calendar display.
   * @param {Array} rides
   */
  setRides(rides) {
    this.rides = rides || [];
    this.indexRidesByDate();
    
    // Jump to the latest ride's month if available
    if (this.rides.length > 0) {
      const latestRide = this.rides[0];
      if (latestRide.startTime instanceof Date && !Number.isNaN(latestRide.startTime)) {
        this.currentDate = new Date(latestRide.startTime);
      }
    }

    this.render();
  }

  indexRidesByDate() {
    this.ridesByDate.clear();
    this.rides.forEach(ride => {
      const d = ride.startTime instanceof Date ? ride.startTime : new Date(ride.startTime);
      if (Number.isNaN(d)) return;
      const key = this.getDateKey(d);
      if (!this.ridesByDate.has(key)) {
        this.ridesByDate.set(key, []);
      }
      this.ridesByDate.get(key).push(ride);
    });
  }

  getDateKey(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  render() {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();

    // Month Label
    let monthName = new Intl.DateTimeFormat('pl-PL', {month: 'long', year: 'numeric'}).format(this.currentDate);
    monthName = monthName.charAt(0).toUpperCase() + monthName.slice(1);
    this.monthLabelEl.textContent = monthName;

    this.gridEl.innerHTML = "";

    // Days in current month
    const firstDayIndex = (new Date(year, month, 1).getDay() + 6) % 7; // Monday = 0
    const totalDays = new Date(year, month + 1, 0).getDate();
    const prevMonthDays = new Date(year, month, 0).getDate();

    const todayKey = this.getDateKey(new Date());

    // 1. Prev month filler days
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const dayNum = prevMonthDays - i;
      const cell = document.createElement("div");
      cell.className = "cal-day cal-day-outside";
      cell.textContent = dayNum;
      this.gridEl.appendChild(cell);
    }

    // 2. Current month days
    for (let day = 1; day <= totalDays; day++) {
      const cellDate = new Date(year, month, day);
      const dateKey = this.getDateKey(cellDate);
      const ridesOnDay = this.ridesByDate.get(dateKey) || [];
      const hasRides = ridesOnDay.length > 0;
      const isToday = dateKey === todayKey;

      const cell = document.createElement("div");
      cell.className = `cal-day ${hasRides ? 'has-rides' : ''} ${isToday ? 'today' : ''}`;
      cell.dataset.date = dateKey;

      // Day number
      const numSpan = document.createElement("span");
      numSpan.className = "cal-day-num";
      numSpan.textContent = day;
      cell.appendChild(numSpan);

      // Dot indicator
      if (hasRides) {
        const dot = document.createElement("span");
        dot.className = "cal-day-dot";
        if (ridesOnDay.length > 1) {
          dot.classList.add("multi-ride");
          const c = ridesOnDay.length;
          dot.title = c >= 2 && c <= 4 ? `${c} treningi` : `${c} treningów`;
        }
        cell.appendChild(dot);

        // Hover events
        cell.addEventListener("mouseenter", () => {
          if (this.popoverTimeout) clearTimeout(this.popoverTimeout);
          this.showPopover(cell, dateKey, ridesOnDay);
        });

        cell.addEventListener("mouseleave", () => {
          this.popoverTimeout = setTimeout(() => {
            this.hidePopover();
          }, 200);
        });

        // Click event: on mobile or desktop click
        cell.addEventListener("click", () => {
          this.showPopover(cell, dateKey, ridesOnDay);
        });
      }

      this.gridEl.appendChild(cell);
    }

    // 3. Next month filler days to complete grid rows
    const totalCellsRendered = firstDayIndex + totalDays;
    const remainingCells = (7 - (totalCellsRendered % 7)) % 7;
    for (let day = 1; day <= remainingCells; day++) {
      const cell = document.createElement("div");
      cell.className = "cal-day cal-day-outside";
      cell.textContent = day;
      this.gridEl.appendChild(cell);
    }
  }

  showPopover(cellEl, dateKey, rides) {
    const formattedDate = new Intl.DateTimeFormat('pl-PL', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }).format(new Date(dateKey + "T12:00:00"));

    const totalDist = rides.reduce((s, r) => s + (r.distanceKm || 0), 0).toFixed(1);

    let listHtml = rides.map(ride => {
      const dur = formatDuration(ride.durationSeconds);
      const timeStr = ride.startTime instanceof Date
          ? ride.startTime.toLocaleTimeString('pl-PL', {hour: '2-digit', minute: '2-digit', hour12: false})
        : '';
      const title = ride.title || `Trening rowerowy`;

      return `
        <div class="popover-ride-item" data-ride-id="${ride.id}">
          <div class="popover-ride-top">
            <span class="popover-ride-time">🕒 ${timeStr}</span>
            <span class="popover-ride-dist">${ride.distanceKm.toFixed(1)} km</span>
          </div>
          <div class="popover-ride-title">${title}</div>
          <div class="popover-ride-meta">
            <span>⏱️ ${dur}</span>
            <span>⚡ ${ride.avgSpeedKmh.toFixed(1)} km/h</span>
            <span class="popover-arrow">➔</span>
          </div>
        </div>
      `;
    }).join("");

    const count = rides.length;
    const ridesPlural = count === 1 ? 'trening' : (count >= 2 && count <= 4 ? 'treningi' : 'treningów');

    this.popoverEl.innerHTML = `
      <div class="popover-header">
        <div class="popover-date">${formattedDate}</div>
        <div class="popover-badge">${count} ${ridesPlural} • ${totalDist} km</div>
      </div>
      <div class="popover-rides-list">
        ${listHtml}
      </div>
    `;

    // Add click listeners to rides
    this.popoverEl.querySelectorAll(".popover-ride-item").forEach(item => {
      item.addEventListener("click", () => {
        const rideId = item.dataset.rideId;
        if (this.onSelectRide) {
          this.onSelectRide(rideId);
        }
        this.hidePopover();
      });
    });

    // Positioning popover near cell
    this.popoverEl.style.display = "block";
    const cellRect = cellEl.getBoundingClientRect();
    const popRect = this.popoverEl.getBoundingClientRect();

    // Position horizontally centered with cell or clamped inside window
    let left = cellRect.left + (cellRect.width / 2) - (popRect.width / 2);
    if (left < 10) left = 10;
    if (left + popRect.width > window.innerWidth - 10) {
      left = window.innerWidth - popRect.width - 10;
    }

    // Position below or above cell depending on available vertical space
    let top = cellRect.bottom + 8;
    if (top + popRect.height > window.innerHeight - 10) {
      top = cellRect.top - popRect.height - 8;
    }
    if (top < 10) top = 10;

    this.popoverEl.style.left = `${left}px`;
    this.popoverEl.style.top = `${top}px`;
    this.popoverEl.classList.add("visible");
  }

  hidePopover() {
    if (this.popoverEl) {
      this.popoverEl.classList.remove("visible");
      this.popoverEl.style.display = "none";
    }
  }
}

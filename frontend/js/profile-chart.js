/**
 * Interactive Dual-Axis Ride Profile Chart (Speed & Elevation)
 * Supports:
 * - Dynamic Canvas rendering (crisp HiDPI)
 * - Toggleable Speed / Elevation curves via interactive legend
 * - Scrubbing / Hover synchronization with Leaflet map
 */

export class ProfileChart {
    constructor(options) {
        this.container = options.container;
        this.canvas = options.canvas;
        this.ctx = this.canvas.getContext('2d');

        // UI Elements
        this.btnSpeed = options.btnSpeed;
        this.btnElevation = options.btnElevation;
        this.btnCollapse = options.btnCollapse;
        this.chartBody = options.chartBody;
        this.tooltipContainer = options.tooltipContainer;
        this.scrubDist = options.scrubDist;
        this.scrubSpeed = options.scrubSpeed;
        this.scrubEle = options.scrubEle;
        this.collapseIcon = options.collapseIcon;

        // Callbacks
        this.onPointHover = options.onPointHover || (() => {
        });

        // State
        this.points = [];
        this.showSpeed = true;
        this.showElevation = true;
        this.hoverIndex = null;
        this.isCollapsed = false;

        // Layout configuration
        this.padding = {
            top: 18,
            right: 48,
            bottom: 28,
            left: 48
        };

        this.initEvents();
        this.initResizeObserver();
    }

    initEvents() {
        // Legend: Toggle Speed
        if (this.btnSpeed) {
            this.btnSpeed.addEventListener('click', () => {
                if (this.showSpeed && !this.showElevation) {
                    this.showElevation = true;
                }
                this.showSpeed = !this.showSpeed;
                this.updateLegendUi();
                this.render();
                this.updateTooltip();
            });
        }

        // Legend: Toggle Elevation
        if (this.btnElevation) {
            this.btnElevation.addEventListener('click', () => {
                if (this.showElevation && !this.showSpeed) {
                    this.showSpeed = true;
                }
                this.showElevation = !this.showElevation;
                this.updateLegendUi();
                this.render();
                this.updateTooltip();
            });
        }

        // Collapse Toggle
        if (this.btnCollapse) {
            this.btnCollapse.addEventListener('click', () => {
                this.isCollapsed = !this.isCollapsed;
                if (this.container) {
                    this.container.classList.toggle('collapsed', this.isCollapsed);
                }
                if (this.chartBody) {
                    this.chartBody.style.display = this.isCollapsed ? 'none' : 'block';
                }
                if (this.collapseIcon) {
                    this.collapseIcon.textContent = this.isCollapsed ? '▴' : '▾';
                }
                if (!this.isCollapsed) {
                    this.resizeCanvas();
                    this.render();
                }
            });
        }

        // Mouse interactions on canvas
        this.canvas.addEventListener('mousemove', (e) => this.handlePointerMove(e));
        this.canvas.addEventListener('mouseleave', () => this.handlePointerLeave());

        // Touch interactions on canvas
        this.canvas.addEventListener('touchstart', (e) => {
            if (e.touches.length > 0) this.handlePointerMove(e.touches[0]);
        }, {passive: true});
        this.canvas.addEventListener('touchmove', (e) => {
            if (e.touches.length > 0) this.handlePointerMove(e.touches[0]);
        }, {passive: true});
        this.canvas.addEventListener('touchend', () => this.handlePointerLeave());
    }

    initResizeObserver() {
        if (window.ResizeObserver && this.chartBody) {
            this.resizeObserver = new ResizeObserver(() => {
                if (!this.isCollapsed && this.points.length > 0) {
                    this.resizeCanvas();
                    this.render();
                }
            });
            this.resizeObserver.observe(this.chartBody);
        }
    }

    setData(points) {
        this.points = points || [];
        this.hoverIndex = null;
        this.handlePointerLeave();

        if (this.points.length < 2) {
            this.hide();
            return;
        }

        this.show();
        this.resizeCanvas();
        this.updateLegendUi();
        this.render();
    }

    show() {
        if (this.container) {
            this.container.classList.add('active');
        }
    }

    hide() {
        if (this.container) {
            this.container.classList.remove('active');
        }
        this.handlePointerLeave();
    }

    resizeCanvas() {
        if (!this.chartBody) return;
        const rect = this.chartBody.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        const width = rect.width || 600;
        const height = 150;

        this.canvas.width = width * dpr;
        this.canvas.height = height * dpr;
        this.canvas.style.width = `${width}px`;
        this.canvas.style.height = `${height}px`;

        this.ctx.setTransform(1, 0, 0, 1, 0, 0);
        this.ctx.scale(dpr, dpr);
        this.displayWidth = width;
        this.displayHeight = height;
    }

    updateLegendUi() {
        if (this.btnSpeed) {
            this.btnSpeed.classList.toggle('active', this.showSpeed);
            this.btnSpeed.setAttribute('aria-pressed', this.showSpeed.toString());
        }
        if (this.btnElevation) {
            this.btnElevation.classList.toggle('active', this.showElevation);
            this.btnElevation.setAttribute('aria-pressed', this.showElevation.toString());
        }
    }

    handlePointerMove(event) {
        if (!this.points || this.points.length < 2 || !this.displayWidth) return;

        const rect = this.canvas.getBoundingClientRect();
        const mouseX = event.clientX - rect.left;
        const plotLeft = this.padding.left;
        const plotRight = this.displayWidth - this.padding.right;
        const plotWidth = plotRight - plotLeft;

        if (mouseX < plotLeft || mouseX > plotRight) {
            this.handlePointerLeave();
            return;
        }

        const fraction = Math.max(0, Math.min(1, (mouseX - plotLeft) / plotWidth));
        const totalDist = this.points.at(-1).distanceKm;
        const targetDist = fraction * totalDist;

        // Find nearest point by distance
        let closestIndex = 0;
        let minDiff = Infinity;
        for (let i = 0; i < this.points.length; i++) {
            const diff = Math.abs(this.points[i].distanceKm - targetDist);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        this.hoverIndex = closestIndex;
        const activePoint = this.points[closestIndex];

        this.render();
        this.updateTooltip(activePoint);
        this.onPointHover(activePoint);
    }

    handlePointerLeave() {
        this.hoverIndex = null;
        this.render();
        if (this.tooltipContainer) {
            this.tooltipContainer.style.display = 'none';
        }
        this.onPointHover(null);
    }

    updateTooltip(point = null) {
        const pt = point || (this.hoverIndex !== null ? this.points[this.hoverIndex] : null);
        if (!pt || !this.tooltipContainer) {
            if (this.tooltipContainer) this.tooltipContainer.style.display = 'none';
            return;
        }

        this.tooltipContainer.style.display = 'flex';
        if (this.scrubDist) {
            this.scrubDist.textContent = `${pt.distanceKm.toFixed(2)} km`;
        }
        if (this.scrubSpeed) {
            if (this.showSpeed) {
                this.scrubSpeed.style.display = 'inline-flex';
                this.scrubSpeed.textContent = `⚡ ${pt.speedKmh.toFixed(1)} km/h`;
            } else {
                this.scrubSpeed.style.display = 'none';
            }
        }
        if (this.scrubEle) {
            if (this.showElevation) {
                this.scrubEle.style.display = 'inline-flex';
                this.scrubEle.textContent = `⛰️ ${Math.round(pt.elevationM)} m`;
            } else {
                this.scrubEle.style.display = 'none';
            }
        }
    }

    render() {
        const ctx = this.ctx;
        const w = this.displayWidth;
        const h = this.displayHeight;
        if (!w || !h) return;

        ctx.clearRect(0, 0, w, h);

        if (!this.points || this.points.length < 2) return;

        const plotLeft = this.padding.left;
        const plotRight = w - this.padding.right;
        const plotTop = this.padding.top;
        const plotBottom = h - this.padding.bottom;
        const plotWidth = plotRight - plotLeft;
        const plotHeight = plotBottom - plotTop;

        const totalDist = Math.max(0.01, this.points.at(-1).distanceKm);

        // Speed bounds
        const maxSpeed = Math.max(15, ...this.points.map(p => p.speedKmh || 0));

        // Elevation bounds with 10% padding
        const rawMinEle = Math.min(...this.points.map(p => p.elevationM || 0));
        const rawMaxEle = Math.max(...this.points.map(p => p.elevationM || 0));
        const eleSpan = Math.max(10, rawMaxEle - rawMinEle);
        const minEle = rawMinEle - eleSpan * 0.1;
        const maxEle = rawMaxEle + eleSpan * 0.1;

        // Helper conversion functions
        const getX = (dist) => plotLeft + (dist / totalDist) * plotWidth;
        const getSpeedY = (speed) => plotBottom - (speed / maxSpeed) * plotHeight;
        const getEleY = (ele) => plotBottom - ((ele - minEle) / (maxEle - minEle)) * plotHeight;

        // 1. Grid lines and Y-axis labels
        const gridLines = 3;
        ctx.lineWidth = 1;
        ctx.setLineDash([4, 4]);
        ctx.font = '10px Outfit, sans-serif';
        ctx.textAlign = 'right';
        ctx.textBaseline = 'middle';

        for (let i = 0; i <= gridLines; i++) {
            const frac = i / gridLines;
            const y = plotBottom - frac * plotHeight;

            // Draw dashed horizontal grid line
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.07)';
            ctx.beginPath();
            ctx.moveTo(plotLeft, y);
            ctx.lineTo(plotRight, y);
            ctx.stroke();

            // Left Axis: Speed (Orange)
            if (this.showSpeed) {
                const speedVal = frac * maxSpeed;
                ctx.fillStyle = '#FF6B35';
                ctx.textAlign = 'right';
                ctx.fillText(`${Math.round(speedVal)}`, plotLeft - 8, y);
            }

            // Right Axis: Elevation (Cyan)
            if (this.showElevation) {
                const eleVal = minEle + frac * (maxEle - minEle);
                ctx.fillStyle = '#4ECDC4';
                ctx.textAlign = 'left';
                ctx.fillText(`${Math.round(eleVal)}m`, plotRight + 8, y);
            }
        }

        // Reset dash for chart curves
        ctx.setLineDash([]);

        // 2. Draw Elevation Area & Curve
        if (this.showElevation) {
            // Elevation Gradient Fill
            const eleGrad = ctx.createLinearGradient(0, plotTop, 0, plotBottom);
            eleGrad.addColorStop(0, 'rgba(78, 205, 196, 0.28)');
            eleGrad.addColorStop(1, 'rgba(78, 205, 196, 0.01)');

            ctx.beginPath();
            ctx.moveTo(getX(this.points[0].distanceKm), plotBottom);
            for (const element of this.points) {
                ctx.lineTo(getX(element.distanceKm), getEleY(element.elevationM));
            }
            ctx.lineTo(getX(this.points.at(-1).distanceKm), plotBottom);
            ctx.closePath();
            ctx.fillStyle = eleGrad;
            ctx.fill();

            // Elevation Line
            ctx.beginPath();
            for (let i = 0; i < this.points.length; i++) {
                const px = getX(this.points[i].distanceKm);
                const py = getEleY(this.points[i].elevationM);
                if (i === 0) ctx.moveTo(px, py);
                else ctx.lineTo(px, py);
            }
            ctx.strokeStyle = '#4ECDC4';
            ctx.lineWidth = 2.2;
            ctx.lineJoin = 'round';
            ctx.lineCap = 'round';
            ctx.stroke();
        }

        // 3. Draw Speed Curve
        if (this.showSpeed) {
            ctx.beginPath();
            for (let i = 0; i < this.points.length; i++) {
                const px = getX(this.points[i].distanceKm);
                const py = getSpeedY(this.points[i].speedKmh);
                if (i === 0) ctx.moveTo(px, py);
                else ctx.lineTo(px, py);
            }
            ctx.strokeStyle = '#FF6B35';
            ctx.lineWidth = 2.2;
            ctx.lineJoin = 'round';
            ctx.lineCap = 'round';
            ctx.stroke();
        }

        // 4. X-Axis Distance labels
        ctx.font = '10px Outfit, sans-serif';
        ctx.fillStyle = '#9595B2';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';

        const xTicks = 4;
        for (let i = 0; i <= xTicks; i++) {
            const frac = i / xTicks;
            const dist = frac * totalDist;
            const x = plotLeft + frac * plotWidth;

            let align = 'center';
            if (i === 0) align = 'left';
            else if (i === xTicks) align = 'right';

            ctx.textAlign = align;
            ctx.fillText(`${dist.toFixed(1)} km`, x, plotBottom + 8);
        }

        // 5. Scrubber Line & Marker Dots
        if (this.hoverIndex !== null && this.points[this.hoverIndex]) {
            const activePt = this.points[this.hoverIndex];
            const activeX = getX(activePt.distanceKm);

            // Vertical guide line
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.7)';
            ctx.lineWidth = 1.2;
            ctx.setLineDash([3, 3]);
            ctx.beginPath();
            ctx.moveTo(activeX, plotTop);
            ctx.lineTo(activeX, plotBottom);
            ctx.stroke();
            ctx.setLineDash([]);

            // Elevation Dot
            if (this.showElevation) {
                const activeEleY = getEleY(activePt.elevationM);
                ctx.beginPath();
                ctx.arc(activeX, activeEleY, 5, 0, Math.PI * 2);
                ctx.fillStyle = '#0B0B1A';
                ctx.fill();
                ctx.strokeStyle = '#4ECDC4';
                ctx.lineWidth = 2.5;
                ctx.stroke();
            }

            // Speed Dot
            if (this.showSpeed) {
                const activeSpeedY = getSpeedY(activePt.speedKmh);
                ctx.beginPath();
                ctx.arc(activeX, activeSpeedY, 5, 0, Math.PI * 2);
                ctx.fillStyle = '#0B0B1A';
                ctx.fill();
                ctx.strokeStyle = '#FF6B35';
                ctx.lineWidth = 2.5;
                ctx.stroke();
            }
        }
    }
}

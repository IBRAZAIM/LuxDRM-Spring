(() => {
    "use strict";

    const PERIODS = new Set(["TODAY", "WEEK", "MONTH", "QUARTER", "YEAR", "ALL", "CUSTOM"]);
    const PERIOD_LABELS = {
        TODAY: "сегодня",
        WEEK: "неделя",
        MONTH: "месяц",
        QUARTER: "квартал",
        YEAR: "год",
        ALL: "за все время",
        CUSTOM: "пользовательский"
    };

    const STORAGE_KEYS = {
        period: "sales.period",
        start: "sales.customStart",
        end: "sales.customEnd"
    };

    const reports = document.querySelector(".reports");
    const periodButtons = Array.from(document.querySelectorAll(".period-btn"));
    const customStartInput = document.getElementById("customStart");
    const customEndInput = document.getElementById("customEnd");
    const applyCustomButton = document.getElementById("applyCustom");
    const periodLabel = document.getElementById("periodLabel");
    const updatedAt = document.getElementById("updatedAt");
    const insightsList = document.getElementById("insightsList");

    const kpi = {
        sales: document.getElementById("kpiSales"),
        visits: document.getElementById("kpiVisits"),
        orders: document.getElementById("kpiOrders"),
        conversion: document.getElementById("kpiConversion"),
        averageCheck: document.getElementById("kpiAverageCheck"),
        revenuePerVisit: document.getElementById("kpiRevenuePerVisit")
    };

    const chartCanvases = {
        revenue: document.getElementById("revenueChart"),
        traffic: document.getElementById("trafficChart"),
        conversion: document.getElementById("conversionChart")
    };

    const state = {
        period: "TODAY",
        customStart: "",
        customEnd: "",
        requestToken: 0,
        charts: {
            revenue: null,
            traffic: null,
            conversion: null
        }
    };

    const hasChartJs = typeof window.Chart !== "undefined";
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    const numberFormat = new Intl.NumberFormat("ru-RU");
    const moneyFormat = new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 });
    const percentFormat = new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 1 });
    const dateLabelFormat = new Intl.DateTimeFormat("ru-RU", { day: "2-digit", month: "2-digit" });
    const monthLabelFormat = new Intl.DateTimeFormat("ru-RU", { month: "short", year: "2-digit" });
    const timeLabelFormat = new Intl.DateTimeFormat("ru-RU", { hour: "2-digit", minute: "2-digit" });

    function toNumber(value) {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    function formatTenge(value) {
        return `${moneyFormat.format(Math.round(toNumber(value)))} ₸`;
    }

    function formatPercent(value) {
        return `${percentFormat.format(toNumber(value))}%`;
    }

    function formatCount(value) {
        return numberFormat.format(Math.round(toNumber(value)));
    }

    function toIsoDate(date) {
        return date.toISOString().slice(0, 10);
    }

    function parseIsoDate(value) {
        if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
            return null;
        }

        const [year, month, day] = value.split("-").map(Number);
        const date = new Date(year, month - 1, day);
        if (
            Number.isNaN(date.getTime()) ||
            date.getFullYear() !== year ||
            date.getMonth() !== month - 1 ||
            date.getDate() !== day
        ) {
            return null;
        }
        return date;
    }

    function normalizeLabel(raw) {
        if (typeof raw !== "string") {
            return "";
        }

        if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
            const parsed = parseIsoDate(raw);
            return parsed ? dateLabelFormat.format(parsed) : raw;
        }

        if (/^\d{4}-\d{2}$/.test(raw)) {
            const [year, month] = raw.split("-").map(Number);
            const date = new Date(year, month - 1, 1);
            return Number.isNaN(date.getTime()) ? raw : monthLabelFormat.format(date);
        }

        return raw;
    }

    function setLoading(loading) {
        if (reports) {
            reports.classList.toggle("is-loading", loading);
        }

        periodButtons.forEach((button) => {
            button.disabled = loading;
        });

        if (customStartInput) customStartInput.disabled = loading;
        if (customEndInput) customEndInput.disabled = loading;

        if (applyCustomButton) {
            applyCustomButton.disabled = loading;
            applyCustomButton.classList.toggle("is-busy", loading);
        }
    }

    function setPeriodButtons(period) {
        periodButtons.forEach((button) => {
            button.classList.toggle("active", button.dataset.period === period);
        });
    }

    function setMeta(period) {
        if (periodLabel) {
            periodLabel.textContent = `Период: ${PERIOD_LABELS[period] || "выбранный"}`;
        }
        if (updatedAt) {
            updatedAt.textContent = `Обновлено: ${timeLabelFormat.format(new Date())}`;
        }
    }

    function animateValue(element, targetValue, formatter, duration = 600) {
        if (!element) {
            return;
        }

        const target = toNumber(targetValue);
        if (reducedMotion) {
            element.textContent = formatter(target);
            element.dataset.value = String(target);
            return;
        }

        const start = toNumber(element.dataset.value);
        const startTime = performance.now();
        const delta = target - start;

        function tick(now) {
            const progress = Math.min((now - startTime) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            const current = start + delta * eased;
            element.textContent = formatter(current);

            if (progress < 1) {
                requestAnimationFrame(tick);
                return;
            }

            element.dataset.value = String(target);
            element.textContent = formatter(target);
        }

        requestAnimationFrame(tick);
    }

    function buildChartGradient(ctx, colorFrom, colorTo) {
        const gradient = ctx.createLinearGradient(0, 0, 0, 240);
        gradient.addColorStop(0, colorFrom);
        gradient.addColorStop(1, colorTo);
        return gradient;
    }

    function buildCommonChartOptions() {
        return {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: "index", intersect: false },
            plugins: {
                legend: {
                    labels: {
                        color: "#4b5563",
                        boxWidth: 10,
                        boxHeight: 10,
                        usePointStyle: true,
                        pointStyle: "circle",
                        font: { family: "Manrope", size: 11, weight: "700" }
                    }
                },
                tooltip: {
                    backgroundColor: "rgba(12, 16, 22, 0.92)",
                    titleColor: "#f8fafc",
                    bodyColor: "#e5e7eb",
                    borderColor: "rgba(255,255,255,0.08)",
                    borderWidth: 1,
                    padding: 10
                }
            },
            scales: {
                x: {
                    grid: { color: "rgba(148, 163, 184, 0.14)", drawBorder: false },
                    ticks: { color: "#6b7280", font: { family: "Manrope", size: 11, weight: "700" } }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: "rgba(148, 163, 184, 0.14)", drawBorder: false },
                    ticks: { color: "#6b7280", font: { family: "Manrope", size: 11, weight: "700" } }
                }
            }
        };
    }

    function ensureCharts(labels, series) {
        if (!hasChartJs) {
            return;
        }

        const normalizedLabels = labels.map(normalizeLabel);

        if (!state.charts.revenue && chartCanvases.revenue) {
            const ctx = chartCanvases.revenue.getContext("2d");
            const common = buildCommonChartOptions();

            state.charts.revenue = new Chart(ctx, {
                type: "line",
                data: {
                    labels: normalizedLabels,
                    datasets: [{
                        label: "Выручка",
                        data: series.sales,
                        borderColor: "#1f365f",
                        borderWidth: 2,
                        backgroundColor: buildChartGradient(ctx, "rgba(31,54,95,0.22)", "rgba(31,54,95,0.02)"),
                        fill: true,
                        tension: 0.35,
                        pointRadius: 2.5,
                        pointHoverRadius: 4
                    }]
                },
                options: {
                    ...common,
                    plugins: {
                        ...common.plugins,
                        tooltip: {
                            ...common.plugins.tooltip,
                            callbacks: {
                                label(context) {
                                    return ` ${formatTenge(context.parsed.y)}`;
                                }
                            }
                        }
                    },
                    scales: {
                        ...common.scales,
                        y: {
                            ...common.scales.y,
                            ticks: {
                                ...common.scales.y.ticks,
                                callback(value) {
                                    return formatTenge(value);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (!state.charts.traffic && chartCanvases.traffic) {
            const ctx = chartCanvases.traffic.getContext("2d");
            const common = buildCommonChartOptions();

            state.charts.traffic = new Chart(ctx, {
                type: "bar",
                data: {
                    labels: normalizedLabels,
                    datasets: [
                        {
                            type: "bar",
                            label: "Посещения",
                            data: series.visits,
                            borderRadius: 8,
                            backgroundColor: "rgba(31,54,95,0.78)",
                            borderSkipped: false
                        },
                        {
                            type: "line",
                            label: "Заказы",
                            data: series.orders,
                            borderColor: "#0f9a94",
                            borderWidth: 2,
                            pointRadius: 2,
                            pointHoverRadius: 4,
                            tension: 0.34
                        }
                    ]
                },
                options: {
                    ...common,
                    scales: {
                        ...common.scales,
                        y: {
                            ...common.scales.y,
                            ticks: {
                                ...common.scales.y.ticks,
                                callback(value) {
                                    return formatCount(value);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (!state.charts.conversion && chartCanvases.conversion) {
            const ctx = chartCanvases.conversion.getContext("2d");
            const common = buildCommonChartOptions();

            state.charts.conversion = new Chart(ctx, {
                type: "line",
                data: {
                    labels: normalizedLabels,
                    datasets: [{
                        label: "Конверсия",
                        data: series.conversion,
                        borderColor: "#0f9a94",
                        borderWidth: 2,
                        backgroundColor: buildChartGradient(ctx, "rgba(15,154,148,0.22)", "rgba(15,154,148,0.02)"),
                        fill: true,
                        tension: 0.35,
                        pointRadius: 2.5,
                        pointHoverRadius: 4
                    }]
                },
                options: {
                    ...common,
                    plugins: {
                        ...common.plugins,
                        tooltip: {
                            ...common.plugins.tooltip,
                            callbacks: {
                                label(context) {
                                    return ` ${formatPercent(context.parsed.y)}`;
                                }
                            }
                        }
                    },
                    scales: {
                        ...common.scales,
                        y: {
                            ...common.scales.y,
                            ticks: {
                                ...common.scales.y.ticks,
                                callback(value) {
                                    return formatPercent(value);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    function updateCharts(labels, series) {
        if (!hasChartJs) {
            return;
        }

        ensureCharts(labels, series);
        const normalizedLabels = labels.map(normalizeLabel);

        if (state.charts.revenue) {
            state.charts.revenue.data.labels = normalizedLabels;
            state.charts.revenue.data.datasets[0].data = series.sales;
            state.charts.revenue.update();
        }

        if (state.charts.traffic) {
            state.charts.traffic.data.labels = normalizedLabels;
            state.charts.traffic.data.datasets[0].data = series.visits;
            state.charts.traffic.data.datasets[1].data = series.orders;
            state.charts.traffic.update();
        }

        if (state.charts.conversion) {
            state.charts.conversion.data.labels = normalizedLabels;
            state.charts.conversion.data.datasets[0].data = series.conversion;
            state.charts.conversion.update();
        }
    }

    function buildInsights(payload) {
        if (!insightsList) {
            return;
        }

        const overview = payload.overview || {};
        const labels = Array.isArray(payload.labels) ? payload.labels : [];
        const salesSeries = Array.isArray(payload.salesSeries) ? payload.salesSeries : [];
        const ordersSeries = Array.isArray(payload.ordersSeries) ? payload.ordersSeries : [];
        const visitsSeries = Array.isArray(payload.visitsSeries) ? payload.visitsSeries : [];
        const conversionSeries = Array.isArray(payload.conversionSeries) ? payload.conversionSeries : [];

        insightsList.innerHTML = "";

        const totalSales = toNumber(overview.totalSales);
        const totalOrders = toNumber(overview.ordersToday);
        const totalVisits = toNumber(overview.visitsToday);
        const currentConversion = toNumber(overview.conversion);

        const peakSales = salesSeries.reduce((best, value, index) => {
            const current = toNumber(value);
            if (current > best.value) {
                return { value: current, index };
            }
            return best;
        }, { value: 0, index: -1 });

        const peakOrders = ordersSeries.reduce((best, value, index) => {
            const current = toNumber(value);
            if (current > best.value) {
                return { value: current, index };
            }
            return best;
        }, { value: 0, index: -1 });

        const averageConversion = conversionSeries.length
            ? conversionSeries.reduce((sum, value) => sum + toNumber(value), 0) / conversionSeries.length
            : currentConversion;

        const insights = [
            `Выручка за период: ${formatTenge(totalSales)}.`,
            `Заказы: ${formatCount(totalOrders)}, посещения: ${formatCount(totalVisits)}, конверсия: ${formatPercent(currentConversion)}.`,
            `Средний чек: ${formatTenge(payload.averageOrderValue)}; доход с визита: ${formatTenge(payload.revenuePerVisit)}.`
        ];

        if (peakSales.index >= 0 && labels[peakSales.index]) {
            insights.push(`Пик выручки: ${formatTenge(peakSales.value)} (${normalizeLabel(labels[peakSales.index])}).`);
        }

        if (peakOrders.index >= 0 && labels[peakOrders.index]) {
            insights.push(`Пик заказов: ${formatCount(peakOrders.value)} (${normalizeLabel(labels[peakOrders.index])}).`);
        }

        insights.push(`Средняя конверсия по периоду: ${formatPercent(averageConversion)}.`);

        const hasAnyActivity = salesSeries.some((value) => toNumber(value) > 0)
            || ordersSeries.some((value) => toNumber(value) > 0)
            || visitsSeries.some((value) => toNumber(value) > 0);

        if (!hasAnyActivity) {
            insights.length = 0;
            insights.push("За выбранный период нет активности. Попробуйте расширить диапазон дат.");
        }

        insights.slice(0, 6).forEach((text) => {
            const item = document.createElement("li");
            item.textContent = text;
            insightsList.appendChild(item);
        });
    }

    function updateKpi(payload) {
        const overview = payload.overview || {};
        animateValue(kpi.sales, overview.totalSales, formatTenge);
        animateValue(kpi.visits, overview.visitsToday, formatCount);
        animateValue(kpi.orders, overview.ordersToday, formatCount);
        animateValue(kpi.conversion, overview.conversion, formatPercent);
        animateValue(kpi.averageCheck, payload.averageOrderValue, formatTenge);
        animateValue(kpi.revenuePerVisit, payload.revenuePerVisit, formatTenge);
    }

    function collectRequestParams(period) {
        const params = new URLSearchParams();
        params.set("period", period);
        if (period === "CUSTOM") {
            params.set("customStart", state.customStart);
            params.set("customEnd", state.customEnd);
        }
        return params;
    }

    function saveFilters() {
        localStorage.setItem(STORAGE_KEYS.period, state.period);
        localStorage.setItem(STORAGE_KEYS.start, state.customStart || "");
        localStorage.setItem(STORAGE_KEYS.end, state.customEnd || "");
    }

    function loadFilters() {
        const savedPeriod = localStorage.getItem(STORAGE_KEYS.period);
        const savedStart = localStorage.getItem(STORAGE_KEYS.start);
        const savedEnd = localStorage.getItem(STORAGE_KEYS.end);

        if (PERIODS.has(savedPeriod)) {
            state.period = savedPeriod;
        }
        if (savedStart) {
            state.customStart = savedStart;
        }
        if (savedEnd) {
            state.customEnd = savedEnd;
        }

        const today = toIsoDate(new Date());

        if (customStartInput) {
            customStartInput.value = state.customStart || today;
            state.customStart = customStartInput.value;
        }

        if (customEndInput) {
            customEndInput.value = state.customEnd || today;
            state.customEnd = customEndInput.value;
        }
    }

    function isValidCustomRange() {
        if (!customStartInput || !customEndInput) {
            return false;
        }

        customStartInput.setCustomValidity("");
        customEndInput.setCustomValidity("");

        if (!customStartInput.value) {
            customStartInput.setCustomValidity("Укажите дату начала.");
            customStartInput.reportValidity();
            return false;
        }

        if (!customEndInput.value) {
            customEndInput.setCustomValidity("Укажите дату окончания.");
            customEndInput.reportValidity();
            return false;
        }

        if (customEndInput.value < customStartInput.value) {
            customEndInput.setCustomValidity("Дата окончания должна быть не раньше даты начала.");
            customEndInput.reportValidity();
            return false;
        }

        state.customStart = customStartInput.value;
        state.customEnd = customEndInput.value;
        return true;
    }

    async function refresh(period) {
        const requestedPeriod = PERIODS.has(period) ? period : "TODAY";
        state.period = requestedPeriod;

        saveFilters();
        setPeriodButtons(requestedPeriod);
        setMeta(requestedPeriod);
        setLoading(true);

        const requestToken = ++state.requestToken;
        const params = collectRequestParams(requestedPeriod);

        try {
            const response = await fetch(`/sales/api/dashboard?${params.toString()}`, {
                headers: { Accept: "application/json" }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const payload = await response.json();
            if (requestToken !== state.requestToken) {
                return;
            }

            const labels = Array.isArray(payload.labels) ? payload.labels : [];
            const series = {
                visits: Array.isArray(payload.visitsSeries) ? payload.visitsSeries.map(toNumber) : [],
                orders: Array.isArray(payload.ordersSeries) ? payload.ordersSeries.map(toNumber) : [],
                sales: Array.isArray(payload.salesSeries) ? payload.salesSeries.map(toNumber) : [],
                conversion: Array.isArray(payload.conversionSeries) ? payload.conversionSeries.map(toNumber) : []
            };

            updateKpi(payload);
            updateCharts(labels, series);
            buildInsights(payload);
            setMeta(requestedPeriod);
        } catch (error) {
            if (requestToken !== state.requestToken) {
                return;
            }

            if (insightsList) {
                insightsList.innerHTML = "";
                const item = document.createElement("li");
                item.textContent = "Не удалось загрузить аналитику. Обновите страницу или проверьте сервер.";
                insightsList.appendChild(item);
            }

            console.error("Sales dashboard loading failed:", error);
        } finally {
            if (requestToken === state.requestToken) {
                setLoading(false);
            }
        }
    }

    function bindEvents() {
        periodButtons.forEach((button) => {
            button.addEventListener("click", () => {
                const period = button.dataset.period;
                if (PERIODS.has(period)) {
                    refresh(period);
                }
            });
        });

        if (applyCustomButton) {
            applyCustomButton.addEventListener("click", () => {
                if (!isValidCustomRange()) {
                    return;
                }
                refresh("CUSTOM");
            });
        }

        const applyOnEnter = (event) => {
            if (event.key !== "Enter") {
                return;
            }
            if (!isValidCustomRange()) {
                return;
            }
            refresh("CUSTOM");
        };

        if (customStartInput) {
            customStartInput.addEventListener("keydown", applyOnEnter);
            customStartInput.addEventListener("change", () => {
                state.customStart = customStartInput.value;
                saveFilters();
            });
        }

        if (customEndInput) {
            customEndInput.addEventListener("keydown", applyOnEnter);
            customEndInput.addEventListener("change", () => {
                state.customEnd = customEndInput.value;
                saveFilters();
            });
        }
    }

    function bootstrap() {
        loadFilters();
        bindEvents();

        if (state.period === "CUSTOM" && state.customStart && state.customEnd) {
            refresh("CUSTOM");
            return;
        }

        const activeButton = periodButtons.find((button) => button.classList.contains("active"));
        refresh(activeButton?.dataset.period || state.period || "TODAY");
    }

    bootstrap();
})();

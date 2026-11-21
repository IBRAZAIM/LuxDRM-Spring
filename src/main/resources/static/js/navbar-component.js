/* ════════════════════════════════════════════════════════════
   Навбар + общие UX-улучшения (подключается на всех страницах)
   ════════════════════════════════════════════════════════════ */
(function () {
    if (window.__uxdNavbarInit) return;
    window.__uxdNavbarInit = true;

/* ── 1. Боковое меню ───────────────────────────────────────── */
(function () {
    window.openSidebar = function () {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('overlay');
        if (sidebar) sidebar.classList.add('active');
        if (overlay) overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    };

    window.closeSidebar = function () {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('overlay');
        if (sidebar) sidebar.classList.remove('active');
        if (overlay) overlay.classList.remove('active');
        document.body.style.overflow = '';
    };

    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;

    const close = () => {
        window.closeSidebar();
    };

    const menuIcon = document.getElementById('menu-icon');
    if (menuIcon) {
        menuIcon.addEventListener('click', window.openSidebar);
    }
    const overlay = document.getElementById('overlay');
    if (overlay) {
        overlay.addEventListener('click', window.closeSidebar);
    }
    document.querySelectorAll('.close-btn').forEach(btn => btn.addEventListener('click', window.closeSidebar));

    // Закрытие по клику на пункт меню и свайпом влево на телефоне
    sidebar.querySelectorAll('.panel-item a, .sidebar-cat-link, .sidebar-service-links a').forEach((a) => a.addEventListener('click', close));

    let sbTouchX = 0, sbTouchY = 0;
    sidebar.addEventListener('touchstart', (e) => {
        if (!e.touches || !e.touches[0]) return;
        sbTouchX = e.touches[0].clientX;
        sbTouchY = e.touches[0].clientY;
    }, { passive: true });
    sidebar.addEventListener('touchend', (e) => {
        if (!e.changedTouches || !e.changedTouches[0]) return;
        const dx = e.changedTouches[0].clientX - sbTouchX;
        const dy = e.changedTouches[0].clientY - sbTouchY;
        if (dx < -55 && Math.abs(dy) < 65) close();
    }, { passive: true });

    // Подсветка текущего раздела: с учётом query-параметров (?section=..., ?category=..., ?sale=1)
    const path = window.location.pathname.replace(/\/+$/, '') || '/';
    const params = new URLSearchParams(window.location.search);
    const curSection = (params.get('section') || '').toLowerCase();
    const curCategory = (params.get('category') || '').toLowerCase();
    const curSale = params.get('sale') === '1';

    function matchNavUrl(rawHref) {
        if (!rawHref) return false;
        const [hrefPathRaw, hrefQuery] = rawHref.split('?');
        const hrefPath = (hrefPathRaw || '').replace(/\/+$/, '') || '/';
        if (hrefQuery) {
            if (hrefPath !== path) return false;
            const hp = new URLSearchParams(hrefQuery);
            if (hp.get('section')) return hp.get('section').toLowerCase() === curSection;
            if (hp.get('category')) return hp.get('category').toLowerCase() === curCategory;
            if (hp.get('sale')) return curSale;
            return ('?' + hrefQuery) === window.location.search;
        }
        if (hrefPath === '/products') {
            return path === '/products' && !curSection && !curCategory && !curSale;
        }
        return hrefPath === path || (hrefPath !== '/' && path.startsWith(hrefPath + '/'));
    }

    sidebar.querySelectorAll('.panel-item').forEach((li) => li.classList.remove('active'));
    sidebar.querySelectorAll('.panel-item a').forEach((a) => {
        if (matchNavUrl(a.getAttribute('href'))) {
            a.closest('.panel-item').classList.add('active');
        }
    });

    document.querySelectorAll('.subnav-link').forEach((a) => {
        a.classList.toggle('active', matchNavUrl(a.getAttribute('href')));
    });

    // Двухъярусная шапка: плавное сворачивание нижнего яруса при прокрутке вниз
    const siteHeader = document.getElementById('siteHeader') || document.querySelector('.header');
    if (siteHeader) {
        let lastScrollY = window.scrollY;
        let scrollTicking = false;
        window.addEventListener('scroll', function () {
            if (scrollTicking) return;
            scrollTicking = true;
            requestAnimationFrame(function () {
                const y = window.scrollY;
                const delta = y - lastScrollY;
                if (y < 48 || delta < -6) {
                    siteHeader.classList.remove('header--compact');
                } else if (y > 96 && delta > 8) {
                    siteHeader.classList.add('header--compact');
                }
                lastScrollY = y;
                scrollTicking = false;
            });
        }, { passive: true });
    }
})();

/* ── 2. Общие UX-помощники ─────────────────────────────────── */
(function () {
    'use strict';

    // 2.1 Подтверждение опасных действий: <form data-confirm="..."> / <a data-confirm="...">
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (!(form instanceof HTMLFormElement)) return;
        const msg = form.dataset.confirm;
        if (msg && !window.confirm(msg)) {
            e.preventDefault();
        }
    }, true);

    document.addEventListener('click', function (e) {
        const link = e.target.closest('a[data-confirm]');
        if (link && !window.confirm(link.dataset.confirm)) {
            e.preventDefault();
        }
    });

    // 2.2 Защита от двойной отправки POST-форм
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (!(form instanceof HTMLFormElement) || e.defaultPrevented) return;
        if ((form.method || 'get').toLowerCase() !== 'post') return;
        setTimeout(function () {
            form.querySelectorAll('button[type="submit"], input[type="submit"], button:not([type])')
                .forEach(function (btn) { btn.classList.add('is-submitting'); });
        }, 0);
    });

    // Возврат из кэша браузера (кнопка «Назад») — снимаем блокировку
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('.is-submitting').forEach(function (el) {
            el.classList.remove('is-submitting');
        });
    });

    // 2.3 Автоскрытие уведомлений об успехе (ошибки остаются до закрытия)
    document.querySelectorAll('.alert-success, .alert[role="status"]').forEach(function (el) {
        setTimeout(function () {
            el.classList.add('is-hiding');
            setTimeout(function () { el.remove(); }, 450);
        }, 6000);
    });

    // 2.4 Кнопка «Наверх»
    const topBtn = document.createElement('button');
    topBtn.type = 'button';
    topBtn.className = 'back-to-top';
    topBtn.setAttribute('aria-label', 'Наверх');
    topBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="18 15 12 9 6 15"/></svg>';
    topBtn.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    document.body.appendChild(topBtn);

    let ticking = false;
    window.addEventListener('scroll', function () {
        if (ticking) return;
        ticking = true;
        requestAnimationFrame(function () {
            topBtn.classList.toggle('visible', window.scrollY > 600);
            ticking = false;
        });
    }, { passive: true });

    // 2.5 Горячая клавиша «/» — фокус на поле поиска
    document.addEventListener('keydown', function (e) {
        if (e.key !== '/' || e.ctrlKey || e.metaKey || e.altKey) return;
        const tag = (document.activeElement && document.activeElement.tagName) || '';
        if (/^(INPUT|TEXTAREA|SELECT)$/.test(tag)) return;
        const search = document.querySelector('#search, #orderSearchInput, input[type="search"]');
        if (search) {
            e.preventDefault();
            search.focus();
            search.select();
        }
    });

    // 2.6 Глобальный счётчик «Избранное» в шапке (индивидуально для каждого пользователя)
    function getWishlistStorageKey() {
        const badge = document.getElementById('navWishlistCount');
        const userKey = (badge && badge.getAttribute('data-user-key')) || 'guest';
        return 'uxd_wishlist_ids_' + userKey;
    }

    // Инициализация серверных данных для авторизованного пользователя (даже на страницах без product-card.js)
    (function hydrateServerWishlist() {
        const badge = document.getElementById('navWishlistCount');
        if (!badge) return;
        const isAuth = badge.getAttribute('data-authenticated') === 'true';
        if (isAuth) {
            const serverIdsRaw = badge.getAttribute('data-server-ids') || '';
            const serverIds = serverIdsRaw ? serverIdsRaw.split(',').map(function (s) { return s.trim(); }).filter(Boolean) : [];
            try {
                localStorage.setItem(getWishlistStorageKey(), JSON.stringify(serverIds));
            } catch (_) {}
        }
    })();

    window.syncNavbarWishlistCount = function () {
        const badge = document.getElementById('navWishlistCount');
        if (!badge) return;
        try {
            const raw = localStorage.getItem(getWishlistStorageKey());
            const list = raw ? JSON.parse(raw) : [];
            const count = Array.isArray(list) ? list.length : 0;
            badge.textContent = String(count);
            badge.style.display = count > 0 ? 'flex' : 'none';
        } catch (_) {
            badge.style.display = 'none';
        }
    };
    window.syncNavbarWishlistCount();
    window.addEventListener('storage', function (e) {
        if (e.key && e.key.indexOf('uxd_wishlist_ids_') === 0) window.syncNavbarWishlistCount();
    });
})();


/* ── НАДЁЖНОЕ ОТКРЫТИЕ БОКОВОГО МЕНЮ (DELEGATION & GLOBAL API) ── */
(function() {
    window.openSidebar = function() {
        const sb = document.getElementById('sidebar');
        const ov = document.getElementById('overlay');
        if (sb) {
            sb.classList.add('active');
            sb.style.transform = 'translateX(0)';
            sb.style.zIndex = '999999';
        }
        if (ov) {
            ov.classList.add('active');
            ov.style.visibility = 'visible';
            ov.style.opacity = '1';
            ov.style.zIndex = '999998';
        }
        document.body.style.overflow = 'hidden';
    };

    window.closeSidebar = function() {
        const sb = document.getElementById('sidebar');
        const ov = document.getElementById('overlay');
        if (sb) {
            sb.classList.remove('active');
            sb.style.transform = '';
        }
        if (ov) {
            ov.classList.remove('active');
            ov.style.visibility = '';
            ov.style.opacity = '';
        }
        document.body.style.overflow = '';
    };

    // Глобальное делегирование клика на кнопку Меню и Overlay
    document.addEventListener('click', function(e) {
        const menuBtn = e.target.closest('#menu-icon, .menu-icon');
        if (menuBtn) {
            e.preventDefault();
            e.stopPropagation();
            window.openSidebar();
            return;
        }

        const closeBtn = e.target.closest('.close-btn');
        if (closeBtn && closeBtn.closest('#sidebar')) {
            e.preventDefault();
            window.closeSidebar();
            return;
        }

        if (e.target.id === 'overlay' || e.target.classList.contains('overlay')) {
            window.closeSidebar();
        }
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            window.closeSidebar();
        }
    });
})();
})();

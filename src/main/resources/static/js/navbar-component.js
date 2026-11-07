/* ════════════════════════════════════════════════════════════
   Навбар + общие UX-улучшения (подключается на всех страницах)
   ════════════════════════════════════════════════════════════ */

/* ── 1. Боковое меню ───────────────────────────────────────── */
(function () {
    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;

    // Открытие/закрытие/Escape уже обеспечивает inline-скрипт в navbar.html
    // (openSidebar / closeSidebar), здесь только дополнительное поведение.
    const close = () => {
        if (typeof window.closeSidebar === 'function') window.closeSidebar();
    };

    // Закрытие по клику на пункт меню (удобно на мобильных)
    sidebar.querySelectorAll('.panel-item a').forEach((a) => a.addEventListener('click', close));

    // Подсветка текущего раздела: точное совпадение или вложенный путь
    const path = window.location.pathname.replace(/\/+$/, '') || '/';
    let best = null;
    let bestLen = -1;
    sidebar.querySelectorAll('.panel-item').forEach((li) => li.classList.remove('active'));
    sidebar.querySelectorAll('.panel-item a').forEach((a) => {
        const href = (a.getAttribute('href') || '').split('?')[0].replace(/\/+$/, '') || '/';
        const match = href === path || (href !== '/' && path.startsWith(href + '/'));
        if (match && href.length > bestLen) {
            best = a;
            bestLen = href.length;
        }
    });
    if (best) best.closest('.panel-item').classList.add('active');
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

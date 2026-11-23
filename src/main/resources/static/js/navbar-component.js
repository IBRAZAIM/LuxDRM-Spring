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

/* ── ПОЛНОЦЕННАЯ БОКОВАЯ КОРЗИНА (UXD CART DRAWER) ──────────── */
(function () {
    'use strict';

    function getCsrfHeaders() {
        const badge = document.getElementById('navWishlistCount');
        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json',
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        };
        if (badge) {
            const token = badge.getAttribute('data-csrf-token');
            const headerName = badge.getAttribute('data-csrf-header') || 'X-CSRF-TOKEN';
            if (token) headers[headerName] = token;
        }
        return headers;
    }

    function fmtKzt(val) {
        const n = Math.round(Number(val || 0));
        return n.toLocaleString('ru-RU') + '\u00a0₸';
    }

    function escHtml(s) {
        return String(s || '')
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function syncNavCartBadge(count) {
        const badge = document.getElementById('navCartCount') || document.querySelector('.cart-count:not(#navWishlistCount)');
        if (!badge) return;
        const num = Number(count || 0);
        badge.textContent = String(num);
        badge.style.display = num > 0 ? 'flex' : 'none';
    }

    function ensureDrawer() {
        let backdrop = document.getElementById('uxdCartDrawerBackdrop');
        let drawer = document.getElementById('uxdCartDrawer');
        if (backdrop && drawer) return { backdrop, drawer };

        backdrop = document.createElement('div');
        backdrop.id = 'uxdCartDrawerBackdrop';
        backdrop.className = 'uxd-cd-backdrop';
        backdrop.addEventListener('click', window.closeUxdCartDrawer);
        document.body.appendChild(backdrop);

        drawer = document.createElement('aside');
        drawer.id = 'uxdCartDrawer';
        drawer.className = 'uxd-cd';
        drawer.setAttribute('role', 'dialog');
        drawer.setAttribute('aria-label', 'Корзина покупок');
        drawer.innerHTML =
            '<div class="uxd-cd__head">' +
                '<div class="uxd-cd__head-title">' +
                    '<span class="uxd-cd__heading">Корзина</span>' +
                    '<span class="uxd-cd__count" id="uxdCdCount"></span>' +
                '</div>' +
                '<button type="button" class="uxd-cd__close" aria-label="Закрыть корзину">✕</button>' +
            '</div>' +
            '<div class="uxd-cd__ship" id="uxdCdShip" style="display:none;">' +
                '<div class="uxd-cd__ship-text" id="uxdCdShipText"></div>' +
                '<div class="uxd-cd__ship-track" aria-hidden="true"><div class="uxd-cd__ship-fill" id="uxdCdShipFill"></div></div>' +
            '</div>' +
            '<div class="uxd-cd__body" id="uxdCdBody"></div>' +
            '<div class="uxd-cd__foot" id="uxdCdFoot" style="display:none;">' +
                '<div class="uxd-cd__saving" id="uxdCdSaving" style="display:none;">' +
                    '<span>Ваша экономия</span>' +
                    '<strong id="uxdCdSavingVal"></strong>' +
                '</div>' +
                '<div class="uxd-cd__total-row">' +
                    '<span class="uxd-cd__total-lbl">Итого</span>' +
                    '<span class="uxd-cd__total-val" id="uxdCdTotal">0 ₸</span>' +
                '</div>' +
                '<a href="/checkout" class="uxd-cd__btn uxd-cd__btn--primary">Оформить заказ</a>' +
                '<a href="/cart" class="uxd-cd__btn uxd-cd__btn--secondary">Перейти в корзину</a>' +
            '</div>';

        drawer.querySelector('.uxd-cd__close').addEventListener('click', window.closeUxdCartDrawer);

        let sx = 0, sy = 0;
        drawer.addEventListener('touchstart', function (e) {
            if (!e.touches || !e.touches[0]) return;
            sx = e.touches[0].clientX;
            sy = e.touches[0].clientY;
        }, { passive: true });
        drawer.addEventListener('touchend', function (e) {
            if (!e.changedTouches || !e.changedTouches[0]) return;
            const dx = e.changedTouches[0].clientX - sx;
            const dy = e.changedTouches[0].clientY - sy;
            if (dx > 60 && Math.abs(dy) < 65) window.closeUxdCartDrawer();
        }, { passive: true });

        drawer.addEventListener('click', function (e) {
            const qtyBtn = e.target.closest('.uxd-cd__qty-btn');
            if (qtyBtn && !qtyBtn.disabled) {
                const row = qtyBtn.closest('.uxd-cd__item');
                const id = row && row.dataset.itemId;
                const nextQty = Number(qtyBtn.dataset.nextQty || 1);
                if (!id) return;
                row.classList.add('is-busy');
                fetch('/cart/update/' + encodeURIComponent(id), {
                    method: 'POST',
                    headers: getCsrfHeaders(),
                    body: 'quantity=' + encodeURIComponent(nextQty)
                })
                    .then(function (r) { return r.ok ? r.json() : null; })
                    .then(function (data) { if (data) renderDrawerData(data); })
                    .catch(function () { row.classList.remove('is-busy'); });
                return;
            }

            const delBtn = e.target.closest('.uxd-cd__del');
            if (delBtn) {
                const row = delBtn.closest('.uxd-cd__item');
                const id = row && row.dataset.itemId;
                if (!id) return;
                row.classList.add('is-busy');
                fetch('/cart/delete', {
                    method: 'POST',
                    headers: getCsrfHeaders(),
                    body: 'id=' + encodeURIComponent(id)
                })
                    .then(function (r) { return r.ok ? r.json() : null; })
                    .then(function (data) { if (data) renderDrawerData(data); })
                    .catch(function () { row.classList.remove('is-busy'); });
            }
        });

        document.body.appendChild(drawer);
        return { backdrop, drawer };
    }

    function renderDrawerData(data) {
        const drawer = document.getElementById('uxdCartDrawer');
        if (!drawer || !data) return;

        const items = Array.isArray(data.items) ? data.items : [];
        const count = Number(data.cartItemsCount || 0);
        syncNavCartBadge(count);

        const countEl = drawer.querySelector('#uxdCdCount');
        if (countEl) countEl.textContent = count > 0 ? ('· ' + (data.cartItemsLabel || (count + ' шт.'))) : '';

        const shipBox = drawer.querySelector('#uxdCdShip');
        const shipTxt = drawer.querySelector('#uxdCdShipText');
        const shipFill = drawer.querySelector('#uxdCdShipFill');
        const bodyEl = drawer.querySelector('#uxdCdBody');
        const footEl = drawer.querySelector('#uxdCdFoot');

        if (!items.length) {
            if (shipBox) shipBox.style.display = 'none';
            if (footEl) footEl.style.display = 'none';
            bodyEl.innerHTML =
                '<div class="uxd-cd__empty">' +
                    '<svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" aria-hidden="true">' +
                        '<path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>' +
                        '<line x1="3" y1="6" x2="21" y2="6"></line>' +
                        '<path d="M16 10a4 4 0 0 1-8 0"></path>' +
                    '</svg>' +
                    '<p class="uxd-cd__empty-title">Ваша корзина пуста</p>' +
                    '<p class="uxd-cd__empty-sub">Добавьте понравившиеся модели из каталога, чтобы оформить заказ.</p>' +
                    '<a href="/products" class="uxd-cd__btn uxd-cd__btn--primary" style="width:100%;max-width:240px;">Перейти в каталог</a>' +
                '</div>';
            return;
        }

        const threshold = Number(data.freeShippingThreshold || 50000);
        const finalPrice = Math.max(0, Number(data.finalPrice || 0));
        const pct = Math.min(100, Math.round((finalPrice / threshold) * 100));
        if (shipBox && shipTxt && shipFill) {
            shipBox.style.display = 'block';
            shipFill.style.width = pct + '%';
            if (finalPrice >= threshold) {
                shipBox.classList.add('is-unlocked');
                shipTxt.textContent = 'Бесплатная доставка по Казахстану активирована';
            } else {
                shipBox.classList.remove('is-unlocked');
                const left = Math.ceil(threshold - finalPrice);
                shipTxt.innerHTML = 'До бесплатной доставки осталось <strong>' + fmtKzt(left) + '</strong>';
            }
        }

        bodyEl.innerHTML = items.map(function (it) {
            const disc = Number(it.discount || 0);
            const hasDisc = disc > 0;
            const qty = Math.max(1, Number(it.quantity || 1));
            const priceHtml = hasDisc
                ? '<span class="uxd-cd__price is-sale">' + fmtKzt(it.lineFinalPrice) + '</span>' +
                  '<span class="uxd-cd__old-price">' + fmtKzt(it.lineSubtotal) + '</span>' +
                  '<span class="uxd-cd__ticket">−' + disc + '%</span>'
                : '<span class="uxd-cd__price">' + fmtKzt(it.lineFinalPrice) + '</span>';

            return (
                '<div class="uxd-cd__item" data-item-id="' + escHtml(it.id) + '">' +
                    '<a href="/products/' + encodeURIComponent(it.productId) + '" class="uxd-cd__thumb-link">' +
                        '<img class="uxd-cd__thumb" src="' + escHtml(it.image || '/images/product-placeholder.svg') + '" alt="' + escHtml(it.name) + '" loading="lazy" onerror="this.onerror=null;this.src=\'/images/product-placeholder.svg\';">' +
                    '</a>' +
                    '<div class="uxd-cd__info">' +
                        '<div class="uxd-cd__cat">' + escHtml(it.category) + '</div>' +
                        '<a href="/products/' + encodeURIComponent(it.productId) + '" class="uxd-cd__name">' + escHtml(it.name) + '</a>' +
                        (it.size ? '<div class="uxd-cd__size">Размер: ' + escHtml(it.size) + '</div>' : '') +
                        '<div class="uxd-cd__prices">' + priceHtml + '</div>' +
                        '<div class="uxd-cd__controls">' +
                            '<div class="uxd-cd__stepper">' +
                                '<button type="button" class="uxd-cd__qty-btn" data-next-qty="' + (qty - 1) + '"' + (qty <= 1 ? ' disabled' : '') + ' aria-label="Уменьшить">−</button>' +
                                '<span class="uxd-cd__qty-val">' + qty + '</span>' +
                                '<button type="button" class="uxd-cd__qty-btn" data-next-qty="' + (qty + 1) + '"' + (qty >= 20 ? ' disabled' : '') + ' aria-label="Увеличить">+</button>' +
                            '</div>' +
                            '<button type="button" class="uxd-cd__del">Удалить</button>' +
                        '</div>' +
                    '</div>' +
                '</div>'
            );
        }).join('');

        const totalSaved = Number(data.discountAmount || 0) + Number(data.promoDiscount || 0);
        const savEl = drawer.querySelector('#uxdCdSaving');
        const savVal = drawer.querySelector('#uxdCdSavingVal');
        if (savEl && savVal) {
            if (totalSaved > 0) {
                savEl.style.display = 'flex';
                savVal.textContent = '−' + fmtKzt(totalSaved);
            } else {
                savEl.style.display = 'none';
            }
        }
        const totEl = drawer.querySelector('#uxdCdTotal');
        if (totEl) totEl.textContent = fmtKzt(finalPrice);
        if (footEl) footEl.style.display = 'flex';
    }

    window.openUxdCartDrawer = function () {
        const { backdrop, drawer } = ensureDrawer();
        const bodyEl = drawer.querySelector('#uxdCdBody');
        if (bodyEl && !bodyEl.innerHTML.trim()) {
            bodyEl.innerHTML = '<div class="uxd-cd__loading">Загрузка корзины…</div>';
        }
        requestAnimationFrame(function () {
            backdrop.classList.add('is-open');
            drawer.classList.add('is-open');
            document.body.style.overflow = 'hidden';
        });
        fetch('/cart/api/summary', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (data) { if (data) renderDrawerData(data); })
            .catch(function () {});
    };

    window.closeUxdCartDrawer = function () {
        const backdrop = document.getElementById('uxdCartDrawerBackdrop');
        const drawer = document.getElementById('uxdCartDrawer');
        if (drawer) drawer.classList.remove('is-open');
        if (backdrop) backdrop.classList.remove('is-open');
        document.body.style.overflow = '';
    };

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') window.closeUxdCartDrawer();
    });

    document.addEventListener('click', function (e) {
        const cartLink = e.target.closest('.header a.icon-link[href="/cart"]');
        if (!cartLink) return;
        const p = window.location.pathname.replace(/\/+$/, '');
        if (p === '/cart' || p === '/checkout') return;
        e.preventDefault();
        window.openUxdCartDrawer();
    });
})();
})();

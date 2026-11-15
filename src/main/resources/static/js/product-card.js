/* ══════════════════════════════════════════════════════════════════════════
   UXD (LUXDRM) — СКРИПТ КАРТОЧКИ ТОВАРА И ИЗБРАННОГО
   1. Выдвижная панель выбора размера по клику на «В корзину» и отправка в корзину
   2. Добавление/удаление из «Избранного» (персонально для каждого пользователя: БД + localStorage)
   ══════════════════════════════════════════════════════════════════════════ */
(function () {
    'use strict';

    let toastTimer = null;

    function getNavBadge() {
        return document.getElementById('navWishlistCount');
    }

    function getStorageKey() {
        const badge = getNavBadge();
        const userKey = (badge && badge.getAttribute('data-user-key')) || 'guest';
        return 'uxd_wishlist_ids_' + userKey;
    }

    function getCsrfHeaders() {
        const badge = getNavBadge();
        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
        };
        if (badge) {
            const token = badge.getAttribute('data-csrf-token');
            const headerName = badge.getAttribute('data-csrf-header') || 'X-CSRF-TOKEN';
            if (token) headers[headerName] = token;
        }
        return headers;
    }

    // Гидратация из БД для авторизованного пользователя
    function initWishlistFromServer() {
        const badge = getNavBadge();
        if (!badge) return;
        const isAuth = badge.getAttribute('data-authenticated') === 'true';
        if (isAuth) {
            const rawServer = badge.getAttribute('data-server-ids') || '';
            const serverList = rawServer
                ? rawServer.split(',').map(function (s) { return s.trim(); }).filter(Boolean)
                : [];
            try {
                localStorage.setItem(getStorageKey(), JSON.stringify(serverList));
            } catch (_) {}
        }
    }

    initWishlistFromServer();

    function getWishlist() {
        try {
            const raw = localStorage.getItem(getStorageKey());
            const parsed = raw ? JSON.parse(raw) : [];
            return Array.isArray(parsed) ? parsed.map(String) : [];
        } catch (_) {
            return [];
        }
    }

    function saveWishlist(list) {
        try {
            localStorage.setItem(getStorageKey(), JSON.stringify(list));
        } catch (_) {}
        if (typeof window.syncNavbarWishlistCount === 'function') {
            window.syncNavbarWishlistCount();
        }
        window.dispatchEvent(new CustomEvent('uxd:wishlist-change', { detail: { ids: list } }));
    }

    function clearWishlist() {
        saveWishlist([]);
        syncWishlistUI(document);
        fetch('/wishlist/clear', {
            method: 'POST',
            headers: getCsrfHeaders()
        }).catch(function () {});
    }

    function syncWishlistUI(root) {
        const scope = root || document;
        const likedSet = new Set(getWishlist());
        scope.querySelectorAll('.uxd-card__wish').forEach(function (btn) {
            const id = String(btn.getAttribute('data-product-id') || '');
            const isLiked = likedSet.has(id);
            btn.classList.toggle('is-liked', isLiked);
            btn.setAttribute('aria-pressed', String(isLiked));
            btn.setAttribute('aria-label', isLiked ? 'Удалить из избранного' : 'Добавить в избранное');
        });
        if (typeof window.syncNavbarWishlistCount === 'function') {
            window.syncNavbarWishlistCount();
        }
    }

    function notify(msg) {
        if (typeof window.showToast === 'function') {
            window.showToast(msg);
            return;
        }
        let el = document.getElementById('uxdCardToast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'uxdCardToast';
            el.className = 'uxd-card-toast';
            el.setAttribute('role', 'status');
            el.setAttribute('aria-live', 'polite');
            document.body.appendChild(el);
        }
        el.textContent = msg;
        el.classList.add('is-visible');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            el.classList.remove('is-visible');
        }, 2500);
    }

    function updateNavCartCount(count) {
        const badge = document.getElementById('navCartCount') || document.querySelector('.cart-count:not(#navWishlistCount)');
        if (!badge) return;
        const num = Number(count) || 0;
        badge.textContent = String(num);
        badge.style.display = num > 0 ? 'flex' : 'none';
    }

    function closePanel(panel) {
        if (!panel) return;
        panel.classList.remove('is-open');
        const card = panel.closest('.uxd-card');
        const btn = card && card.querySelector('.uxd-card__cart');
        if (btn) btn.setAttribute('aria-expanded', 'false');
    }

    function closeAll(except) {
        document.querySelectorAll('.uxd-card__sizes.is-open').forEach(function (p) {
            if (p !== except) closePanel(p);
        });
    }

    document.addEventListener('click', function (e) {
        const wishBtn = e.target.closest('.uxd-card__wish');
        if (wishBtn) {
            e.preventDefault();
            e.stopPropagation();
            const id = String(wishBtn.getAttribute('data-product-id') || '');
            if (!id) return;
            const list = getWishlist();
            const idx = list.indexOf(id);
            let added = false;
            if (idx >= 0) {
                list.splice(idx, 1);
            } else {
                list.push(id);
                added = true;
            }
            saveWishlist(list);
            syncWishlistUI(document);
            notify(added ? 'Добавлено в избранное ♥' : 'Удалено из избранного');

            // Синхронизация с сервером (БД для авторизованных / сессия для гостей)
            fetch('/wishlist/toggle/' + encodeURIComponent(id), {
                method: 'POST',
                headers: getCsrfHeaders()
            })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (data) {
                    if (data && Array.isArray(data.ids)) {
                        saveWishlist(data.ids.map(String));
                        syncWishlistUI(document);
                    }
                })
                .catch(function () {});
            return;
        }

        const cartBtn = e.target.closest('.uxd-card__cart');
        const sizeBtn = e.target.closest('.uxd-card__size');
        const closeBtn = e.target.closest('.uxd-card__sizes-close');

        if (cartBtn) {
            const card = cartBtn.closest('.uxd-card');
            const panel = card && card.querySelector('.uxd-card__sizes');
            if (!panel) return;
            closeAll(panel);
            panel.classList.add('is-open');
            cartBtn.setAttribute('aria-expanded', 'true');
            return;
        }

        if (sizeBtn) {
            const form = sizeBtn.closest('form');
            if (!form) return;
            const chosenSize = sizeBtn.dataset.size || sizeBtn.textContent.trim();
            const sizeInput = form.querySelector('input[name="size"]');
            if (sizeInput) sizeInput.value = chosenSize;
            sizeBtn.classList.add('is-selected');
            form.querySelectorAll('button').forEach(function (b) { b.disabled = true; });

            const panel = sizeBtn.closest('.uxd-card__sizes');
            const formData = new URLSearchParams(new FormData(form));

            fetch(form.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'application/json'
                },
                body: formData.toString()
            })
                .then(function (res) {
                    if (!res.ok) throw new Error('Request failed');
                    return res.json();
                })
                .then(function (data) {
                    if (data && typeof data.cartItemsCount !== 'undefined') {
                        updateNavCartCount(data.cartItemsCount);
                    }
                    notify('Добавлено в корзину (размер ' + chosenSize + ')');
                    setTimeout(function () {
                        closePanel(panel);
                        sizeBtn.classList.remove('is-selected');
                        form.querySelectorAll('button').forEach(function (b) { b.disabled = false; });
                    }, 220);
                })
                .catch(function () {
                    form.submit();
                });
            return;
        }

        if (closeBtn) {
            closePanel(closeBtn.closest('.uxd-card__sizes'));
            return;
        }

        // Клик вне открытого поповера закрывает его
        if (!e.target.closest('.uxd-card__sizes')) {
            closeAll(null);
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeAll(null);
    });

    document.addEventListener('DOMContentLoaded', function () {
        syncWishlistUI(document);
    });

    window.UXDWishlist = {
        getIds: getWishlist,
        saveIds: saveWishlist,
        clear: clearWishlist,
        sync: syncWishlistUI
    };
})();

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

    function closeMiniCart() {
        const drawer = document.getElementById('uxdMiniCart');
        const backdrop = document.getElementById('uxdMiniCartBackdrop');
        if (drawer) drawer.classList.remove('is-open');
        if (backdrop) backdrop.classList.remove('is-open');
        document.body.style.overflow = '';
    }

    function openMiniCart(itemInfo) {
        let backdrop = document.getElementById('uxdMiniCartBackdrop');
        let drawer = document.getElementById('uxdMiniCart');
        if (!backdrop) {
            backdrop = document.createElement('div');
            backdrop.id = 'uxdMiniCartBackdrop';
            backdrop.className = 'uxd-minicart-backdrop';
            backdrop.addEventListener('click', closeMiniCart);
            document.body.appendChild(backdrop);
        }
        if (!drawer) {
            drawer = document.createElement('aside');
            drawer.id = 'uxdMiniCart';
            drawer.className = 'uxd-minicart';
            drawer.setAttribute('role', 'dialog');
            drawer.setAttribute('aria-label', 'Товар добавлен в корзину');
            drawer.innerHTML =
                '<div class="uxd-minicart__head">' +
                    '<span class="uxd-minicart__title">Добавлено в корзину</span>' +
                    '<button type="button" class="uxd-minicart__close" aria-label="Закрыть">✕</button>' +
                '</div>' +
                '<div class="uxd-minicart__item">' +
                    '<img class="uxd-minicart__img" id="uxdMcImg" src="" alt="">' +
                    '<div class="uxd-minicart__meta">' +
                        '<div class="uxd-minicart__cat" id="uxdMcCat"></div>' +
                        '<div class="uxd-minicart__name" id="uxdMcName"></div>' +
                        '<div class="uxd-minicart__size" id="uxdMcSize"></div>' +
                        '<div class="uxd-minicart__price-row">' +
                            '<span class="uxd-minicart__price" id="uxdMcPrice"></span>' +
                            '<span class="uxd-minicart__old-price" id="uxdMcOldPrice" style="display:none;"></span>' +
                            '<span class="uxd-minicart__ticket" id="uxdMcBadge" style="display:none;"></span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                '<div class="uxd-minicart__foot">' +
                    '<div class="uxd-minicart__ship" id="uxdMcShip">' +
                        '<div class="uxd-minicart__ship-text" id="uxdMcShipText"></div>' +
                        '<div class="uxd-minicart__ship-track" aria-hidden="true"><div class="uxd-minicart__ship-fill" id="uxdMcShipFill"></div></div>' +
                    '</div>' +
                    '<div class="uxd-minicart__count" id="uxdMcCount"></div>' +
                    '<a href="/checkout" class="uxd-minicart__btn uxd-minicart__btn--primary">Оформить заказ</a>' +
                    '<a href="/cart" class="uxd-minicart__btn uxd-minicart__btn--secondary">Перейти в корзину</a>' +
                    '<button type="button" class="uxd-minicart__continue">Продолжить покупки</button>' +
                '</div>';
            drawer.querySelector('.uxd-minicart__close').addEventListener('click', closeMiniCart);
            drawer.querySelector('.uxd-minicart__continue').addEventListener('click', closeMiniCart);
            document.body.appendChild(drawer);
        }

        const imgEl = drawer.querySelector('#uxdMcImg');
        if (imgEl) {
            imgEl.src = itemInfo.img || '/images/product-placeholder.svg';
            imgEl.alt = itemInfo.name || '';
        }
        const catEl = drawer.querySelector('#uxdMcCat');
        if (catEl) catEl.textContent = itemInfo.cat || 'UXD';
        const nameEl = drawer.querySelector('#uxdMcName');
        if (nameEl) nameEl.textContent = itemInfo.name || '';
        const sizeEl = drawer.querySelector('#uxdMcSize');
        if (sizeEl) sizeEl.textContent = itemInfo.size ? ('Размер: ' + itemInfo.size) : '';
        const priceEl = drawer.querySelector('#uxdMcPrice');
        if (priceEl) {
            priceEl.textContent = itemInfo.price || '';
            priceEl.classList.toggle('is-sale', Number(itemInfo.disc || 0) > 0);
        }
        const oldPriceEl = drawer.querySelector('#uxdMcOldPrice');
        if (oldPriceEl) {
            if (itemInfo.oldPrice && Number(itemInfo.disc || 0) > 0) {
                oldPriceEl.textContent = itemInfo.oldPrice;
                oldPriceEl.style.display = 'inline';
            } else {
                oldPriceEl.textContent = '';
                oldPriceEl.style.display = 'none';
            }
        }
        const badgeEl = drawer.querySelector('#uxdMcBadge');
        if (badgeEl) {
            const discNum = Number(itemInfo.disc || 0);
            if (discNum > 0) {
                badgeEl.textContent = '−' + discNum + '%';
                badgeEl.style.display = 'inline-flex';
            } else {
                badgeEl.textContent = '';
                badgeEl.style.display = 'none';
            }
        }
        const countEl = drawer.querySelector('#uxdMcCount');
        if (countEl) {
            const c = Number(itemInfo.cartCount || 1);
            countEl.textContent = 'В корзине сейчас: ' + c + ' шт.';
        }

        const FREE_SHIP_THRESHOLD = 50000;
        const shipBox = drawer.querySelector('#uxdMcShip');
        const shipText = drawer.querySelector('#uxdMcShipText');
        const shipFill = drawer.querySelector('#uxdMcShipFill');
        if (shipBox && shipText && shipFill) {
            const totalAmount = Math.max(0, Number(itemInfo.cartTotal || 0));
            const pct = Math.min(100, Math.round((totalAmount / FREE_SHIP_THRESHOLD) * 100));
            shipFill.style.width = pct + '%';
            if (totalAmount >= FREE_SHIP_THRESHOLD) {
                shipBox.classList.add('is-unlocked');
                shipText.textContent = 'Бесплатная доставка по Казахстану активирована';
            } else {
                shipBox.classList.remove('is-unlocked');
                const left = Math.ceil(FREE_SHIP_THRESHOLD - totalAmount);
                shipText.innerHTML = 'До бесплатной доставки осталось <strong>' + left.toLocaleString('ru-RU') + '\u00a0₸</strong>';
            }
        }

        requestAnimationFrame(function () {
            backdrop.classList.add('is-open');
            drawer.classList.add('is-open');
            document.body.style.overflow = 'hidden';
        });
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
            if (cartBtn.disabled) return;
            const card = cartBtn.closest('.uxd-card');
            const panel = card && card.querySelector('.uxd-card__sizes');
            if (!panel) return;
            const availSizeBtns = panel.querySelectorAll('.uxd-card__size:not(:disabled)');
            if (availSizeBtns.length === 1) {
                availSizeBtns[0].click();
                return;
            }
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
                    const card = panel && panel.closest('.uxd-card');
                    const imgEl = card && card.querySelector('.uxd-card__slide');
                    const nameEl = card && card.querySelector('.uxd-card__title');
                    const catEl = card && card.querySelector('.uxd-card__category');
                    const priceEl = card && card.querySelector('.uxd-card__price-current');
                    const oldPriceEl = card && card.querySelector('.uxd-card__price-old');
                    const discVal = card ? Number(card.dataset.disc || 0) : 0;

                    setTimeout(function () {
                        closePanel(panel);
                        sizeBtn.classList.remove('is-selected');
                        form.querySelectorAll('button').forEach(function (b) { b.disabled = false; });
                        if (typeof window.openUxdCartDrawer === 'function') {
                            window.openUxdCartDrawer();
                        } else {
                            openMiniCart({
                                img: imgEl ? imgEl.getAttribute('src') : '/images/product-placeholder.svg',
                                name: nameEl ? nameEl.textContent.trim() : '',
                                cat: catEl ? catEl.textContent.trim() : '',
                                price: priceEl ? priceEl.textContent.trim() : '',
                                oldPrice: oldPriceEl ? oldPriceEl.textContent.trim() : '',
                                disc: discVal,
                                size: chosenSize,
                                cartCount: data && typeof data.cartItemsCount !== 'undefined' ? data.cartItemsCount : 1,
                                cartTotal: (data && typeof data.cartTotal !== 'undefined')
                                    ? Number(data.cartTotal)
                                    : (card ? Number(card.dataset.finalPrice || card.dataset.price || 0) : 0)
                            });
                        }
                    }, 180);
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
        if (e.key === 'Escape') {
            closeAll(null);
            closeMiniCart();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        syncWishlistUI(document);
    });

    // Обновление точек-индикаторов при свайпе фото в карточке на телефоне
    document.addEventListener('scroll', function (e) {
        const track = e.target;
        if (!track || !track.classList || !track.classList.contains('uxd-card__track')) return;
        const w = track.clientWidth;
        if (!w) return;
        const idx = Math.round(track.scrollLeft / w);
        const media = track.closest('.uxd-card__media');
        if (!media) return;
        const dots = media.querySelectorAll('.uxd-card__dot');
        dots.forEach(function (dot, i) {
            dot.classList.toggle('is-active', i === idx);
        });
    }, true);

    window.UXDWishlist = {
        getIds: getWishlist,
        saveIds: saveWishlist,
        clear: clearWishlist,
        sync: syncWishlistUI
    };
})();

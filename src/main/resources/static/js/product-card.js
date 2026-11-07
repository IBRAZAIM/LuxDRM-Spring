/* ══════════════════════════════════════════════════════════════════════════
   UXD (LUXDRM) — СКРИПТ КАРТОЧКИ ТОВАРА
   Выдвижная панель выбора размера по клику на «В корзину» и отправка в корзину
   ══════════════════════════════════════════════════════════════════════════ */
(function () {
    'use strict';

    let toastTimer = null;

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
        const badge = document.getElementById('navCartCount') || document.querySelector('.cart-count');
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
})();

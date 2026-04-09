(function () {
    'use strict';

    var TIP_CLASS = 'reaction-tooltip';
    var TIP_VISIBLE = 'is-visible';
    var OFFSET = 8;

    var tipEl = null;
    var activeTarget = null;

    function ensureTip() {
        if (!tipEl) {
            tipEl = document.createElement('div');
            tipEl.className = TIP_CLASS;
            tipEl.setAttribute('role', 'tooltip');
            tipEl.setAttribute('aria-hidden', 'true');
            document.body.appendChild(tipEl);
        }
        return tipEl;
    }

    function hideTip() {
        if (!tipEl) {
            return;
        }
        tipEl.classList.remove(TIP_VISIBLE);
        tipEl.textContent = '';
        tipEl.setAttribute('aria-hidden', 'true');
        activeTarget = null;
    }

    function positionTip(target, el) {
        var text = target.getAttribute('data-tooltip');
        if (!text || !text.trim()) {
            hideTip();
            return;
        }
        el.textContent = text;
        el.classList.add(TIP_VISIBLE);
        el.setAttribute('aria-hidden', 'false');

        requestAnimationFrame(function () {
            var tr = target.getBoundingClientRect();
            var tw = el.offsetWidth;
            var th = el.offsetHeight;
            var left = tr.left + tr.width / 2 - tw / 2;
            left = Math.max(OFFSET, Math.min(left, window.innerWidth - tw - OFFSET));
            var top = tr.top - th - OFFSET;
            if (top < OFFSET) {
                top = tr.bottom + OFFSET;
            }
            el.style.left = left + 'px';
            el.style.top = top + 'px';
        });
    }

    function showTarget(target) {
        var text = target.getAttribute('data-tooltip');
        if (!text || !text.trim()) {
            return;
        }
        activeTarget = target;
        positionTip(target, ensureTip());
    }

    function onScrollOrResize() {
        if (activeTarget && document.body.contains(activeTarget)) {
            positionTip(activeTarget, ensureTip());
        } else {
            hideTip();
        }
    }

    function init() {
        document.addEventListener(
            'mouseover',
            function (e) {
                var chip = e.target.closest('.reaction-chip[data-tooltip]');
                if (!chip) {
                    return;
                }
                var from = e.relatedTarget;
                if (from && chip.contains(from)) {
                    return;
                }
                showTarget(chip);
            },
            true
        );

        document.addEventListener(
            'mouseout',
            function (e) {
                var chip = e.target.closest('.reaction-chip[data-tooltip]');
                if (!chip) {
                    return;
                }
                var to = e.relatedTarget;
                if (to && chip.contains(to)) {
                    return;
                }
                hideTip();
            },
            true
        );

        document.addEventListener(
            'focusin',
            function (e) {
                var chip = e.target.closest('.reaction-chip[data-tooltip]');
                if (chip) {
                    showTarget(chip);
                }
            },
            true
        );

        document.addEventListener(
            'focusout',
            function (e) {
                var chip = e.target.closest('.reaction-chip[data-tooltip]');
                if (!chip) {
                    return;
                }
                var rel = e.relatedTarget;
                if (rel && chip.contains(rel)) {
                    return;
                }
                hideTip();
            },
            true
        );

        window.addEventListener('scroll', onScrollOrResize, true);
        window.addEventListener('resize', onScrollOrResize);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();

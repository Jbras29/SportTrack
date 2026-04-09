(function () {
    'use strict';

    function setExpanded(toggle, open) {
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    }

    function openComposer(toggle, composer) {
        composer.hidden = false;
        setExpanded(toggle, true);
        var input = composer.querySelector('.comment-composer-input');
        if (input) {
            requestAnimationFrame(function () {
                input.focus();
            });
        }
    }

    function closeComposer(toggle, composer) {
        composer.hidden = true;
        setExpanded(toggle, false);
        var input = composer.querySelector('.comment-composer-input');
        if (input) {
            input.blur();
        }
    }

    function isComposerOpen(composer) {
        return !composer.hidden;
    }

    document.addEventListener('click', function (e) {
        var toggle = e.target.closest('.comment-composer-toggle');
        if (!toggle) {
            return;
        }
        var card = toggle.closest('.post-card');
        if (!card) {
            return;
        }
        var controlsId = toggle.getAttribute('aria-controls');
        var composer = controlsId ? document.getElementById(controlsId) : card.querySelector('.comment-composer');
        if (!composer) {
            return;
        }
        e.preventDefault();
        if (isComposerOpen(composer)) {
            closeComposer(toggle, composer);
        } else {
            openComposer(toggle, composer);
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') {
            return;
        }
        var active = document.activeElement;
        if (!active || !active.classList.contains('comment-composer-input')) {
            return;
        }
        var composer = active.closest('.comment-composer');
        if (!composer || composer.hidden) {
            return;
        }
        var card = composer.closest('.post-card');
        var toggle = card && card.querySelector('.comment-composer-toggle');
        if (toggle) {
            closeComposer(toggle, composer);
            toggle.focus();
        }
    });
})();

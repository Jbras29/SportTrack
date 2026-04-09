(function () {
    'use strict';

    /* ——— Sélecteur d’emoji (emoji-mart) : bouton + à côté de « Commenter » ——— */
    var EMOJI_DATA_URL = 'https://cdn.jsdelivr.net/npm/@emoji-mart/data@latest/sets/15/native.json';
    var emojiDataPromise = null;
    var emojiPickerWrap = null;
    var emojiPickerAnchor = null;

    function getEmojiData() {
        if (!emojiDataPromise) {
            emojiDataPromise = fetch(EMOJI_DATA_URL).then(function (r) {
                return r.json();
            });
        }
        return emojiDataPromise;
    }

    function hideEmojiPicker() {
        if (emojiPickerWrap) {
            emojiPickerWrap.hidden = true;
            emojiPickerWrap.setAttribute('aria-hidden', 'true');
        }
        emojiPickerAnchor = null;
    }

    function isEmojiPickerVisible() {
        return emojiPickerWrap && !emojiPickerWrap.hidden;
    }

    /** Clic à l’intérieur du popover (y compris shadow DOM du web component emoji-mart). */
    function isClickInsideEmojiPopover(e) {
        if (!emojiPickerWrap) {
            return false;
        }
        if (typeof e.composedPath === 'function') {
            return e.composedPath().indexOf(emojiPickerWrap) !== -1;
        }
        return emojiPickerWrap.contains(e.target);
    }

    function positionEmojiPicker(anchor) {
        var rect = anchor.getBoundingClientRect();
        var w = 352;
        var left = Math.max(8, Math.min(rect.left, window.innerWidth - w - 8));
        var top = rect.bottom + 8;
        var estimatedH = 420;
        if (top + estimatedH > window.innerHeight - 8 && rect.top > estimatedH + 8) {
            top = rect.top - estimatedH - 8;
        }
        if (top < 8) {
            top = 8;
        }
        emojiPickerWrap.style.left = left + 'px';
        emojiPickerWrap.style.top = top + 'px';
    }

    function showEmojiPicker(anchor) {
        if (typeof EmojiMart === 'undefined' || !EmojiMart.Picker) {
            return;
        }
        emojiPickerAnchor = anchor;
        getEmojiData().then(function (data) {
            if (!emojiPickerWrap) {
                emojiPickerWrap = document.createElement('div');
                emojiPickerWrap.id = 'emoji-mart-popover';
                emojiPickerWrap.className = 'emoji-mart-popover';
                emojiPickerWrap.setAttribute('role', 'dialog');
                emojiPickerWrap.setAttribute('aria-label', 'Choisir un emoji');
                emojiPickerWrap.hidden = true;
                emojiPickerWrap.setAttribute('aria-hidden', 'true');
                document.body.appendChild(emojiPickerWrap);

                var picker = new EmojiMart.Picker({
                    data: data,
                    locale: 'fr',
                    theme: 'light',
                    onEmojiSelect: function (emoji) {
                        console.log(emoji);
                        hideEmojiPicker();
                    }
                });
                emojiPickerWrap.appendChild(picker);
            }

            positionEmojiPicker(anchor);
            /* Après la fin du clic courant : sinon onClickOutside / détection « outside »
               voyait le même clic d’ouverture et refermait tout de suite le picker. */
            setTimeout(function () {
                if (!emojiPickerWrap || emojiPickerAnchor !== anchor) {
                    return;
                }
                emojiPickerWrap.hidden = false;
                emojiPickerWrap.setAttribute('aria-hidden', 'false');
            }, 0);
        });
    }

    function toggleEmojiPicker(anchor) {
        if (isEmojiPickerVisible() && emojiPickerAnchor === anchor) {
            hideEmojiPicker();
            return;
        }
        showEmojiPicker(anchor);
    }

    document.addEventListener('click', function (e) {
        var plusBtn = e.target.closest('.post-actions-plus-btn');
        if (!plusBtn) {
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        toggleEmojiPicker(plusBtn);
    });

    /* Fermeture au clic extérieur : le bouton + est exclu (toggle géré ci-dessus). */
    document.addEventListener(
        'click',
        function (e) {
            if (!isEmojiPickerVisible()) {
                return;
            }
            if (isClickInsideEmojiPopover(e)) {
                return;
            }
            if (e.target.closest('.post-actions-plus-btn')) {
                return;
            }
            hideEmojiPicker();
        },
        true
    );

    window.addEventListener('resize', function () {
        if (isEmojiPickerVisible() && emojiPickerAnchor) {
            positionEmojiPicker(emojiPickerAnchor);
        }
    });

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
        if (isEmojiPickerVisible()) {
            hideEmojiPicker();
            e.preventDefault();
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

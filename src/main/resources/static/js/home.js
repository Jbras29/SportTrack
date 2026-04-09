(function () {
    'use strict';

    /* ——— Sélecteur d’emoji (emoji-mart) : bouton + à côté de « Commenter » ——— */
    var EMOJI_DATA_URL = 'https://cdn.jsdelivr.net/npm/@emoji-mart/data@latest/sets/15/native.json';
    var emojiDataPromise = null;
    var emojiPickerWrap = null;
    var emojiPickerAnchor = null;
    var toastHideTimer = null;

    function showToastErreur(message) {
        var el = document.getElementById('home-feedback-toast');
        if (!el) {
            window.alert(message);
            return;
        }
        el.textContent = message || 'Une erreur est survenue.';
        el.hidden = false;
        el.classList.add('is-visible');
        if (toastHideTimer) {
            clearTimeout(toastHideTimer);
        }
        toastHideTimer = setTimeout(function () {
            el.hidden = true;
            el.classList.remove('is-visible');
            toastHideTimer = null;
        }, 6000);
    }

    function extraireEmojiNatif(emoji) {
        if (!emoji) {
            return '';
        }
        if (emoji.native) {
            return emoji.native;
        }
        if (emoji.skins && emoji.skins.length && emoji.skins[0].native) {
            return emoji.skins[0].native;
        }
        return '';
    }

    /** Aligné sur {@code Activite.LIMITE_REACTIONS_AFFICHEES} côté serveur */
    var LIMITE_REACTIONS_AFFICHEES = 5;

    /**
     * Regroupe les réactions comme {@code Activite#getReactionsGroupees} (ordre d’apparition, ASC).
     */
    function groupReactionsFromCommentaires(commentaires) {
        if (!commentaires || !commentaires.length) {
            return [];
        }
        var reactions = commentaires.filter(function (c) {
            return c.type === 'REACTION' && c.auteur && c.message;
        });
        reactions.sort(function (a, b) {
            var da = a.dateCreation ? new Date(a.dateCreation).getTime() : 0;
            var db = b.dateCreation ? new Date(b.dateCreation).getTime() : 0;
            return da - db;
        });
        var order = [];
        var byEmoji = {};
        reactions.forEach(function (c) {
            var e = c.message;
            var nom = ((c.auteur.prenom || '') + ' ' + (c.auteur.nom || '')).trim();
            if (!byEmoji[e]) {
                byEmoji[e] = [];
                order.push(e);
            }
            byEmoji[e].push(nom);
        });
        return order.map(function (emoji) {
            var names = byEmoji[emoji];
            return {
                emoji: emoji,
                nombre: names.length,
                nomsDesReacteurs: names.join(', ')
            };
        });
    }

    function utilisateurAEmitReactionAvecEmoji(commentaires, utilisateurId, emoji) {
        return idCommentaireReactionUtilisateur(commentaires, utilisateurId, emoji) != null;
    }

    function idCommentaireReactionUtilisateur(commentaires, utilisateurId, emoji) {
        if (utilisateurId == null || utilisateurId === '' || !emoji || !commentaires) {
            return null;
        }
        var uid = String(utilisateurId);
        for (var i = 0; i < commentaires.length; i++) {
            var c = commentaires[i];
            if (
                c.type === 'REACTION' &&
                c.message === emoji &&
                c.auteur &&
                String(c.auteur.id) === uid
            ) {
                return c.id;
            }
        }
        return null;
    }

    function renderReactionsBarIntoCard(card, commentaires) {
        var activiteId = card.getAttribute('data-activite-id');
        var groupes = groupReactionsFromCommentaires(commentaires);
        var stats = card.querySelector('.activity-stats');
        var existingBar = card.querySelector('.reactions-bar');

        if (groupes.length === 0) {
            if (existingBar) {
                existingBar.remove();
            }
            return;
        }

        var affichees = groupes.slice(0, LIMITE_REACTIONS_AFFICHEES);
        var masquees = Math.max(0, groupes.length - LIMITE_REACTIONS_AFFICHEES);
        var currentUserId = document.body.getAttribute('data-current-user-id');

        var track = document.createElement('div');
        track.className = 'reactions-bar-track';

        affichees.forEach(function (g) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'reaction-chip';
            var isMine = utilisateurAEmitReactionAvecEmoji(commentaires, currentUserId, g.emoji);
            if (isMine) {
                btn.classList.add('reaction-chip--mine');
            }
            btn.setAttribute('data-tooltip', g.nomsDesReacteurs);
            btn.setAttribute('data-emoji', g.emoji);
            btn.setAttribute('aria-label', g.emoji + ' : ' + g.nomsDesReacteurs);
            if (isMine) {
                var cid = idCommentaireReactionUtilisateur(commentaires, currentUserId, g.emoji);
                if (cid != null) {
                    btn.setAttribute('data-commentaire-id', String(cid));
                }
            }

            var main = document.createElement('span');
            main.className = 'reaction-chip-main';
            var spanE = document.createElement('span');
            spanE.className = 'reaction-chip-emoji';
            spanE.setAttribute('role', 'img');
            spanE.setAttribute('aria-hidden', 'true');
            spanE.textContent = g.emoji;
            var spanC = document.createElement('span');
            spanC.className = 'reaction-chip-count';
            spanC.textContent = String(g.nombre);
            var loading = document.createElement('span');
            loading.className = 'reaction-chip-loading';
            loading.setAttribute('aria-hidden', 'true');
            main.appendChild(spanE);
            main.appendChild(spanC);
            main.appendChild(loading);
            btn.appendChild(main);

            if (isMine) {
                var rem = document.createElement('span');
                rem.className = 'reaction-chip-remove';
                rem.setAttribute('aria-label', 'Retirer la réaction');
                rem.setAttribute('title', 'Retirer la réaction');
                rem.textContent = '\u00D7';
                btn.appendChild(rem);
            }

            track.appendChild(btn);
        });

        if (masquees > 0) {
            var moreBtn = document.createElement('button');
            moreBtn.type = 'button';
            moreBtn.className = 'reactions-more-btn';
            var autrePart = masquees > 1 ? 'autres' : 'autre';
            var typePart = masquees > 1 ? 'types' : 'type';
            moreBtn.setAttribute(
                'aria-label',
                masquees + ' ' + autrePart + ' ' + typePart + ' de réaction'
            );
            var spanPlus = document.createElement('span');
            spanPlus.className = 'reactions-more-icon';
            spanPlus.setAttribute('aria-hidden', 'true');
            spanPlus.textContent = '+';
            var spanCnt = document.createElement('span');
            spanCnt.className = 'reactions-more-count';
            spanCnt.textContent = String(masquees);
            moreBtn.appendChild(spanPlus);
            moreBtn.appendChild(spanCnt);
            track.appendChild(moreBtn);
        }

        var bar = document.createElement('div');
        bar.className = 'reactions-bar';
        if (activiteId) {
            bar.setAttribute('data-activite-id', activiteId);
        }
        bar.appendChild(track);

        if (existingBar) {
            existingBar.replaceWith(bar);
        } else if (stats) {
            stats.insertAdjacentElement('afterend', bar);
        }
    }

    /**
     * Met à jour la barre de réactions d’une carte à partir de l’API (sans recharger la page).
     */
    function syncReactionsBarForActivite(activiteId) {
        return fetch(
            '/api/commentaires/activite/' + encodeURIComponent(String(activiteId)),
            {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            }
        ).then(function (r) {
            if (!r.ok) {
                throw new Error('commentaires');
            }
            return r.json();
        }).then(function (commentaires) {
            var card = document.querySelector(
                '.post-card[data-activite-id="' + String(activiteId) + '"]'
            );
            if (!card) {
                return;
            }
            renderReactionsBarIntoCard(card, commentaires);
        });
    }

    function supprimerMaReaction(activiteId, commentaireId, utilisateurId, chip) {
        var url =
            '/api/activites/' +
            encodeURIComponent(String(activiteId)) +
            '/reactions/' +
            encodeURIComponent(String(commentaireId)) +
            '?utilisateurId=' +
            encodeURIComponent(String(utilisateurId));
        chip.classList.add('reaction-chip--pending');
        return fetch(url, {
            method: 'DELETE',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        })
            .then(function (r) {
                return r.json().catch(function () {
                    return null;
                }).then(function (data) {
                    return { ok: r.ok, status: r.status, data: data };
                });
            })
            .then(function (result) {
                if (result.data && result.data.success === true) {
                    return syncReactionsBarForActivite(activiteId).catch(function () {
                        showToastErreur(
                            'Réaction supprimée, mais le fil n’a pas pu être mis à jour.'
                        );
                    });
                }
                var msg =
                    result.data && result.data.message
                        ? result.data.message
                        : 'Impossible de retirer la réaction.';
                if (!result.ok && !result.data) {
                    msg =
                        result.status === 401 || result.status === 403
                            ? 'Accès refusé. Reconnectez-vous.'
                            : 'Erreur serveur (' + result.status + ').';
                }
                showToastErreur(msg);
            })
            .catch(function () {
                showToastErreur('Erreur réseau. Vérifiez votre connexion.');
            })
            .finally(function () {
                if (chip && chip.parentNode) {
                    chip.classList.remove('reaction-chip--pending');
                }
            });
    }

    document.addEventListener(
        'click',
        function (e) {
            var removeBtn = e.target.closest('.reaction-chip-remove');
            if (!removeBtn) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            var chip = removeBtn.closest('.reaction-chip');
            if (!chip || !chip.classList.contains('reaction-chip--mine')) {
                return;
            }
            var card = chip.closest('.post-card');
            var activiteId = card && card.getAttribute('data-activite-id');
            var commentaireId = chip.getAttribute('data-commentaire-id');
            var userAttr = document.body.getAttribute('data-current-user-id');
            if (!activiteId || !commentaireId || !userAttr) {
                return;
            }
            var utilisateurId = parseInt(userAttr, 10);
            if (isNaN(utilisateurId)) {
                return;
            }
            supprimerMaReaction(activiteId, commentaireId, utilisateurId, chip);
        },
        true
    );

    function setPlusButtonLoading(btn, loading) {
        if (!btn) {
            return;
        }
        if (loading) {
            btn.classList.add('post-actions-plus-btn--loading');
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');
        } else {
            btn.classList.remove('post-actions-plus-btn--loading');
            btn.disabled = false;
            btn.removeAttribute('aria-busy');
        }
    }

    function posterReaction(activiteId, emojiNatif, plusBtn) {
        var auteurAttr = document.body.getAttribute('data-current-user-id');
        if (!auteurAttr) {
            showToastErreur('Impossible d’identifier l’utilisateur. Rechargez la page.');
            return;
        }
        var auteurId = parseInt(auteurAttr, 10);
        if (isNaN(auteurId)) {
            showToastErreur('Session invalide.');
            return;
        }
        setPlusButtonLoading(plusBtn, true);
        fetch('/api/activites/' + encodeURIComponent(String(activiteId)) + '/reactions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({ auteurId: auteurId, emoji: emojiNatif })
        })
            .then(function (r) {
                return r.json().catch(function () {
                    return null;
                }).then(function (data) {
                    return { ok: r.ok, status: r.status, data: data };
                });
            })
            .then(function (result) {
                if (result.data && result.data.success === true) {
                    return syncReactionsBarForActivite(activiteId).catch(function () {
                        showToastErreur(
                            'Réaction enregistrée, mais le fil n’a pas pu être mis à jour.'
                        );
                    });
                }
                var msg =
                    result.data && result.data.message
                        ? result.data.message
                        : 'Impossible d’enregistrer la réaction.';
                if (!result.ok && !result.data) {
                    msg =
                        result.status === 401 || result.status === 403
                            ? 'Accès refusé. Reconnectez-vous.'
                            : 'Erreur serveur (' + result.status + ').';
                }
                showToastErreur(msg);
            })
            .catch(function () {
                showToastErreur('Erreur réseau. Vérifiez votre connexion.');
            })
            .finally(function () {
                setPlusButtonLoading(plusBtn, false);
            });
    }

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
                        var plusBtn = emojiPickerAnchor;
                        var card =
                            plusBtn && plusBtn.closest('.post-card');
                        var activiteId =
                            card && card.getAttribute('data-activite-id');
                        var natif = extraireEmojiNatif(emoji);
                        hideEmojiPicker();
                        if (!activiteId || !natif) {
                            return;
                        }
                        posterReaction(activiteId, natif, plusBtn);
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

    function urlPhotoProfilAuteur(auteur) {
        if (!auteur) {
            return '/images/profile-placeholder.svg';
        }
        var p = auteur.photoProfil;
        if (p != null && String(p).trim() !== '') {
            return String(p).trim();
        }
        return '/images/profile-placeholder.svg';
    }

    function ensureCommentsSection(card) {
        var section = card.querySelector('.comments-section');
        if (section) {
            return section;
        }
        var composer = card.querySelector('.comment-composer');
        if (!composer) {
            return null;
        }
        section = document.createElement('div');
        section.className = 'comments-section';
        composer.insertAdjacentElement('afterend', section);
        return section;
    }

    function buildCommentElement(c) {
        var auteur = c.auteur || {};
        var wrap = document.createElement('div');
        wrap.className = 'comment';
        var img = document.createElement('img');
        img.className = 'comment-avatar';
        img.src = urlPhotoProfilAuteur(auteur);
        img.alt = auteur.prenom || '';
        var bubble = document.createElement('div');
        bubble.className = 'comment-bubble';
        var strong = document.createElement('strong');
        strong.textContent = ((auteur.prenom || '') + ' ' + (auteur.nom || '')).trim();
        var span = document.createElement('span');
        span.textContent = c.message || '';
        bubble.appendChild(strong);
        bubble.appendChild(document.createTextNode(' '));
        bubble.appendChild(span);
        wrap.appendChild(img);
        wrap.appendChild(bubble);
        return wrap;
    }

    function updateFeedCommentCount(card, delta) {
        var n = parseInt(card.getAttribute('data-feed-comment-count') || '0', 10);
        n = Math.max(0, n + delta);
        card.setAttribute('data-feed-comment-count', String(n));
        var el = card.querySelector('.js-feed-comment-count');
        if (el) {
            el.textContent = n + ' commentaire' + (n !== 1 ? 's' : '');
        }
    }

    function setCommentComposerLoading(btn, input, loading) {
        if (!btn) {
            return;
        }
        if (loading) {
            btn.classList.add('comment-composer-post--loading');
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');
        } else {
            btn.classList.remove('comment-composer-post--loading');
            btn.disabled = false;
            btn.removeAttribute('aria-busy');
        }
        if (input) {
            input.disabled = !!loading;
        }
    }

    function posterCommentaireTexte(activiteId, card, composer, btn, input) {
        var message = (input.value || '').trim();
        if (!message) {
            showToastErreur('Le message ne peut pas être vide.');
            return;
        }
        var auteurAttr = document.body.getAttribute('data-current-user-id');
        if (!auteurAttr) {
            showToastErreur('Impossible d’identifier l’utilisateur. Rechargez la page.');
            return;
        }
        var auteurId = parseInt(auteurAttr, 10);
        if (isNaN(auteurId)) {
            showToastErreur('Session invalide.');
            return;
        }
        setCommentComposerLoading(btn, input, true);
        fetch(
            '/api/activites/' + encodeURIComponent(String(activiteId)) + '/commentaires',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify({ auteurId: auteurId, message: message })
            }
        )
            .then(function (r) {
                return r.json().catch(function () {
                    return null;
                }).then(function (data) {
                    return { ok: r.ok, status: r.status, data: data };
                });
            })
            .then(function (result) {
                if (result.data && result.data.success === true && result.data.commentaire) {
                    var section = ensureCommentsSection(card);
                    if (section) {
                        section.appendChild(buildCommentElement(result.data.commentaire));
                    }
                    updateFeedCommentCount(card, 1);
                    input.value = '';
                    return;
                }
                var msg =
                    result.data && result.data.message
                        ? result.data.message
                        : 'Impossible de publier le commentaire.';
                if (!result.ok && !result.data) {
                    msg =
                        result.status === 401 || result.status === 403
                            ? 'Accès refusé. Reconnectez-vous.'
                            : 'Erreur serveur (' + result.status + ').';
                }
                showToastErreur(msg);
            })
            .catch(function () {
                showToastErreur('Erreur réseau. Vérifiez votre connexion.');
            })
            .finally(function () {
                setCommentComposerLoading(btn, input, false);
            });
    }

    document.addEventListener('click', function (e) {
        var postBtn = e.target.closest('.comment-composer-post');
        if (!postBtn) {
            return;
        }
        e.preventDefault();
        var composer = postBtn.closest('.comment-composer');
        if (!composer || composer.hidden) {
            return;
        }
        var card = composer.closest('.post-card');
        var activiteId = card && card.getAttribute('data-activite-id');
        var input = composer.querySelector('.comment-composer-input');
        if (!activiteId || !input) {
            return;
        }
        posterCommentaireTexte(activiteId, card, composer, postBtn, input);
    });

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

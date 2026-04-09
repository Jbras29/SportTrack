// Messages : liste avec panneau chargé en XHR (pas de navigation pleine page)
(function() {
    'use strict';

    var PLACEHOLDER_HTML =
        '<div class="messages-content messages-content--placeholder" id="messagesPanel">' +
        '<div class="empty-chat-state">' +
        '<div class="empty-icon">👋</div>' +
        '<h3>Sélectionnez une conversation</h3>' +
        '<p>Cliquez sur une conversation pour commencer à discuter</p>' +
        '</div></div>';

    function isMessagesListPage() {
        return document.body.classList.contains('messages-list-page');
    }

    function panelUrl(destinataireId) {
        return '/messages/api/conversation-panel/' + destinataireId;
    }

    function scrollChatToBottom() {
        var el = document.getElementById('chatMessages');
        if (el) {
            el.scrollTop = el.scrollHeight;
        }
    }

    function setActiveConversation(destinataireId) {
        document.querySelectorAll('.conversation-item.active').forEach(function(el) {
            el.classList.remove('active');
        });
        document.querySelectorAll('.js-conversation-trigger[data-destinataire-id]').forEach(function(link) {
            if (String(link.getAttribute('data-destinataire-id')) === String(destinataireId)) {
                var item = link.querySelector('.conversation-item');
                if (item) {
                    item.classList.add('active');
                }
            }
        });
    }

    function bindTextareaBehaviors(root) {
        var ta = root.querySelector('textarea[name="contenu"]');
        if (!ta) {
            return;
        }
        ta.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';
        });
        ta.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                var form = this.closest('form');
                if (form && this.value.trim()) {
                    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
                }
            }
        });
    }

    function loadConversation(destinataireId, options) {
        options = options || {};
        var pushState = options.pushState !== false;
        var mount = document.getElementById('messagesPanelMount');
        if (!mount) {
            return Promise.resolve();
        }
        mount.setAttribute('aria-busy', 'true');
        return fetch(panelUrl(destinataireId), {
            credentials: 'same-origin',
            headers: { Accept: 'text/html' }
        })
            .then(function(res) {
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return res.text();
            })
            .then(function(html) {
                mount.innerHTML = html;
                scrollChatToBottom();
                bindTextareaBehaviors(mount);
                if (pushState) {
                    history.pushState({ open: destinataireId }, '', '/messages?open=' + encodeURIComponent(destinataireId));
                }
                setActiveConversation(destinataireId);
            })
            .catch(function(err) {
                console.error(err);
                window.alert('Impossible de charger la conversation.');
            })
            .finally(function() {
                mount.removeAttribute('aria-busy');
            });
    }

    function clearPanel() {
        var mount = document.getElementById('messagesPanelMount');
        if (mount) {
            mount.innerHTML = PLACEHOLDER_HTML;
        }
        document.querySelectorAll('.conversation-item.active').forEach(function(el) {
            el.classList.remove('active');
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var mount = document.getElementById('messagesPanelMount');
        if (mount && document.getElementById('messageSendForm')) {
            bindTextareaBehaviors(mount);
        }

        document.addEventListener('click', function(e) {
            var a = e.target.closest('.js-conversation-trigger');
            if (!a || !isMessagesListPage()) {
                return;
            }
            e.preventDefault();
            var id = a.getAttribute('data-destinataire-id');
            if (!id) {
                return;
            }
            var dialog = document.getElementById('friendPickerDialog');
            if (dialog && dialog.open) {
                dialog.close();
            }
            loadConversation(id);
        }, true);

        document.addEventListener('submit', function(e) {
            var form = e.target;
            if (form.id !== 'messageSendForm' || !form.closest('#messagesPanelMount') || !isMessagesListPage()) {
                return;
            }
            e.preventDefault();
            var fd = new FormData(form);
            var destinataireId = fd.get('destinataireId');
            var contenu = fd.get('contenu');
            if (!contenu || !String(contenu).trim()) {
                return;
            }
            fetch('/messages/api/envoyer', {
                method: 'POST',
                body: fd,
                credentials: 'same-origin'
            })
                .then(function(res) {
                    if (!res.ok) {
                        return res.text().then(function(t) {
                            throw new Error(t || 'Erreur envoi');
                        });
                    }
                    var ta = form.querySelector('textarea[name="contenu"]');
                    if (ta) {
                        ta.value = '';
                        ta.style.height = 'auto';
                    }
                    return loadConversation(destinataireId, { pushState: false });
                })
                .catch(function(err) {
                    console.error(err);
                    window.alert('Envoi du message impossible.');
                });
        });

        window.addEventListener('popstate', function() {
            if (!isMessagesListPage()) {
                return;
            }
            var params = new URLSearchParams(window.location.search);
            var open = params.get('open');
            if (open) {
                loadConversation(open, { pushState: false });
            } else {
                clearPanel();
            }
        });

        var friendPickerDialog = document.getElementById('friendPickerDialog');
        var openFriendPickers = document.querySelectorAll('.open-friend-picker');
        var closeFriendPicker = document.getElementById('closeFriendPicker');
        if (friendPickerDialog && openFriendPickers.length) {
            openFriendPickers.forEach(function(btn) {
                btn.addEventListener('click', function() {
                    friendPickerDialog.showModal();
                });
            });
            if (closeFriendPicker) {
                closeFriendPicker.addEventListener('click', function() {
                    friendPickerDialog.close();
                });
            }
            friendPickerDialog.addEventListener('click', function(e) {
                if (e.target === friendPickerDialog) {
                    friendPickerDialog.close();
                }
            });
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape' && friendPickerDialog.open) {
                    friendPickerDialog.close();
                }
            });
        }
    });
})();

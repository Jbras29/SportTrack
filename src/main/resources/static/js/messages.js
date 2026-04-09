// Messages JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Auto-resize textarea
    const messageTextarea = document.querySelector('textarea[name="contenu"]');
    if (messageTextarea) {
        messageTextarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';
        });
    }

    // Enter to send message (Shift+Enter for new line)
    if (messageTextarea) {
        messageTextarea.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const form = this.closest('form');
                if (form && this.value.trim()) {
                    form.submit();
                }
            }
        });
    }

    // Scroll to bottom of chat
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // Conversation item click
    const conversationItems = document.querySelectorAll('.conversation-item');
    conversationItems.forEach(item => {
        item.addEventListener('click', function() {
            const link = this.closest('a');
            if (link) {
                window.location.href = link.href;
            }
        });
    });

    // Mark messages as read when viewing conversation
    if (window.location.pathname.includes('/conversation/')) {
        // Messages are marked as read server-side, but we could add client-side feedback here
    }

    const friendPickerDialog = document.getElementById('friendPickerDialog');
    const openFriendPickers = document.querySelectorAll('.open-friend-picker');
    const closeFriendPicker = document.getElementById('closeFriendPicker');
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
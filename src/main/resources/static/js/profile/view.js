// Profile view JavaScript for social network features

document.addEventListener('DOMContentLoaded', function() {
    // Events tabs functionality
    const tabButtons = document.querySelectorAll('.tab-btn');
    const eventsLists = document.querySelectorAll('.events-list');

    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            const tabName = this.getAttribute('data-tab');

            // Remove active class from all tabs
            tabButtons.forEach(btn => btn.classList.remove('active'));

            // Add active class to clicked tab
            this.classList.add('active');

            // Hide all event lists
            eventsLists.forEach(list => list.classList.remove('active'));

            // Show the selected event list
            const activeList = document.querySelector(`.events-list[data-tab="${tabName}"]`);
            if (activeList) {
                activeList.classList.add('active');
            }
        });
    });

    // Add hover effects for interactive elements
    const friendItems = document.querySelectorAll('.friend-item');
    friendItems.forEach(item => {
        item.addEventListener('mouseenter', function() {
            this.style.transform = 'scale(1.05)';
        });

        item.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1)';
        });
    });

    // Activity posts hover effect
    const activityPosts = document.querySelectorAll('.activity-post');
    activityPosts.forEach(post => {
        post.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
        });

        post.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    });
});
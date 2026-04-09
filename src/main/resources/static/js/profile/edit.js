(function () {
    var btn = document.querySelector(".edit-avatar-btn");
    var input = document.getElementById("input-photo-profil");
    var form = document.getElementById("form-photo-profil");
    if (!btn || !input || !form) {
        return;
    }

    btn.addEventListener("click", function (e) {
        e.preventDefault();
        input.click();
    });

    btn.addEventListener("keydown", function (e) {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            input.click();
        }
    });

    input.addEventListener("change", function () {
        if (input.files && input.files.length) {
            form.submit();
        }
    });
})();

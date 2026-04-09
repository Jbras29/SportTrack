(function () {
    var registerButton = document.getElementById("go-register");
    if (!registerButton) {
        return;
    }

    registerButton.addEventListener("click", function () {
        window.location.href = "/register";
    });
})();

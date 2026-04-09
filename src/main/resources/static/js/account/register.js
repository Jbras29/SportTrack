(function () {
    var form = document.getElementById("register-form");
    if (!form) {
        return;
    }

    form.addEventListener("submit", function (e) {
        var password = document.getElementById("motdepasse").value;
        if (password.length < 8) {
            alert("Le mot de passe doit contenir au moins 8 caractères.");
            e.preventDefault();
            return;
        }

        var age = document.getElementById("age").value;
        if (age && (age < 1 || age > 120)) {
            alert("Veuillez entrer un âge valide.");
            e.preventDefault();
        }
    });
})();

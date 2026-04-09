(function () {
    var input = document.getElementById("date");
    if (!input) {
        return;
    }

    function todayLocalIso() {
        var d = new Date();
        var m = String(d.getMonth() + 1).padStart(2, "0");
        var day = String(d.getDate()).padStart(2, "0");
        return d.getFullYear() + "-" + m + "-" + day;
    }

    input.max = todayLocalIso();

    function syncValidity() {
        var max = input.max;
        if (input.value && max && input.value > max) {
            input.setCustomValidity("La date ne peut pas être postérieure à aujourd'hui.");
        } else {
            input.setCustomValidity("");
        }
    }

    input.addEventListener("input", syncValidity);
    input.addEventListener("change", syncValidity);

    var form = input.closest("form");
    if (form) {
        form.addEventListener("submit", function (e) {
            syncValidity();
            if (!input.checkValidity()) {
                e.preventDefault();
                input.reportValidity();
            }
        });
    }
})();

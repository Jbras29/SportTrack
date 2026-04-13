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

// ── Prévisualisation météo en temps réel ─────────────────────────────────────
(function () {
    var locationInput  = document.getElementById("location");
    var dateInput      = document.getElementById("date");
    var previewBlock   = document.getElementById("meteo-preview-block");
    var loadingEl      = document.getElementById("meteo-loading");
    var iconEl         = document.getElementById("meteo-preview-icon");
    var conditionEl    = document.getElementById("meteo-preview-condition");
    var tempEl         = document.getElementById("meteo-preview-temp");

    if (!locationInput || !dateInput || !previewBlock) return;

    var debounceTimer = null;

    var ICONS = {
        "Ensoleillé":        "☀️",
        "Nuageux":           "☁️",
        "Pluie":             "🌧️",
        "Averses de pluie":  "🌧️",
        "Bruine":            "🌦️",
        "Neige":             "❄️",
        "Averses de neige":  "🌨️",
        "Orage":             "⛈️",
        "Brouillard":        "🌫️",
    };

    function hidePreview() {
        previewBlock.classList.remove("visible");
        loadingEl.classList.remove("visible");
    }

    function showLoading() {
        previewBlock.classList.remove("visible");
        loadingEl.classList.add("visible");
    }

    function showMeteo(data) {
        loadingEl.classList.remove("visible");
        if (data && data.ok) {
            iconEl.textContent      = ICONS[data.condition] || "🌡️";
            conditionEl.textContent = data.condition;
            tempEl.textContent      = "Température moyenne : " + data.temperature + "°C";
            previewBlock.classList.add("visible");
        } else {
            previewBlock.classList.remove("visible");
        }
    }

    function fetchMeteo() {
        var loc  = locationInput.value.trim();
        var date = dateInput.value;
        if (!loc || !date) { hidePreview(); return; }

        showLoading();
        fetch("/activites/api/meteo-preview?location=" + encodeURIComponent(loc) + "&date=" + encodeURIComponent(date))
            .then(function (r) { return r.json(); })
            .then(showMeteo)
            .catch(function () { hidePreview(); });
    }

    function scheduleMeteo() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(fetchMeteo, 700); // attend 700 ms après la frappe
    }

    locationInput.addEventListener("input",  scheduleMeteo);
    locationInput.addEventListener("change", scheduleMeteo);
    dateInput.addEventListener("change", scheduleMeteo);
})();


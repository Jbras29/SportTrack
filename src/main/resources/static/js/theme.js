(function () {
    var STORAGE_KEY = "sporttrack-theme";

    try {
        var stored = localStorage.getItem(STORAGE_KEY);
        if (stored === "dark" || stored === "light") {
            document.documentElement.setAttribute("data-theme", stored);
        }
    } catch (e) { /* ignore */ }

    function effectiveDark() {
        var t = document.documentElement.getAttribute("data-theme");
        if (t === "dark") {
            return true;
        }
        if (t === "light") {
            return false;
        }
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    }

    function setTheme(mode) {
        if (mode !== "dark" && mode !== "light") {
            return;
        }
        document.documentElement.setAttribute("data-theme", mode);
        try {
            localStorage.setItem(STORAGE_KEY, mode);
        } catch (e) {
            /* ignore */
        }
        window.dispatchEvent(new CustomEvent("sporttrack-theme-change"));
    }

    function toggle() {
        setTheme(effectiveDark() ? "light" : "dark");
        updateToggleButton();
    }

    function updateToggleButton() {
        var btn = document.getElementById("theme-toggle");
        if (!btn) {
            return;
        }
        var dark = effectiveDark();
        btn.classList.toggle("theme-toggle--dark", dark);
        btn.setAttribute("aria-pressed", dark ? "true" : "false");
        btn.setAttribute("aria-label", dark ? "Passer en mode clair" : "Passer en mode sombre");
        btn.title = dark ? "Mode clair" : "Mode sombre";
    }

    document.addEventListener("DOMContentLoaded", function () {
        var btn = document.getElementById("theme-toggle");
        if (btn) {
            btn.addEventListener("click", toggle);
            updateToggleButton();
        }

        if (window.matchMedia) {
            var mq = window.matchMedia("(prefers-color-scheme: dark)");
            var onSystemChange = function () {
                if (!document.documentElement.hasAttribute("data-theme")) {
                    updateToggleButton();
                    window.dispatchEvent(new CustomEvent("sporttrack-theme-change"));
                }
            };
            if (typeof mq.addEventListener === "function") {
                mq.addEventListener("change", onSystemChange);
            } else if (typeof mq.addListener === "function") {
                mq.addListener(onSystemChange);
            }
        }

        window.addEventListener("storage", function (ev) {
            if (ev.key === STORAGE_KEY && (ev.newValue === "dark" || ev.newValue === "light")) {
                document.documentElement.setAttribute("data-theme", ev.newValue);
                updateToggleButton();
                window.dispatchEvent(new CustomEvent("sporttrack-theme-change"));
            }
        });
    });
})();

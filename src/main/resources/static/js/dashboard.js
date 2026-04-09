(function () {
    var weekData = window.dashboardWeekData;
    if (!Array.isArray(weekData)) {
        weekData = [];
    }

    var shell = document.getElementById("week-chart-shell");
    var canvas = document.getElementById("week-activity-chart");
    var emptyEl = document.getElementById("week-chart-empty");
    var legend = document.getElementById("week-chart-legend");
    if (!shell || !canvas || !emptyEl) {
        return;
    }

    var totalMin = weekData.reduce(function (s, d) {
        return s + (Number(d.minutes) || 0);
    }, 0);

    if (totalMin === 0) {
        shell.classList.add("is-empty");
        emptyEl.classList.add("is-visible");
        return;
    }

    legend.hidden = false;

    var COLORS = {
        blue: "#135077",
        light: "#21A2D7",
        orange: "#F39434",
        grid: "rgba(100, 116, 139, 0.15)",
        text: "#64748B"
    };

    var ctx = canvas.getContext("2d");
    var animProgress = 0;
    var hoverIndex = -1;

    function layoutSize() {
        var dpr = window.devicePixelRatio || 1;
        var rect = canvas.getBoundingClientRect();
        var w = Math.max(320, rect.width);
        var h = 200;
        canvas.width = Math.floor(w * dpr);
        canvas.height = Math.floor(h * dpr);
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        return { w: w, h: h };
    }

    function draw() {
        var size = layoutSize();
        var w = size.w;
        var h = size.h;
        ctx.clearRect(0, 0, w, h);

        var pad = { l: 36, r: 12, t: 16, b: 36 };
        var chartW = w - pad.l - pad.r;
        var chartH = h - pad.t - pad.b;
        var vals = weekData.map(function (d) {
            return Number(d.minutes) || 0;
        });
        var maxVal = Math.max.apply(null, vals);
        maxVal = Math.max(maxVal, 10);
        maxVal = Math.ceil(maxVal * 1.05);

        for (var g = 0; g <= 4; g++) {
            var gy = pad.t + (chartH * g) / 4;
            ctx.beginPath();
            ctx.strokeStyle = COLORS.grid;
            ctx.lineWidth = 1;
            ctx.moveTo(pad.l, gy);
            ctx.lineTo(pad.l + chartW, gy);
            ctx.stroke();
            var labelVal = Math.round(maxVal * (1 - g / 4));
            ctx.fillStyle = COLORS.text;
            ctx.font = "10px Inter, sans-serif";
            ctx.textAlign = "right";
            ctx.fillText(String(labelVal), pad.l - 6, gy + 3);
        }

        var n = weekData.length;
        var gap = Math.min(14, chartW / (n * 3));
        var barW = (chartW - gap * (n + 1)) / n;

        for (var i = 0; i < n; i++) {
            var minutes = Number(weekData[i].minutes) || 0;
            var label = weekData[i].label || "";
            var targetH = chartH * (minutes / maxVal);
            var barH = targetH * animProgress;
            var x = pad.l + gap + i * (barW + gap);
            var radius = Math.min(8, barW / 2);
            var isHover = hoverIndex === i;
            var scale = isHover ? 1.04 : 1;
            var bx = x + (barW * (1 - scale)) / 2;
            var bw = barW * scale;
            var by = pad.t + chartH - barH * scale;
            var bh = barH * scale;
            var r = Math.min(radius, bw / 2);

            if (bh > 0) {
                var grd = ctx.createLinearGradient(bx, by, bx, pad.t + chartH);
                grd.addColorStop(0, COLORS.light);
                grd.addColorStop(0.55, COLORS.blue);
                grd.addColorStop(1, COLORS.orange);
                ctx.fillStyle = grd;
                ctx.shadowColor = "rgba(19, 80, 119, 0.25)";
                ctx.shadowBlur = isHover ? 14 : 8;
                ctx.shadowOffsetY = 4;
                ctx.beginPath();
                ctx.moveTo(bx + r, by);
                ctx.lineTo(bx + bw - r, by);
                ctx.quadraticCurveTo(bx + bw, by, bx + bw, by + r);
                ctx.lineTo(bx + bw, pad.t + chartH);
                ctx.lineTo(bx, pad.t + chartH);
                ctx.lineTo(bx, by + r);
                ctx.quadraticCurveTo(bx, by, bx + r, by);
                ctx.closePath();
                ctx.fill();
                ctx.shadowBlur = 0;
                ctx.shadowOffsetY = 0;
            }

            ctx.fillStyle = COLORS.text;
            ctx.font = "600 11px Inter, sans-serif";
            ctx.textAlign = "center";
            ctx.fillText(label, x + barW / 2, pad.t + chartH + 18);

            if (minutes > 0 && bh > 12) {
                ctx.fillStyle = "rgba(255,255,255,0.92)";
                ctx.font = "700 10px Inter, sans-serif";
                ctx.fillText(String(minutes), bx + bw / 2, by + bh / 2 + 4);
            }
        }
    }

    function animate() {
        animProgress += 0.08;
        if (animProgress > 1) {
            animProgress = 1;
        }
        draw();
        if (animProgress < 1) {
            requestAnimationFrame(animate);
        }
    }

    canvas.addEventListener("mousemove", function (ev) {
        var rect = canvas.getBoundingClientRect();
        var mx = ev.clientX - rect.left;
        var size = { w: rect.width, h: rect.height };
        var pad = { l: 36, r: 12, t: 16, b: 36 };
        var chartW = size.w - pad.l - pad.r;
        var n = weekData.length;
        var gap = Math.min(14, chartW / (n * 3));
        var barW = (chartW - gap * (n + 1)) / n;
        var idx = -1;

        for (var i = 0; i < n; i++) {
            var x = pad.l + gap + i * (barW + gap);
            if (mx >= x && mx <= x + barW) {
                idx = i;
                break;
            }
        }

        if (idx !== hoverIndex) {
            hoverIndex = idx;
            canvas.title = idx >= 0
                ? weekData[idx].label + " : " + (Number(weekData[idx].minutes) || 0) + " min"
                : "";
            draw();
        }
    });

    canvas.addEventListener("mouseleave", function () {
        hoverIndex = -1;
        canvas.title = "";
        draw();
    });

    window.addEventListener("resize", function () {
        animProgress = 1;
        draw();
    });

    requestAnimationFrame(animate);
})();

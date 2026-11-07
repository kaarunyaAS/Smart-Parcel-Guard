const API_BASE_URL = "http://localhost:8081";
const state = { token: sessionStorage.getItem("spg.token"), user: null, view: "dashboard", parcelId: null, map: null, layer: null, evidenceUrls: [] };
const parcelStatuses = ["REGISTERED", "READY_FOR_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELAYED", "UNDER_INVESTIGATION", "OUT_FOR_DELIVERY", "DELIVERED", "RETURNED"];
const deviceStatuses = ["OFFLINE", "ONLINE", "WARNING", "MAINTENANCE"];
const riskSettingFields = [
    ["impactThresholdG", "Impact threshold (g)", "number", "0", "100", "0.01"],
    ["vibrationThresholdG", "Vibration threshold (g)", "number", "0", "100", "0.01"],
    ["impactMinDurationMs", "Minimum impact duration (ms)", "number", "1", "60000", "1"],
    ["deviceOfflineAfterSeconds", "Device offline timeout (s)", "number", "1", "86400", "1"],
    ["riskDedupeWindowSeconds", "Risk deduplication window (s)", "number", "1", "86400", "1"],
    ["riskStrongImpactPoints", "Strong impact points", "number", "0", "100", "1"],
    ["riskUnauthorizedOpeningPoints", "Unauthorized opening points", "number", "0", "100", "1"],
    ["riskUnusualVibrationPoints", "Repeated vibration points", "number", "0", "100", "1"],
    ["riskRouteDeviationPoints", "GPS anomaly points", "number", "0", "100", "1"],
    ["riskDeviceOfflinePoints", "Device offline points", "number", "0", "100", "1"],
    ["riskCorrelationBonusPoints", "Correlation bonus", "number", "0", "100", "1"],
    ["riskMaxScore", "Maximum risk score", "number", "1", "100", "1"],
    ["riskCorrelationWindowSeconds", "Correlation window (s)", "number", "1", "86400", "1"],
    ["riskVibrationRepeatCount", "Vibrations required", "number", "1", "100", "1"],
    ["riskLevelMedium", "MEDIUM starts at", "number", "1", "100", "1"],
    ["riskLevelHigh", "HIGH starts at", "number", "2", "100", "1"],
    ["riskLevelCritical", "CRITICAL starts at", "number", "3", "100", "1"],
    ["gpsAnomalyMinimumJumpMeters", "Minimum GPS jump (m)", "number", "0.01", "1000000", "0.01"],
    ["gpsAnomalyMaximumPlausibleSpeedMps", "Maximum GPS speed (m/s)", "number", "0.01", "1000", "0.01"]
];
const operatorStatusTransitions = {
    REGISTERED: ["READY_FOR_PICKUP"],
    READY_FOR_PICKUP: ["PICKED_UP"],
    PICKED_UP: ["IN_TRANSIT"],
    IN_TRANSIT: ["DELAYED", "UNDER_INVESTIGATION", "OUT_FOR_DELIVERY"],
    DELAYED: ["IN_TRANSIT", "UNDER_INVESTIGATION"],
    UNDER_INVESTIGATION: ["IN_TRANSIT", "RETURNED"],
    OUT_FOR_DELIVERY: ["DELIVERED", "RETURNED"]
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) headers.set("Authorization", "Bearer " + state.token);
    if (options.json) {
        headers.set("Content-Type", "application/json");
        options.body = JSON.stringify(options.json);
    }
    const response = await fetch(new URL(path, API_BASE_URL), { ...options, headers });
    if (response.status === 401 && path !== "/api/auth/login") {
        logout();
        throw new Error("Sign in again");
    }
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : null;
    if (!response.ok) {
        const error = new Error(body?.message || "Request failed");
        error.status = response.status;
        throw error;
    }
    return body;
}

function showError(error) {
    $("page-error").hidden = false;
    $("page-error").textContent = error.message || "Something went wrong";
}

function setLoading(loading) {
    $("page-loading").hidden = !loading;
}

$("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    $("login-error").hidden = true;
    const form = new FormData(event.target);
    try {
        const result = await api("/api/auth/login", { method: "POST", json: { username: form.get("username"), password: form.get("password") } });
        state.token = result.data.accessToken;
        state.user = result.data.user;
        await openApp();
        $("login-form").elements.password.value = "";
    } catch (error) {
        logout();
        $("login-error").hidden = false;
        $("login-error").textContent = error.message;
    }
});

$("logout").addEventListener("click", logout);
document.querySelectorAll(".nav button[data-view]").forEach((button) => {
    button.addEventListener("click", () => show(button.dataset.view));
});
$("back-parcels").addEventListener("click", () => show("parcels"));
$("parcel-search").addEventListener("click", loadParcels);
$("parcel-status-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await api("/api/parcels/" + state.parcelId + "/status", {
            method: "PATCH",
            json: { status: form.get("status"), note: form.get("note") || null }
        });
        await loadDetail();
    } catch (error) { showError(error); }
});

function logout() {
    sessionStorage.removeItem("spg.token");
    state.token = null;
    state.user = null;
    state.view = "dashboard";
    if (state.socket) {
        state.socket.close();
        state.socket = null;
    }
    state.evidenceUrls.forEach(URL.revokeObjectURL);
    state.evidenceUrls = [];
    $("app").hidden = true;
    $("login-view").hidden = false;
    $("login-form").elements.password.value = "";
}

async function openApp() {
    if (!state.token) throw new Error("Sign in to continue.");

    let payload;
    try {
        const encodedPayload = state.token.split(".")[1];
        if (!encodedPayload) throw new Error("Missing token payload");
        const base64 = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
        payload = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "=")));
    } catch {
        throw new Error("Stored session is invalid. Sign in again.");
    }

    const role = state.user?.role || payload.role;
    const username = state.user?.username || payload.sub;
    if (!username || !["ADMIN", "OPERATOR"].includes(role) ||
        payload.sub !== username || payload.role !== role ||
        !Number.isFinite(payload.exp) || payload.exp * 1000 <= Date.now()) {
        throw new Error("Stored session is invalid or expired. Sign in again.");
    }

    state.user = { username, role };
    const dashboard = await api("/api/dashboard");
    sessionStorage.setItem("spg.token", state.token);
    $("login-view").hidden = true;
    $("app").hidden = false;
    $("role-label").textContent = state.user.role + " · " + state.user.username;
    document.querySelectorAll("[data-admin]").forEach((item) => item.hidden = state.user.role !== "ADMIN");
    buildForms();
    connectLive();
    show("dashboard", dashboard.data);
}

function show(view, dashboardData = null) {
    const adminOnlyViews = new Set(["register", "devices", "users", "risk-settings"]);
    const titles = { dashboard: "Dashboard", parcels: "Parcels", register: "Register parcel", detail: "Parcel detail", alerts: "Alerts", devices: "Devices", users: "Users", "risk-settings": "Risk settings" };
    if (!state.token || !state.user || !Object.hasOwn(titles, view)) {
        return;
    }
    if (adminOnlyViews.has(view) && state.user.role !== "ADMIN") {
        show("dashboard");
        return;
    }
    state.view = view;
    document.querySelectorAll(".view").forEach((section) => section.hidden = true);
    document.querySelectorAll(".nav button").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
    $("view-title").textContent = titles[view];
    $(view === "detail" ? "view-detail" : "view-" + view).hidden = false;
    $("page-error").hidden = true;
    if (view === "dashboard") loadDashboard(dashboardData);
    if (view === "parcels") loadParcels();
    if (view === "register") loadRegistrationOptions();
    if (view === "alerts") loadAlerts();
    if (view === "devices") loadDevices();
    if (view === "users") loadUsers();
    if (view === "risk-settings") loadRiskSettings();
    if (view === "detail") loadDetail();
}

async function loadDashboard(initialData = null) {
    setLoading(true);
    try {
        const data = initialData || (await api("/api/dashboard")).data;
        const cards = [["Parcels", data.totalParcels], ["In transit", data.inTransit], ["Delivered", data.delivered], ["Delayed", data.delayed], ["High risk", data.highRisk], ["Devices online", data.devicesOnline], ["Devices offline", data.devicesOffline]];
        $("stats").innerHTML = cards.map(([label, value]) => `<div class="stat"><span>${label}</span><strong>${value}</strong></div>`).join("");
        $("dash-alerts").innerHTML = data.recentAlerts.length ? data.recentAlerts.map((alert) => alertCard(alert)).join("") : `<p class="muted">No alerts stored.</p>`;
        $("dash-activity").innerHTML = data.recentActivities.length ? data.recentActivities.map((item) => `<div class="row"><span>${escapeHTML(item.parcelCode)}<br><span class="muted">${escapeHTML(item.status)}</span></span><span class="muted">${formatTime(item.changedAt)}</span></div>`).join("") : `<p class="muted">No activity stored.</p>`;
        const max = Math.max(1, ...Object.values(data.riskDistribution));
        $("risk-bars").innerHTML = Object.entries(data.riskDistribution).map(([level, count]) => `<div><span>${escapeHTML(level)} · ${count}</span><div class="bar"><span style="width:${count / max * 100}%"></span></div></div>`).join("");
    } catch (error) { showError(error); } finally { setLoading(false); }
}

async function loadParcels() {
    setLoading(true);
    try {
        const query = $("parcel-query").value.trim();
        const data = (await api("/api/parcels?size=50" + (query ? "&query=" + encodeURIComponent(query) : ""))).data;
        $("parcel-list").innerHTML = data.content.length ? data.content.map((parcel) => `<button class="card" data-id="${parcel.id}"><strong>${escapeHTML(parcel.parcelCode)}</strong> ${escapeHTML(parcel.name)}<br><span class="badge ${escapeHTML(parcel.latestRiskLevel || "LOW")}">${escapeHTML(parcel.latestRiskLevel || "UNSCORED")} ${escapeHTML(parcel.latestRiskScore ?? "")}</span> <span class="muted">${escapeHTML(parcel.status)}</span></button>`).join("") : `<p class="muted">No parcels found.</p>`;
        $("parcel-list").querySelectorAll("[data-id]").forEach((button) => button.addEventListener("click", () => { state.parcelId = button.dataset.id; show("detail"); }));
    } catch (error) { showError(error); } finally { setLoading(false); }
}

async function loadRegistrationOptions() {
    try {
        const [usersPage, devicesPage] = await Promise.all([
            api("/api/users?size=100&sort=username,asc"),
            api("/api/devices?size=100&sort=deviceCode,asc")
        ]);
        const operators = usersPage.data.content.filter((user) => user.role === "OPERATOR" && user.enabled);
        const devices = devicesPage.data.content.filter((device) => device.enabled);
        $("parcel-form").elements.operatorId.innerHTML = `<option value="">Unassigned</option>` +
            operators.map((user) => `<option value="${user.id}">${escapeHTML(user.fullName)} (${escapeHTML(user.username)})</option>`).join("");
        $("parcel-form").elements.deviceId.innerHTML = `<option value="">No device</option>` +
            devices.map((device) => `<option value="${device.id}">${escapeHTML(device.deviceCode)}${device.assignedParcelCode ? ` · assigned to ${escapeHTML(device.assignedParcelCode)}` : ""}</option>`).join("");
    } catch (error) { showError(error); }
}

async function loadDetail() {
    if (!state.parcelId) return;
    setLoading(true);
    try {
        const parcel = (await api("/api/parcels/" + state.parcelId)).data;
        const risk = await api("/api/parcels/" + state.parcelId + "/risk").then((body) => body.data).catch((error) => {
            if (error.status === 404) return null;
            throw error;
        });
        const locations = (await api("/api/parcels/" + state.parcelId + "/locations")).data;
        const sensors = (await api("/api/parcels/" + state.parcelId + "/sensors")).data;
        const evidence = (await api("/api/parcels/" + state.parcelId + "/evidence")).data;
        const history = (await api("/api/parcels/" + state.parcelId + "/history")).data;
        const riskHistory = (await api("/api/parcels/" + state.parcelId + "/risk/history")).data;
        $("detail-summary").innerHTML = [["Code", parcel.parcelCode], ["Status", parcel.status], ["Risk", risk ? risk.level + " " + risk.score : "Unscored"], ["Device", parcel.deviceCode || "None"]].map(([label, value]) => `<div class="stat"><span>${escapeHTML(label)}</span><strong>${escapeHTML(value)}</strong></div>`).join("");
        const statuses = state.user.role === "ADMIN"
            ? parcelStatuses.filter((status) => status !== parcel.status)
            : operatorStatusTransitions[parcel.status] || [];
        const statusForm = $("parcel-status-form");
        statusForm.hidden = statuses.length === 0;
        statusForm.elements.status.innerHTML = `<option value="">Select a status</option>` + statuses.map((status) => `<option value="${status}">${status.replaceAll("_", " ")}</option>`).join("");
        drawMap(locations);
        drawSensors(sensors);
        await drawEvidence(evidence);
        const events = [
            ...history.map((item) => ({ at: item.changedAt, text: item.fromStatus ? `${item.fromStatus} → ${item.toStatus}` : item.toStatus })),
            ...locations.map((item) => ({ at: item.recordedAt, text: `GPS fix · ${item.gpsSignalStatus}` })),
            ...sensors.map((item) => ({ at: item.recordedAt, text: `Sensor · ${item.movementStatus}${item.impactDetected ? " · impact" : ""}${item.unauthorizedOpening ? " · unauthorized opening" : ""}` })),
            ...evidence.map((item) => ({ at: item.capturedAt, text: `${item.cameraRole} camera evidence` })),
            ...riskHistory.map((item) => ({ at: item.calculatedAt, text: `Risk ${item.level} (${item.score})` }))
        ].sort((a, b) => a.at.localeCompare(b.at));
        $("timeline").innerHTML = events.length ? events.map((item) => `<div class="row"><span>${escapeHTML(item.text)}</span><span class="muted">${formatTime(item.at)}</span></div>`).join("") : `<p class="muted">No timeline events.</p>`;
        if (state.socket && state.socket.readyState === 1) state.socket.send(JSON.stringify({ action: "subscribe", parcelId: Number(state.parcelId) }));
    } catch (error) { showError(error); } finally { setLoading(false); }
}

function drawMap(locations) {
    if (!state.map) {
        state.map = L.map("map").setView([20.6, 78.9], 4);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { attribution: "&copy; OpenStreetMap" }).addTo(state.map);
    }
    if (state.layer) state.layer.remove();
    $("map-empty").textContent = locations.length ? "" : "No GPS history stored.";
    if (!locations.length) return;
    const points = locations.map((item) => item.latLng);
    state.layer = L.layerGroup([L.polyline(points, { color: "#d7a15f" }), L.marker(points[points.length - 1])]).addTo(state.map);
    state.map.fitBounds(points, { padding: [20, 20] });
    setTimeout(() => state.map.invalidateSize(), 100);
}

function drawSensors(sensors) {
    const canvas = $("sensor-chart");
    const context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);
    $("sensor-empty").textContent = sensors.length ? "" : "No sensor history stored.";
    if (!sensors.length) return;
    const ordered = [...sensors].reverse();
    const values = ordered.map((item) => Number(item.magnitude));
    const max = Math.max(...values, 1);
    context.strokeStyle = "#d7a15f";
    context.beginPath();
    values.forEach((value, index) => {
        const x = index / Math.max(values.length - 1, 1) * canvas.width;
        const y = canvas.height - value / max * (canvas.height - 10);
        index === 0 ? context.moveTo(x, y) : context.lineTo(x, y);
    });
    context.stroke();
}

async function drawEvidence(items) {
    state.evidenceUrls.forEach(URL.revokeObjectURL);
    state.evidenceUrls = [];
    if (!items.length) {
        $("evidence").innerHTML = `<p class="muted">No evidence stored.</p>`;
        return;
    }
    const cards = [];
    for (const item of items) {
        const response = await fetch(new URL(item.contentPath, API_BASE_URL), { headers: { Authorization: "Bearer " + state.token } });
        if (response.status === 401) {
            logout();
            throw new Error("Sign in again");
        }
        if (!response.ok) throw new Error("Could not load camera evidence");
        const url = URL.createObjectURL(await response.blob());
        state.evidenceUrls.push(url);
        cards.push(`<figure><img alt="${escapeHTML(item.cameraRole)} evidence" src="${url}"><figcaption class="muted">${escapeHTML(item.cameraRole)} · ${escapeHTML(item.dataSource)}</figcaption></figure>`);
    }
    $("evidence").innerHTML = cards.join("") || `<p class="muted">Evidence metadata exists, but the files could not be loaded.</p>`;
}

async function loadAlerts() {
    setLoading(true);
    try {
        const alerts = (await api("/api/alerts")).data;
        $("alert-list").innerHTML = alerts.length ? alerts.map((alert) => alertCard(alert, true)).join("") : `<p class="muted">No alerts stored.</p>`;
        $("alert-list").querySelectorAll("button[data-action]").forEach((button) => button.addEventListener("click", async () => {
            const id = button.dataset.id;
            try {
                if (button.dataset.action === "read") await api("/api/alerts/" + id + "/read", { method: "PATCH" });
                if (button.dataset.action === "ack") await api("/api/alerts/" + id + "/acknowledge", { method: "POST" });
                if (button.dataset.action === "resolve") {
                    const notes = button.closest("[data-alert-id]").querySelector("textarea").value.trim();
                    await api("/api/alerts/" + id + "/resolve", { method: "POST", json: { notes: notes || null } });
                }
                await loadAlerts();
            } catch (error) { showError(error); }
        }));
    } catch (error) { showError(error); } finally { setLoading(false); }
}

function alertCard(alert, actions = false) {
    const controls = actions
        ? `<div class="alert-actions">${alert.unread ? `<button data-action="read" data-id="${alert.id}" class="ghost">Mark read</button>` : ""}
            ${alert.status === "OPEN" ? `<button data-action="ack" data-id="${alert.id}" class="ghost">Acknowledge</button>` : ""}
            ${alert.status !== "RESOLVED" ? `<label>Resolution notes<textarea maxlength="2000" placeholder="Optional investigation notes"></textarea></label><button data-action="resolve" data-id="${alert.id}">Resolve</button>` : ""}</div>`
        : "";
    return `<div class="card" data-alert-id="${alert.id}"><strong>${escapeHTML(alert.parcelCode)}</strong> <span class="badge ${escapeHTML(alert.severity)}">${escapeHTML(alert.severity)}</span> ${alert.unread ? "<span class=\"warn\">Unread</span>" : ""}<p>${escapeHTML(alert.description)}</p><p class="muted">${escapeHTML(alert.status)} · ${formatTime(alert.triggeredAt)}</p>${controls}</div>`;
}

function buildForms() {
    if ($("parcel-form").dataset.ready) return;
    $("parcel-form").dataset.ready = "1";
    const fields = [
        ["name", "text", true],
        ["description", "textarea", false],
        ["category", "text", true],
        ["declaredValue", "number", true],
        ["senderName", "text", true],
        ["senderPhone", "tel", false],
        ["receiverName", "text", true],
        ["receiverPhone", "tel", false],
        ["pickupAddress", "text", true],
        ["destinationAddress", "text", true],
        ["expectedDeliveryDate", "date", false]
    ];
    $("parcel-form").innerHTML = fields.map(([field, type, required]) =>
        type === "textarea"
            ? `<label class="wide">${field}<textarea name="${field}" maxlength="2000"></textarea></label>`
            : `<label>${field}<input name="${field}" type="${type}" ${field === "declaredValue" ? 'min="0" step="0.01"' : ""} ${required ? "required" : ""}></label>`
    ).join("") +
        `<label>Priority<select name="priority"><option>LOW</option><option>NORMAL</option><option>HIGH</option><option>CRITICAL</option></select></label>
        <label>Operator<select name="operatorId"><option value="">Unassigned</option></select></label>
        <label>Device<select name="deviceId"><option value="">No device</option></select></label>
        <button class="wide" type="submit">Create parcel</button>`;
    $("parcel-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = Object.fromEntries(new FormData(event.target).entries());
        form.declaredValue = Number(form.declaredValue);
        form.operatorId = form.operatorId ? Number(form.operatorId) : null;
        form.deviceId = form.deviceId ? Number(form.deviceId) : null;
        form.expectedDeliveryDate = form.expectedDeliveryDate || null;
        try {
            await api("/api/parcels", { method: "POST", json: form });
            event.target.reset();
            show("parcels");
        } catch (error) { showError(error); }
    });
    $("user-form").innerHTML = `<h2 class="wide">Create user</h2>
        <label>Username<input name="username" required maxlength="80"></label>
        <label>Email<input name="email" type="email" required maxlength="160"></label>
        <label>Full name<input name="fullName" required maxlength="160"></label>
        <label>Password<input name="password" type="password" required minlength="8" maxlength="72"></label>
        <label>Role<select name="role"><option>OPERATOR</option><option>ADMIN</option></select></label>
        <button class="wide" type="submit">Create user</button>`;
    $("user-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            await api("/api/users", { method: "POST", json: Object.fromEntries(new FormData(event.target).entries()) });
            event.target.reset();
            await loadUsers();
        } catch (error) { showError(error); }
    });
    $("device-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const created = await api("/api/devices", { method: "POST", json: { firmwareVersion: new FormData(event.target).get("firmwareVersion") } });
            $("device-token").hidden = false;
            $("device-token").textContent = "Copy this device token now. It will not be shown again: " + created.data.deviceToken;
            event.target.reset();
            await loadDevices();
        } catch (error) { showError(error); }
    });
    $("risk-settings-form").innerHTML = riskSettingFields.map(([name, label, type, min, max, step]) =>
        `<label>${label}<input name="${name}" type="${type}" min="${min}" max="${max}" step="${step}" required></label>`
    ).join("") + `<button class="wide" type="submit">Save risk settings</button>`;
    $("risk-settings-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(event.target).entries());
        const settings = Object.fromEntries(riskSettingFields.map(([name]) => [name,
            name === "impactThresholdG" || name === "vibrationThresholdG" ||
            name === "gpsAnomalyMinimumJumpMeters" || name === "gpsAnomalyMaximumPlausibleSpeedMps"
                ? Number(values[name])
                : Number.parseInt(values[name], 10)
        ]));
        try {
            await api("/api/admin/risk-settings", { method: "PUT", json: settings });
            await loadRiskSettings();
        } catch (error) { showError(error); }
    });
}

async function loadRiskSettings() {
    try {
        const settings = (await api("/api/admin/risk-settings")).data;
        const form = $("risk-settings-form");
        riskSettingFields.forEach(([name]) => { form.elements[name].value = settings[name]; });
    } catch (error) { showError(error); }
}

async function loadDevices() {
    try {
        const [devicePage, parcelPage] = await Promise.all([
            api("/api/devices?size=100&sort=deviceCode,asc"),
            api("/api/parcels?size=100&sort=parcelCode,asc")
        ]);
        const parcels = parcelPage.data.content;
        const devices = devicePage.data.content;
        $("device-list").innerHTML = devices.length ? devices.map((device) => {
            const parcelOptions = `<option value="">Select a parcel</option>` + parcels.map((parcel) =>
                `<option value="${parcel.id}" ${parcel.id === device.assignedParcelId ? "selected" : ""}>${escapeHTML(parcel.parcelCode)} · ${escapeHTML(parcel.name)}</option>`
            ).join("");
            return `<article class="management-card">
                <h3>${escapeHTML(device.deviceCode)}</h3>
                <p class="muted">${device.online ? "Online" : "Offline"} · ${escapeHTML(device.status)} · ${device.enabled ? "Enabled" : "Disabled"} · ${device.batteryPercent ?? "—"}% battery</p>
                <p>Assigned parcel: ${escapeHTML(device.assignedParcelCode || "Unassigned")}</p>
                <form class="device-update inline-form" data-id="${device.id}">
                    <label>Firmware<input name="firmwareVersion" maxlength="40" value="${escapeHTML(device.firmwareVersion || "")}"></label>
                    <label>Status<select name="status">${deviceStatuses.map((status) => `<option ${status === device.status ? "selected" : ""}>${status}</option>`).join("")}</select></label>
                    <label class="check-label"><input name="enabled" type="checkbox" ${device.enabled ? "checked" : ""}> Enabled</label>
                    <button type="submit">Save device</button>
                </form>
                <form class="device-assignment inline-form" data-id="${device.id}">
                    <label>Assign parcel<select name="parcelId">${parcelOptions}</select></label>
                    <button type="submit">Assign</button>
                    <button type="button" class="ghost" data-unassign ${device.assignedParcelId ? "" : "disabled"}>Unassign</button>
                </form>
            </article>`;
        }).join("") : `<p class="muted">No devices stored.</p>`;
        $("device-list").querySelectorAll(".device-update").forEach((form) => form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const values = new FormData(form);
            try {
                await api("/api/devices/" + form.dataset.id, {
                    method: "PUT",
                    json: { firmwareVersion: values.get("firmwareVersion"), status: values.get("status"), enabled: values.has("enabled") }
                });
                await loadDevices();
            } catch (error) { showError(error); }
        }));
        $("device-list").querySelectorAll(".device-assignment").forEach((form) => {
            form.addEventListener("submit", async (event) => {
                event.preventDefault();
                const parcelId = new FormData(form).get("parcelId");
                if (!parcelId) {
                    showError(new Error("Select a parcel before assigning this device"));
                    return;
                }
                try {
                    await api("/api/devices/" + form.dataset.id + "/assign", { method: "POST", json: { parcelId: Number(parcelId) } });
                    await loadDevices();
                } catch (error) { showError(error); }
            });
            form.querySelector("[data-unassign]").addEventListener("click", async () => {
                try {
                    await api("/api/devices/" + form.dataset.id + "/unassign", { method: "POST" });
                    await loadDevices();
                } catch (error) { showError(error); }
            });
        });
    } catch (error) { showError(error); }
}

async function loadUsers() {
    try {
        const users = (await api("/api/users?size=100&sort=username,asc")).data.content;
        $("user-list").innerHTML = users.length ? users.map((user) => `<article class="management-card">
            <h3>${escapeHTML(user.username)} <span class="muted">· ${user.enabled ? "Enabled" : "Disabled"}</span></h3>
            <form class="user-update form-grid" data-id="${user.id}">
                <label>Email<input name="email" type="email" required maxlength="160" value="${escapeHTML(user.email)}"></label>
                <label>Full name<input name="fullName" required maxlength="160" value="${escapeHTML(user.fullName)}"></label>
                <label>Role<select name="role">${["OPERATOR", "ADMIN"].map((role) => `<option ${role === user.role ? "selected" : ""}>${role}</option>`).join("")}</select></label>
                <label>New password<input name="password" type="password" minlength="8" maxlength="72" placeholder="Leave blank to keep current"></label>
                <button type="submit">Save user</button>
                <button type="button" class="ghost" data-toggle-user>${user.enabled ? "Disable" : "Enable"} account</button>
            </form>
        </article>`).join("") : `<p class="muted">No users stored.</p>`;
        $("user-list").querySelectorAll(".user-update").forEach((form) => {
            form.addEventListener("submit", async (event) => {
                event.preventDefault();
                const values = new FormData(form);
                const request = Object.fromEntries(values.entries());
                if (!request.password) delete request.password;
                try {
                    await api("/api/users/" + form.dataset.id, { method: "PUT", json: request });
                    await loadUsers();
                } catch (error) { showError(error); }
            });
            form.querySelector("[data-toggle-user]").addEventListener("click", async () => {
                const enabled = form.querySelector("[data-toggle-user]").textContent.trim() === "Disable account";
                try {
                    await api("/api/users/" + form.dataset.id + "/status", { method: "PATCH", json: { enabled: !enabled } });
                    await loadUsers();
                } catch (error) { showError(error); }
            });
        });
    } catch (error) { showError(error); }
}

function connectLive() {
    const backendUrl = new URL(API_BASE_URL);
    const protocol = backendUrl.protocol === "https:" ? "wss:" : "ws:";
    state.socket = new WebSocket(protocol + "//" + backendUrl.host + "/ws?access_token=" + encodeURIComponent(state.token));
    state.socket.addEventListener("open", () => {
        $("live-state").textContent = "Live updates on";
        state.socket.send(JSON.stringify({ action: "dashboard" }));
    });
    state.socket.addEventListener("close", () => { $("live-state").textContent = "Live updates off"; });
    state.socket.addEventListener("message", () => {
        if (state.view === "dashboard") loadDashboard();
        if (state.view === "detail") loadDetail();
        if (state.view === "alerts") loadAlerts();
    });
}

function formatTime(value) { return value ? new Date(value).toLocaleString() : ""; }

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

if (state.token) {
    openApp().catch((error) => {
        logout();
        $("login-error").hidden = false;
        $("login-error").textContent = error.message || "Sign in again";
    });
}

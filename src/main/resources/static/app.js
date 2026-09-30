const TOKEN_KEY = "ems_token";
const USER_KEY  = "ems_user";

const getToken  = () => localStorage.getItem(TOKEN_KEY);
const getUser   = () => { const r = localStorage.getItem(USER_KEY); return r ? JSON.parse(r) : null; };
const clearAuth = () => { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); };
const logout    = () => { clearAuth(); location.href = "index.html"; };

async function login(username, password) {
    const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });
    if (!res.ok) throw new Error("Invalid username or password");
    const data = await res.json();
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data));
}

async function api(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const t = getToken();
    if (t) headers.Authorization = "Bearer " + t;
    const res = await fetch(path, { ...options, headers });
    if (res.status === 401) { logout(); throw new Error("Session expired"); }
    if (!res.ok) {
        let msg = "HTTP " + res.status;
        try { const j = await res.json(); if (j.message) msg = j.message; } catch (_) {}
        throw new Error(msg);
    }
    if (res.status === 204) return null;
    return res.json();
}

const ymd      = d => d.toISOString().slice(0,10);
const today    = () => ymd(new Date());
const nDaysAgo = n => { const d = new Date(); d.setDate(d.getDate()-n); return ymd(d); };

function esc(s) {
    if (s == null) return "";
    return String(s).replace(/[&<>"']/g, c => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[c]));
}

function openModal(html) {
    document.getElementById("modalContent").innerHTML = html;
    document.getElementById("modal").hidden = false;
}
function closeModal() {
    document.getElementById("modal").hidden = true;
    document.getElementById("modalContent").innerHTML = "";
}
window.closeModal = closeModal;

if (document.getElementById("loginForm")) {
    document.getElementById("loginForm").addEventListener("submit", async e => {
        e.preventDefault();
        const err = document.getElementById("loginError");
        err.hidden = true;
        try {
            await login(
                document.getElementById("username").value.trim(),
                document.getElementById("password").value
            );
            location.href = "dashboard.html";
        } catch (ex) {
            err.textContent = ex.message;
            err.hidden = false;
        }
    });
}

if (document.getElementById("dashboard")) initDashboard();

function initDashboard() {
    const user = getUser();
    if (!user) { location.href = "index.html"; return; }

    document.getElementById("userLabel").textContent = user.username + " - " + user.role;
    document.getElementById("logoutBtn").onclick = logout;

    const isPrivileged = ["ADMIN","HR","MANAGER"].includes(user.role);
    const isHrOrAdmin  = ["ADMIN","HR"].includes(user.role);
    const isAdmin      = user.role === "ADMIN";

    if (!isPrivileged) document.querySelector('[data-tab="employees"]').hidden = true;
    if (!isPrivileged) document.querySelector('[data-tab="performance"]').hidden = true;
    if (!isAdmin)      document.querySelector('[data-tab="users"]').hidden = true;
    if (isPrivileged)  document.getElementById("pendingLeavesSection").hidden = false;
    if (isHrOrAdmin)   document.getElementById("orgReportSection").hidden = false;
    if (!isHrOrAdmin)  document.getElementById("addEmpBtn").hidden = true;
    if (!isPrivileged) document.getElementById("addReviewBtn").hidden = true;

    document.querySelectorAll("[data-tab]").forEach(btn => {
        btn.onclick = () => showTab(btn.dataset.tab);
    });

    document.getElementById("empSearchBtn").onclick = loadEmployees;
    document.getElementById("empSearch").addEventListener("keydown", e => {
        if (e.key === "Enter") loadEmployees();
    });
    document.getElementById("addEmpBtn").onclick = () => openEmployeeModal(null);
    document.getElementById("addReviewBtn").onclick = () => openReviewModal();
    const addUserBtn = document.getElementById("addUserBtn");
    if (addUserBtn) addUserBtn.onclick = () => openUserModal();

    document.getElementById("checkInBtn").onclick = async () => {
        try { await api("/api/attendance/check-in", { method: "POST" }); loadAttendance(); }
        catch (ex) { alert(ex.message); }
    };
    document.getElementById("checkOutBtn").onclick = async () => {
        try { await api("/api/attendance/check-out", { method: "POST" }); loadAttendance(); }
        catch (ex) { alert(ex.message); }
    };

    document.getElementById("leaveForm").addEventListener("submit", async e => {
        e.preventDefault();
        try {
            await api("/api/leaves/apply", {
                method: "POST",
                body: JSON.stringify({
                    fromDate: document.getElementById("leaveFrom").value,
                    toDate:   document.getElementById("leaveTo").value,
                    reason:   document.getElementById("leaveReason").value
                })
            });
            e.target.reset();
            loadLeaves();
        } catch (ex) { alert(ex.message); }
    });

    document.getElementById("loadMyReport").onclick = loadMyAiReport;
    const orgBtn = document.getElementById("loadOrgReport");
    if (orgBtn) orgBtn.onclick = loadOrgAiReport;

    const first = [...document.querySelectorAll("[data-tab]")].find(b => !b.hidden);
    showTab(first.dataset.tab);
}

function showTab(name) {
    document.querySelectorAll("[data-tab]").forEach(b => b.classList.toggle("active", b.dataset.tab === name));
    document.querySelectorAll(".tab-content").forEach(s => { s.hidden = s.id !== "tab-" + name; });
    if (name === "employees")   loadEmployees();
    if (name === "attendance")  loadAttendance();
    if (name === "leaves")      loadLeaves();
    if (name === "performance") loadReviews();
    if (name === "ai")          loadMyAiReport();
    if (name === "users")       loadUsers();
}

async function loadEmployees() {
    const q = document.getElementById("empSearch").value.trim();
    const url = "/api/employees?page=0&size=50" + (q ? "&q=" + encodeURIComponent(q) : "");
    const body = document.getElementById("empList");
    body.innerHTML = '<tr><td colspan="7">Loading...</td></tr>';
    const user = getUser();
    const canEdit = ["ADMIN","HR"].includes(user.role);
    const canDelete = user.role === "ADMIN";
    try {
        const page = await api(url);
        const rows = page.content || [];
        if (!rows.length) { body.innerHTML = '<tr><td colspan="7">No employees found.</td></tr>'; return; }
        body.innerHTML = rows.map(e => `
            <tr>
                <td>${e.id}</td>
                <td>${esc(e.fullName)}</td>
                <td>${esc(e.email)}</td>
                <td>${esc(e.department || "-")}</td>
                <td>${esc(e.designation || "-")}</td>
                <td><span class="pill pill-${e.status.toLowerCase()}">${e.status}</span></td>
                <td class="actions">
                    ${canEdit ? `<button class="btn btn-sm btn-outline" onclick="editEmployee(${e.id})">Edit</button>` : ""}
                    ${canEdit && e.status !== "TERMINATED" ? `<button class="btn btn-sm btn-secondary" onclick="terminateEmployee(${e.id})">Terminate</button>` : ""}
                    ${canDelete ? `<button class="btn btn-sm btn-danger" onclick="deleteEmployee(${e.id})">Delete</button>` : ""}
                </td>
            </tr>`).join("");
    } catch (ex) {
        body.innerHTML = `<tr><td colspan="7" class="error">${esc(ex.message)}</td></tr>`;
    }
}

window.openEmployeeModal = function(e) {
    e = e || {};
    const statuses = ["ACTIVE","PROBATION","ON_LEAVE","TERMINATED"];
    openModal(`
        <form id="empForm" onsubmit="saveEmployee(event)" data-id="${e.id || ""}">
            <h2>${e.id ? "Edit" : "Add"} Employee</h2>
            <div class="form-grid">
                <label>First name * <input name="firstName" required value="${esc(e.firstName||"")}"></label>
                <label>Last name *  <input name="lastName" required value="${esc(e.lastName||"")}"></label>
                <label>Email *      <input type="email" name="email" required value="${esc(e.email||"")}"></label>
                <label>Phone        <input name="phone" value="${esc(e.phone||"")}"></label>
                <label>Department   <input name="department" value="${esc(e.department||"")}"></label>
                <label>Designation  <input name="designation" value="${esc(e.designation||"")}"></label>
                <label>Salary       <input type="number" step="0.01" name="salary" value="${e.salary != null ? e.salary : ""}"></label>
                <label>Joining date <input type="date" name="joiningDate" value="${e.joiningDate || today()}"></label>
                <label>Status
                    <select name="status">
                        ${statuses.map(s => `<option value="${s}" ${e.status===s?"selected":""}>${s}</option>`).join("")}
                    </select>
                </label>
                <label>Manager ID   <input type="number" name="managerId" value="${e.managerId != null ? e.managerId : ""}"></label>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-outline" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Save</button>
            </div>
        </form>
    `);
};

window.editEmployee = async function(id) {
    try {
        const e = await api("/api/employees/" + id);
        openEmployeeModal(e);
    } catch (ex) { alert(ex.message); }
};

window.saveEmployee = async function(ev) {
    ev.preventDefault();
    const form = ev.target;
    const data = Object.fromEntries(new FormData(form).entries());
    ["phone","department","designation"].forEach(k => { if (!data[k]) delete data[k]; });
    if (!data.salary)    delete data.salary;    else data.salary = Number(data.salary);
    if (!data.managerId) delete data.managerId; else data.managerId = Number(data.managerId);
    if (!data.joiningDate) delete data.joiningDate;
    const id = form.dataset.id;
    try {
        if (id) await api("/api/employees/" + id, { method: "PUT", body: JSON.stringify(data) });
        else    await api("/api/employees",        { method: "POST", body: JSON.stringify(data) });
        closeModal();
        loadEmployees();
    } catch (ex) { alert(ex.message); }
};

window.terminateEmployee = async function(id) {
    if (!confirm("Terminate this employee?")) return;
    try { await api(`/api/employees/${id}/terminate`, { method: "PATCH" }); loadEmployees(); }
    catch (ex) { alert(ex.message); }
};

window.deleteEmployee = async function(id) {
    if (!confirm("Permanently delete this employee? This cannot be undone.")) return;
    try { await api("/api/employees/" + id, { method: "DELETE" }); loadEmployees(); }
    catch (ex) { alert(ex.message); }
};

async function loadAttendance() {
    const from = nDaysAgo(30), to = today();
    const body = document.getElementById("attHistory");
    body.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const rows = await api(`/api/attendance/me?from=${from}&to=${to}`);
        if (!rows.length) body.innerHTML = '<tr><td colspan="5">No records in the last 30 days.</td></tr>';
        else body.innerHTML = rows.map(a => `
            <tr>
                <td>${a.workDate}</td>
                <td>${a.checkIn || "-"}</td>
                <td>${a.checkOut || "-"}</td>
                <td><span class="pill pill-${a.status.toLowerCase()}">${a.status}</span></td>
                <td>${a.hoursWorked != null ? a.hoursWorked : "-"}</td>
            </tr>`).join("");
        const todayRow = rows.find(r => r.workDate === to);
        const inBtn    = document.getElementById("checkInBtn");
        const outBtn   = document.getElementById("checkOutBtn");
        const status   = document.getElementById("attStatus");
        if (!todayRow)          { inBtn.hidden=false; outBtn.hidden=true;  status.textContent = "You have not checked in today."; }
        else if (!todayRow.checkOut) { inBtn.hidden=true; outBtn.hidden=false; status.textContent = "Checked in at " + todayRow.checkIn + "."; }
        else                    { inBtn.hidden=true; outBtn.hidden=true;  status.textContent = "Completed: " + todayRow.checkIn + " to " + todayRow.checkOut + "."; }
    } catch (ex) {
        body.innerHTML = `<tr><td colspan="5" class="error">${esc(ex.message)}</td></tr>`;
    }
}

async function loadLeaves() {
    const mine = document.getElementById("myLeaves");
    mine.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const rows = await api("/api/leaves/me");
        if (!rows.length) mine.innerHTML = '<tr><td colspan="5">No leave requests.</td></tr>';
        else mine.innerHTML = rows.map(l => `
            <tr>
                <td>${l.fromDate} to ${l.toDate}</td>
                <td>${l.workingDays}</td>
                <td>${esc(l.reason)}</td>
                <td><span class="pill pill-${l.status.toLowerCase()}">${l.status}</span></td>
                <td>${l.appliedOn}</td>
            </tr>`).join("");
    } catch (ex) {
        mine.innerHTML = `<tr><td colspan="5" class="error">${esc(ex.message)}</td></tr>`;
    }
    const sec = document.getElementById("pendingLeavesSection");
    if (sec.hidden) return;
    const pending = document.getElementById("pendingLeaves");
    pending.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const rows = await api("/api/leaves/pending");
        if (!rows.length) { pending.innerHTML = '<tr><td colspan="5">No pending requests.</td></tr>'; return; }
        pending.innerHTML = rows.map(l => `
            <tr>
                <td>${esc(l.employeeName)}</td>
                <td>${l.fromDate} to ${l.toDate}</td>
                <td>${l.workingDays}</td>
                <td>${esc(l.reason)}</td>
                <td>
                    <button class="btn btn-sm btn-success" onclick="approveLeave(${l.id})">Approve</button>
                    <button class="btn btn-sm btn-danger"  onclick="rejectLeave(${l.id})">Reject</button>
                </td>
            </tr>`).join("");
    } catch (ex) {
        pending.innerHTML = `<tr><td colspan="5" class="error">${esc(ex.message)}</td></tr>`;
    }
}

window.approveLeave = async function(id) {
    const note = prompt("Approval note (optional):") || "";
    try { await api(`/api/leaves/${id}/approve`, { method: "POST", body: JSON.stringify({ note }) }); loadLeaves(); }
    catch (ex) { alert(ex.message); }
};
window.rejectLeave = async function(id) {
    const note = prompt("Reason for rejection:") || "";
    try { await api(`/api/leaves/${id}/reject`, { method: "POST", body: JSON.stringify({ note }) }); loadLeaves(); }
    catch (ex) { alert(ex.message); }
};

async function loadReviews() {
    const body = document.getElementById("reviewList");
    body.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const rows = await api("/api/performance/reviews");
        if (!rows.length) { body.innerHTML = '<tr><td colspan="5">No reviews yet.</td></tr>'; return; }
        body.innerHTML = rows.map(r => `
            <tr>
                <td>${esc(r.employeeName)}</td>
                <td>${r.reviewDate}</td>
                <td>${r.rating}/5</td>
                <td>${r.goalsMetPercent != null ? r.goalsMetPercent + "%" : "-"}</td>
                <td>${esc(r.reviewerName || "-")}</td>
            </tr>`).join("");
    } catch (ex) {
        body.innerHTML = `<tr><td colspan="5" class="error">${esc(ex.message)}</td></tr>`;
    }
}

window.openReviewModal = async function() {
    let employees = [];
    try {
        const page = await api("/api/employees?page=0&size=200");
        employees = page.content || [];
    } catch (ex) { alert("Could not load employees: " + ex.message); return; }
    openModal(`
        <form onsubmit="saveReview(event)">
            <h2>Add Performance Review</h2>
            <div class="form-grid">
                <label>Employee *
                    <select name="employeeId" required>
                        <option value="">-- select --</option>
                        ${employees.map(e => `<option value="${e.id}">${esc(e.fullName)} (${esc(e.department||"")})</option>`).join("")}
                    </select>
                </label>
                <label>Review date * <input type="date" name="reviewDate" value="${today()}" required></label>
                <label>Rating (1-5) * <input type="number" min="1" max="5" name="rating" required></label>
                <label>Goals met % <input type="number" min="0" max="100" name="goalsMetPercent"></label>
                <label>Reviewer <input name="reviewerName" value="${esc(getUser().username)}"></label>
                <label class="span-2">Strengths  <textarea name="strengths" rows="2" maxlength="1000"></textarea></label>
                <label class="span-2">Improvements <textarea name="improvements" rows="2" maxlength="1000"></textarea></label>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-outline" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Review</button>
            </div>
        </form>
    `);
};

window.saveReview = async function(ev) {
    ev.preventDefault();
    const data = Object.fromEntries(new FormData(ev.target).entries());
    data.employeeId = Number(data.employeeId);
    data.rating = Number(data.rating);
    if (!data.goalsMetPercent) delete data.goalsMetPercent; else data.goalsMetPercent = Number(data.goalsMetPercent);
    ["strengths","improvements","reviewerName"].forEach(k => { if (!data[k]) delete data[k]; });
    try {
        await api("/api/performance/reviews", { method: "POST", body: JSON.stringify(data) });
        closeModal();
        loadReviews();
    } catch (ex) { alert(ex.message); }
};

async function loadUsers() {
    const body = document.getElementById("userList");
    body.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const rows = await api("/api/auth/users");
        if (!rows.length) { body.innerHTML = '<tr><td colspan="5">No users.</td></tr>'; return; }
        body.innerHTML = rows.map(u => `
            <tr>
                <td>${u.id}</td>
                <td>${esc(u.username)}</td>
                <td>${u.role}</td>
                <td>${u.employeeId != null ? u.employeeId : "-"}</td>
                <td>${u.enabled ? "Yes" : "No"}</td>
            </tr>`).join("");
    } catch (ex) {
        body.innerHTML = `<tr><td colspan="5" class="error">${esc(ex.message)}</td></tr>`;
    }
}

window.openUserModal = async function() {
    let employees = [];
    try {
        const page = await api("/api/employees?page=0&size=200");
        employees = page.content || [];
    } catch (ex) {}
    openModal(`
        <form onsubmit="saveUser(event)">
            <h2>Create Login</h2>
            <div class="form-grid">
                <label>Username * <input name="username" required minlength="4" maxlength="60"></label>
                <label>Password * <input type="password" name="password" required minlength="6" maxlength="100"></label>
                <label>Role *
                    <select name="role" required>
                        <option value="EMPLOYEE">EMPLOYEE</option>
                        <option value="MANAGER">MANAGER</option>
                        <option value="HR">HR</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </label>
                <label>Link to employee
                    <select name="employeeId">
                        <option value="">-- none --</option>
                        ${employees.map(e => `<option value="${e.id}">${esc(e.fullName)} (ID ${e.id})</option>`).join("")}
                    </select>
                </label>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-outline" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Create</button>
            </div>
        </form>
    `);
};

window.saveUser = async function(ev) {
    ev.preventDefault();
    const data = Object.fromEntries(new FormData(ev.target).entries());
    if (!data.employeeId) delete data.employeeId; else data.employeeId = Number(data.employeeId);
    try {
        await api("/api/auth/register", { method: "POST", body: JSON.stringify(data) });
        closeModal();
        loadUsers();
    } catch (ex) { alert(ex.message); }
};

async function loadMyAiReport() {
    const el = document.getElementById("myReport");
    el.innerHTML = "Loading...";
    try {
        const r = await api("/api/reports/ai/me");
        el.innerHTML = `
            <h3>${esc(r.employeeName)} <span class="pill pill-${r.riskBand.toLowerCase()}">${r.riskBand} risk</span></h3>
            <p><strong>Score:</strong> ${r.attritionRiskScore}/100 &middot;
               <strong>Attendance (30d):</strong> ${r.attendanceRate}% &middot;
               <strong>Late:</strong> ${r.lateRate}% &middot;
               <strong>Leave (90d):</strong> ${r.approvedLeaveDaysLast90} days</p>
            <p>${esc(r.narrative)}</p>
            ${r.insights.length ? "<h4>Insights</h4><ul>" + r.insights.map(i => `<li>${esc(i)}</li>`).join("") + "</ul>" : ""}
            ${r.recommendations.length ? "<h4>Recommendations</h4><ul>" + r.recommendations.map(i => `<li>${esc(i)}</li>`).join("") + "</ul>" : ""}
            <p class="muted">Generated by ${esc(r.generatedBy)}</p>`;
    } catch (ex) { el.innerHTML = `<p class="error">${esc(ex.message)}</p>`; }
}

async function loadOrgAiReport() {
    const el = document.getElementById("orgReport");
    el.innerHTML = "Loading...";
    try {
        const r = await api("/api/reports/ai/organization");
        el.innerHTML = `
            <p><strong>${r.headcount}</strong> employees &middot;
               <strong>${r.activeEmployees}</strong> active &middot;
               attendance <strong>${r.overallAttendanceRate}%</strong></p>
            <p>${esc(r.narrative)}</p>
            <h4>Top performers</h4>
            ${r.topPerformers.length ? "<ul>" + r.topPerformers.map(x => `<li>${esc(x)}</li>`).join("") + "</ul>" : "<p>None above threshold.</p>"}
            <h4>High-risk employees</h4>
            ${r.highRiskEmployees.length ? "<ul>" + r.highRiskEmployees.map(x => `<li>${esc(x)}</li>`).join("") + "</ul>" : "<p>None.</p>"}`;
    } catch (ex) { el.innerHTML = `<p class="error">${esc(ex.message)}</p>`; }
}
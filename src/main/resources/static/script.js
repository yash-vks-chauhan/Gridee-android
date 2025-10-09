// src/main/resources/static/script.js

const baseUrl = window.location.origin + "/api";

function pad(n) { return n < 10 ? "0" + n : n; }
function toBackendIsoString(dtStr) {
    if (!dtStr) return "";
    const d = new Date(dtStr);
    if (isNaN(d.getTime())) return "";
    const off = -d.getTimezoneOffset();
    const sign = off >= 0 ? "+" : "-";
    const hr = pad(Math.abs(Math.trunc(off / 60)));
    const min = pad(Math.abs(off % 60));
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00${sign}${hr}:${min}`;
}
function showSection(name) {
    document.querySelectorAll(".section").forEach((s) => s.classList.add("hidden"));
    document.getElementById(name).classList.remove("hidden");
    clearResponseAndTable(name);
    if (name === "bookings") {
        fetchUserVehicles();
    }
}
function clearResponseAndTable(name) {
    if (name === "wallet") {
        document.getElementById("walletResponse").textContent = "";
        document.querySelector("#walletTable tbody").innerHTML = "";
        document.querySelector("#walletTxTable tbody").innerHTML = "";
    }
    const responseElem = document.getElementById(name + "Response");
    if (responseElem) responseElem.textContent = "";
    const tbody = document.querySelector(`#${name}Table tbody`);
    if (tbody) tbody.innerHTML = "";
    if (name === "bookings") {
        document.getElementById("bookingAmountSection").style.display = "none";
        document.getElementById("bookingAmount").textContent = "0.00";
        document.getElementById("bookingVehicleNumber").innerHTML = "<option value=''>-- Select Vehicle --</option>";
        document.getElementById("bookingUserId").value = "";
    }
    if (name === "spots") {
        document.getElementById('spotLotId').value = "";
        document.getElementById('spotZoneName').value = "";
        document.getElementById('spotCapacity').value = "";
        document.getElementById('spotAvailable').value = "";
    }
    if (name === "lots") {
        document.getElementById('lotName').value = "";
        document.getElementById('lotLocation').value = "";
        document.getElementById('lotDescription').value = "";
    }
    if (name === "wallet") {
        document.getElementById('walletUserId').value = "";
        document.getElementById('topUpUserId').value = "";
        document.getElementById('topUpAmount').value = "";
    }
}

// USERS
async function fetchUsers() {
    const res = await fetch(`${baseUrl}/users`);
    const data = await res.json();
    displayUsers(data);
    document.getElementById("usersResponse").textContent = "Fetched users successfully";
}
function displayUsers(users) {
    const tbody = document.querySelector("#usersTable tbody");
    tbody.innerHTML = "";
    users.forEach((user) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${user.id || ""}</td><td>${user.name || ""}</td><td>${user.email || ""}</td><td>${user.phone || ""}</td><td>${Array.isArray(user.vehicleNumbers) ? user.vehicleNumbers.join(", ") : user.vehicleNumber || ""}</td><td>${user.walletCoins || 0}</td>`;
        tbody.appendChild(tr);
    });
}
async function createUser() {
    const name = document.getElementById("userName").value.trim();
    const email = document.getElementById("userEmail").value.trim();
    const phone = document.getElementById("userPhone").value.trim();
    const vehicleNumbers = document.getElementById("userVehicleNumbers").value.split(",").map((v) => v.trim()).filter(Boolean);
    const password = document.getElementById("userPassword").value;
    if (!name || !email || !phone || !password || vehicleNumbers.length === 0) {
        alert("Fill all required fields (name, email, phone, password, at least one vehicle number).");
        return;
    }
    try {
        const res = await fetch(`${baseUrl}/users/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, email, phone, vehicleNumbers, password }),
        });
        if (res.ok) {
            fetchUsers();
        } else {
            alert("Failed to create user");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function updateUser() {
    const id = document.getElementById("updateUserId").value.trim();
    if (!id) { alert("Please enter user ID"); return; }
    const name = document.getElementById("updateUserName").value.trim();
    const email = document.getElementById("updateUserEmail").value.trim();
    const phone = document.getElementById("updateUserPhone").value.trim();
    const vehicleNumbers = document.getElementById("updateUserVehicleNumbers").value.split(",").map((v) => v.trim()).filter(Boolean);
    const password = document.getElementById("updateUserPassword").value;
    const payload = {};
    if (name) payload.name = name;
    if (email) payload.email = email;
    if (phone) payload.phone = phone;
    if (vehicleNumbers.length > 0) payload.vehicleNumbers = vehicleNumbers;
    if (password) payload.passwordHash = password;
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(id)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (res.ok) {
            fetchUsers();
        } else {
            alert("Failed to update user");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function deleteUser() {
    const id = document.getElementById("deleteUserId").value.trim();
    if (!id) { alert("Please enter user ID"); return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (res.ok) {
            fetchUsers();
        } else {
            alert("Failed to delete user");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// BOOKINGS
function bookingsUserIdInput() { return document.getElementById("bookingUserId").value.trim(); }
async function fetchBookings() {
    const userId = bookingsUserIdInput();
    if (!userId) { alert("Enter User ID to fetch bookings"); return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(userId)}/all-bookings`);
        if (!res.ok) {
            alert("Failed to fetch bookings");
            return;
        }
        const data = await res.json();
        displayBookings(data);
        document.getElementById("bookingsResponse").textContent = "Fetched bookings successfully";
    } catch (e) {
        alert("Error fetching bookings: " + e.message);
    }
}
function displayBookings(bookings) {
    const statuses = {
        pending: [],
        active: [],
        completed: []
    };
    bookings.forEach((b) => {
        const status = (b.status || "").toLowerCase();
        if (status === "pending") statuses.pending.push(b);
        else if (status === "active") statuses.active.push(b);
        else statuses.completed.push(b);
    });

    function renderTable(bookings, tableId, title) {
        let section = document.getElementById(tableId);
        if (!section) {
            section = document.createElement("div");
            section.id = tableId;
            section.innerHTML = `<h3>${title}</h3><table><thead>
                <tr>
                    <th>ID</th><th>User</th><th>Spot</th><th>Lot</th>
                    <th>Check-In</th><th>Check-Out</th><th>Status</th>
                    <th>Amount</th><th>Vehicle</th><th>QR</th><th>Actions</th>
                </tr>
            </thead><tbody></tbody></table>`;
            document.getElementById("bookingsTable").parentNode.appendChild(section);
        }
        const tbody = section.querySelector("tbody");
        tbody.innerHTML = "";
        bookings.forEach((b) => {
            // QR code is just the booking ID
            const qrImg = b.id ? `<img src="${getQrCodeImageUrl(b.id)}" alt="QR" width="80" height="80"/>` : "N/A";
            const tr = document.createElement("tr");
            let actions = "";
            if (b.status === "pending") {
                actions += `<button onclick="checkIn('${b.userId}','${b.id}')">Check In</button> `;
                actions += `<button onclick="cancelBooking('${b.userId}','${b.id}')">Cancel</button>`;
            } else if (b.status === "active") {
                actions += `<button onclick="checkOut('${b.userId}','${b.id}')">Check Out</button> `;
                actions += `<button onclick="fetchPenaltyInfo('${b.userId}','${b.id}')">Penalty Info</button>`;
            }
            tr.innerHTML = `
                <td>${b.id || ""}</td>
                <td>${b.userId || ""}</td>
                <td>${b.spotId || ""}</td>
                <td>${b.lotId || ""}</td>
                <td>${b.checkInTime || ""}</td>
                <td>${b.checkOutTime || ""}</td>
                <td>${b.status || ""}</td>
                <td>${b.amount?.toFixed(2) || "0.00"}</td>
                <td>${b.vehicleNumber || ""}</td>
                <td>${qrImg}</td>
                <td>${actions}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    ["pendingBookings", "activeBookings", "completedBookings"].forEach(id => {
        const old = document.getElementById(id);
        if (old) old.remove();
    });

    renderTable(statuses.pending, "pendingBookings", "Pending Bookings");
    renderTable(statuses.active, "activeBookings", "Active Bookings");
    renderTable(statuses.completed, "completedBookings", "Completed Bookings");
}
async function fetchUserVehicles() {
    const userId = bookingsUserIdInput();
    const select = document.getElementById("bookingVehicleNumber");
    select.innerHTML = "<option value=''>-- Select Vehicle --</option>";
    if (!userId) { return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(userId)}`);
        if (!res.ok) { return; }
        const user = await res.json();
        const vehicles = Array.isArray(user.vehicleNumbers) ? user.vehicleNumbers : [];
        vehicles.forEach((v) => {
            const opt = document.createElement("option");
            opt.value = v;
            opt.textContent = v;
            select.appendChild(opt);
        });
    } catch (e) {}
}
function calculateBookingAmount() {
    const checkInTimeStr = document.getElementById("bookingCheckIn").value;
    const checkOutTimeStr = document.getElementById("bookingCheckOut").value;
    const amountSpan = document.getElementById("bookingAmount");
    const amountSection = document.getElementById("bookingAmountSection");
    if (!checkInTimeStr || !checkOutTimeStr) {
        amountSection.style.display = "none";
        return;
    }
    const checkInTime = new Date(checkInTimeStr);
    const checkOutTime = new Date(checkOutTimeStr);
    if (checkOutTime <= checkInTime) {
        amountSpan.textContent = "Invalid times";
        amountSection.style.display = "block";
        return;
    }
    const durationMs = checkOutTime - checkInTime;
    const hours = Math.ceil(durationMs / (1000 * 60 * 60));
    const amount = (hours * 5).toFixed(2);
    amountSpan.textContent = amount;
    amountSection.style.display = "block";
}
document.getElementById("bookingCheckIn").addEventListener("change", calculateBookingAmount);
document.getElementById("bookingCheckOut").addEventListener("change", calculateBookingAmount);

function isBookingTimeAllowed(checkInRaw, checkOutRaw) {
    const now = new Date();
    const cutoff = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 20, 0, 0);
    const checkIn = new Date(checkInRaw);
    const checkOut = new Date(checkOutRaw);

    if (now < cutoff) {
        if (
            checkIn.getFullYear() !== now.getFullYear() ||
            checkIn.getMonth() !== now.getMonth() ||
            checkIn.getDate() !== now.getDate() ||
            checkOut > cutoff
        ) {
            return false;
        }
    } else {
        const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
        if (
            checkIn.getFullYear() !== tomorrow.getFullYear() ||
            checkIn.getMonth() !== tomorrow.getMonth() ||
            checkIn.getDate() !== tomorrow.getDate()
        ) {
            return false;
        }
    }
    return true;
}

async function createBooking() {
    const userId = bookingsUserIdInput();
    const spotId = document.getElementById("bookingSpotId").value.trim();
    const lotId = document.getElementById("bookingLotId").value.trim();
    const checkInRaw = document.getElementById("bookingCheckIn").value;
    const checkOutRaw = document.getElementById("bookingCheckOut").value;
    const vehicleNumber = document.getElementById("bookingVehicleNumber").value.trim();
    if (!userId || !spotId || !lotId || !checkInRaw || !checkOutRaw || !vehicleNumber) {
        alert("Fill all booking fields, including user ID, check-in/out, and vehicle number.");
        return;
    }
    if (!isBookingTimeAllowed(checkInRaw, checkOutRaw)) {
        alert("Booking not allowed: Only bookings for today until 8 pm are allowed before 8 pm. After 8 pm, only bookings for tomorrow are allowed.");
        return;
    }
    const checkInTime = toBackendIsoString(checkInRaw);
    const checkOutTime = toBackendIsoString(checkOutRaw);
    if (!checkInTime || !checkOutTime) {
        alert("Invalid date-time format");
        return;
    }
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/start?spotId=${encodeURIComponent(spotId)}&lotId=${encodeURIComponent(lotId)}&checkInTime=${encodeURIComponent(checkInTime)}&checkOutTime=${encodeURIComponent(checkOutTime)}&vehicleNumber=${encodeURIComponent(vehicleNumber)}`;
        const res = await fetch(url, { method: "POST" });
        if (res.ok) {
            fetchBookings();
        } else if (res.status === 409) {
            alert("Booking conflict: Spot may already be booked.");
        } else if (res.status === 400) {
            const errorText = await res.text();
            alert("Booking error: " + errorText);
        } else {
            alert("Failed to create booking");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function updateBooking() {
    const userId = bookingsUserIdInput();
    const id = document.getElementById("updateBookingId").value.trim();
    if (!userId || !id) { alert("Enter User ID and Booking ID"); return; }
    const status = document.getElementById("updateBookingStatus").value;
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(id)}`;
        const res = await fetch(url, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status }),
        });
        if (res.ok) {
            fetchBookings();
        } else {
            alert("Failed to update booking");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function deleteBooking(userId, id) {
    if (!userId || !id) { alert("Enter User ID and Booking ID"); return; }
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(id)}`;
        const res = await fetch(url, { method: "DELETE" });
        if (res.ok) {
            fetchBookings();
        } else {
            alert("Failed to delete booking");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
function getQrCodeImageUrl(qrCode) {
    const qr = encodeURIComponent(qrCode);
    return `https://chart.googleapis.com/chart?cht=qr&chs=150x150&chl=${qr}`;
}

// Booking History
async function fetchBookingHistory() {
    const userId = bookingsUserIdInput();
    if (!userId) { alert("Enter User ID to fetch booking history"); return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(userId)}/all-bookings/history`);
        if (!res.ok) {
            const errorText = await res.text();
            alert("Failed to fetch booking history: " + errorText);
            return;
        }
        const history = await res.json();
        displayBookingHistory(history);
        document.getElementById("bookingsResponse").textContent = "Fetched booking history successfully";
    } catch (e) {
        alert("Error fetching booking history: " + e.message);
    }
}
function displayBookingHistory(history) {
    const tbody = document.querySelector("#bookingHistoryTable tbody");
    tbody.innerHTML = "";
    history.forEach((b) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${b.id || ""}</td>
            <td>${b.userId || ""}</td>
            <td>${b.spotId || ""}</td>
            <td>${b.lotId || ""}</td>
            <td>${b.checkInTime || ""}</td>
            <td>${b.checkOutTime || ""}</td>
            <td>${b.status || ""}</td>
            <td>${b.amount?.toFixed(2) || "0.00"}</td>
            <td>${b.vehicleNumber || ""}</td>
        `;
        tbody.appendChild(tr);
    });
}

// Filtered Bookings
async function fetchFilteredBookings(status, lotId, fromDate, toDate, page, size) {
    let url = `${baseUrl}/bookings?`;
    if (status) url += `status=${encodeURIComponent(status)}&`;
    if (lotId) url += `lotId=${encodeURIComponent(lotId)}&`;
    if (fromDate) url += `fromDate=${encodeURIComponent(fromDate)}&`;
    if (toDate) url += `toDate=${encodeURIComponent(toDate)}&`;
    if (page) url += `page=${encodeURIComponent(page)}&`;
    if (size) url += `size=${encodeURIComponent(size)}&`;
    try {
        const res = await fetch(url);
        if (!res.ok) {
            alert("Failed to fetch filtered bookings");
            return;
        }
        const bookings = await res.json();
        displayBookings(bookings);
        document.getElementById("bookingsResponse").textContent = "Fetched filtered bookings";
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// Check-in/out and Penalty
async function checkIn(userId, bookingId) {
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(bookingId)}/checkin`;
        // Send bookingId as qrCode in body
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ qrCode: bookingId })
        });
        if (res.ok) {
            fetchBookings();
        } else {
            const errorText = await res.text();
            alert("Check-in failed: " + errorText);
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function checkOut(userId, bookingId) {
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(bookingId)}/checkout`;
        // Send bookingId as qrCode in body
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ qrCode: bookingId })
        });
        if (res.ok) {
            fetchBookings();
        } else {
            const errorText = await res.text();
            alert("Check-out failed: " + errorText);
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function cancelBooking(userId, bookingId) {
    if (!userId || !bookingId) { alert("Enter User ID and Booking ID"); return; }
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(bookingId)}/cancel`;
        const res = await fetch(url, { method: "POST" });
        if (res.ok) {
            fetchBookings();
        } else {
            const errorText = await res.text();
            alert("Cancel failed: " + errorText);
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function fetchPenaltyInfo(userId, bookingId) {
    try {
        const url = `${baseUrl}/users/${encodeURIComponent(userId)}/bookings/${encodeURIComponent(bookingId)}/penalty`;
        const res = await fetch(url);
        if (!res.ok) {
            alert("Failed to fetch penalty info");
            return;
        }
        const penalty = await res.json();
        alert("Penalty amount: " + penalty);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// Get all bookings
async function fetchAllBookings() {
    try {
        const res = await fetch(`${baseUrl}/bookings`);
        if (!res.ok) {
            alert("Failed to fetch all bookings");
            return;
        }
        const bookings = await res.json();
        displayBookings(bookings);
        document.getElementById("bookingsResponse").textContent = "Fetched all bookings";
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// SPOTS
async function resetAllSpots() {
    if (!confirm("Are you sure you want to reset all parking spots to max capacity?")) return;
    try {
        const res = await fetch(`${baseUrl}/admin/reset-spots`, { method: "POST" });
        if (res.ok) {
            alert("All parking spots have been reset.");
            fetchSpots(); // Refresh the spots table
        } else {
            const errorText = await res.text();
            alert("Failed to reset spots: " + errorText);
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function fetchSpots() {
    const res = await fetch(`${baseUrl}/parking-spots`);
    if (!res.ok) { alert("Failed to fetch parking spots"); return; }
    const spots = await res.json();
    displaySpots(spots);
}
function displaySpots(spots) {
    const tbody = document.querySelector("#spotsTable tbody");
    tbody.innerHTML = "";
    spots.forEach((s) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
        <td>${s.id || ""}</td>
        <td>${s.lotId || ""}</td>
        <td>${s.zoneName || ""}</td>
        <td>${s.capacity || ""}</td>
        <td>${s.available || ""}</td>
        <td>
            <button onclick="deleteSpot('${s.id}')">Delete</button>
        </td>
    `;
        tbody.appendChild(tr);
    });
}
async function createSpot() {
    const lotId = document.getElementById("spotLotId").value.trim();
    const zoneName = document.getElementById("spotZoneName").value.trim();
    const capacity = parseInt(document.getElementById("spotCapacity").value);
    const available = parseInt(document.getElementById("spotAvailable").value);
    if (!lotId || !zoneName || isNaN(capacity) || isNaN(available)) {
        alert("Fill all required spot fields correctly.");
        return;
    }
    const spot = { lotId, zoneName, capacity, available };
    try {
        const res = await fetch(`${baseUrl}/parking-spots`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(spot),
        });
        if (res.ok) {
            fetchSpots();
        } else {
            alert("Failed to create spot");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function deleteSpot(id) {
    if (!confirm("Are you sure you want to delete this spot?")) return;
    try {
        const res = await fetch(`${baseUrl}/parking-spots/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (res.ok) {
            fetchSpots();
        } else {
            alert("Failed to delete spot");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// LOTS
async function fetchLots() {
    const res = await fetch(`${baseUrl}/parking-lots`);
    if (!res.ok) { alert("Failed to fetch parking lots"); return; }
    const lots = await res.json();
    displayLots(lots);
}
function displayLots(lots) {
    const tbody = document.querySelector("#lotsTable tbody");
    tbody.innerHTML = "";
    lots.forEach((lot) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
        <td>${lot.id || ""}</td>
        <td>${lot.name || ""}</td>
        <td>${lot.location || ""}</td>
        <td>${lot.description || ""}</td>
        <td>
            <button onclick="deleteLot('${lot.id}')">Delete</button>
        </td>
    `;
        tbody.appendChild(tr);
    });
}
async function createLot() {
    const name = document.getElementById("lotName").value.trim();
    const location = document.getElementById("lotLocation").value.trim();
    const description = document.getElementById("lotDescription").value.trim();
    if (!name || !location) {
        alert("Fill at least name and location for the lot.");
        return;
    }
    const lot = { name, location, description };
    try {
        const res = await fetch(`${baseUrl}/parking-lots`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(lot),
        });
        if (res.ok) {
            fetchLots();
        } else {
            alert("Failed to create lot");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}
async function deleteLot(id) {
    if (!confirm("Are you sure you want to delete this lot?")) return;
    try {
        const res = await fetch(`${baseUrl}/parking-lots/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (res.ok) {
            fetchLots();
        } else {
            alert("Failed to delete lot");
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// WALLET
async function fetchWallet() {
    const userId = document.getElementById("walletUserId").value.trim();
    if (!userId) { alert("Enter User ID"); return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(userId)}/wallet`);
        if (!res.ok) { alert("Wallet not found"); return; }
        const wallet = await res.json();
        displayWallet(wallet);
        document.getElementById("walletResponse").textContent = "Wallet fetched successfully";
    } catch (e) {
        alert("Error: " + e.message);
    }
}
// Add to WALLET section

// Initiate payment (calls backend, shows orderId)
async function initiateWalletPayment() {
    const userId = document.getElementById("topUpUserId").value.trim();
    const amount = parseFloat(document.getElementById("topUpAmount").value);
    if (!userId || isNaN(amount) || amount <= 0) {
        alert("Enter valid User ID and amount (positive number)");
        return;
    }
    try {
        const res = await fetch(`${baseUrl}/payments/initiate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId, amount }),
        });
        const data = await res.json();
        if (res.ok && data.orderId) {
            document.getElementById("walletResponse").textContent = "Payment order created: " + data.orderId;
            // For demo: simulate payment success after 2s
            setTimeout(() => {
                handleWalletPaymentCallback(data.orderId, "payment_test_" + Date.now(), true, userId, amount);
            }, 2000);
        } else {
            alert("Failed to initiate payment: " + (data.error || ""));
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// Simulate payment callback (in real, this is from Razorpay/webhook)
async function handleWalletPaymentCallback(orderId, paymentId, success, userId, amount) {
    try {
        const res = await fetch(`${baseUrl}/payments/callback`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ orderId, paymentId, success, userId, amount }),
        });
        const data = await res.json();
        if (res.ok && data.status === "success") {
            document.getElementById("walletResponse").textContent = "Wallet top-up successful!";
            fetchWallet();
        } else {
            alert("Payment failed: " + (data.error || ""));
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function topUpWallet() {
    const userId = document.getElementById("topUpUserId").value.trim();
    const amount = parseFloat(document.getElementById("topUpAmount").value);
    if (!userId || isNaN(amount) || amount <= 0) {
        alert("Enter valid User ID and amount (positive number)");
        return;
    }
    try {
        const res = await fetch(`${baseUrl}/payments/initiate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId, amount }),
        });
        const data = await res.json();
        if (res.ok && data.orderId) {
            const options = {
                key: "YOUR_RAZORPAY_KEY", // Replace with your Razorpay test key
                amount: amount * 100,
                currency: "INR",
                name: "Parking App Wallet Top-Up",
                description: "Wallet Recharge",
                order_id: data.orderId,
                handler: async function (response) {
                    await fetch(`${baseUrl}/payments/callback`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            orderId: data.orderId,
                            paymentId: response.razorpay_payment_id,
                            success: true,
                            userId,
                            amount
                        }),
                    });
                    document.getElementById("walletResponse").textContent = "Wallet top-up successful!";
                    fetchWallet();
                },
                prefill: { email: "", contact: "" },
                theme: { color: "#3399cc" }
            };
            const rzp = new Razorpay(options);
            rzp.open();
        } else {
            alert("Failed to initiate payment: " + (data.error || ""));
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// Optionally, add a button in your HTML for "Simulate Payment Success" if you want manual control
function displayWallet(wallet) {
    const tbody = document.querySelector("#walletTable tbody");
    tbody.innerHTML = "";
    const tr = document.createElement("tr");
    tr.innerHTML = `
    <td>${wallet.id || ""}</td>
    <td>${wallet.userId || ""}</td>
    <td>${wallet.balance?.toFixed(2) || "0.00"}</td>
    <td>${wallet.lastUpdated ? new Date(wallet.lastUpdated).toLocaleString() : ""}</td>
`;
    tbody.appendChild(tr);
}
async function fetchWalletTransactions() {
    const userId = document.getElementById("walletUserId").value.trim();
    if (!userId) { alert("Enter User ID"); return; }
    try {
        const res = await fetch(`${baseUrl}/users/${encodeURIComponent(userId)}/wallet/transactions`);
        if (!res.ok) { alert("Failed to fetch wallet transactions"); return; }
        const txs = await res.json();
        displayWalletTransactions(txs);
    } catch (e) {
        alert("Error: " + e.message);
    }
}
function displayWalletTransactions(txs) {
    const tbody = document.querySelector("#walletTxTable tbody");
    tbody.innerHTML = "";
    txs.forEach((tx) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
        <td>${tx.referenceId || ""}</td>
        <td>${tx.type || ""}</td>
        <td>${tx.amount?.toFixed(2) || "0.00"}</td>
        <td>${tx.method || ""}</td>
        <td>${tx.timestamp ? new Date(tx.timestamp).toLocaleString() : ""}</td>
        <td>${tx.status || ""}</td>
    `;
        tbody.appendChild(tr);
    });
}

// Initialize on page load
showSection("users");

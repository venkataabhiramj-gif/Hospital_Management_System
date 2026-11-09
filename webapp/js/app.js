// ApexCare Memorial Hospital — ClinicDesk Outpatient System Controller
const API_BASE = '/api';

// ========================================================
// AUTHENTICATION & SESSION STATE MANAGEMENT
// ========================================================
function getAuthUser() {
  try {
    const raw = sessionStorage.getItem('apexcare_auth');
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to parse auth session:', e);
  }
  // Default to demo admin so app is always functional if opened directly
  return {
    success: true,
    profileType: 'ADMIN',
    role: 'ADMIN',
    username: 'admin',
    displayName: 'System Administrator'
  };
}

function setAuthUser(user) {
  sessionStorage.setItem('apexcare_auth', JSON.stringify(user));
}

function logoutUser() {
  sessionStorage.removeItem('apexcare_auth');
  window.location.href = '/login.html';
}

function requireAuth(allowedProfile) {
  const raw = sessionStorage.getItem('apexcare_auth');
  if (!raw) {
    window.location.href = '/login.html';
    return;
  }
  const user = JSON.parse(raw);
  if (allowedProfile && user.profileType !== allowedProfile && user.role !== 'ADMIN') {
    if (user.profileType === 'PATIENT') {
      window.location.href = '/patient-portal.html';
    } else {
      window.location.href = '/';
    }
  }
}

// Injects active user pill and synchronizes sidebar navigation according to role
function syncHospitalInterface() {
  const user = getAuthUser();
  if (!user) return;

  // 1. Update Topbar
  const topbarRight = document.querySelector('.top-bar-right');
  if (topbarRight) {
    // Check if user pill already exists
    let pill = document.getElementById('active-user-pill');
    if (!pill) {
      pill = document.createElement('div');
      pill.id = 'active-user-pill';
      pill.className = 'user-profile-pill';
      topbarRight.prepend(pill);
    }
    const initials = getInitials(user.displayName);
    const roleLabel = (user.role || user.profileType || 'STAFF').replace('_', ' ');

    pill.innerHTML = `
      <div class="user-avatar">${initials}</div>
      <div>
        <div class="user-meta-name">${user.displayName}</div>
        <div style="font-size: 10px; color: var(--text-dim);">${user.specialty || user.patientNumber || 'Facility 01'}</div>
      </div>
      <span class="user-meta-role">${roleLabel}</span>
    `;

    // Ensure sign out button exists
    let signoutBtn = document.getElementById('btn-global-signout');
    if (!signoutBtn) {
      signoutBtn = document.createElement('button');
      signoutBtn.id = 'btn-global-signout';
      signoutBtn.className = 'btn-signout';
      signoutBtn.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
        Switch Profile
      `;
      signoutBtn.onclick = logoutUser;
      topbarRight.appendChild(signoutBtn);
    }
  }

  // 2. Adjust Sidebar Navigation based on profile
  const sidebarNav = document.querySelector('.sidebar-nav');
  if (sidebarNav) {
    const currentPath = window.location.pathname;

    if (user.profileType === 'PATIENT') {
      sidebarNav.innerHTML = `
        <li class="nav-item">
          <a href="/patient-portal.html" class="nav-link ${currentPath.includes('patient-portal') ? 'active' : ''}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="3" width="7" height="9"></rect><rect x="14" y="3" width="7" height="5"></rect><rect x="14" y="12" width="7" height="9"></rect><rect x="3" y="16" width="7" height="5"></rect></svg>
            <span>Patient Health Home</span>
          </a>
        </li>
        <li class="nav-item">
          <a href="/patient-portal.html#book-section" class="nav-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            <span>Book Appointment</span>
          </a>
        </li>
        <li class="nav-item">
          <a href="/patient-portal.html#appointments-section" class="nav-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <span>My Encounters</span>
          </a>
        </li>
        <li class="nav-item">
          <a href="/patient-portal.html#prescriptions-section" class="nav-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
            <span>My Prescriptions</span>
          </a>
        </li>
        <li class="nav-item">
          <a href="/patient-portal.html#billing-section" class="nav-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
            <span>Invoices & Receipts</span>
          </a>
        </li>
      `;
    }
  }
}

// Helper: Generic API client
async function apiCall(endpoint, method = 'GET', data = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  };
  if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    options.body = JSON.stringify(data);
  }

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, options);
    const text = await res.text();
    let result = {};
    try {
      result = text ? JSON.parse(text) : {};
    } catch (parseErr) {
      result = { raw: text };
    }
    if (!res.ok) {
      throw new Error(result.error || result.message || `Error ${res.status}: ${res.statusText}`);
    }
    return result;
  } catch (err) {
    console.error('[EHR-API]', err);
    throw err;
  }
}

// Modal management
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

// System notification toast
function showAlert(message, type = 'success') {
  const alertBox = document.getElementById('global-alert');
  if (!alertBox) return;
  alertBox.className = `alert alert-${type} active`;
  alertBox.innerHTML = `
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      ${type === 'success' 
        ? '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>'
        : '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>'}
    </svg>
    <span>${message}</span>
  `;
  setTimeout(() => {
    alertBox.classList.remove('active');
  }, 4500);
}

// Helper: Extract Initials for Avatars
function getInitials(name) {
  if (!name) return 'HP';
  const parts = name.replace(/^Dr\.\s*/i, '').trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Helper: Format Indian Rupee Currency
function formatINR(amount) {
  return '₹' + Number(amount || 0).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

// ========================================================
// 1. DASHBOARD & ROLE-BASED HOME MODULE
// ========================================================
async function initDashboard() {
  syncHospitalInterface();
  const user = getAuthUser();

  const adminView = document.getElementById('admin-dashboard-view');
  const doctorView = document.getElementById('doctor-opd-view');
  const patientBanner = document.getElementById('patient-redirect-banner');

  if (user.profileType === 'PATIENT') {
    if (adminView) adminView.style.display = 'none';
    if (doctorView) doctorView.style.display = 'none';
    if (patientBanner) patientBanner.style.display = 'block';
    return;
  }

  if (user.role === 'DOCTOR' || user.profileType === 'STAFF') {
    if (adminView) adminView.style.display = 'none';
    if (doctorView) doctorView.style.display = 'block';
    if (patientBanner) patientBanner.style.display = 'none';
    initDoctorOPDDesk(user);
    return;
  }

  // Otherwise, render full executive admin dashboard
  if (adminView) adminView.style.display = 'block';
  if (doctorView) doctorView.style.display = 'none';
  if (patientBanner) patientBanner.style.display = 'none';

  try {
    const stats = await apiCall('/dashboard');
    document.getElementById('stat-patients').textContent = stats.patients;
    document.getElementById('stat-doctors').textContent = stats.doctors;
    document.getElementById('stat-appointments').textContent = stats.appointments;
    document.getElementById('stat-revenue').textContent = formatINR(stats.revenue);

    // Populate active encounters
    const appointments = await apiCall('/appointments');
    const tbody = document.getElementById('recent-appointments-tbody');
    if (tbody) {
      if (appointments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 24px; color: var(--text-dim);">No clinical encounters scheduled for today.</td></tr>';
      } else {
        tbody.innerHTML = appointments.slice(0, 8).map(a => {
          const statusClass = a.status === 'COMPLETED' ? 'status-completed' : (a.status === 'CANCELLED' ? 'status-cancelled' : 'status-booked');
          const dateStr = a.date || (a.startsAt ? new Date(a.startsAt).toLocaleDateString('en-IN') : 'Today');
          const timeStr = a.time || (a.startsAt ? new Date(a.startsAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '09:00 AM');

          return `
            <tr>
              <td><span class="mrn-badge">#APT-${a.id}</span></td>
              <td>
                <div style="font-weight:700; color:var(--brand-navy);">${a.patientName}</div>
                <div style="font-size:11px; color:var(--text-dim);">${a.patientId || ''}</div>
              </td>
              <td><strong>${a.doctorName}</strong></td>
              <td>${dateStr}</td>
              <td style="font-family: var(--font-mono);">${timeStr}</td>
              <td><span class="status-pill ${statusClass}">${a.status}</span></td>
            </tr>
          `;
        }).join('');
      }
    }
  } catch (err) {
    showAlert('Telemetry link error: ' + err.message, 'error');
  }
}

// Doctor OPD Queue & Consultation Desk
let activeSelectedAppointment = null;

async function initDoctorOPDDesk(user) {
  const docGreeting = document.getElementById('doctor-welcome-title');
  if (docGreeting) {
    docGreeting.textContent = `${user.displayName} — OPD Clinical Encounter Desk`;
  }
  const docMeta = document.getElementById('doctor-welcome-meta');
  if (docMeta) {
    docMeta.textContent = `Specialty: ${user.specialty || 'General Medicine & Cardiology'} · Facility 01 Outpatient Room 03`;
  }

  loadDoctorQueue(user.doctorId);
}

async function loadDoctorQueue(doctorId) {
  const queueContainer = document.getElementById('doctor-queue-container');
  if (!queueContainer) return;

  try {
    const queue = await apiCall(`/doctor/queue${doctorId ? '?doctorId=' + doctorId : ''}`);
    if (queue.length === 0) {
      queueContainer.innerHTML = '<div style="padding:24px; text-align:center; color:var(--text-dim);">No waiting patients in today’s OPD queue.</div>';
      return;
    }

    queueContainer.innerHTML = queue.map((item, idx) => {
      const isSelected = activeSelectedAppointment && activeSelectedAppointment.appointment_id === item.appointment_id;
      const statusClass = item.status === 'COMPLETED' ? 'status-completed' : (item.status === 'CANCELLED' ? 'status-cancelled' : 'status-booked');

      return `
        <div class="queue-patient-card ${isSelected ? 'selected' : ''}" onclick="selectQueuePatient(${item.appointment_id})">
          <div class="queue-card-top">
            <span class="mrn-badge">${item.patient_number}</span>
            <span class="status-pill ${statusClass}">${item.status}</span>
          </div>
          <div class="queue-patient-name">${item.patient_name}</div>
          <div style="font-size: 11.5px; color: var(--text-muted);">
            <span>${item.age} Yrs</span> • <span>${item.phone || ''}</span>
          </div>
          <div style="font-size: 11.5px; color: var(--primary); font-weight: 600; margin-top: 2px;">
            Reason: ${item.reason || 'General Outpatient Consultation'}
          </div>
        </div>
      `;
    }).join('');

    // Auto-select first appointment if none selected
    if (!activeSelectedAppointment && queue.length > 0) {
      selectQueuePatient(queue[0].appointment_id);
    }
  } catch (err) {
    console.error('Failed to load doctor queue:', err);
  }
}

async function selectQueuePatient(appointmentId) {
  try {
    const queue = await apiCall('/doctor/queue');
    const selected = queue.find(q => q.appointment_id === appointmentId);
    if (!selected) return;

    activeSelectedAppointment = selected;

    // Highlight card
    document.querySelectorAll('.queue-patient-card').forEach(c => c.classList.remove('selected'));

    // Populate consultation workspace
    document.getElementById('consult-patient-name').textContent = selected.patient_name;
    document.getElementById('consult-patient-mrn').textContent = selected.patient_number;
    document.getElementById('consult-patient-meta').textContent = `${selected.age} Yrs · ${selected.phone || '+91 00000 00001'}`;
    document.getElementById('consult-appointment-id').value = selected.appointment_id;

    document.getElementById('consult-complaint').value = selected.reason || 'Routine medical checkup and symptom assessment';
    document.getElementById('consult-bp').value = '120/80';
    document.getElementById('consult-pulse').value = '74';
    document.getElementById('consult-temp').value = '36.8';
    document.getElementById('consult-weight').value = '68.5';
    document.getElementById('consult-notes').value = 'Patient presented with standard symptoms. Physical examination normal. Vitals stable. Advised follow-up in 2 weeks.';
    document.getElementById('consult-diagnosis').value = 'General Outpatient Evaluation / Essential Hypertension (ICD-10 I10)';
    document.getElementById('consult-prescription').value = 'Tab. Amoxicillin 500mg PO TDS × 5 days\nTab. Paracetamol 650mg PO SOS for fever\nTab. Pantoprazole 40mg OD before breakfast';

    document.getElementById('consultation-room-box').style.display = 'block';
  } catch (err) {
    showAlert('Error selecting patient: ' + err.message, 'error');
  }
}

async function handleFinalizeConsultation(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-finalize-consult');
  btn.disabled = true;

  const payload = {
    appointmentId: document.getElementById('consult-appointment-id').value,
    complaint: document.getElementById('consult-complaint').value.trim(),
    bloodPressure: document.getElementById('consult-bp').value.trim(),
    pulsePerMinute: document.getElementById('consult-pulse').value.trim(),
    temperatureCelsius: document.getElementById('consult-temp').value.trim(),
    weightKg: document.getElementById('consult-weight').value.trim(),
    clinicalNotes: document.getElementById('consult-notes').value.trim(),
    diagnosisText: document.getElementById('consult-diagnosis').value.trim(),
    prescriptionText: document.getElementById('consult-prescription').value.trim()
  };

  try {
    const res = await apiCall('/doctor/consult', 'POST', payload);
    showAlert('Consultation finalized! Vitals and EHR record committed.', 'success');
    loadDoctorQueue();
  } catch (err) {
    showAlert('Failed to finalize encounter: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
  }
}

// ========================================================
// 2. PATIENTS MODULE
// ========================================================
async function initPatients() {
  syncHospitalInterface();
  const tbody = document.getElementById('patients-tbody');
  try {
    const patients = await apiCall('/patients');
    if (patients.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: var(--text-dim);">No active patient records found.</td></tr>';
      return;
    }

    tbody.innerHTML = patients.map(p => `
      <tr>
        <td><span class="mrn-badge">${p.id}</span></td>
        <td>
          <div style="display:flex; align-items:center; gap:10px;">
            <div class="user-avatar" style="width:28px; height:28px; font-size:11px;">${getInitials(p.name)}</div>
            <div>
              <strong style="color:var(--brand-navy);">${p.name}</strong>
            </div>
          </div>
        </td>
        <td>${p.age || '32'}</td>
        <td>${p.gender || 'Unknown'}</td>
        <td><span style="font-family: var(--font-mono);">${p.contact}</span></td>
        <td><span class="status-pill status-active">Active</span></td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="archivePatient('${p.id}')">
            Archive
          </button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function addPatient(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/patients', 'POST', data);
    showAlert(res.message || 'Patient registered successfully', 'success');
    closeModal('patient-modal');
    form.reset();
    initPatients();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function archivePatient(id) {
  if (!confirm(`Archive patient record #${id}?`)) return;
  try {
    const res = await apiCall(`/patients?id=${id}`, 'DELETE');
    showAlert(res.message, 'success');
    initPatients();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

// ========================================================
// 3. DOCTORS MODULE
// ========================================================
async function initDoctors() {
  syncHospitalInterface();
  const tbody = document.getElementById('doctors-tbody');
  try {
    const doctors = await apiCall('/doctors');
    if (doctors.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 32px; color: var(--text-dim);">No registered medical specialists.</td></tr>';
      return;
    }

    tbody.innerHTML = doctors.map(d => `
      <tr>
        <td><span class="mrn-badge">#DOC-${d.id}</span></td>
        <td>
          <div style="display:flex; align-items:center; gap:10px;">
            <div class="user-avatar" style="width:28px; height:28px; font-size:11px;">${getInitials(d.name)}</div>
            <strong style="color:var(--brand-navy);">${d.name}</strong>
          </div>
        </td>
        <td><span class="mrn-badge">${d.specialization}</span></td>
        <td>${d.contact || '+91 80 4000 1200'}</td>
        <td><span class="status-pill status-completed">OPD Active (09:00 - 17:00)</span></td>
        <td>
          <a href="/appointments.html" class="btn btn-secondary btn-sm">Schedule Visit</a>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function addDoctor(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/doctors/web', 'POST', data);
    showAlert(res.message || 'Medical specialist registered', 'success');
    closeModal('doctor-modal');
    form.reset();
    initDoctors();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

// ========================================================
// 4. APPOINTMENTS MODULE
// ========================================================
async function initAppointments() {
  syncHospitalInterface();
  const tbody = document.getElementById('appointments-tbody');

  try {
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    const patientSelect = document.getElementById('appt-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}">${p.name} [${p.id}]</option>`).join('');
    }

    const doctorSelect = document.getElementById('appt-doctor-select');
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Attending Clinician --</option>' +
        doctors.map(d => `<option value="${d.id}">${d.name} (${d.specialization})</option>`).join('');
    }

    const appointments = await apiCall('/appointments');
    if (appointments.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: var(--text-dim);">No scheduled appointments found.</td></tr>';
      return;
    }

    tbody.innerHTML = appointments.map(a => {
      const statusClass = a.status === 'COMPLETED' ? 'status-completed' : (a.status === 'CANCELLED' ? 'status-cancelled' : 'status-booked');
      const dateStr = a.date || (a.startsAt ? new Date(a.startsAt).toLocaleDateString('en-IN') : 'Scheduled');
      const timeStr = a.time || (a.startsAt ? new Date(a.startsAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '10:00 AM');

      return `
        <tr>
          <td><span class="mrn-badge">#APT-${a.id}</span></td>
          <td>
            <strong style="color:var(--brand-navy);">${a.patientName}</strong>
            <div style="font-size:11px; color:var(--text-dim);">${a.patientId || ''}</div>
          </td>
          <td><strong>${a.doctorName}</strong></td>
          <td>${dateStr}</td>
          <td style="font-family: var(--font-mono);">${timeStr}</td>
          <td><span class="status-pill ${statusClass}">${a.status}</span></td>
          <td>
            <button class="btn btn-outline btn-sm" onclick="cancelAppointmentAdmin(${a.id})">
              Cancel
            </button>
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function addAppointment(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/appointments', 'POST', data);
    showAlert(res.message || 'Appointment scheduled', 'success');
    closeModal('appointment-modal');
    form.reset();
    initAppointments();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function cancelAppointmentAdmin(id) {
  if (!confirm(`Cancel appointment #APT-${id}?`)) return;
  try {
    await apiCall(`/appointments?id=${id}`, 'DELETE');
    showAlert('Appointment cancelled', 'success');
    initAppointments();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

// ========================================================
// 5. BILLING & RCM MODULE
// ========================================================
async function initBilling() {
  syncHospitalInterface();
  const tbody = document.getElementById('billing-tbody');

  try {
    const patients = await apiCall('/patients');
    const patientSelect = document.getElementById('bill-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}">${p.name} [${p.id}]</option>`).join('');
    }

    const bills = await apiCall('/bills');
    window._cachedBills = bills;

    if (bills.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: var(--text-dim);">No billing invoices issued.</td></tr>';
      return;
    }

    tbody.innerHTML = bills.map(b => {
      const isPaid = b.paymentStatus === 'PAID';
      return `
        <tr>
          <td><span class="mrn-badge">${b.invoiceNumber || '#INV-' + b.billId}</span></td>
          <td>
            <strong style="color:var(--brand-navy);">${b.patientName}</strong>
            <div style="font-size:11px; color:var(--text-dim);">${b.patientId}</div>
          </td>
          <td>${b.billingDate}</td>
          <td style="font-weight:700; color:var(--brand-navy);">${formatINR(b.totalAmount)}</td>
          <td><span class="status-pill ${isPaid ? 'status-paid' : 'status-pending'}">${b.paymentStatus}</span></td>
          <td>
            <div style="display:flex; gap:6px;">
              ${!isPaid ? `
                <button class="btn btn-primary btn-sm" onclick="markBillPaid(${b.billId})">
                  Collect
                </button>
              ` : ''}
              <button class="btn btn-secondary btn-sm" onclick="showReceipt(${b.billId})">
                Tax Invoice
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function addBill(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/bills', 'POST', data);
    showAlert(res.message || 'Invoice generated', 'success');
    closeModal('bill-modal');
    form.reset();
    initBilling();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function markBillPaid(billId) {
  try {
    const res = await apiCall('/bills', 'POST', { action: 'markPaid', billId });
    showAlert(res.message || 'Payment recorded', 'success');
    initBilling();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

function showReceipt(billId) {
  const bill = (window._cachedBills || []).find(b => b.billId === billId);
  if (!bill) return;

  const modalBody = document.getElementById('receipt-modal-body');
  if (!modalBody) return;

  modalBody.innerHTML = `
    <div class="tax-receipt-sheet">
      <div class="receipt-header-row">
        <div class="receipt-hospital-info">
          <h3>ApexCare Memorial Hospital</h3>
          <p>Facility 01 · Central Outpatient & Diagnostics Center</p>
          <p>GSTIN: 37AAAAA0000A1Z5 · License: AP-MED-2024-8891</p>
        </div>
        <div class="receipt-meta-box">
          <div style="font-weight:700; color:var(--brand-navy);">TAX INVOICE & RECEIPT</div>
          <div style="font-family: var(--font-mono); margin-top:4px;"><strong>Inv:</strong> ${bill.invoiceNumber || 'INV-' + bill.billId}</div>
          <div style="color:var(--text-dim); margin-top:2px;">Date: ${bill.billingDate}</div>
        </div>
      </div>

      <div style="display:flex; justify-content:space-between; background:var(--bg-subtle); padding:12px 16px; border-radius:8px;">
        <div>
          <div style="font-size:11px; text-transform:uppercase; color:var(--text-dim); font-weight:700;">Billed To</div>
          <strong style="color:var(--brand-navy);">${bill.patientName}</strong>
          <div style="font-family:var(--font-mono); font-size:11.5px; color:var(--text-muted);">MRN: ${bill.patientId}</div>
        </div>
        <div style="text-align:right;">
          <div style="font-size:11px; text-transform:uppercase; color:var(--text-dim); font-weight:700;">Status</div>
          <span class="status-pill ${bill.paymentStatus === 'PAID' ? 'status-paid' : 'status-pending'}">${bill.paymentStatus}</span>
        </div>
      </div>

      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="border-bottom:1px solid var(--border-subtle); background:var(--bg-subtle); text-align:left;">
            <th style="padding:8px 10px;">Service / Medicine Item</th>
            <th style="padding:8px 10px; text-align:right;">Amount (₹)</th>
          </tr>
        </thead>
        <tbody>
          <tr style="border-bottom:1px solid var(--border-subtle);">
            <td style="padding:10px;">Doctor Outpatient Consultation</td>
            <td style="padding:10px; text-align:right;">${formatINR(bill.consultationFee)}</td>
          </tr>
          <tr style="border-bottom:1px solid var(--border-subtle);">
            <td style="padding:10px;">Treatment & Clinical Procedures</td>
            <td style="padding:10px; text-align:right;">${formatINR(bill.treatmentCharges)}</td>
          </tr>
          <tr style="border-bottom:1px solid var(--border-subtle);">
            <td style="padding:10px;">Dispensary Medications</td>
            <td style="padding:10px; text-align:right;">${formatINR(bill.medicineCharges)}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td style="padding:12px 10px; font-weight:700; color:var(--brand-navy);">Total Payable:</td>
            <td style="padding:12px 10px; text-align:right; font-weight:800; color:var(--primary); font-size:15px;">${formatINR(bill.totalAmount)}</td>
          </tr>
        </tfoot>
      </table>

      <div class="receipt-stamp-box">
        <div>
          <p>Electronically generated through ClinicDesk Unified System.</p>
          <p>Database: data/hospital.mv.db · No physical signature required.</p>
        </div>
        <div class="official-seal">✓ OFFICIAL RECEIPT PAID</div>
      </div>
    </div>
  `;

  openModal('receipt-modal');
}

// ========================================================
// 6. CLINICAL EHR & MEDICAL RECORDS MODULE
// ========================================================
async function initRecords() {
  syncHospitalInterface();
  const tbody = document.getElementById('records-tbody');

  try {
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    const patientSelect = document.getElementById('rec-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} [${p.id}]</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('rec-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const doctorSelect = document.getElementById('rec-doctor-select');
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Attending Clinician --</option>' +
        doctors.map(d => `<option value="${d.id}" data-name="${d.name}">${d.name} (${d.specialization})</option>`).join('');
      doctorSelect.onchange = function() {
        const selected = doctorSelect.options[doctorSelect.selectedIndex];
        document.getElementById('rec-doctor-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const records = await apiCall('/records');
    window._cachedRecords = records;

    if (records.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: var(--text-dim);">No medical consultation logs in EHR.</td></tr>';
      return;
    }

    tbody.innerHTML = records.map(r => `
      <tr>
        <td><span class="mrn-badge">#EHR-${r.recordId}</span></td>
        <td>
          <div style="font-weight:700; color:var(--brand-navy);">${r.patientName}</div>
          <div style="font-size:11px; color:var(--text-dim);">${r.patientId}</div>
        </td>
        <td><strong>${r.doctorName}</strong></td>
        <td>
          <div style="font-weight:600; color:var(--brand-navy);">${r.diagnosis}</div>
          <div style="font-size:11px; color:var(--text-muted); margin-top:2px;">${r.labTests || 'Routine examination'}</div>
        </td>
        <td>${r.recordDate}</td>
        <td>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-secondary btn-sm" onclick="viewRecordDetail(${r.recordId})">
              Review Chart
            </button>
            <button class="btn btn-danger btn-sm" onclick="deleteRecord(${r.recordId})">
              Delete
            </button>
          </div>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

function viewRecordDetail(recordId) {
  const r = (window._cachedRecords || []).find(rec => rec.recordId === recordId);
  if (!r) return;

  const detailBody = document.getElementById('record-detail-body');
  if (!detailBody) return;

  detailBody.innerHTML = `
    <div style="display:flex; flex-direction:column; gap:16px;">
      <div style="display:flex; justify-content:space-between; border-bottom:1px solid var(--border-subtle); padding-bottom:12px;">
        <div>
          <h3 style="font-size:16px; font-weight:700; color:var(--brand-navy);">${r.patientName}</h3>
          <p style="font-size:12px; color:var(--text-muted);">MRN: <strong>${r.patientId}</strong> · Date: <strong>${r.recordDate}</strong></p>
        </div>
        <div style="text-align:right;">
          <div style="font-weight:700; color:var(--primary);">${r.doctorName}</div>
          <span style="font-size:11px; color:var(--text-dim);">Attending Physician</span>
        </div>
      </div>

      <div>
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:var(--text-muted);">Clinical Diagnosis</label>
        <div style="background:var(--bg-subtle); border:1px solid var(--border-card); border-radius:6px; padding:10px; font-weight:600; margin-top:4px;">
          ${r.diagnosis}
        </div>
      </div>

      <div>
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:var(--text-muted);">Prescribed Medications (Rx)</label>
        <div style="background:var(--bg-subtle); border:1px solid var(--border-card); border-radius:6px; padding:10px; font-size:12.5px; line-height:1.6; white-space:pre-wrap; margin-top:4px;">
          ${r.prescription}
        </div>
      </div>

      <div>
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:var(--text-muted);">Clinical Notes & Assessment</label>
        <div style="background:var(--bg-subtle); border:1px solid var(--border-card); border-radius:6px; padding:10px; font-size:12.5px; color:var(--text-main); margin-top:4px;">
          ${r.notes || 'Routine follow-up prescribed.'}
        </div>
      </div>
    </div>
  `;

  openModal('record-detail-modal');
}

async function addMedicalRecord(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/records', 'POST', data);
    showAlert(res.message, 'success');
    closeModal('record-modal');
    form.reset();
    initRecords();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

async function deleteRecord(id) {
  if (!confirm(`Permanently remove EHR record #EHR-${id}?`)) return;
  try {
    const res = await apiCall(`/records?id=${id}`, 'DELETE');
    showAlert(res.message, 'success');
    initRecords();
  } catch (err) {
    showAlert(err.message, 'error');
  }
}

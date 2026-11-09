// Hospital Management System - Frontend Controller
const API_BASE = '/api';

// Generic API helper
async function apiCall(endpoint, method = 'GET', data = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };
  if (data && (method === 'POST' || method === 'PUT')) {
    options.body = JSON.stringify(data);
  }

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, options);
    const result = await res.json();
    if (!res.ok) {
      throw new Error(result.error || `Error ${res.status}: ${res.statusText}`);
    }
    return result;
  } catch (err) {
    console.error('API Error:', err);
    throw err;
  }
}

// Modal open/close helpers
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

// Global alert banner
function showAlert(message, type = 'success') {
  const alertBox = document.getElementById('global-alert');
  if (!alertBox) return;
  alertBox.className = `alert alert-${type} active`;
  alertBox.textContent = message;
  setTimeout(() => {
    alertBox.classList.remove('active');
  }, 4000);
}

// --- Dashboard Module ---
async function initDashboard() {
  try {
    const stats = await apiCall('/dashboard');
    document.getElementById('stat-patients').textContent = stats.patients;
    document.getElementById('stat-doctors').textContent = stats.doctors;
    document.getElementById('stat-appointments').textContent = stats.appointments;
    document.getElementById('stat-revenue').textContent = `₹${stats.revenue.toFixed(2)}`;

    // Load recent appointments
    const appointments = await apiCall('/appointments');
    const tbody = document.getElementById('recent-appointments-tbody');
    if (tbody) {
      tbody.innerHTML = appointments.slice(0, 5).map(a => `
        <tr>
          <td><strong>#${a.id}</strong></td>
          <td>${a.patientName} (${a.patientId})</td>
          <td>${a.doctorName}</td>
          <td>${a.appointmentDate}</td>
          <td>${a.timeSlot}</td>
          <td><span class="badge ${a.status === 'BOOKED' ? 'badge-warning' : (a.status === 'COMPLETED' ? 'badge-success' : 'badge-danger')}">${a.status}</span></td>
        </tr>
      `).join('') || '<tr><td colspan="6" style="text-align:center; color: #94a3b8;">No appointments found.</td></tr>';
    }
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// --- Patients Module ---
async function initPatients() {
  const tbody = document.getElementById('patients-tbody');
  const searchInput = document.getElementById('patient-search');

  async function render(query = '') {
    try {
      const endpoint = query ? `/patients?q=${encodeURIComponent(query)}` : '/patients';
      const patients = await apiCall(endpoint);
      tbody.innerHTML = patients.map(p => `
        <tr>
          <td><strong>${p.id}</strong></td>
          <td>${p.name}</td>
          <td>${p.age} / ${p.gender}</td>
          <td><span class="badge badge-info">${p.bloodGroup || 'N/A'}</span></td>
          <td>${p.phone}</td>
          <td>${p.email || '—'}</td>
          <td>${p.registeredDate}</td>
          <td>
            <button class="btn btn-danger btn-sm" onclick="deletePatient('${p.id}')">Delete</button>
          </td>
        </tr>
      `).join('') || '<tr><td colspan="8" style="text-align:center; color: #94a3b8;">No patients registered yet.</td></tr>';
    } catch (err) {
      showAlert(err.message, 'danger');
    }
  }

  if (searchInput) {
    searchInput.addEventListener('input', (e) => render(e.target.value));
  }

  render();
}

async function registerPatient(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/patients', 'POST', data);
    showAlert(res.message, 'success');
    closeModal('patient-modal');
    form.reset();
    initPatients();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function deletePatient(id) {
  if (!confirm(`Are you sure you want to delete patient ${id}?`)) return;
  try {
    const res = await apiCall(`/patients?id=${encodeURIComponent(id)}`, 'DELETE');
    showAlert(res.message, 'success');
    initPatients();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// --- Doctors Module ---
async function initDoctors() {
  const tbody = document.getElementById('doctors-tbody');
  const filterSelect = document.getElementById('doctor-spec-filter');

  async function render(spec = '') {
    try {
      const endpoint = spec ? `/doctors?spec=${encodeURIComponent(spec)}` : '/doctors';
      const doctors = await apiCall(endpoint);
      tbody.innerHTML = doctors.map(d => `
        <tr>
          <td><strong>${d.id}</strong></td>
          <td>${d.name}</td>
          <td><span class="badge badge-info">${d.specialization}</span></td>
          <td>${d.qualification}</td>
          <td>${d.phone}</td>
          <td>₹${d.consultationFee.toFixed(2)}</td>
          <td>${d.availableDays} (${d.availableHours})</td>
          <td>
            <button class="btn btn-danger btn-sm" onclick="deleteDoctor('${d.id}')">Remove</button>
          </td>
        </tr>
      `).join('') || '<tr><td colspan="8" style="text-align:center; color: #94a3b8;">No doctors found.</td></tr>';
    } catch (err) {
      showAlert(err.message, 'danger');
    }
  }

  if (filterSelect) {
    filterSelect.addEventListener('change', (e) => render(e.target.value));
  }

  render();
}

async function addDoctor(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/doctors', 'POST', data);
    showAlert(res.message, 'success');
    closeModal('doctor-modal');
    form.reset();
    initDoctors();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function deleteDoctor(id) {
  if (!confirm(`Are you sure you want to remove doctor ${id}?`)) return;
  try {
    const res = await apiCall(`/doctors?id=${encodeURIComponent(id)}`, 'DELETE');
    showAlert(res.message, 'success');
    initDoctors();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// --- Appointments Module ---
async function initAppointments() {
  const tbody = document.getElementById('appointments-tbody');

  try {
    // Populate select options in booking modal
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    const patientSelect = document.getElementById('appt-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} (${p.id})</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('appt-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const doctorSelect = document.getElementById('appt-doctor-select');
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Select Doctor --</option>' +
        doctors.map(d => `<option value="${d.id}" data-name="${d.name}">${d.name} - ${d.specialization}</option>`).join('');
      doctorSelect.onchange = function() {
        const selected = doctorSelect.options[doctorSelect.selectedIndex];
        document.getElementById('appt-doctor-name').value = selected.getAttribute('data-name') || '';
      };
    }

    // Render appointments list
    const appointments = await apiCall('/appointments');
    tbody.innerHTML = appointments.map(a => `
      <tr>
        <td><strong>#${a.id}</strong></td>
        <td>${a.patientName} (${a.patientId})</td>
        <td>${a.doctorName} (${a.doctorId})</td>
        <td>${a.appointmentDate}</td>
        <td>${a.timeSlot}</td>
        <td><span class="badge ${a.status === 'BOOKED' ? 'badge-warning' : (a.status === 'COMPLETED' ? 'badge-success' : 'badge-danger')}">${a.status}</span></td>
        <td>${a.reason || 'Routine Checkup'}</td>
        <td>
          ${a.status === 'BOOKED' ? `
            <button class="btn btn-success btn-sm" onclick="updateApptStatus(${a.id}, 'COMPLETED')">Complete</button>
            <button class="btn btn-danger btn-sm" onclick="updateApptStatus(${a.id}, 'CANCELLED')">Cancel</button>
          ` : '—'}
        </td>
      </tr>
    `).join('') || '<tr><td colspan="8" style="text-align:center; color: #94a3b8;">No appointments scheduled.</td></tr>';
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function bookAppointment(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/appointments', 'POST', data);
    showAlert(res.message, 'success');
    closeModal('appointment-modal');
    form.reset();
    initAppointments();
  } catch (err) {
    // Displays AppointmentConflictException nicely!
    showAlert(err.message, 'danger');
  }
}

async function updateApptStatus(id, status) {
  try {
    const res = await apiCall('/appointments', 'POST', { action: 'updateStatus', id: id.toString(), status });
    showAlert(res.message, 'success');
    initAppointments();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// --- Billing Module ---
async function initBilling() {
  const tbody = document.getElementById('billing-tbody');

  try {
    // Populate patient select for billing
    const patients = await apiCall('/patients');
    const patientSelect = document.getElementById('bill-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} (${p.id})</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('bill-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const bills = await apiCall('/bills');
    tbody.innerHTML = bills.map(b => `
      <tr>
        <td><strong>INV-${b.billId}</strong></td>
        <td>${b.patientName} (${b.patientId})</td>
        <td>₹${b.consultationFee.toFixed(2)}</td>
        <td>₹${b.treatmentCharges.toFixed(2)}</td>
        <td>₹${b.medicineCharges.toFixed(2)}</td>
        <td>${b.discountPercent}%</td>
        <td>${b.taxPercent}%</td>
        <td><strong>₹${b.totalAmount.toFixed(2)}</strong></td>
        <td>${b.billingDate}</td>
        <td><span class="badge ${b.paymentStatus === 'PAID' ? 'badge-success' : 'badge-danger'}">${b.paymentStatus}</span></td>
        <td>
          ${b.paymentStatus !== 'PAID' ? `
            <button class="btn btn-success btn-sm" onclick="markBillPaid(${b.billId})">Pay Now</button>
          ` : '<span style="color:#10b981;font-weight:600;">✓ Settled</span>'}
        </td>
      </tr>
    `).join('') || '<tr><td colspan="11" style="text-align:center; color: #94a3b8;">No invoices generated yet.</td></tr>';
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

function calculateLiveTotal() {
  const consult = parseFloat(document.getElementById('bill-consultation').value) || 0;
  const treat = parseFloat(document.getElementById('bill-treatment').value) || 0;
  const med = parseFloat(document.getElementById('bill-medicine').value) || 0;
  const discPercent = parseFloat(document.getElementById('bill-discount').value) || 0;
  const taxPercent = parseFloat(document.getElementById('bill-tax').value) || 0;

  const subtotal = consult + treat + med;
  const discountAmount = (subtotal * discPercent) / 100.0;
  const discountedSubtotal = subtotal - discountAmount;
  const taxAmount = (discountedSubtotal * taxPercent) / 100.0;
  const finalTotal = discountedSubtotal + taxAmount;

  document.getElementById('live-subtotal').textContent = `₹${subtotal.toFixed(2)}`;
  document.getElementById('live-discount').textContent = `- ₹${discountAmount.toFixed(2)}`;
  document.getElementById('live-tax').textContent = `+ ₹${taxAmount.toFixed(2)}`;
  document.getElementById('live-total').textContent = `₹${finalTotal.toFixed(2)}`;
}

async function createBill(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/bills', 'POST', data);
    showAlert(res.message, 'success');
    closeModal('billing-modal');
    form.reset();
    initBilling();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function markBillPaid(billId) {
  try {
    const res = await apiCall('/bills', 'POST', { action: 'markPaid', billId: billId.toString() });
    showAlert(res.message, 'success');
    initBilling();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// --- Medical Records Module ---
async function initRecords() {
  const tbody = document.getElementById('records-tbody');

  try {
    // Populate select options
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    const patientSelect = document.getElementById('rec-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} (${p.id})</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('rec-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const doctorSelect = document.getElementById('rec-doctor-select');
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Select Doctor --</option>' +
        doctors.map(d => `<option value="${d.id}" data-name="${d.name}">${d.name} (${d.specialization})</option>`).join('');
      doctorSelect.onchange = function() {
        const selected = doctorSelect.options[doctorSelect.selectedIndex];
        document.getElementById('rec-doctor-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const records = await apiCall('/records');
    tbody.innerHTML = records.map(r => `
      <tr>
        <td><strong>#REC-${r.recordId}</strong></td>
        <td>${r.patientName} (${r.patientId})</td>
        <td>${r.doctorName}</td>
        <td><strong>${r.diagnosis}</strong></td>
        <td>${r.prescription}</td>
        <td>${r.labTests || 'None'}</td>
        <td>${r.notes || '—'}</td>
        <td>${r.recordDate}</td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="deleteRecord(${r.recordId})">Delete</button>
        </td>
      </tr>
    `).join('') || '<tr><td colspan="9" style="text-align:center; color: #94a3b8;">No medical records available.</td></tr>';
  } catch (err) {
    showAlert(err.message, 'danger');
  }
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
    showAlert(err.message, 'danger');
  }
}

async function deleteRecord(id) {
  if (!confirm(`Delete medical record #REC-${id}?`)) return;
  try {
    const res = await apiCall(`/records?id=${id}`, 'DELETE');
    showAlert(res.message, 'success');
    initRecords();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

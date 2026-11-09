// ApexCare Clinical EHR - Enterprise Frontend Controller
const API_BASE = '/api';

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
  if (!name) return 'PT';
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
// 1. DASHBOARD MODULE
// ========================================================
async function initDashboard() {
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
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 24px; color: #94a3b8;">No clinical encounters scheduled for today.</td></tr>';
      } else {
        tbody.innerHTML = appointments.slice(0, 6).map(a => {
          const statusClass = a.status === 'BOOKED' ? 'status-booked' : (a.status === 'COMPLETED' ? 'status-completed' : 'status-cancelled');
          return `
            <tr>
              <td><span class="id-tag">#ENC-${a.id}</span></td>
              <td>
                <div class="patient-cell">
                  <div class="avatar-circle">${getInitials(a.patientName)}</div>
                  <div class="patient-info">
                    <strong>${a.patientName}</strong>
                    <span>${a.patientId}</span>
                  </div>
                </div>
              </td>
              <td><strong>${a.doctorName}</strong></td>
              <td>${a.appointmentDate}</td>
              <td><strong>${a.timeSlot}</strong></td>
              <td><span class="status-pill ${statusClass}">${a.status}</span></td>
            </tr>
          `;
        }).join('');
      }
    }
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// ========================================================
// 2. PATIENT REGISTRY MODULE
// ========================================================
async function initPatients() {
  const tbody = document.getElementById('patients-tbody');
  const searchInput = document.getElementById('patient-search');
  const counter = document.getElementById('patient-count-badge');

  async function render(query = '') {
    try {
      const endpoint = query ? `/patients?q=${encodeURIComponent(query)}` : '/patients';
      const patients = await apiCall(endpoint);
      if (counter) counter.textContent = `${patients.length} Active Records`;

      if (patients.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 32px; color: #94a3b8;">No matching patient records found in registry.</td></tr>';
        return;
      }

      tbody.innerHTML = patients.map(p => `
        <tr>
          <td><span class="id-tag">${p.id}</span></td>
          <td>
            <div class="patient-cell">
              <div class="avatar-circle">${getInitials(p.name)}</div>
              <div class="patient-info">
                <strong>${p.name}</strong>
                <span>${p.gender}, ${p.age} yrs</span>
              </div>
            </div>
          </td>
          <td><span class="blood-badge">${p.bloodGroup || 'O+'}</span></td>
          <td><strong>${p.phone}</strong></td>
          <td>${p.email || '—'}</td>
          <td style="max-width: 180px; font-size: 12px; color: #64748b;" title="${p.address || ''}">${p.address || '—'}</td>
          <td>${p.registeredDate}</td>
          <td>
            <button class="btn btn-danger btn-sm" onclick="deletePatient('${p.id}')">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              Archive
            </button>
          </td>
        </tr>
      `).join('');
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

  if (!data.id || data.id.trim() === '') {
    data.id = 'MRN-' + Math.floor(10000 + Math.random() * 90000);
  }

  const age = parseInt(data.age, 10) || 30;
  const birthYear = new Date().getFullYear() - age;
  const dateOfBirth = `${birthYear}-01-15`;

  const payload = {
    fullName: data.name,
    dateOfBirth: dateOfBirth,
    phone: data.phone || '',
    duplicateReviewAcknowledged: true,
    duplicateReviewReason: 'Web patient intake'
  };

  try {
    const res = await apiCall('/patients', 'POST', payload);
    showAlert('Patient registered in centralized database: ' + (res.fullName || data.name), 'success');
    closeModal('patient-modal');
    form.reset();
    initPatients();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function deletePatient(id) {
  if (!confirm(`Confirm clinical record archiving for ${id}? This record will be marked inactive.`)) return;
  try {
    const res = await apiCall(`/patients?id=${encodeURIComponent(id)}`, 'DELETE');
    showAlert(res.message, 'success');
    initPatients();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// ========================================================
// 3. DOCTOR DIRECTORY MODULE
// ========================================================
async function initDoctors() {
  const tbody = document.getElementById('doctors-tbody');
  const filterSelect = document.getElementById('doctor-spec-filter');
  const counter = document.getElementById('doctor-count-badge');

  async function render(spec = '') {
    try {
      const endpoint = spec ? `/doctors?spec=${encodeURIComponent(spec)}` : '/doctors';
      const doctors = await apiCall(endpoint);
      if (counter) counter.textContent = `${doctors.length} On Duty`;

      if (doctors.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: #94a3b8;">No medical specialists match this criteria.</td></tr>';
        return;
      }

      tbody.innerHTML = doctors.map(d => `
        <tr>
          <td><span class="id-tag">${d.id}</span></td>
          <td>
            <div class="patient-cell">
              <div class="avatar-circle" style="background:#e0f2fe; color:#0369a1;">${getInitials(d.name)}</div>
              <div class="patient-info">
                <strong>${d.name}</strong>
                <span>${d.qualification}</span>
              </div>
            </div>
          </td>
          <td><span style="font-weight:600; color:#0284c7;">${d.specialization}</span></td>
          <td><strong>${d.phone}</strong></td>
          <td><strong>${formatINR(d.consultationFee)}</strong></td>
          <td>
            <div style="font-size:12px; font-weight:600; color:#334155;">${d.availableDays}</div>
            <div style="font-size:11px; color:#64748b;">${d.availableHours}</div>
          </td>
          <td>
            <button class="btn btn-secondary btn-sm" onclick="quickScheduleForDoctor('${d.id}', '${d.name.replace(/'/g, "\\'")}')">
              Schedule
            </button>
          </td>
        </tr>
      `).join('');
    } catch (err) {
      showAlert(err.message, 'danger');
    }
  }

  if (filterSelect) {
    filterSelect.addEventListener('change', (e) => render(e.target.value));
  }

  render();
}

function quickScheduleForDoctor(docId, docName) {
  window.location.href = `/appointments.html?docId=${encodeURIComponent(docId)}&docName=${encodeURIComponent(docName)}`;
}

async function addDoctor(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  try {
    const res = await apiCall('/doctors/web', 'POST', data);
    showAlert(res.message || 'Doctor registered in central directory', 'success');
    closeModal('doctor-modal');
    form.reset();
    initDoctors();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// ========================================================
// 4. APPOINTMENT SCHEDULING MODULE
// ========================================================
async function initAppointments() {
  const tbody = document.getElementById('appointments-tbody');

  try {
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    // Populate patient selector
    const patientSelect = document.getElementById('appt-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Search & Select Patient (MRN) --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} [${p.id}] • Phone: ${p.phone}</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('appt-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    // Populate doctor selector
    const doctorSelect = document.getElementById('appt-doctor-select');
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Select Attending Clinician --</option>' +
        doctors.map(d => `<option value="${d.id}" data-name="${d.name}">${d.name} (${d.specialization}) • ${formatINR(d.consultationFee)}</option>`).join('');
      doctorSelect.onchange = function() {
        const selected = doctorSelect.options[doctorSelect.selectedIndex];
        document.getElementById('appt-doctor-name').value = selected.getAttribute('data-name') || '';
      };

      // Check URL query parameters for pre-selected doctor
      const urlParams = new URLSearchParams(window.location.search);
      const preDocId = urlParams.get('docId');
      if (preDocId) {
        doctorSelect.value = preDocId;
        const selected = doctorSelect.options[doctorSelect.selectedIndex];
        if (selected) {
          document.getElementById('appt-doctor-name').value = selected.getAttribute('data-name') || '';
        }
        openModal('appointment-modal');
      }
    }

    const appointments = await apiCall('/appointments');
    if (appointments.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 32px; color: #94a3b8;">No clinical encounters scheduled.</td></tr>';
      return;
    }

    tbody.innerHTML = appointments.map(a => {
      const statusClass = a.status === 'BOOKED' ? 'status-booked' : (a.status === 'COMPLETED' ? 'status-completed' : 'status-cancelled');
      return `
        <tr>
          <td><span class="id-tag">#ENC-${a.id}</span></td>
          <td>
            <div class="patient-cell">
              <div class="avatar-circle">${getInitials(a.patientName)}</div>
              <div class="patient-info">
                <strong>${a.patientName}</strong>
                <span>${a.patientId}</span>
              </div>
            </div>
          </td>
          <td><strong>${a.doctorName}</strong></td>
          <td>${a.appointmentDate}</td>
          <td><strong>${a.timeSlot}</strong></td>
          <td><span class="status-pill ${statusClass}">${a.status}</span></td>
          <td style="max-width: 220px; font-size: 12px; color: #475569;" title="${a.reason || ''}">${a.reason || 'Routine Consultation'}</td>
          <td>
            ${a.status === 'BOOKED' ? `
              <div style="display:flex; gap:6px;">
                <button class="btn btn-success btn-sm" onclick="updateApptStatus(${a.id}, 'COMPLETED')">Complete</button>
                <button class="btn btn-secondary btn-sm" onclick="updateApptStatus(${a.id}, 'CANCELLED')">Cancel</button>
              </div>
            ` : `<span style="font-size:11px; color:#94a3b8; font-weight:600;">Finalized</span>`}
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function bookAppointment(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData.entries());

  let time = data.timeSlot || '09:00 AM';
  let hour = 9, minute = 0;
  const match = time.match(/(\d+):(\d+)\s*(AM|PM)?/i);
  if (match) {
    hour = parseInt(match[1], 10);
    minute = parseInt(match[2], 10);
    if (/PM/i.test(match[3]) && hour < 12) hour += 12;
    if (/AM/i.test(match[3]) && hour === 12) hour = 0;
  }
  const dateStr = data.appointmentDate || new Date().toISOString().split('T')[0];
  const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`;
  const startsAt = `${dateStr}T${timeStr}+05:30`;

  const payload = {
    patientNumber: data.patientId,
    doctorId: parseInt(data.doctorId, 10),
    startsAt: startsAt,
    visitType: 'APPOINTMENT',
    reason: data.reason || 'General Outpatient Care'
  };

  try {
    const res = await apiCall('/appointments', 'POST', payload);
    showAlert('Encounter booked in central database (#ENC-' + (res.id || '') + ')', 'success');
    closeModal('appointment-modal');
    form.reset();
    initAppointments();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

async function updateApptStatus(id, status) {
  try {
    const res = await apiCall(`/appointments/${id}/status`, 'PATCH', {
      status: status,
      reason: 'Status updated via clinical web portal'
    });
    showAlert('Encounter status updated to ' + status, 'success');
    initAppointments();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

// ========================================================
// 5. REVENUE CYCLE & BILLING MODULE
// ========================================================
async function initBilling() {
  const tbody = document.getElementById('billing-tbody');

  try {
    const patients = await apiCall('/patients');
    const patientSelect = document.getElementById('bill-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient Account --</option>' +
        patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} [${p.id}]</option>`).join('');
      patientSelect.onchange = function() {
        const selected = patientSelect.options[patientSelect.selectedIndex];
        document.getElementById('bill-patient-name').value = selected.getAttribute('data-name') || '';
      };
    }

    const bills = await apiCall('/bills');
    window._cachedBills = bills;

    if (bills.length === 0) {
      tbody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 32px; color: #94a3b8;">No invoices registered in system.</td></tr>';
      return;
    }

    tbody.innerHTML = bills.map(b => {
      const isPaid = b.paymentStatus === 'PAID';
      return `
        <tr>
          <td><span class="id-tag">INV-${b.billId}</span></td>
          <td>
            <div class="patient-cell">
              <div class="avatar-circle">${getInitials(b.patientName)}</div>
              <div class="patient-info">
                <strong>${b.patientName}</strong>
                <span>${b.patientId}</span>
              </div>
            </div>
          </td>
          <td>${formatINR(b.consultationFee)}</td>
          <td>${formatINR(b.treatmentCharges)}</td>
          <td>${formatINR(b.medicineCharges)}</td>
          <td>${b.discountPercent}%</td>
          <td>${b.taxPercent}%</td>
          <td><strong style="color:#0f172a; font-size:14px;">${formatINR(b.totalAmount)}</strong></td>
          <td>${b.billingDate}</td>
          <td><span class="status-pill ${isPaid ? 'status-paid' : 'status-pending'}">${b.paymentStatus}</span></td>
          <td>
            <div style="display:flex; gap:6px;">
              ${!isPaid ? `
                <button class="btn btn-success btn-sm" onclick="markBillPaid(${b.billId})">Pay</button>
              ` : ''}
              <button class="btn btn-secondary btn-sm" onclick="printReceipt(${b.billId})">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="6 9 6 2 18 2 18 9"></polyline>
                  <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                  <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                Receipt
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
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

  document.getElementById('live-subtotal').textContent = formatINR(subtotal);
  document.getElementById('live-discount').textContent = `- ${formatINR(discountAmount)}`;
  document.getElementById('live-tax').textContent = `+ ${formatINR(taxAmount)}`;
  document.getElementById('live-total').textContent = formatINR(finalTotal);
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

// Generate Printable Official Tax Receipt
function printReceipt(billId) {
  const bill = (window._cachedBills || []).find(b => b.billId === billId);
  if (!bill) {
    showAlert('Invoice record not found in local cache.', 'danger');
    return;
  }

  const receiptContent = document.getElementById('receipt-modal-body');
  if (!receiptContent) return;

  const subtotal = bill.consultationFee + bill.treatmentCharges + bill.medicineCharges;
  const discount = (subtotal * bill.discountPercent) / 100.0;
  const tax = ((subtotal - discount) * bill.taxPercent) / 100.0;

  receiptContent.innerHTML = `
    <div class="receipt-paper">
      <div class="receipt-header">
        <div class="receipt-hospital">
          <h2>APEXCARE MULTISPECIALTY HOSPITAL</h2>
          <p>Sector 14, Healthcare Corridor • Tel: +91 (863) 244-8000</p>
          <p>GSTIN: 37AAAAA0000A1Z5 • Clinical Establishment Reg: AP-GNT-2024/991</p>
        </div>
        <div>
          <span class="receipt-badge-paid">${bill.paymentStatus}</span>
        </div>
      </div>

      <div class="receipt-grid">
        <div><strong>Patient Name:</strong> ${bill.patientName}</div>
        <div><strong>Invoice No:</strong> INV-${bill.billId}</div>
        <div><strong>Patient MRN:</strong> ${bill.patientId}</div>
        <div><strong>Date of Service:</strong> ${bill.billingDate}</div>
      </div>

      <table class="receipt-table">
        <thead>
          <tr>
            <th>Description of Clinical Service</th>
            <th style="text-align:right;">Charge Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Specialist Outpatient Consultation (OPD)</td>
            <td style="text-align:right;">${formatINR(bill.consultationFee)}</td>
          </tr>
          <tr>
            <td>Diagnostic Investigations & Clinical Treatment</td>
            <td style="text-align:right;">${formatINR(bill.treatmentCharges)}</td>
          </tr>
          <tr>
            <td>Pharmacy Dispensation & Medical Consumables</td>
            <td style="text-align:right;">${formatINR(bill.medicineCharges)}</td>
          </tr>
        </tbody>
      </table>

      <div style="margin-left:auto; width:260px; font-size:12.5px;">
        <div style="display:flex; justify-content:space-between; margin-bottom:4px;">
          <span>Gross Subtotal:</span>
          <strong>${formatINR(subtotal)}</strong>
        </div>
        <div style="display:flex; justify-content:space-between; margin-bottom:4px; color:#059669;">
          <span>Institutional Discount (${bill.discountPercent}%):</span>
          <span>- ${formatINR(discount)}</span>
        </div>
        <div style="display:flex; justify-content:space-between; margin-bottom:6px; color:#64748b;">
          <span>GST / Healthcare Cess (${bill.taxPercent}%):</span>
          <span>+ ${formatINR(tax)}</span>
        </div>
      </div>

      <div class="receipt-total-box">
        <span>Net Balance Payable:</span>
        <span style="color:#0284c7;">${formatINR(bill.totalAmount)}</span>
      </div>

      <div style="margin-top:24px; padding-top:12px; border-top:1px dashed #cbd5e1; display:flex; justify-content:space-between; font-size:11px; color:#64748b;">
        <span>Prepared by: Automated HIS Gateway</span>
        <span>Computer Generated Invoice • No Signature Required</span>
      </div>
    </div>
  `;

  openModal('receipt-modal');
}

// ========================================================
// 6. CLINICAL EHR & MEDICAL RECORDS MODULE
// ========================================================
async function initRecords() {
  const tbody = document.getElementById('records-tbody');

  try {
    const patients = await apiCall('/patients');
    const doctors = await apiCall('/doctors');

    const patientSelect = document.getElementById('rec-patient-select');
    if (patientSelect) {
      patientSelect.innerHTML = '<option value="">-- Select Patient (MRN) --</option>' +
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
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 32px; color: #94a3b8;">No medical consultation logs in EHR.</td></tr>';
      return;
    }

    tbody.innerHTML = records.map(r => `
      <tr>
        <td><span class="id-tag">#EHR-${r.recordId}</span></td>
        <td>
          <div class="patient-cell">
            <div class="avatar-circle">${getInitials(r.patientName)}</div>
            <div class="patient-info">
              <strong>${r.patientName}</strong>
              <span>${r.patientId}</span>
            </div>
          </div>
        </td>
        <td><strong>${r.doctorName}</strong></td>
        <td>
          <div style="font-weight:600; color:#0f172a;">${r.diagnosis}</div>
          <div style="font-size:11px; color:#64748b; margin-top:2px;">${r.labTests || 'No labs ordered'}</div>
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
    showAlert(err.message, 'danger');
  }
}

function viewRecordDetail(recordId) {
  const r = (window._cachedRecords || []).find(rec => rec.recordId === recordId);
  if (!r) return;

  const detailBody = document.getElementById('record-detail-body');
  if (!detailBody) return;

  detailBody.innerHTML = `
    <div style="border:1px solid #e2e8f0; border-radius:8px; padding:18px; background:#ffffff;">
      <div style="display:flex; justify-content:space-between; margin-bottom:14px; border-bottom:1px solid #e2e8f0; padding-bottom:12px;">
        <div>
          <h3 style="font-size:16px; font-weight:700; color:#0f172a;">${r.patientName}</h3>
          <p style="font-size:12px; color:#64748b;">MRN: <strong>${r.patientId}</strong> • Date of Encounter: <strong>${r.recordDate}</strong></p>
        </div>
        <div style="text-align:right;">
          <div style="font-weight:600; color:#0284c7;">${r.doctorName}</div>
          <span style="font-size:11px; color:#94a3b8;">Attending Physician</span>
        </div>
      </div>

      <div style="margin-bottom:14px;">
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:#475569; letter-spacing:0.5px;">Clinical Diagnosis & ICD Indication</label>
        <div style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px; padding:10px; font-size:13px; font-weight:600; color:#0f172a; margin-top:4px;">
          ${r.diagnosis}
        </div>
      </div>

      <div style="margin-bottom:14px;">
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:#475569; letter-spacing:0.5px;">Rx - Medication Orders & Dosing Regimen</label>
        <div style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px; padding:10px; font-size:12.5px; line-height:1.6; white-space:pre-wrap; margin-top:4px;">${r.prescription}</div>
      </div>

      <div style="margin-bottom:14px;">
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:#475569; letter-spacing:0.5px;">Diagnostic Laboratory & Radiology Orders</label>
        <div style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px; padding:10px; font-size:12.5px; color:#334155; margin-top:4px;">
          ${r.labTests || 'None requested.'}
        </div>
      </div>

      <div>
        <label style="font-size:11px; font-weight:700; text-transform:uppercase; color:#475569; letter-spacing:0.5px;">Clinician's Progress Notes & Care Plan</label>
        <div style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px; padding:10px; font-size:12.5px; color:#334155; margin-top:4px;">
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
    showAlert(err.message, 'danger');
  }
}

async function deleteRecord(id) {
  if (!confirm(`Permanently remove EHR record #EHR-${id}?`)) return;
  try {
    const res = await apiCall(`/records?id=${id}`, 'DELETE');
    showAlert(res.message, 'success');
    initRecords();
  } catch (err) {
    showAlert(err.message, 'danger');
  }
}

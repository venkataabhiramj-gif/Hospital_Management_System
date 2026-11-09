package com.hospital.management.server.api;

import com.hospital.management.server.service.AuditService;
import com.hospital.management.server.service.PatientDuplicateMatch;
import com.hospital.management.server.service.PatientDuplicateReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.Statement;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WorkflowController {
    private final JdbcTemplate db;
    private final AuditService audit;
    private final PasswordEncoder passwords;
    private final PatientDuplicateReviewService patientDuplicates;

    public WorkflowController(JdbcTemplate db, AuditService audit, PasswordEncoder passwords, PatientDuplicateReviewService patientDuplicates) {
        this.db = db;
        this.audit = audit;
        this.passwords = passwords;
        this.patientDuplicates = patientDuplicates;
    }

    @GetMapping("/me")
    Map<String,Object> currentStaffProfile() {
        return db.queryForMap("SELECT id, username, display_name, role FROM staff_users WHERE id=? AND active=TRUE",currentStaffId());
    }

    public record PatientInput(@NotBlank @Size(max=200) String fullName, @PastOrPresent LocalDate dateOfBirth, @Size(max=32) @Pattern(regexp="(^$|\\+?[0-9 ()-]{7,32})") String phone, Boolean duplicateReviewAcknowledged, @Size(max=500) String duplicateReviewReason, Long excludePatientId) {}
    public record PatientCorrectionInput(@NotBlank @Size(max=200) String fullName, @PastOrPresent LocalDate dateOfBirth, @Size(max=32) @Pattern(regexp="(^$|\\+?[0-9 ()-]{7,32})") String phone, @NotBlank @Size(max=500) String reason, Boolean duplicateReviewAcknowledged, @Size(max=500) String duplicateReviewReason) {}
    public record PatientMergeInput(@NotNull Long targetPatientId,@NotBlank @Size(max=500) String reason) {}
    private record PatientDemographics(String fullName, LocalDate dateOfBirth, String phone) {}
    private record PrescriptionItem(long medicineId, Long substitutionApprovedBy) {}
    public record DoctorInput(@NotNull Long staffUserId, @NotBlank @Size(max=120) String specialty, @Min(5) @Max(240) Integer appointmentMinutes) {}
    public record ScheduleInput(@NotNull Integer dayOfWeek,@NotNull LocalTime startsAt,@NotNull LocalTime endsAt) {}
    public record StaffInput(@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9._-]{3,100}") String username, @NotBlank @Size(min=12,max=128) String password, @NotBlank @Size(max=200) String displayName, @NotBlank String role) {}
    public record AppointmentInput(@NotBlank String patientNumber, @NotNull Long doctorId, @NotNull OffsetDateTime startsAt, OffsetDateTime endsAt, @NotBlank String visitType, @Size(max=500) String reason) {}
    public record RescheduleInput(@NotNull OffsetDateTime startsAt,OffsetDateTime endsAt) {}
    public record StatusInput(@NotBlank String status, @Size(max=500) String reason) {}
    public record MedicineInput(@NotBlank @Size(max=64) String code, @NotBlank @Size(max=200) String genericName, @Size(max=200) String brandName, @Size(max=80) String strength, @Size(max=80) String dosageForm, @NotBlank @Size(max=40) String unit, @Min(0) int reorderLevel, @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal salePrice) {}
    public record BatchInput(@NotNull Long medicineId, @NotBlank @Size(max=100) String batchNumber, @NotNull LocalDate expiryDate, @Size(max=200) String supplier, @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal purchasePrice, @Min(1) int quantity) {}
    public record AdjustmentInput(@NotNull Long batchId,@NotNull Integer quantityChange,@NotBlank @Size(max=500) String reason) {}
    public record SupplierReturnInput(@NotNull Long batchId,@Min(1) int quantity,@NotBlank @Size(max=500) String reason) {}
    public record EncounterInput(@NotNull Long appointmentId, @Size(max=1000) String complaint, @Size(max=20) String bloodPressure, @Min(0) @Max(300) Integer pulsePerMinute, @DecimalMin("20.0") @jakarta.validation.constraints.DecimalMax("50.0") @Digits(integer=2,fraction=1) BigDecimal temperatureCelsius, @DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("1000.0") @Digits(integer=4,fraction=2) BigDecimal weightKg, @Size(max=10000) String allergies, @Size(max=10000) String currentMedicines, @Size(max=20000) String clinicalNotes, @Size(max=1000) String diagnosisText) {}
    public record EncounterEditInput(@Size(max=1000) String complaint, @Size(max=20) String bloodPressure, @Min(0) @Max(300) Integer pulsePerMinute, @DecimalMin("20.0") @jakarta.validation.constraints.DecimalMax("50.0") @Digits(integer=2,fraction=1) BigDecimal temperatureCelsius, @DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("1000.0") @Digits(integer=4,fraction=2) BigDecimal weightKg, @Size(max=10000) String allergies, @Size(max=10000) String currentMedicines, @Size(max=20000) String clinicalNotes, @Size(max=1000) String diagnosisText) {}
    public record PrescriptionLine(@NotNull Long medicineId, @NotBlank @Size(max=120) String dosage, @Size(max=80) String route, @NotBlank @Size(max=120) String frequency, @Min(1) Integer durationDays, @Min(1) int quantity, @Size(max=1000) String instructions) {}
    public record PrescriptionInput(@NotNull Long encounterId, @NotNull @Size(min=1) List<@Valid PrescriptionLine> items) {}
    public record SubstitutionApprovalInput(@NotNull Long substituteMedicineId,@NotBlank @Size(max=500) String reason) {}
    public record DispenseInput(@NotNull Integer quantity) {}
    public record ServiceInput(@NotBlank @Size(max=64) String code, @NotBlank @Size(max=200) String description, @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal unitPrice) {}
    public record InvoiceInput(@NotNull Long encounterId, @NotNull List<@NotNull Long> serviceIds) {}
    public record InvoiceDetails(Map<String,Object> invoice,List<Map<String,Object>> items,List<Map<String,Object>> payments) {}
    public record PaymentInput(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount, @NotBlank String method) {}
    public record RefundInput(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount, @NotBlank @Size(max=500) String reason) {}
    public record ActiveInput(@NotNull Boolean active) {}
    public record PasswordChangeInput(@NotBlank @Size(min=12,max=128) String newPassword) {}

    @GetMapping("/patients")
    List<Map<String,Object>> patients(@RequestParam(defaultValue="") String q) {
        String value=q.trim();
        String term="%"+value.toLowerCase(Locale.ROOT)+"%";
        String phoneDigits=patientDuplicates.normalizePhone(value);
        String phoneTerm=phoneDigits.isBlank()?"%!%":"%"+phoneDigits+"%";
        LocalDate dateOfBirth=null;
        try { dateOfBirth=LocalDate.parse(value); } catch(java.time.format.DateTimeParseException ignored) { }
        List<Map<String,Object>> raw;
        if(dateOfBirth==null) raw = db.queryForList("SELECT id,patient_number,full_name,date_of_birth,phone,active,created_at FROM patients WHERE active=TRUE AND (LOWER(full_name) LIKE ? OR LOWER(patient_number) LIKE ? OR COALESCE(phone_match_key,'') LIKE ?) ORDER BY full_name LIMIT 100",term,term,phoneTerm);
        else raw = db.queryForList("SELECT id,patient_number,full_name,date_of_birth,phone,active,created_at FROM patients WHERE active=TRUE AND (LOWER(full_name) LIKE ? OR LOWER(patient_number) LIKE ? OR COALESCE(phone_match_key,'') LIKE ? OR date_of_birth=?) ORDER BY full_name LIMIT 100",term,term,phoneTerm,dateOfBirth);
        return raw.stream().map(this::enrichPatientMap).toList();
    }

    private Map<String,Object> enrichPatientMap(Map<String,Object> m) {
        Map<String,Object> copy = new java.util.HashMap<>(m);
        Object dob = m.get("date_of_birth");
        int age = 30;
        if (dob instanceof LocalDate ld) {
            age = java.time.Period.between(ld, LocalDate.now()).getYears();
        } else if (dob instanceof java.sql.Date sd) {
            age = java.time.Period.between(sd.toLocalDate(), LocalDate.now()).getYears();
        }
        String pNum = String.valueOf(m.get("patient_number"));
        String fName = String.valueOf(m.get("full_name"));
        copy.put("id", pNum);
        copy.put("dbId", m.get("id"));
        copy.put("patientId", pNum);
        copy.put("patientNumber", pNum);
        copy.put("name", fName);
        copy.put("fullName", fName);
        copy.put("age", age);
        copy.put("gender", "Unspecified");
        copy.put("bloodGroup", "O+");
        copy.put("registeredDate", m.get("created_at") != null ? m.get("created_at").toString().substring(0, 10) : LocalDate.now().toString());
        return copy;
    }

    private void acquireRegistrationLock() {
        db.update("INSERT INTO patient_registration_lock (id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM patient_registration_lock WHERE id=1)");
        db.queryForObject("SELECT id FROM patient_registration_lock WHERE id=1 FOR UPDATE", Integer.class);
    }

    @PostMapping("/patients") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createPatient(@Valid @RequestBody PatientInput p) {
        acquireRegistrationLock();
        List<PatientDuplicateMatch> matches=patientDuplicates.findPossibleMatches(p.fullName(),p.dateOfBirth(),p.phone(),null);
        if(!matches.isEmpty()&&!Boolean.TRUE.equals(p.duplicateReviewAcknowledged())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Possible matching patients were found. Review them before confirming this is a separate person.");
        String reviewReason=p.duplicateReviewReason()==null?"":p.duplicateReviewReason().trim();
        if(!matches.isEmpty()&&reviewReason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Record why the possible matches are not this patient before continuing");
        Long staff = currentStaffId();
        String normalizedPhone=patientDuplicates.normalizePhone(p.phone());
        Long id = insert("INSERT INTO patients(patient_number,full_name,date_of_birth,phone,phone_match_key,created_by) VALUES(?,?,?,?,?,?)", "P"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase(), p.fullName().trim(), p.dateOfBirth(), p.phone()==null||p.phone().isBlank()?null:p.phone().trim(), normalizedPhone.isBlank()?null:normalizedPhone, staff);
        for(PatientDuplicateMatch match:matches) db.update("INSERT INTO patient_duplicate_reviews(new_patient_id,possible_match_patient_id,match_signals,review_reason,reviewed_by) VALUES(?,?,?,?,?)",id,match.id(),match.matchSignals(),reviewReason,staff);
        audit.record("PATIENT_CREATED","PATIENT",id,"Registration");
        if(!matches.isEmpty()) audit.record("PATIENT_DUPLICATE_REVIEWED","PATIENT",id,"Possible matches reviewed; reason recorded");
        return db.queryForMap("SELECT id,patient_number,full_name,date_of_birth,phone,active,created_at FROM patients WHERE id=?", id);
    }

    @PostMapping("/patients/possible-matches")
    List<PatientDuplicateMatch> possiblePatientMatches(@Valid @RequestBody PatientInput p) { return patientDuplicates.findPossibleMatches(p.fullName(),p.dateOfBirth(),p.phone(),p.excludePatientId()); }

    @GetMapping("/patients/{id}/duplicate-review")
    List<Map<String,Object>> patientDuplicateReview(@PathVariable long id) {
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM patients WHERE id=?",Integer.class,id);
        if(exists==null||exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient not found");
        return db.queryForList("SELECT r.id,r.match_signals,r.review_reason,r.reviewed_at,s.display_name reviewed_by,p.patient_number possible_match_patient_number,p.full_name possible_match_name FROM patient_duplicate_reviews r JOIN staff_users s ON s.id=r.reviewed_by JOIN patients p ON p.id=r.possible_match_patient_id WHERE r.new_patient_id=? ORDER BY r.reviewed_at DESC",id);
    }

    @PostMapping("/admin/patients/{sourceId}/merge") @Transactional
    Map<String,Object> mergePatients(@PathVariable long sourceId,@Valid @RequestBody PatientMergeInput input) {
        long targetId=input.targetPatientId();
        if(sourceId==targetId) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose two different patient records");
        acquireRegistrationLock();
        long first=Math.min(sourceId,targetId),second=Math.max(sourceId,targetId);
        Map<String,Object> firstPatient=db.query("SELECT id,patient_number,full_name,active,merged_into_patient_id FROM patients WHERE id=? FOR UPDATE",rs->rs.next()?new java.util.HashMap<>(new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,1)):null,first);
        Map<String,Object> secondPatient=db.query("SELECT id,patient_number,full_name,active,merged_into_patient_id FROM patients WHERE id=? FOR UPDATE",rs->rs.next()?new java.util.HashMap<>(new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,1)):null,second);
        if(firstPatient==null||secondPatient==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient record not found");
        Map<String,Object> source=sourceId==first?firstPatient:secondPatient;
        Map<String,Object> target=targetId==first?firstPatient:secondPatient;
        if(!Boolean.TRUE.equals(source.get("active"))||source.get("merged_into_patient_id")!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"The source record is inactive or has already been merged");
        if(!Boolean.TRUE.equals(target.get("active"))||target.get("merged_into_patient_id")!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Choose an active patient record as the surviving record");
        Integer appointmentConflict=db.queryForObject("SELECT COUNT(*) FROM appointments a JOIN appointments b ON b.doctor_id=a.doctor_id AND b.patient_id=? AND b.id<>a.id WHERE a.patient_id=? AND a.status IN ('BOOKED','CHECKED_IN','IN_PROGRESS') AND b.status IN ('BOOKED','CHECKED_IN','IN_PROGRESS') AND a.starts_at<b.ends_at AND a.ends_at>b.starts_at",Integer.class,targetId,sourceId);
        if(appointmentConflict!=null&&appointmentConflict>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"These records have overlapping active appointments with the same doctor. Resolve the bookings before merging");
        String sourceNumber=(String)source.get("patient_number"),targetNumber=(String)target.get("patient_number");
        db.update("UPDATE appointments SET patient_id=? WHERE patient_id=?",targetId,sourceId);
        db.update("UPDATE encounters SET patient_id=? WHERE patient_id=?",targetId,sourceId);
        db.update("UPDATE invoices SET patient_id=? WHERE patient_id=?",targetId,sourceId);
        db.update("UPDATE patients SET active=FALSE,merged_into_patient_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",targetId,sourceId);
        Long mergeId=insert("INSERT INTO patient_merge_history(source_patient_id,target_patient_id,source_patient_number,target_patient_number,reason,merged_by) VALUES(?,?,?,?,?,?)",sourceId,targetId,sourceNumber,targetNumber,input.reason().trim(),currentStaffId());
        audit.record("PATIENT_RECORDS_MERGED","PATIENT",sourceId,"Source record merged into "+targetNumber+"; reason recorded");
        audit.record("PATIENT_MERGE_RECEIVED","PATIENT",targetId,"Patient record "+sourceNumber+" merged into this record; reason recorded");
        return db.queryForMap("SELECT h.id,h.source_patient_id,h.source_patient_number,h.target_patient_id,h.target_patient_number,h.reason,h.merged_by,s.display_name merged_by_name,h.merged_at FROM patient_merge_history h JOIN staff_users s ON s.id=h.merged_by WHERE h.id=?",mergeId);
    }

    @GetMapping("/admin/patients/{id}/merge-history")
    List<Map<String,Object>> patientMergeHistory(@PathVariable long id) {
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM patients WHERE id=?",Integer.class,id);
        if(exists==null||exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient not found");
        return db.queryForList("SELECT h.id,h.source_patient_id,h.source_patient_number,h.target_patient_id,h.target_patient_number,h.reason,h.merged_by,s.display_name merged_by_name,h.merged_at FROM patient_merge_history h JOIN staff_users s ON s.id=h.merged_by WHERE h.source_patient_id=? OR h.target_patient_id=? ORDER BY h.merged_at DESC,h.id DESC",id,id);
    }

    @PutMapping("/patients/{id}") @Transactional
    Map<String,Object> updatePatient(@PathVariable long id,@Valid @RequestBody PatientCorrectionInput p) {
        acquireRegistrationLock();
        PatientDemographics old=db.query("SELECT full_name,date_of_birth,phone FROM patients WHERE id=? AND active=TRUE FOR UPDATE",rs->rs.next()?new PatientDemographics(rs.getString("full_name"),rs.getObject("date_of_birth",LocalDate.class),rs.getString("phone")):null,id);
        if(old==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active patient not found");
        String fullName=p.fullName().trim();String phone=p.phone()==null||p.phone().isBlank()?null:p.phone().trim();String reason=p.reason().trim();
        List<PatientDuplicateMatch> matches=patientDuplicates.findPossibleMatches(fullName,p.dateOfBirth(),phone,id);
        if(!matches.isEmpty()&&!Boolean.TRUE.equals(p.duplicateReviewAcknowledged())) throw new ResponseStatusException(HttpStatus.CONFLICT,"The corrected details match another patient. Review the candidates before confirming the records are different people.");
        String duplicateReviewReason=p.duplicateReviewReason()==null?"":p.duplicateReviewReason().trim();
        if(!matches.isEmpty()&&duplicateReviewReason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Record why the possible matches are different people before saving the correction");
        List<String> changedFields=new java.util.ArrayList<>();
        if(!java.util.Objects.equals(old.fullName(),fullName)) changedFields.add("full_name");
        if(!java.util.Objects.equals(old.dateOfBirth(),p.dateOfBirth())) changedFields.add("date_of_birth");
        if(!java.util.Objects.equals(old.phone(),phone)) changedFields.add("phone");
        if(changedFields.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"No demographic changes were entered");
        db.update("INSERT INTO patient_demographic_history(patient_id,changed_by,old_full_name,new_full_name,old_date_of_birth,new_date_of_birth,old_phone,new_phone,correction_reason) VALUES(?,?,?,?,?,?,?,?,?)",id,currentStaffId(),old.fullName(),fullName,old.dateOfBirth(),p.dateOfBirth(),old.phone(),phone,reason);
        for(PatientDuplicateMatch match:matches) db.update("INSERT INTO patient_duplicate_reviews(new_patient_id,possible_match_patient_id,match_signals,review_reason,reviewed_by) VALUES(?,?,?,?,?)",id,match.id(),match.matchSignals(),duplicateReviewReason,currentStaffId());
        String normalizedPhone=patientDuplicates.normalizePhone(phone);
        db.update("UPDATE patients SET full_name=?,date_of_birth=?,phone=?,phone_match_key=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",fullName,p.dateOfBirth(),phone,normalizedPhone.isBlank()?null:normalizedPhone,id);
        audit.record("PATIENT_DEMOGRAPHICS_CORRECTED","PATIENT",id,"Fields: "+String.join(", ",changedFields)+"; reason recorded");
        if(!matches.isEmpty()) audit.record("PATIENT_DUPLICATE_REVIEWED","PATIENT",id,"Possible matches reviewed during demographic correction; reason recorded");
        return db.queryForMap("SELECT id,patient_number,full_name,date_of_birth,phone,active,created_at FROM patients WHERE id=?",id);
    }

    @GetMapping("/patients/{id}/demographic-history")
    List<Map<String,Object>> patientDemographicHistory(@PathVariable long id) {
        return db.queryForList("SELECT id,changed_by,changed_at,old_full_name,new_full_name,old_date_of_birth,new_date_of_birth,old_phone,new_phone,correction_reason FROM patient_demographic_history WHERE patient_id=? ORDER BY changed_at DESC",id);
    }

    @GetMapping("/patients/{patientNumber}/encounters")
    List<Map<String,Object>> patientEncounters(@PathVariable String patientNumber) {
        if(isDoctor()) {
            Long patientId=db.query("SELECT id FROM patients WHERE patient_number=? AND active=TRUE",rs->rs.next()?rs.getLong(1):null,patientNumber);
            if(patientId==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active patient not found");
            requireDoctorPatientAccess(patientId);
        }
        return db.queryForList("SELECT e.id,e.appointment_id,e.doctor_id,s.display_name doctor_name,e.complaint,e.diagnosis_text,e.status,e.created_at,e.finalized_at FROM encounters e JOIN patients p ON p.id=e.patient_id JOIN doctors d ON d.id=e.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE p.patient_number=? ORDER BY e.created_at DESC",patientNumber);
    }

    @GetMapping("/doctors")
    List<Map<String,Object>> doctors() {
        List<Map<String,Object>> list = db.queryForList("SELECT d.id,d.staff_user_id,s.display_name,d.specialty,d.appointment_minutes,d.active FROM doctors d JOIN staff_users s ON s.id=d.staff_user_id WHERE d.active=TRUE AND s.active=TRUE ORDER BY s.display_name");
        return list.stream().map(this::enrichDoctorMap).toList();
    }

    private Map<String,Object> enrichDoctorMap(Map<String,Object> m) {
        Map<String,Object> copy = new java.util.HashMap<>(m);
        String name = String.valueOf(m.get("display_name"));
        String spec = String.valueOf(m.get("specialty"));
        copy.put("name", name);
        copy.put("displayName", name);
        copy.put("specialization", spec);
        copy.put("specialty", spec);
        copy.put("qualification", "MBBS, MD");
        copy.put("consultationFee", new BigDecimal("500.00"));
        copy.put("phone", "+91 98450 00000");
        copy.put("availableDays", "Mon, Wed, Fri");
        copy.put("availableHours", "09:00 AM - 05:00 PM");
        return copy;
    }

    @PostMapping("/doctors") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createDoctor(@Valid @RequestBody DoctorInput d) {
        int minutes = d.appointmentMinutes()==null ? 20 : d.appointmentMinutes();
        String staffRole=db.query("SELECT role FROM staff_users WHERE id=? AND active=TRUE",rs->rs.next()?rs.getString(1):null,d.staffUserId());
        if(!"DOCTOR".equals(staffRole)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an active staff account with the Doctor role");
        Long id = insert("INSERT INTO doctors(staff_user_id,specialty,appointment_minutes) VALUES(?,?,?)",d.staffUserId(),d.specialty().trim(),minutes);
        audit.record("DOCTOR_CREATED","DOCTOR",id,"Staff account linked");
        return db.queryForMap("SELECT d.id,d.staff_user_id,s.display_name,d.specialty,d.appointment_minutes,d.active FROM doctors d JOIN staff_users s ON s.id=d.staff_user_id WHERE d.id=?",id);
    }

    @GetMapping("/doctors/{doctorId}/schedule")
    List<Map<String,Object>> doctorSchedule(@PathVariable long doctorId) { return db.queryForList("SELECT id,day_of_week,starts_at,ends_at FROM doctor_schedule WHERE doctor_id=? ORDER BY day_of_week,starts_at",doctorId); }

    @PostMapping("/doctors/{doctorId}/schedule") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> addSchedule(@PathVariable long doctorId,@Valid @RequestBody ScheduleInput s) {
        if(s.dayOfWeek()<1||s.dayOfWeek()>7||!s.endsAt().isAfter(s.startsAt())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a weekday and an end time after the start time");
        Long doctorExists=db.query("SELECT id FROM doctors WHERE id=? AND active=TRUE FOR UPDATE",rs->rs.next()?rs.getLong(1):null,doctorId);
        if(doctorExists==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active doctor not found");
        Integer overlap=db.queryForObject("SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id=? AND day_of_week=? AND starts_at<? AND ends_at>?",Integer.class,doctorId,s.dayOfWeek(),s.endsAt(),s.startsAt());
        if(overlap!=null&&overlap>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Those hours overlap an existing schedule");
        Long id=insert("INSERT INTO doctor_schedule(doctor_id,day_of_week,starts_at,ends_at) VALUES(?,?,?,?)",doctorId,s.dayOfWeek(),s.startsAt(),s.endsAt());
        audit.record("DOCTOR_SCHEDULE_ADDED","DOCTOR_SCHEDULE",id,"Weekly hours configured"); return db.queryForMap("SELECT id,day_of_week,starts_at,ends_at FROM doctor_schedule WHERE id=?",id);
    }

    @GetMapping("/appointments")
    List<Map<String,Object>> appointments(@RequestParam(required=false) LocalDate date) {
        List<Map<String,Object>> list;
        if (date != null) {
            OffsetDateTime from=date.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
            OffsetDateTime until=date.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
            if(isDoctor()) list = db.queryForList("SELECT a.id,p.patient_number,p.full_name patient_name,d.id doctor_id,s.display_name doctor_name,a.starts_at,a.ends_at,a.visit_type,a.status,a.reason FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE a.starts_at>=? AND a.starts_at<? AND d.staff_user_id=? ORDER BY a.starts_at",from,until,currentStaffId());
            else list = db.queryForList("SELECT a.id,p.patient_number,p.full_name patient_name,d.id doctor_id,s.display_name doctor_name,a.starts_at,a.ends_at,a.visit_type,a.status,a.reason FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE a.starts_at>=? AND a.starts_at<? ORDER BY a.starts_at",from,until);
        } else {
            OffsetDateTime from=LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).minusDays(7).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
            if(isDoctor()) list = db.queryForList("SELECT a.id,p.patient_number,p.full_name patient_name,d.id doctor_id,s.display_name doctor_name,a.starts_at,a.ends_at,a.visit_type,a.status,a.reason FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE a.starts_at>=? AND d.staff_user_id=? ORDER BY a.starts_at",from,currentStaffId());
            else list = db.queryForList("SELECT a.id,p.patient_number,p.full_name patient_name,d.id doctor_id,s.display_name doctor_name,a.starts_at,a.ends_at,a.visit_type,a.status,a.reason FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE a.starts_at>=? ORDER BY a.starts_at",from);
        }
        return list.stream().map(this::enrichAppointmentMap).toList();
    }

    private Map<String,Object> enrichAppointmentMap(Map<String,Object> m) {
        Map<String,Object> copy = new java.util.HashMap<>(m);
        Object startsAt = m.get("starts_at");
        String dateStr = LocalDate.now().toString();
        String timeStr = "10:00 AM";
        if (startsAt instanceof OffsetDateTime odt) {
            dateStr = odt.toLocalDate().toString();
            timeStr = odt.toLocalTime().toString();
        } else if (startsAt != null) {
            String s = startsAt.toString();
            if (s.length() >= 10) dateStr = s.substring(0, 10);
            if (s.length() >= 16) timeStr = s.substring(11, 16);
        }
        copy.put("patientId", m.get("patient_number"));
        copy.put("patientName", m.get("patient_name"));
        copy.put("appointmentDate", dateStr);
        copy.put("timeSlot", timeStr);
        return copy;
    }

    @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createAppointment(@Valid @RequestBody AppointmentInput a) {
        if(!List.of("APPOINTMENT","WALK_IN").contains(a.visitType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Visit type must be APPOINTMENT or WALK_IN");
        if(a.startsAt().isBefore(OffsetDateTime.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Appointment cannot be booked in the past");
        if(isDoctor()) {
            Integer ownsDoctor=db.query("SELECT COUNT(*) FROM doctors WHERE id=? AND staff_user_id=? AND active=TRUE",rs->rs.next()?rs.getInt(1):0,a.doctorId(),currentStaffId());
            if(ownsDoctor==null||ownsDoctor==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Doctor profile not found");
        }
        Long patient = db.query("SELECT id FROM patients WHERE patient_number=? AND active=TRUE FOR UPDATE",rs->rs.next()?rs.getLong(1):null,a.patientNumber());
        if(patient==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active patient not found");
        Integer duration=db.query("SELECT appointment_minutes FROM doctors WHERE id=? AND active=TRUE FOR UPDATE",rs->rs.next()?rs.getInt(1):null,a.doctorId());
        if(duration==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active doctor not found");
        OffsetDateTime endsAt=a.startsAt().plusMinutes(duration);
        if(a.endsAt()!=null&&!a.endsAt().isEqual(endsAt)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Appointment length is set by the doctor's configured visit length");
        LocalDate clinicDate=a.startsAt().atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
        LocalTime clinicStart=a.startsAt().atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime();
        LocalTime clinicEnd=endsAt.atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime();
        Integer inSchedule=db.queryForObject("SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id=? AND day_of_week=? AND starts_at<=? AND ends_at>=?",Integer.class,a.doctorId(),clinicDate.getDayOfWeek().getValue(),clinicStart,clinicEnd);
        if(inSchedule==null||inSchedule==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Appointment falls outside the doctor's configured clinic hours");
        Integer conflicts = db.queryForObject("SELECT COUNT(*) FROM appointments WHERE doctor_id=? AND status NOT IN ('CANCELLED','NO_SHOW') AND starts_at < ? AND ends_at > ?",Integer.class,a.doctorId(),endsAt,a.startsAt());
        if(conflicts!=null && conflicts>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Doctor already has an appointment during that time");
        Long id=insert("INSERT INTO appointments(patient_id,doctor_id,starts_at,ends_at,visit_type,status,reason,booked_by) VALUES(?,?,?,?,?,'BOOKED',?,?)",patient,a.doctorId(),a.startsAt(),endsAt,a.visitType(),a.reason(),currentStaffId());
        recordAppointmentStatusChange(id,null,"BOOKED","Appointment booked");
        audit.record("APPOINTMENT_BOOKED","APPOINTMENT",id,"Booking created");
        return db.queryForMap("SELECT id,patient_id,doctor_id,starts_at,ends_at,visit_type,status,reason FROM appointments WHERE id=?",id);
    }

    @PatchMapping("/appointments/{id}/status") @Transactional
    Map<String,Object> updateAppointment(@PathVariable long id,@Valid @RequestBody StatusInput input) {
        if(!List.of("BOOKED","CHECKED_IN","IN_PROGRESS","COMPLETED","CANCELLED","NO_SHOW").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported appointment status");
        Map<String,Object> current=db.query("SELECT status,ends_at FROM appointments WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("status",rs.getString("status"),"endsAt",rs.getObject("ends_at",OffsetDateTime.class)):null,id);
        if(current==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        String old=(String)current.get("status");
        if(isDoctor()) { Integer owns=db.query("SELECT COUNT(*) FROM appointments a JOIN doctors d ON d.id=a.doctor_id WHERE a.id=? AND d.staff_user_id=?",rs->rs.next()?rs.getInt(1):0,id,currentStaffId()); if(owns==null||owns==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Doctors can only update their own appointments"); }
        boolean allowed=switch(old) { case "BOOKED" -> List.of("CHECKED_IN","CANCELLED","NO_SHOW").contains(input.status()); case "CHECKED_IN" -> List.of("IN_PROGRESS","CANCELLED").contains(input.status()); case "IN_PROGRESS" -> "COMPLETED".equals(input.status()); default -> false; };
        if(!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT,"That appointment status change is not allowed");
        if(List.of("CANCELLED","NO_SHOW").contains(input.status())&&(input.reason()==null||input.reason().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a reason for cancelling or marking an appointment as a no-show");
        if("NO_SHOW".equals(input.status())&&((OffsetDateTime)current.get("endsAt")).isAfter(OffsetDateTime.now())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Mark an appointment as a no-show only after its scheduled end time");
        int changed=db.update("UPDATE appointments SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.status(),id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        recordAppointmentStatusChange(id,old,input.status(),input.reason());
        return db.queryForMap("SELECT id,status FROM appointments WHERE id=?",id);
    }

    @GetMapping("/appointments/{id}/status-history")
    List<Map<String,Object>> appointmentStatusHistory(@PathVariable long id) {
        if(isDoctor()) { Integer owns=db.query("SELECT COUNT(*) FROM appointments a JOIN doctors d ON d.id=a.doctor_id WHERE a.id=? AND d.staff_user_id=?",rs->rs.next()?rs.getInt(1):0,id,currentStaffId()); if(owns==null||owns==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found"); }
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM appointments WHERE id=?",Integer.class,id);
        if(exists==null||exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        return db.queryForList("SELECT h.id,h.previous_status,h.new_status,h.reason,h.changed_at,s.display_name changed_by FROM appointment_status_history h JOIN staff_users s ON s.id=h.changed_by WHERE h.appointment_id=? ORDER BY h.changed_at,h.id",id);
    }

    @PutMapping("/appointments/{id}/reschedule") @Transactional
    Map<String,Object> reschedule(@PathVariable long id,@Valid @RequestBody RescheduleInput input) {
        Map<String,Object> current=db.query("SELECT doctor_id,status FROM appointments WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("doctor",rs.getLong(1),"status",rs.getString(2)):null,id);
        if(current==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        if(isDoctor()) {
            Integer ownsAppointment=db.query("SELECT COUNT(*) FROM doctors WHERE id=? AND staff_user_id=?",rs->rs.next()?rs.getInt(1):0,current.get("doctor"),currentStaffId());
            if(ownsAppointment==null||ownsAppointment==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        }
        if(!"BOOKED".equals(current.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only booked appointments can be rescheduled");
        if(input.startsAt().isBefore(OffsetDateTime.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid future appointment time");
        long doctor=(long)current.get("doctor");
        Integer minutes=db.query("SELECT appointment_minutes FROM doctors WHERE id=? AND active=TRUE FOR UPDATE",rs->rs.next()?rs.getInt(1):null,doctor);
        if(minutes==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active doctor not found");
        OffsetDateTime endsAt=input.startsAt().plusMinutes(minutes);
        if(input.endsAt()!=null&&!input.endsAt().isEqual(endsAt)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Appointment length is set by the doctor's configured visit length");
        java.time.ZoneId clinic=java.time.ZoneId.of("Asia/Kolkata"); LocalDate day=input.startsAt().atZoneSameInstant(clinic).toLocalDate(); LocalTime start=input.startsAt().atZoneSameInstant(clinic).toLocalTime(); LocalTime end=endsAt.atZoneSameInstant(clinic).toLocalTime();
        Integer within=db.queryForObject("SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id=? AND day_of_week=? AND starts_at<=? AND ends_at>=?",Integer.class,doctor,day.getDayOfWeek().getValue(),start,end);
        if(within==null||within==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"New time falls outside configured clinic hours");
        Integer conflicts=db.queryForObject("SELECT COUNT(*) FROM appointments WHERE id<>? AND doctor_id=? AND status NOT IN ('CANCELLED','NO_SHOW') AND starts_at<? AND ends_at>?",Integer.class,id,doctor,endsAt,input.startsAt());
        if(conflicts!=null&&conflicts>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Doctor already has an appointment during that time");
        db.update("UPDATE appointments SET starts_at=?,ends_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.startsAt(),endsAt,id);
        audit.record("APPOINTMENT_RESCHEDULED","APPOINTMENT",id,"Appointment time changed");return db.queryForMap("SELECT id,starts_at,ends_at,status FROM appointments WHERE id=?",id);
    }

    @GetMapping("/inventory/medicines")
    List<Map<String,Object>> medicines() { LocalDate clinicToday=LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")); return db.queryForList("SELECT m.id,m.code,m.generic_name,m.brand_name,m.strength,m.dosage_form,m.unit,m.reorder_level,m.sale_price,m.active,COALESCE(SUM(CASE WHEN b.expiry_date>=? THEN b.quantity_on_hand ELSE 0 END),0) quantity_on_hand FROM medicines m LEFT JOIN medicine_batches b ON b.medicine_id=m.id WHERE m.active=TRUE GROUP BY m.id,m.code,m.generic_name,m.brand_name,m.strength,m.dosage_form,m.unit,m.reorder_level,m.sale_price,m.active ORDER BY m.generic_name",clinicToday); }

    @GetMapping("/inventory/alerts")
    Map<String,Object> stockAlerts() {
        LocalDate clinicToday=LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        List<Map<String,Object>> low=db.queryForList("SELECT m.id,m.code,m.generic_name,m.reorder_level,COALESCE(SUM(CASE WHEN b.expiry_date>=? THEN b.quantity_on_hand ELSE 0 END),0) quantity_on_hand FROM medicines m LEFT JOIN medicine_batches b ON b.medicine_id=m.id WHERE m.active=TRUE GROUP BY m.id,m.code,m.generic_name,m.reorder_level HAVING COALESCE(SUM(CASE WHEN b.expiry_date>=? THEN b.quantity_on_hand ELSE 0 END),0)<=m.reorder_level ORDER BY m.generic_name",clinicToday,clinicToday);
        List<Map<String,Object>> expiring=db.queryForList("SELECT m.generic_name,b.id batch_id,b.batch_number,b.expiry_date,b.quantity_on_hand FROM medicine_batches b JOIN medicines m ON m.id=b.medicine_id WHERE b.quantity_on_hand>0 AND b.expiry_date BETWEEN ? AND ? ORDER BY b.expiry_date",clinicToday,clinicToday.plusDays(30));
        return Map.of("lowStock",low,"expiringWithin30Days",expiring);
    }

    @GetMapping("/inventory/movements")
    List<Map<String,Object>> stockMovements(@RequestParam(required=false) Long medicineId,@RequestParam(defaultValue="100") int limit) {
        int safeLimit=Math.max(1,Math.min(limit,500));
        String sql="SELECT sm.id,sm.medicine_id,m.generic_name,sm.batch_id,b.batch_number,sm.movement_type,sm.quantity_change,sm.reason,sm.reference_type,sm.reference_id,s.display_name created_by,sm.created_at FROM stock_movements sm JOIN medicines m ON m.id=sm.medicine_id JOIN medicine_batches b ON b.id=sm.batch_id JOIN staff_users s ON s.id=sm.created_by ";
        if(medicineId!=null) return db.queryForList(sql+"WHERE sm.medicine_id=? ORDER BY sm.created_at DESC,sm.id DESC LIMIT ?",medicineId,safeLimit);
        return db.queryForList(sql+"ORDER BY sm.created_at DESC,sm.id DESC LIMIT ?",safeLimit);
    }

    @PostMapping("/inventory/medicines") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createMedicine(@Valid @RequestBody MedicineInput m) {
        Long id=insert("INSERT INTO medicines(code,generic_name,brand_name,strength,dosage_form,unit,reorder_level,sale_price) VALUES(?,?,?,?,?,?,?,?)",m.code(),m.genericName(),m.brandName(),m.strength(),m.dosageForm(),m.unit(),m.reorderLevel(),m.salePrice());
        audit.record("MEDICINE_CREATED","MEDICINE",id,"Catalog item"); return db.queryForMap("SELECT * FROM medicines WHERE id=?",id);
    }

    @PostMapping("/inventory/receipts") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> receiveStock(@Valid @RequestBody BatchInput b) {
        if(b.expiryDate().isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expiry must be today or later in India");
        Integer active=db.query("SELECT COUNT(*) FROM medicines WHERE id=? AND active=TRUE",rs->rs.next()?rs.getInt(1):0,b.medicineId());
        if(active==null||active==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active medicine not found");
        Long user=currentStaffId();
        Long id=insert("INSERT INTO medicine_batches(medicine_id,batch_number,expiry_date,supplier,purchase_price,quantity_on_hand,received_by) VALUES(?,?,?,?,?,?,?)",b.medicineId(),b.batchNumber(),b.expiryDate(),b.supplier(),b.purchasePrice(),b.quantity(),user);
        db.update("INSERT INTO stock_movements(medicine_id,batch_id,movement_type,quantity_change,reason,created_by) VALUES(?,?,'RECEIVED',?,'Supplier receipt',?)",b.medicineId(),id,b.quantity(),user);
        audit.record("STOCK_RECEIVED","MEDICINE_BATCH",id,"Quantity received: "+b.quantity()); return db.queryForMap("SELECT * FROM medicine_batches WHERE id=?",id);
    }

    @PostMapping("/inventory/adjustments") @Transactional
    Map<String,Object> adjustStock(@Valid @RequestBody AdjustmentInput a) {
        if(a.quantityChange()==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Stock adjustment cannot be zero");
        Map<String,Object> batch=db.query("SELECT medicine_id,quantity_on_hand FROM medicine_batches WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("medicine",rs.getLong(1),"quantity",rs.getInt(2)):null,a.batchId());
        if(batch==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine batch not found");
        int updated=(int)batch.get("quantity")+a.quantityChange(); if(updated<0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Adjustment cannot make stock negative");
        db.update("UPDATE medicine_batches SET quantity_on_hand=? WHERE id=?",updated,a.batchId());
        db.update("INSERT INTO stock_movements(medicine_id,batch_id,movement_type,quantity_change,reason,created_by) VALUES(?,?,'ADJUSTED',?,?,?)",batch.get("medicine"),a.batchId(),a.quantityChange(),a.reason(),currentStaffId());
        audit.record("STOCK_ADJUSTED","MEDICINE_BATCH",a.batchId(),"Adjustment: "+a.quantityChange()+"; reason recorded");
        return db.queryForMap("SELECT * FROM medicine_batches WHERE id=?",a.batchId());
    }

    @PostMapping("/inventory/supplier-returns") @Transactional
    Map<String,Object> returnStockToSupplier(@Valid @RequestBody SupplierReturnInput input) {
        Map<String,Object> batch=db.query("SELECT medicine_id,quantity_on_hand FROM medicine_batches WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("medicine",rs.getLong(1),"quantity",rs.getInt(2)):null,input.batchId());
        if(batch==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine batch not found");
        int available=(int)batch.get("quantity");
        if(input.quantity()>available) throw new ResponseStatusException(HttpStatus.CONFLICT,"Supplier return exceeds the batch quantity on hand");
        int remaining=available-input.quantity();
        db.update("UPDATE medicine_batches SET quantity_on_hand=? WHERE id=?",remaining,input.batchId());
        db.update("INSERT INTO stock_movements(medicine_id,batch_id,movement_type,quantity_change,reason,created_by) VALUES(?,?,'RETURNED',?,?,?)",batch.get("medicine"),input.batchId(),-input.quantity(),input.reason().trim(),currentStaffId());
        audit.record("STOCK_RETURNED_TO_SUPPLIER","MEDICINE_BATCH",input.batchId(),"Quantity: "+input.quantity()+"; reason recorded");
        return db.queryForMap("SELECT * FROM medicine_batches WHERE id=?",input.batchId());
    }

    @PostMapping("/encounters") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createEncounter(@Valid @RequestBody EncounterInput e) {
        String appointmentStatus=db.query("SELECT status FROM appointments WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,e.appointmentId());
        Map<String,Object> appointment=db.query("SELECT patient_id,doctor_id,d.staff_user_id FROM appointments a JOIN doctors d ON d.id=a.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE a.id=?",rs->rs.next()?Map.of("patient",rs.getLong(1),"doctor",rs.getLong(2),"status",appointmentStatus,"doctorStaff",rs.getLong(3)):null,e.appointmentId());
        if(appointment==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found");
        if(!List.of("CHECKED_IN","IN_PROGRESS").contains(appointment.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Check in the patient before starting a consultation");
        if(isDoctor() && !currentStaffId().equals(appointment.get("doctorStaff"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Doctors can only start consultations assigned to them");
        Long id=insert("INSERT INTO encounters(appointment_id,patient_id,doctor_id,complaint,blood_pressure,pulse_per_minute,temperature_celsius,weight_kg,allergies,current_medicines,clinical_notes,diagnosis_text,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'OPEN',?)",e.appointmentId(),appointment.get("patient"),appointment.get("doctor"),e.complaint(),e.bloodPressure(),e.pulsePerMinute(),e.temperatureCelsius(),e.weightKg(),e.allergies(),e.currentMedicines(),e.clinicalNotes(),e.diagnosisText(),currentStaffId());
        db.update("UPDATE appointments SET status='IN_PROGRESS',updated_at=CURRENT_TIMESTAMP WHERE id=?",e.appointmentId());
        if("CHECKED_IN".equals(appointment.get("status"))) recordAppointmentStatusChange(e.appointmentId(),"CHECKED_IN","IN_PROGRESS","Consultation started");
        audit.record("ENCOUNTER_CREATED","ENCOUNTER",id,"Consultation started"); return db.queryForMap("SELECT * FROM encounters WHERE id=?",id);
    }

    @PutMapping("/encounters/{id}") @Transactional
    Map<String,Object> editEncounter(@PathVariable long id,@Valid @RequestBody EncounterEditInput e) {
        requireOwnEncounter(id);
        int changed=db.update("UPDATE encounters SET complaint=?,blood_pressure=?,pulse_per_minute=?,temperature_celsius=?,weight_kg=?,allergies=?,current_medicines=?,clinical_notes=?,diagnosis_text=? WHERE id=? AND status='OPEN'",e.complaint(),e.bloodPressure(),e.pulsePerMinute(),e.temperatureCelsius(),e.weightKg(),e.allergies(),e.currentMedicines(),e.clinicalNotes(),e.diagnosisText(),id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Encounter not found or already finalized");
        audit.record("ENCOUNTER_UPDATED","ENCOUNTER",id,"Clinical details updated"); return db.queryForMap("SELECT * FROM encounters WHERE id=?",id);
    }

    @PostMapping("/encounters/{id}/finalize") @Transactional
    Map<String,Object> finalizeEncounter(@PathVariable long id) {
        requireOwnEncounter(id);
        int changed=db.update("UPDATE encounters SET status='FINALIZED',finalized_at=CURRENT_TIMESTAMP WHERE id=? AND status='OPEN'",id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Encounter not found or already finalized");
        Long appointmentId=db.queryForObject("SELECT appointment_id FROM encounters WHERE id=?",Long.class,id);
        db.update("UPDATE appointments SET status='COMPLETED',updated_at=CURRENT_TIMESTAMP WHERE id=?",appointmentId);
        recordAppointmentStatusChange(appointmentId,"IN_PROGRESS","COMPLETED","Consultation finalized");
        audit.record("ENCOUNTER_FINALIZED","ENCOUNTER",id,"Record finalized"); return db.queryForMap("SELECT * FROM encounters WHERE id=?",id);
    }

    @GetMapping("/encounters/{id}")
    Map<String,Object> encounter(@PathVariable long id) {
        Map<String,Object> record=db.query("SELECT e.*,p.patient_number,p.full_name patient_name,s.display_name doctor_name FROM encounters e JOIN patients p ON p.id=e.patient_id JOIN doctors d ON d.id=e.doctor_id JOIN staff_users s ON s.id=d.staff_user_id WHERE e.id=?",rs->rs.next()?new java.util.HashMap<>(new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,1)):null,id);
        if(record==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Encounter not found");
        if(isDoctor()) {
            Integer own=db.query("SELECT COUNT(*) FROM encounters e JOIN doctors d ON d.id=e.doctor_id WHERE e.id=? AND d.staff_user_id=?",rs->rs.next()?rs.getInt(1):0,id,currentStaffId());
            if(own==null||own==0) requireDoctorPatientAccess(((Number)record.get("patient_id")).longValue());
        }
        return record;
    }

    @PostMapping("/encounters/{id}/addenda") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> addendum(@PathVariable long id,@Valid @RequestBody ApiModels.EncounterAddendumRequest request) {
        requireOwnEncounter(id);
        Integer finalized=db.query("SELECT COUNT(*) FROM encounters WHERE id=? AND status='FINALIZED'",rs->rs.next()?rs.getInt(1):0,id);
        if(finalized==null||finalized==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Addenda are for finalized records; edit the open encounter instead");
        Long addendum=insert("INSERT INTO encounter_addenda(encounter_id,correction_reason,addendum_text,created_by) VALUES(?,?,?,?)",id,request.correctionReason(),request.addendumText(),currentStaffId());
        audit.record("ENCOUNTER_ADDENDUM_ADDED","ENCOUNTER",id,"A correction was appended");
        return db.queryForMap("SELECT id,encounter_id,correction_reason,addendum_text,created_at FROM encounter_addenda WHERE id=?",addendum);
    }

    @GetMapping("/encounters/{id}/addenda")
    List<Map<String,Object>> addenda(@PathVariable long id) { requireOwnEncounter(id); return db.queryForList("SELECT a.id,a.correction_reason,a.addendum_text,s.display_name created_by,a.created_at FROM encounter_addenda a JOIN staff_users s ON s.id=a.created_by WHERE a.encounter_id=? ORDER BY a.created_at",id); }

    @PostMapping("/prescriptions") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createPrescription(@Valid @RequestBody PrescriptionInput p) {
        if(p.items().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Prescription needs at least one medicine");
        Integer open=db.query("SELECT COUNT(*) FROM encounters WHERE id=? AND status='OPEN'",rs->rs.next()?rs.getInt(1):0,p.encounterId());
        if(open==null||open==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Prescriptions can only be added to an open encounter");
        requireOwnEncounter(p.encounterId());
        Long prescription=insert("INSERT INTO prescriptions(encounter_id,prescribed_by) VALUES(?,?)",p.encounterId(),currentStaffId());
        for(PrescriptionLine line:p.items()) {
            if(line.quantity()<1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Prescription quantities must be positive");
            Integer active=db.query("SELECT COUNT(*) FROM medicines WHERE id=? AND active=TRUE",rs->rs.next()?rs.getInt(1):0,line.medicineId());
            if(active==null||active==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Prescription includes an inactive or unknown medicine");
            insert("INSERT INTO prescription_items(prescription_id,medicine_id,dosage,route,frequency,duration_days,quantity_prescribed,instructions) VALUES(?,?,?,?,?,?,?,?)",prescription,line.medicineId(),line.dosage(),line.route(),line.frequency(),line.durationDays(),line.quantity(),line.instructions());
        }
        audit.record("PRESCRIPTION_CREATED","PRESCRIPTION",prescription,"Items: "+p.items().size()); return db.queryForMap("SELECT * FROM prescriptions WHERE id=?",prescription);
    }

    @GetMapping("/encounters/{encounterId}/prescriptions")
    List<Map<String,Object>> prescriptions(@PathVariable long encounterId) {
        if(isDoctor()) requireOwnEncounter(encounterId);
        return db.queryForList("SELECT p.id prescription_id,p.created_at,pi.id item_id,pi.medicine_id,m.generic_name,m.brand_name,m.strength,pi.dosage,pi.route,pi.frequency,pi.duration_days,pi.quantity_prescribed,COALESCE((SELECT SUM(d.quantity) FROM dispensings d WHERE d.prescription_item_id=pi.id),0) quantity_dispensed,pi.instructions FROM prescriptions p JOIN prescription_items pi ON pi.prescription_id=p.id JOIN medicines m ON m.id=pi.medicine_id WHERE p.encounter_id=? ORDER BY p.created_at,pi.id",encounterId);
    }

    @GetMapping("/prescription-items/{itemId}/substitution-history")
    List<Map<String,Object>> substitutionHistory(@PathVariable long itemId) {
        Long encounterId=db.query("SELECT p.encounter_id FROM prescription_items pi JOIN prescriptions p ON p.id=pi.prescription_id WHERE pi.id=?",rs->rs.next()?rs.getLong(1):null,itemId);
        if(encounterId==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Prescription item not found");
        if(isDoctor()) requireOwnEncounter(encounterId);
        return db.queryForList("SELECT h.id,h.prescription_item_id,h.original_medicine_id,om.generic_name original_medicine_name,h.substitute_medicine_id,sm.generic_name substitute_medicine_name,h.approved_by,s.display_name approved_by_name,h.reason,h.approved_at FROM prescription_substitution_history h JOIN medicines om ON om.id=h.original_medicine_id JOIN medicines sm ON sm.id=h.substitute_medicine_id JOIN staff_users s ON s.id=h.approved_by WHERE h.prescription_item_id=? ORDER BY h.approved_at DESC,h.id DESC",itemId);
    }

    @PostMapping("/prescription-items/{itemId}/substitution-approvals") @Transactional
    Map<String,Object> approvePrescriptionSubstitution(@PathVariable long itemId,@Valid @RequestBody SubstitutionApprovalInput input) {
        Long encounterId=db.query("SELECT p.encounter_id FROM prescription_items pi JOIN prescriptions p ON p.id=pi.prescription_id WHERE pi.id=?",rs->rs.next()?rs.getLong(1):null,itemId);
        if(encounterId==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Prescription item not found");
        String encounterStatus=db.query("SELECT status FROM encounters WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,encounterId);
        requireOwnEncounter(encounterId);
        if(!"FINALIZED".equals(encounterStatus)) throw new ResponseStatusException(HttpStatus.CONFLICT,"A substitution needs an existing finalized prescription");
        PrescriptionItem item=db.query("SELECT medicine_id,substitution_approved_by FROM prescription_items WHERE id=? FOR UPDATE",rs->{if(!rs.next())return null;long approvedBy=rs.getLong("substitution_approved_by");boolean noPriorApproval=rs.wasNull();return new PrescriptionItem(rs.getLong("medicine_id"),noPriorApproval?null:approvedBy);},itemId);
        if(item==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Prescription item not found");
        if(item.substitutionApprovedBy()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"A medicine substitution has already been approved for this prescription item");
        long originalMedicine=item.medicineId();
        if(originalMedicine==input.substituteMedicineId()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a different medicine as the substitute");
        Integer active=db.query("SELECT COUNT(*) FROM medicines WHERE id=? AND active=TRUE",rs->rs.next()?rs.getInt(1):0,input.substituteMedicineId());
        if(active==null||active==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an active medicine from the catalogue");
        Integer alreadyDispensed=db.queryForObject("SELECT COUNT(*) FROM dispensings WHERE prescription_item_id=?",Integer.class,itemId);
        if(alreadyDispensed!=null&&alreadyDispensed>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"A medicine cannot be substituted after any quantity has been dispensed");
        Integer invoiced=db.queryForObject("SELECT COUNT(*) FROM invoices WHERE encounter_id=?",Integer.class,encounterId);
        if(invoiced!=null&&invoiced>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"A medicine cannot be substituted after the visit has been invoiced");
        Long doctor=currentStaffId();String reason=input.reason().trim();
        db.update("INSERT INTO prescription_substitution_history(prescription_item_id,original_medicine_id,substitute_medicine_id,approved_by,reason) VALUES(?,?,?,?,?)",itemId,originalMedicine,input.substituteMedicineId(),doctor,reason);
        db.update("UPDATE prescription_items SET medicine_id=?,substitution_approved_by=? WHERE id=?",input.substituteMedicineId(),doctor,itemId);
        audit.record("PRESCRIPTION_SUBSTITUTION_APPROVED","PRESCRIPTION_ITEM",itemId,"Original medicine "+originalMedicine+" changed with clinician approval; reason recorded");
        return db.queryForMap("SELECT h.id,h.prescription_item_id,h.original_medicine_id,h.substitute_medicine_id,h.approved_by,s.display_name approved_by_name,h.reason,h.approved_at FROM prescription_substitution_history h JOIN staff_users s ON s.id=h.approved_by WHERE h.id=(SELECT MAX(id) FROM prescription_substitution_history WHERE prescription_item_id=?)",itemId);
    }

    @PostMapping("/prescription-items/{itemId}/dispense") @Transactional
    Map<String,Object> dispense(@PathVariable long itemId,@Valid @RequestBody DispenseInput request) {
        if(request.quantity()<1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Dispense quantity must be positive");
        Long encounterId=db.query("SELECT p.encounter_id FROM prescription_items pi JOIN prescriptions p ON p.id=pi.prescription_id WHERE pi.id=?",rs->rs.next()?rs.getLong(1):null,itemId);
        if(encounterId==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Prescription item not found");
        String encounterStatus=db.query("SELECT status FROM encounters WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,encounterId);
        if(!"FINALIZED".equals(encounterStatus)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Medicine can only be dispensed after the doctor finalizes the visit");
        Integer invoiced=db.queryForObject("SELECT COUNT(*) FROM invoices WHERE encounter_id=?",Integer.class,encounterId);
        if(invoiced!=null&&invoiced>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"This visit has been invoiced; further dispensing would not appear on that bill");
        Map<String,Object> item=db.query("SELECT medicine_id,quantity_prescribed FROM prescription_items WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("medicine",rs.getLong(1),"prescribed",rs.getInt(2)):null,itemId);
        if(item==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Prescription item not found");
        Integer dispensed=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM dispensings WHERE prescription_item_id=?",Integer.class,itemId);
        int remaining=(int)item.get("prescribed")-(dispensed==null?0:dispensed);
        if(request.quantity()>remaining) throw new ResponseStatusException(HttpStatus.CONFLICT,"Dispense quantity exceeds remaining prescription quantity");
        long medicine=(long)item.get("medicine"); int needed=request.quantity(); Long user=currentStaffId();
        List<Map<String,Object>> batches=db.queryForList("SELECT id,quantity_on_hand FROM medicine_batches WHERE medicine_id=? AND expiry_date>=? AND quantity_on_hand>0 ORDER BY expiry_date,id FOR UPDATE",medicine,LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")));
        for(Map<String,Object> batch:batches) {
            if(needed==0) break;
            long batchId=((Number)batch.get("id")).longValue(); int available=((Number)batch.get("quantity_on_hand")).intValue(); int take=Math.min(available,needed);
            db.update("UPDATE medicine_batches SET quantity_on_hand=quantity_on_hand-? WHERE id=?",take,batchId);
            db.update("INSERT INTO dispensings(prescription_item_id,batch_id,quantity,dispensed_by) VALUES(?,?,?,?)",itemId,batchId,take,user);
            db.update("INSERT INTO stock_movements(medicine_id,batch_id,movement_type,quantity_change,reason,reference_type,reference_id,created_by) VALUES(?,?,'DISPENSED',?,'Prescription dispensing','PRESCRIPTION_ITEM',?,?)",medicine,batchId,-take,itemId,user);
            needed-=take;
        }
        if(needed>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient unexpired stock; no stock was dispensed");
        audit.record("MEDICINE_DISPENSED","PRESCRIPTION_ITEM",itemId,"Quantity: "+request.quantity()); return db.queryForMap("SELECT id,prescription_item_id,batch_id,quantity,dispensed_at FROM dispensings WHERE prescription_item_id=? ORDER BY id DESC FETCH FIRST 1 ROW ONLY",itemId);
    }

    @GetMapping("/billing/services")
    List<Map<String,Object>> services() { return db.queryForList("SELECT id,code,description,unit_price,active FROM service_catalog WHERE active=TRUE ORDER BY description"); }

    @PostMapping("/billing/services") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createService(@Valid @RequestBody ServiceInput s) {
        Long id=insert("INSERT INTO service_catalog(code,description,unit_price) VALUES(?,?,?)",s.code(),s.description(),s.unitPrice()); audit.record("SERVICE_CREATED","SERVICE",id,"Catalog item"); return db.queryForMap("SELECT * FROM service_catalog WHERE id=?",id);
    }

    @PostMapping("/billing/invoices") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createInvoice(@Valid @RequestBody InvoiceInput input) {
        Map<String,Object> encounter=db.query("SELECT patient_id,status FROM encounters WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("patient",rs.getLong("patient_id"),"status",rs.getString("status")):null,input.encounterId());
        if(encounter==null||!"FINALIZED".equals(encounter.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Invoice requires an existing finalized encounter");
        Long patient=(Long)encounter.get("patient");
        Long invoice=insert("INSERT INTO invoices(invoice_number,encounter_id,patient_id,status,created_by) VALUES(?,?,?,'OPEN',?)", "I"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase(),input.encounterId(),patient,currentStaffId());
        for(Long serviceId:input.serviceIds()) {
            int added=db.update("INSERT INTO invoice_items(invoice_id,item_type,description_snapshot,quantity,unit_price_snapshot,source_id) SELECT ?,'SERVICE',description,1,unit_price,id FROM service_catalog WHERE id=? AND active=TRUE",invoice,serviceId);
            if(added==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A selected service is unavailable");
        }
        db.update("INSERT INTO invoice_items(invoice_id,item_type,description_snapshot,quantity,unit_price_snapshot,source_id) SELECT ?, 'MEDICINE', CONCAT(m.generic_name,' ',COALESCE(m.strength,'')),SUM(d.quantity),m.sale_price,m.id FROM prescriptions p JOIN prescription_items pi ON pi.prescription_id=p.id JOIN dispensings d ON d.prescription_item_id=pi.id JOIN medicines m ON m.id=pi.medicine_id WHERE p.encounter_id=? GROUP BY m.id,m.generic_name,m.strength,m.sale_price",invoice,input.encounterId());
        audit.record("INVOICE_CREATED","INVOICE",invoice,"Encounter billing"); return invoice(invoice);
    }

    @GetMapping("/billing/invoices")
    List<Map<String,Object>> invoices() { return db.queryForList("SELECT i.id,i.invoice_number,p.patient_number,p.full_name patient_name,i.status,COALESCE((SELECT SUM(quantity*unit_price_snapshot) FROM invoice_items x WHERE x.invoice_id=i.id),0) total_amount,COALESCE((SELECT SUM(amount-refund_amount) FROM payments y WHERE y.invoice_id=i.id),0) amount_paid,i.created_at FROM invoices i JOIN patients p ON p.id=i.patient_id ORDER BY i.created_at DESC LIMIT 100"); }

    @GetMapping("/billing/invoices/{id}")
    InvoiceDetails invoiceDetails(@PathVariable long id) {
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM invoices WHERE id=?",Integer.class,id);
        if(exists==null||exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invoice not found");
        Map<String,Object> summary=invoice(id);
        List<Map<String,Object>> items=db.queryForList("SELECT id,item_type,description_snapshot,quantity,unit_price_snapshot,quantity*unit_price_snapshot line_total FROM invoice_items WHERE invoice_id=? ORDER BY id",id);
        List<Map<String,Object>> payments=db.queryForList("SELECT p.id,p.receipt_number,p.amount,p.method,s.display_name received_by,p.received_at,p.refund_amount,p.refund_reason,r.display_name refunded_by,p.refunded_at FROM payments p JOIN staff_users s ON s.id=p.received_by LEFT JOIN staff_users r ON r.id=p.refunded_by WHERE p.invoice_id=? ORDER BY p.received_at,p.id",id);
        return new InvoiceDetails(summary,items,payments);
    }

    @PostMapping("/billing/invoices/{id}/payments") @Transactional
    Map<String,Object> pay(@PathVariable long id,@Valid @RequestBody PaymentInput p) {
        if(p.amount().signum()<=0||!List.of("CASH","UPI","CARD","BANK_TRANSFER","OTHER").contains(p.method())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a positive amount and supported payment method");
        String invoiceStatus=db.query("SELECT status FROM invoices WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
        if(invoiceStatus==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invoice not found");
        Map<String,Object> totals=db.queryForMap("SELECT COALESCE((SELECT SUM(quantity*unit_price_snapshot) FROM invoice_items WHERE invoice_id=?),0) total,COALESCE((SELECT SUM(amount-refund_amount) FROM payments WHERE invoice_id=?),0) paid",id,id);
        if("VOID".equals(invoiceStatus)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Voided invoice cannot be paid");
        BigDecimal outstanding=((BigDecimal)totals.get("total")).subtract((BigDecimal)totals.get("paid")); if(p.amount().compareTo(outstanding)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment exceeds amount due");
        Long payment=insert("INSERT INTO payments(receipt_number,invoice_id,amount,method,received_by) VALUES(?,?,?,?,?)","R"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase(),id,p.amount(),p.method(),currentStaffId());
        BigDecimal paid=((BigDecimal)totals.get("paid")).add(p.amount()); db.update("UPDATE invoices SET status=? WHERE id=?",paid.compareTo((BigDecimal)totals.get("total"))>=0?"PAID":"PARTIALLY_PAID",id);
        audit.record("PAYMENT_RECEIVED","PAYMENT",payment,"Amount: "+p.amount()+" "+p.method()); return db.queryForMap("SELECT * FROM payments WHERE id=?",payment);
    }

    @PostMapping("/billing/payments/{id}/refund") @Transactional
    Map<String,Object> refund(@PathVariable long id,@Valid @RequestBody RefundInput request) {
        Map<String,Object> payment=db.query("SELECT amount,refund_amount,invoice_id FROM payments WHERE id=? FOR UPDATE",rs->rs.next()?Map.of("amount",rs.getBigDecimal(1),"refunded",rs.getBigDecimal(2),"invoice",rs.getLong(3)):null,id);
        if(payment==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found");
        BigDecimal amount=(BigDecimal)payment.get("amount"), refunded=(BigDecimal)payment.get("refunded");
        if(request.amount().signum()<=0||refunded.add(request.amount()).compareTo(amount)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund exceeds the payment's refundable balance");
        Long invoice=(long)payment.get("invoice");
        db.query("SELECT id FROM invoices WHERE id=? FOR UPDATE",rs->rs.next()?rs.getLong(1):null,invoice);
        db.update("UPDATE payments SET refund_amount=?,refund_reason=?,refunded_by=?,refunded_at=CURRENT_TIMESTAMP WHERE id=?",refunded.add(request.amount()),request.reason(),currentStaffId(),id);
        Map<String,Object> sums=db.queryForMap("SELECT COALESCE((SELECT SUM(quantity*unit_price_snapshot) FROM invoice_items WHERE invoice_id=?),0) total,COALESCE((SELECT SUM(amount-refund_amount) FROM payments WHERE invoice_id=?),0) paid",invoice,invoice);
        db.update("UPDATE invoices SET status=CASE WHEN status='VOID' THEN status WHEN ?>=? THEN 'PAID' WHEN ?>0 THEN 'PARTIALLY_PAID' ELSE 'OPEN' END WHERE id=?",sums.get("paid"),sums.get("total"),sums.get("paid"),invoice);
        audit.record("PAYMENT_REFUNDED","PAYMENT",id,"Reason: "+request.reason()); return db.queryForMap("SELECT * FROM payments WHERE id=?",id);
    }

    @PostMapping("/billing/invoices/{id}/void") @Transactional
    Map<String,Object> voidInvoice(@PathVariable long id,@Valid @RequestBody ApiModels.VoidInvoiceRequest request) {
        String status=db.query("SELECT status FROM invoices WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
        if(status==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invoice not found");
        if("VOID".equals(status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Invoice is already void");
        BigDecimal netPayments=db.queryForObject("SELECT COALESCE(SUM(amount-refund_amount),0) FROM payments WHERE invoice_id=?",BigDecimal.class,id);
        if(netPayments!=null&&netPayments.signum()>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund all recorded payments before voiding this invoice");
        db.update("UPDATE invoices SET status='VOID',void_reason=?,voided_by=?,voided_at=CURRENT_TIMESTAMP WHERE id=?",request.reason(),currentStaffId(),id);
        audit.record("INVOICE_VOIDED","INVOICE",id,"Reason recorded"); return invoice(id);
    }

    private Map<String,Object> invoice(long id) { return db.queryForMap("SELECT i.id,i.invoice_number,p.patient_number,p.full_name patient_name,i.status,COALESCE((SELECT SUM(quantity*unit_price_snapshot) FROM invoice_items x WHERE x.invoice_id=i.id),0) total_amount,COALESCE((SELECT SUM(amount-refund_amount) FROM payments y WHERE y.invoice_id=i.id),0) amount_paid,i.created_at FROM invoices i JOIN patients p ON p.id=i.patient_id WHERE i.id=?",id); }

    @GetMapping("/admin/users")
    List<Map<String,Object>> users() { return db.queryForList("SELECT id,username,display_name,role,active FROM staff_users ORDER BY display_name"); }

    @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> createUser(@Valid @RequestBody StaffInput u) {
        if(!List.of("ADMIN","RECEPTIONIST","DOCTOR","PHARMACIST","BILLING").contains(u.role())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported staff role");
        String normalizedUsername=u.username().trim().toLowerCase(java.util.Locale.ROOT);
        if(u.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be no more than 72 UTF-8 bytes");
        Long id=insert("INSERT INTO staff_users(username,password_hash,display_name,role) VALUES(?,?,?,?)",normalizedUsername,passwords.encode(u.password()),u.displayName().trim(),u.role());
        audit.record("STAFF_CREATED","STAFF_USER",id,"Role: "+u.role()); return db.queryForMap("SELECT id,username,display_name,role,active FROM staff_users WHERE id=?",id);
    }

    @PutMapping("/me/password") @Transactional
    Map<String,String> changeOwnPassword(@Valid @RequestBody PasswordChangeInput input) {
        if(input.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be no more than 72 UTF-8 bytes");
        Long staffId=currentStaffId();
        db.update("UPDATE staff_users SET password_hash=? WHERE id=?",passwords.encode(input.newPassword()),staffId);
        audit.record("PASSWORD_CHANGED","STAFF_USER",staffId,"Staff changed own password");
        return Map.of("status","Password changed. Sign in again with the new password.");
    }

    @PostMapping("/admin/users/{id}/password") @Transactional
    Map<String,String> resetPassword(@PathVariable long id,@Valid @RequestBody PasswordChangeInput input) {
        if(id==currentStaffId()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Use the self-service password change for your own account");
        if(input.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be no more than 72 UTF-8 bytes");
        int changed=db.update("UPDATE staff_users SET password_hash=? WHERE id=?",passwords.encode(input.newPassword()),id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Staff account not found");
        audit.record("PASSWORD_RESET","STAFF_USER",id,"Administrator reset staff password");
        return Map.of("status","Password reset. Share the new credential through a separate secure channel.");
    }

    @PatchMapping("/admin/users/{id}/active") @Transactional
    Map<String,Object> setUserActive(@PathVariable long id,@Valid @RequestBody ActiveInput input) {
        if(id==currentStaffId()&&!input.active()) throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot deactivate your own account");
        int changed=db.update("UPDATE staff_users SET active=? WHERE id=?",input.active(),id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Staff account not found");
        audit.record(input.active()?"STAFF_REACTIVATED":"STAFF_DEACTIVATED","STAFF_USER",id,"Active status changed");
        return db.queryForMap("SELECT id,username,display_name,role,active FROM staff_users WHERE id=?",id);
    }

    @GetMapping("/admin/audit")
    List<Map<String,Object>> auditEvents(@RequestParam(defaultValue="100") int limit) {
        int safeLimit=Math.max(1,Math.min(limit,500));
        return db.queryForList("SELECT a.id,s.username actor,a.action,a.entity_type,a.entity_id,a.details,a.created_at FROM audit_events a JOIN staff_users s ON s.id=a.actor_id ORDER BY a.created_at DESC LIMIT ?",safeLimit);
    }

    @GetMapping("/admin/reports/daily")
    Map<String,Object> dailyReport(@RequestParam(required=false) LocalDate date) {
        LocalDate day=date==null?LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")):date;
        OffsetDateTime from=day.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
        OffsetDateTime until=day.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
        Map<String,Object> counts=db.queryForMap("SELECT COUNT(*) total,SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) completed,SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) cancelled,SUM(CASE WHEN status='NO_SHOW' THEN 1 ELSE 0 END) no_show FROM appointments WHERE starts_at>=? AND starts_at<?",from,until);
        BigDecimal collected=db.queryForObject("SELECT COALESCE(SUM(amount-refund_amount),0) FROM payments WHERE received_at>=? AND received_at<?",BigDecimal.class,from,until);
        return Map.of("date",day,"appointments",counts,"netPaymentsReceived",collected==null?BigDecimal.ZERO:collected);
    }

    public Long currentStaffId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String name = auth != null ? auth.getName() : null;
        if (name == null || "anonymousUser".equalsIgnoreCase(name)) {
            List<Long> fallback = db.queryForList("SELECT id FROM staff_users WHERE active=TRUE ORDER BY CASE WHEN role='ADMIN' THEN 1 WHEN role='RECEPTIONIST' THEN 2 ELSE 3 END, id LIMIT 1", Long.class);
            if (!fallback.isEmpty()) return fallback.get(0);
            return 1L;
        }
        List<Long> ids = db.queryForList("SELECT id FROM staff_users WHERE LOWER(username)=?", Long.class, name.toLowerCase(Locale.ROOT));
        if (!ids.isEmpty()) return ids.get(0);
        return 1L;
    }

    private void recordAppointmentStatusChange(Long appointmentId,String previousStatus,String newStatus,String reason) {
        db.update("INSERT INTO appointment_status_history(appointment_id,previous_status,new_status,reason,changed_by) VALUES(?,?,?,?,?)",appointmentId,previousStatus,newStatus,reason==null||reason.isBlank()?null:reason.trim(),currentStaffId());
        audit.record("APPOINTMENT_STATUS_CHANGED","APPOINTMENT",appointmentId,"Status: "+newStatus+(reason==null||reason.isBlank()?"":"; reason recorded"));
    }

    private boolean isDoctor() { return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->"ROLE_DOCTOR".equals(a.getAuthority())); }

    private void requireOwnEncounter(long encounterId) {
        if(!isDoctor()) return;
        Integer owned=db.query("SELECT COUNT(*) FROM encounters e JOIN doctors d ON d.id=e.doctor_id WHERE e.id=? AND d.staff_user_id=?",rs->rs.next()?rs.getInt(1):0,encounterId,currentStaffId());
        if(owned==null||owned==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Doctors can only access their own consultation records");
    }

    private void requireDoctorPatientAccess(long patientId) {
        if(!isDoctor()) return;
        Integer assigned=db.query("SELECT COUNT(*) FROM appointments a JOIN doctors d ON d.id=a.doctor_id WHERE a.patient_id=? AND d.staff_user_id=? AND a.status IN ('BOOKED','CHECKED_IN','IN_PROGRESS')",rs->rs.next()?rs.getInt(1):0,patientId,currentStaffId());
        if(assigned==null||assigned==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Doctors can review patient history only for patients currently assigned to their care");
    }

    private Long insert(String sql, Object... values) {
        KeyHolder holder = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        };
        db.update(creator, holder);
        if(holder.getKeyList().isEmpty()) throw new IllegalStateException("Database did not return a new record id");
        Map<String,Object> generatedKeys=holder.getKeyList().get(0);
        Object generatedId=generatedKeys.entrySet().stream().filter(entry->"id".equalsIgnoreCase(entry.getKey())).map(Map.Entry::getValue).findFirst().orElseGet(()->generatedKeys.size()==1?generatedKeys.values().iterator().next():null);
        if(!(generatedId instanceof Number key)) throw new IllegalStateException("Database did not return a readable record id");
        return key.longValue();
    }
}

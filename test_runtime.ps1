$ErrorActionPreference = "Stop"
$base = "http://localhost:8088"
$results = [System.Collections.Generic.List[string]]::new()

function Test-Endpoint {
    param($name, [scriptblock]$action)
    try {
        & $action
        $results.Add("PASS: $name")
        Write-Host "PASS: $name" -ForegroundColor Green
    } catch {
        $results.Add("FAIL: $name - $($_.Exception.Message)")
        Write-Host "FAIL: $name - $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Basic-Header($user, $pass) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("$($user):$($pass)")
    $encoded = [System.Convert]::ToBase64String($bytes)
    return @{ "Authorization" = "Basic $encoded" }
}

Write-Host "=== STARTING COMPREHENSIVE RUNTIME SYSTEM CHECK ===" -ForegroundColor Cyan

# 1. Static HTML Assets
$pages = @("/", "/index.html", "/patients.html", "/doctors.html", "/appointments.html", "/pharmacy.html", "/billing.html", "/records.html", "/login.html", "/patient-portal.html")
foreach ($p in $pages) {
    Test-Endpoint "Static Web Page '$p'" {
        $res = Invoke-WebRequest -Uri "$base$p" -Method GET -UseBasicParsing
        if ($res.StatusCode -ne 200) { throw "Status was $($res.StatusCode)" }
    }
}

Test-Endpoint "Mobile APK Download File Available" {
    $res = Invoke-WebRequest -Uri "$base/downloads/apexcare-mobile.apk" -Method HEAD -UseBasicParsing
    if ($res.StatusCode -ne 200) { throw "APK not reachable (Status $($res.StatusCode))" }
}

# 2. Admin Capabilities & Maximum Access
$adminAuth = Basic-Header "admin" "DemoAdminPass123!"

Test-Endpoint "Admin Auth & Profile Verification" {
    $res = Invoke-RestMethod -Uri "$base/api/me" -Headers $adminAuth -Method GET
    if ($res.role -ne "ADMIN") { throw "Expected ADMIN role, got $($res.role)" }
}

# Patient Record System: Add & Delete
$testPatientMrn = "MRN-TEST-" + (Get-Random -Minimum 10000 -Maximum 99999)
$createdPatientId = $null

Test-Endpoint "Admin Add Patient (Patient Record System Lifecycle)" {
    $payload = @{
        fullName = "Test Integration Patient $testPatientMrn"
        phone = "+91 99999 " + (Get-Random -Minimum 10000 -Maximum 99999)
        dateOfBirth = "1990-05-15"
        duplicateReviewAcknowledged = $true
        duplicateReviewReason = "Runtime integration verification"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/patients" -Headers $adminAuth -Method POST -Body $payload -ContentType "application/json"
    $script:createdPatientId = $res.id
    if (-not $res.id) { throw "No patient ID returned" }
}

Test-Endpoint "Admin Delete/Archive Patient (MRN/ID Query Param)" {
    $res = Invoke-RestMethod -Uri "$base/api/patients?id=$($script:createdPatientId)" -Headers $adminAuth -Method DELETE
    if ($res.message -notmatch "archived successfully") { throw "Unexpected deletion response: $($res.message)" }
}

# Add another patient to test path variable deletion
$testPatient2 = "MRN-TEST2-" + (Get-Random -Minimum 10000 -Maximum 99999)
$p2Id = $null
Test-Endpoint "Admin Add Second Patient" {
    $payload = @{
        fullName = "Path Test Patient $testPatient2"
        phone = "+91 98888 " + (Get-Random -Minimum 10000 -Maximum 99999)
        dateOfBirth = "1988-12-01"
        duplicateReviewAcknowledged = $true
        duplicateReviewReason = "Path variable test"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/patients" -Headers $adminAuth -Method POST -Body $payload -ContentType "application/json"
    $script:p2Id = $res.id
}

Test-Endpoint "Admin Delete Patient Path Variable (/api/patients/{id})" {
    $res = Invoke-RestMethod -Uri "$base/api/patients/$($script:p2Id)" -Headers $adminAuth -Method DELETE
    if ($res.message -notmatch "archived successfully") { throw "Unexpected path deletion response: $($res.message)" }
}

# Pharmacy Record System: Add, Receive, Adjust, Delete
$testMedCode = "TEST-DRUG-" + (Get-Random -Minimum 1000 -Maximum 9999)
$createdMedId = $null

Test-Endpoint "Admin Add Medicine (Pharmacy Record System)" {
    $payload = @{
        code = $testMedCode
        genericName = "Test Amoxicillin $testMedCode"
        brandName = "Moxikind Test"
        strength = "500mg"
        dosageForm = "CAPSULE"
        unit = "capsule"
        reorderLevel = 25
        salePrice = 8.50
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/inventory/medicines" -Headers $adminAuth -Method POST -Body $payload -ContentType "application/json"
    $script:createdMedId = $res.id
    if (-not $res.id) { throw "No medicine ID returned" }
}

Test-Endpoint "Pharmacy Listing Includes Active New Medicine" {
    $list = Invoke-RestMethod -Uri "$base/api/inventory/medicines" -Headers $adminAuth -Method GET
    $found = $list | Where-Object { $_.code -eq $testMedCode }
    if (-not $found) { throw "Created medicine not found in active formulary" }
}

$createdBatchId = $null
Test-Endpoint "Admin Receive Medicine Batch" {
    $payload = @{
        medicineId = $script:createdMedId
        batchNumber = "BATCH-" + (Get-Random -Minimum 1000 -Maximum 9999)
        expiryDate = (Get-Date).AddMonths(12).ToString("yyyy-MM-dd")
        purchasePrice = 4.25
        quantity = 200
        supplier = "ApexCare Verified Wholesaler Ltd"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/inventory/receipts" -Headers $adminAuth -Method POST -Body $payload -ContentType "application/json"
    $script:createdBatchId = $res.id
    if (-not $res.id) { throw "No batch ID returned" }
}

Test-Endpoint "Admin Stock Adjustment (Audit trail)" {
    $payload = @{
        batchId = $script:createdBatchId
        quantityChange = -10
        reason = "Damaged packaging identified during inventory audit"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/inventory/adjustments" -Headers $adminAuth -Method POST -Body $payload -ContentType "application/json"
    if ($res.quantity_on_hand -ne 190) { throw "Expected 190 remaining, got $($res.quantity_on_hand)" }
}

Test-Endpoint "Admin Delete/Archive Medicine from Formulary" {
    $res = Invoke-RestMethod -Uri "$base/api/inventory/medicines/$($script:createdMedId)" -Headers $adminAuth -Method DELETE
    if ($res.message -notmatch "archived successfully") { throw "Unexpected medicine archive response: $($res.message)" }
}

Test-Endpoint "Archived Medicine No Longer In Active Formulary" {
    $list = Invoke-RestMethod -Uri "$base/api/inventory/medicines" -Headers $adminAuth -Method GET
    $found = $list | Where-Object { $_.code -eq $testMedCode }
    if ($found) { throw "Medicine still returned after archival" }
}

# Clinical Automation Engine
Test-Endpoint "Admin Autonomous Engine Run-Now Trigger" {
    $res = Invoke-RestMethod -Uri "$base/api/automation/run-now" -Headers $adminAuth -Method POST
    if (-not $res.summary) { throw "Automation cycle did not return summary" }
}

Test-Endpoint "Admin Automation Status Telemetry" {
    $res = Invoke-RestMethod -Uri "$base/api/automation/status" -Headers $adminAuth -Method GET
    if ($res.engineStatus -ne "ACTIVE") { throw "Expected ACTIVE engineStatus, got $($res.engineStatus)" }
}

# 3. Role-Based Access Control (RBAC) Enforcement
$doctorAuth = Basic-Header "demo.doctor" "DemoStaffPass123!"
$receptionAuth = Basic-Header "demo.reception" "DemoStaffPass123!"

Test-Endpoint "Doctor Cannot Access Admin User Management (403 Forbidden)" {
    try {
        Invoke-RestMethod -Uri "$base/api/admin/users" -Headers $doctorAuth -Method GET
        throw "Should have thrown 403"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 403) { throw "Expected 403, got $code" }
    }
}

Test-Endpoint "Doctor Cannot Delete Patients (403 Forbidden)" {
    try {
        Invoke-RestMethod -Uri "$base/api/patients?id=1" -Headers $doctorAuth -Method DELETE
        throw "Should have thrown 403"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 403) { throw "Expected 403, got $code" }
    }
}

Test-Endpoint "Doctor Cannot Receive Pharmacy Stock Batches (403 Forbidden)" {
    try {
        $body = @{ medicineId = 1; batchNumber = "B-TEST"; expiryDate = "2027-01-01"; purchasePrice = 1.0; quantity = 10 } | ConvertTo-Json
        Invoke-RestMethod -Uri "$base/api/inventory/receipts" -Headers $doctorAuth -Method POST -Body $body -ContentType "application/json"
        throw "Should have thrown 403"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 403) { throw "Expected 403, got $code" }
    }
}

Test-Endpoint "Receptionist Can Register Patient" {
    $mrn = "MRN-RECEP-" + (Get-Random -Minimum 10000 -Maximum 99999)
    $payload = @{
        fullName = "Reception Patient $mrn"
        phone = "+91 97777 " + (Get-Random -Minimum 10000 -Maximum 99999)
        dateOfBirth = "1994-03-20"
        duplicateReviewAcknowledged = $true
        duplicateReviewReason = "Intake at reception counter"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$base/api/patients" -Headers $receptionAuth -Method POST -Body $payload -ContentType "application/json"
    if (-not $res.id) { throw "Reception patient intake failed" }
}

Test-Endpoint "Receptionist Cannot Delete Patients (403 Forbidden)" {
    try {
        Invoke-RestMethod -Uri "$base/api/patients?id=1" -Headers $receptionAuth -Method DELETE
        throw "Should have thrown 403"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 403) { throw "Expected 403, got $code" }
    }
}

Test-Endpoint "Unauthenticated Patient Portal IDOR Protection Without DOB" {
    try {
        Invoke-RestMethod -Uri "$base/api/patient/overview?mrn=MRN-00001" -Method GET
        throw "Should have rejected without DOB matching"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 400 -and $code -ne 403) { throw "Expected 400 or 403, got $code" }
    }
}

Write-Host "`n=== TEST SUMMARY ===" -ForegroundColor Cyan
$passCount = ($results | Where-Object { $_ -match "^PASS" }).Count
$failCount = ($results | Where-Object { $_ -match "^FAIL" }).Count
Write-Host "Total Tests: $($results.Count) | Passed: $passCount | Failed: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
if ($failCount -gt 0) { exit 1 }

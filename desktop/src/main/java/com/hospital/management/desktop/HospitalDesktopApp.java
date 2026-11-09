package com.hospital.management.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.util.concurrent.CompletableFuture;

public class HospitalDesktopApp extends Application {
    private final ApiClient api = new ApiClient();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final Label status = new Label("Connect to the clinic service to begin.");
    private final TextArea results = new TextArea();
    private String activeName = "";
    private String activeUsername = "";
    private String activeRole = "";
    private Stage primaryStage;

    @Override public void start(Stage stage) {
        primaryStage=stage;
        stage.setTitle("Clinic Desk");
        stage.setMinWidth(920); stage.setMinHeight(640);
        stage.setScene(new Scene(login(stage), 1000, 700));
        stage.show();
    }

    private VBox login(Stage stage) {
        TextField host = new TextField("http://localhost:8080");
        TextField username = new TextField(); username.setPromptText("Staff username");
        PasswordField password = new PasswordField(); password.setPromptText("Password");
        Label title = new Label("Clinic Desk"); title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        Label hint = new Label("Staff workspace · connect to your clinic service");
        Button connect = new Button("Sign in"); connect.setDefaultButton(true);
        Label message = new Label();
        connect.setOnAction(e -> { String serviceAddress=host.getText(),user=username.getText().trim(),secret=password.getText();connect.setDisable(true);message.setText("Connecting…");
            api.login(serviceAddress,user,secret).whenComplete((response,error)->Platform.runLater(()->{connect.setDisable(false);if(error!=null)message.setText(loginFailure(error));else{try{JsonNode profile=json.readTree(response);activeUsername=profile.path("username").asText(user);activeName=profile.path("displayName").asText(user);activeRole=profile.path("role").asText("");stage.getScene().setRoot(workspace());}catch(Exception parseError){message.setText("The service did not return a valid staff profile.");}}})); });
        VBox box = new VBox(14,title,hint,new Label("Service address"),host,new Label("Username"),username,new Label("Password"),password,connect,message);
        box.setMaxWidth(420); box.setPadding(new Insets(38)); box.setStyle("-fx-background-color: white; -fx-background-radius: 14;");
        StackPane root = new StackPane(box); root.setPadding(new Insets(24)); root.setStyle("-fx-background-color: #eef3f3;"); return new VBox(root);
    }

    private BorderPane workspace() {
        Label brand = new Label("Clinic Desk"); brand.setStyle("-fx-font-size: 21px; -fx-font-weight: bold;");
        Label account = new Label(activeName + " · " + activeRole);
        Button changePassword=new Button("Change password");changePassword.setOnAction(e->changePassword());
        Button signOut=new Button("Sign out");signOut.setOnAction(e->{api.clearCredentials();activeName="";activeUsername="";activeRole="";primaryStage.getScene().setRoot(login(primaryStage));});
        HBox header = new HBox(16,brand,new Region(),account,changePassword,signOut); HBox.setHgrow(header.getChildren().get(1),Priority.ALWAYS); header.setPadding(new Insets(18,24,18,24)); header.setStyle("-fx-background-color: white;");
        TabPane tabs = new TabPane();
        tabs.getTabs().add(tab("Overview", overview()));
        if (hasRole("ADMIN", "RECEPTIONIST", "BILLING")) tabs.getTabs().add(tab("Patients", patients()));
        if (hasRole("ADMIN", "RECEPTIONIST", "DOCTOR")) tabs.getTabs().add(tab("Appointments", appointments()));
        if (hasRole("ADMIN", "DOCTOR")) tabs.getTabs().add(tab("Consultations", consultations()));
        if (hasRole("ADMIN", "PHARMACIST")) tabs.getTabs().add(tab("Pharmacy", pharmacy()));
        if (hasRole("ADMIN", "BILLING")) tabs.getTabs().add(tab("Billing", billing()));
        if (hasRole("ADMIN")) tabs.getTabs().add(tab("Staff & doctors", staff()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        results.setEditable(false); results.setWrapText(true); results.setPrefRowCount(10); results.setStyle("-fx-font-size: 14px; -fx-control-inner-background: white; -fx-background-radius: 8;");
        VBox content = new VBox(12,tabs,status,results); content.setPadding(new Insets(20)); VBox.setVgrow(tabs,Priority.ALWAYS);
        BorderPane root = new BorderPane(content); root.setTop(header); root.setStyle("-fx-background-color: #f2f5f5;"); return root;
    }

    private Tab tab(String title, javafx.scene.Node content) { ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.setStyle("-fx-background-color: transparent;");return new Tab(title,scroll); }
    private boolean hasRole(String... allowed) { return java.util.Arrays.asList(allowed).contains(activeRole); }
    private String loginFailure(Throwable error) {
        Throwable cause=error;
        while(cause.getCause()!=null) cause=cause.getCause();
        String detail=cause.getMessage();
        if(detail!=null&&detail.contains("Use HTTPS")) return detail;
        return "Sign-in failed. Confirm the clinic address and ask an administrator to check your staff account and credentials.";
    }
    private void changePassword() {
        PasswordField next=new PasswordField();next.setPromptText("New password (12–72 UTF-8 bytes)");PasswordField confirm=new PasswordField();confirm.setPromptText("Confirm new password");
        GridPane fields=new GridPane();fields.setVgap(10);fields.setHgap(10);fields.addRow(0,new Label("New password"),next);fields.addRow(1,new Label("Confirm"),confirm);
        Dialog<ButtonType> dialog=new Dialog<>();dialog.initOwner(primaryStage);dialog.setTitle("Change password");dialog.setHeaderText("Choose a long password you do not use elsewhere.");dialog.getDialogPane().setContent(fields);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button->{if(!next.getText().equals(confirm.getText())||next.getText().length()<12){status.setText("Passwords must match and contain at least 12 characters.");return;}String value=next.getText();status.setText("Changing password…");api.put("/api/me/password",java.util.Map.of("newPassword",value)).whenComplete((result,error)->Platform.runLater(()->{if(error!=null){status.setText("Password could not be changed.");results.setText(friendlyError(error.getMessage()));}else{api.setCredentials(activeUsername,value);status.setText("Password changed. The Windows app is signed in with the new password.");show(result);}}));});
    }
    private VBox overview() {
        Label title = new Label("Today at the clinic"); title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        VBox page = new VBox(16,title,new Label("Quick access to the work available to your staff role."));
        HBox shortcuts = new HBox(10);
        if (hasRole("ADMIN", "RECEPTIONIST", "BILLING")) {
            Button patients = new Button("Find patients"); patients.setOnAction(e -> load("/api/patients", "Patient directory")); shortcuts.getChildren().add(patients);
        }
        if (hasRole("ADMIN", "RECEPTIONIST", "DOCTOR")) {
            Button appointments = new Button("View today's appointments"); appointments.setOnAction(e -> load("/api/appointments", "Today's appointments")); shortcuts.getChildren().add(appointments);
        }
        if (hasRole("ADMIN", "PHARMACIST")) {
            Button medicines = new Button("View medicine stock"); medicines.setOnAction(e -> load("/api/inventory/medicines", "Medicine inventory")); shortcuts.getChildren().add(medicines);
        }
        page.getChildren().add(shortcuts);
        return page;
    }
    private VBox patients() {
        VBox page = new VBox(12,new Label("Patient directory"));
        if (hasRole("ADMIN", "RECEPTIONIST")) {
            TextField name = new TextField(); name.setPromptText("Patient full name"); TextField phone = new TextField(); phone.setPromptText("Phone number (optional)"); DatePicker dob = new DatePicker(); CheckBox reviewedMatches=new CheckBox("I reviewed the possible matches and confirm this is a different person");reviewedMatches.setDisable(true);TextField reviewReason=new TextField();reviewReason.setPromptText("Reason if a possible match is a different person");Button checkMatches=new Button("Check for possible existing patients");
            Runnable clearReview=()->{reviewedMatches.setSelected(false);reviewedMatches.setDisable(true);};name.textProperty().addListener((o,old,value)->clearReview.run());phone.textProperty().addListener((o,old,value)->clearReview.run());dob.valueProperty().addListener((o,old,value)->clearReview.run());
            checkMatches.setOnAction(e->run(()->{var body=new java.util.LinkedHashMap<String,Object>();body.put("fullName",name.getText());body.put("phone",phone.getText());body.put("dateOfBirth",dob.getValue()==null?null:dob.getValue().toString());api.post("/api/patients/possible-matches",body).whenComplete((text,error)->Platform.runLater(()->{if(error!=null){status.setText("Possible matches could not be checked.");results.setText(friendlyError(error.getMessage()));return;}show(text);try{JsonNode candidates=json.readTree(text);reviewedMatches.setDisable(!candidates.isArray()||candidates.size()==0);}catch(Exception parseError){reviewedMatches.setDisable(true);}status.setText("Review any possible matches before registering.");}));}));
            Button save = new Button("Register patient");
            save.setOnAction(e -> run(() -> { var body = new java.util.LinkedHashMap<String,Object>(); body.put("fullName",name.getText()); if(dob.getValue()!=null)body.put("dateOfBirth",dob.getValue().toString()); body.put("phone",phone.getText());body.put("duplicateReviewAcknowledged",reviewedMatches.isSelected());body.put("duplicateReviewReason",reviewReason.getText()); show(api.post("/api/patients",body),"Patient registered.",()->{name.clear();phone.clear();reviewReason.clear();reviewedMatches.setSelected(false);reviewedMatches.setDisable(true);}); }));
            TextField patientId=new TextField();patientId.setPromptText("Patient record ID to correct");TextField correctedName=new TextField();correctedName.setPromptText("Correct full name");TextField correctedPhone=new TextField();correctedPhone.setPromptText("Correct phone (blank if none)");DatePicker correctedDob=new DatePicker();TextField correctionReason=new TextField();correctionReason.setPromptText("Reason for the correction");CheckBox correctionReviewed=new CheckBox("I reviewed the matches and confirm these are different people");correctionReviewed.setDisable(true);TextField duplicateCorrectionReason=new TextField();duplicateCorrectionReason.setPromptText("Reason if a possible match is a different person");Button checkCorrectionMatches=new Button("Check corrected details against other patients");
            Runnable clearCorrectionReview=()->{correctionReviewed.setSelected(false);correctionReviewed.setDisable(true);};correctedName.textProperty().addListener((o,old,value)->clearCorrectionReview.run());correctedPhone.textProperty().addListener((o,old,value)->clearCorrectionReview.run());correctedDob.valueProperty().addListener((o,old,value)->clearCorrectionReview.run());
            checkCorrectionMatches.setOnAction(e->run(()->{var body=new java.util.LinkedHashMap<String,Object>();body.put("fullName",correctedName.getText());body.put("phone",correctedPhone.getText());body.put("dateOfBirth",correctedDob.getValue()==null?null:correctedDob.getValue().toString());body.put("excludePatientId",Long.parseLong(patientId.getText().trim()));api.post("/api/patients/possible-matches",body).whenComplete((text,error)->Platform.runLater(()->{if(error!=null){status.setText("Possible matches could not be checked.");results.setText(friendlyError(error.getMessage()));return;}show(text);try{JsonNode candidates=json.readTree(text);correctionReviewed.setDisable(!candidates.isArray()||candidates.size()==0);}catch(Exception parseError){correctionReviewed.setDisable(true);}status.setText("Review any possible matches before saving the correction.");}));}));
            Button correct=new Button("Save demographic correction");
            correct.setOnAction(e->run(()->{var body=new java.util.LinkedHashMap<String,Object>();body.put("fullName",correctedName.getText());body.put("phone",correctedPhone.getText());body.put("reason",correctionReason.getText());body.put("duplicateReviewAcknowledged",correctionReviewed.isSelected());body.put("duplicateReviewReason",duplicateCorrectionReason.getText());if(correctedDob.getValue()!=null)body.put("dateOfBirth",correctedDob.getValue().toString());show(api.put("/api/patients/"+Long.parseLong(patientId.getText().trim()),body),"Patient details corrected.",()->{correctionReviewed.setSelected(false);correctionReviewed.setDisable(true);duplicateCorrectionReason.clear();});}));
            page.getChildren().addAll(new Label("Register a patient"),name,phone,new HBox(8,new Label("Date of birth"),dob),checkMatches,reviewedMatches,reviewReason,save,new Separator(),new Label("Correct an existing record · search first and enter all corrected details. A blank date or phone clears it."),patientId,correctedName,correctedPhone,new HBox(8,new Label("Correct date of birth"),correctedDob),checkCorrectionMatches,correctionReviewed,duplicateCorrectionReason,correctionReason,correct,new Separator());
        }
        Button refresh = new Button("Search patients"); TextField search = new TextField(); search.setPromptText("Name, patient number, phone or date of birth (YYYY-MM-DD)");
        refresh.setOnAction(e -> load("/api/patients?q="+java.net.URLEncoder.encode(search.getText(),java.nio.charset.StandardCharsets.UTF_8),"Patient directory"));
        page.getChildren().addAll(search,refresh);
        if (hasRole("ADMIN", "DOCTOR")) {
            TextField patientNumber=new TextField();patientNumber.setPromptText("Patient number for visit history");Button history=new Button("Open visit history");history.setOnAction(e->load("/api/patients/"+java.net.URLEncoder.encode(patientNumber.getText().trim(),java.nio.charset.StandardCharsets.UTF_8)+"/encounters","Patient visit history"));
            page.getChildren().addAll(new Separator(),patientNumber,history);
        }
        if (hasRole("ADMIN")) {
            TextField patientId=new TextField();patientId.setPromptText("Patient record ID for demographic history");Button history=new Button("View demographic corrections");history.setOnAction(e->run(()->load("/api/patients/"+Long.parseLong(patientId.getText().trim())+"/demographic-history","Demographic correction history")));
            Button duplicateHistory=new Button("View possible duplicate reviews");duplicateHistory.setOnAction(e->run(()->load("/api/patients/"+Long.parseLong(patientId.getText().trim())+"/duplicate-review","Possible duplicate review history")));
            page.getChildren().addAll(new Separator(),patientId,history,duplicateHistory);
        }
        return page;
    }
    private VBox appointments() {
        TextField patient = new TextField(); patient.setPromptText("Patient number"); TextField doctor = new TextField(); doctor.setPromptText("Doctor ID");
        DatePicker date = new DatePicker(java.time.LocalDate.now()); TextField time = new TextField("09:00"); TextField reason = new TextField(); reason.setPromptText("Visit reason (optional)");
        ComboBox<String> visitType=new ComboBox<>(FXCollections.observableArrayList("APPOINTMENT","WALK_IN"));visitType.getSelectionModel().selectFirst();
        Button book = new Button("Book appointment"); Button refresh = new Button("Load selected day"); TextField appointmentId=new TextField();appointmentId.setPromptText("Appointment ID");Button checkIn=new Button("Check in");checkIn.setOnAction(e->run(()->show(api.sendPatch("/api/appointments/"+Long.parseLong(appointmentId.getText())+"/status",java.util.Map.of("status","CHECKED_IN","reason","")))));
        TextField statusReason=new TextField();statusReason.setPromptText("Reason required for cancellation or no-show");
        Button cancel=new Button("Cancel appointment");cancel.setOnAction(e->{Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Cancel this appointment? The reason will be kept in its status history.",ButtonType.CANCEL,ButtonType.OK);confirm.setHeaderText("Confirm cancellation");if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK)run(()->show(api.sendPatch("/api/appointments/"+Long.parseLong(appointmentId.getText())+"/status",java.util.Map.of("status","CANCELLED","reason",statusReason.getText().trim()))));});
        Button noShow=new Button("Mark no-show");noShow.setOnAction(e->{Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Confirm the scheduled visit has ended and the patient did not attend.",ButtonType.CANCEL,ButtonType.OK);confirm.setHeaderText("Confirm no-show");if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK)run(()->show(api.sendPatch("/api/appointments/"+Long.parseLong(appointmentId.getText())+"/status",java.util.Map.of("status","NO_SHOW","reason",statusReason.getText().trim()))));});
        Button statusHistory=new Button("View appointment status history");statusHistory.setOnAction(e->run(()->load("/api/appointments/"+Long.parseLong(appointmentId.getText())+"/status-history","Appointment status history")));
        book.setOnAction(e -> run(() -> { java.time.OffsetDateTime starts=java.time.LocalDateTime.of(date.getValue(),java.time.LocalTime.parse(time.getText())).atZone(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
            show(api.post("/api/appointments",java.util.Map.of("patientNumber",patient.getText(),"doctorId",Long.parseLong(doctor.getText()),"startsAt",starts.toString(),"visitType",visitType.getValue(),"reason",reason.getText())),"Appointment booked.",()->{}); }));
        refresh.setOnAction(e -> load("/api/appointments?date="+date.getValue(),"Appointments for "+date.getValue()));
        DatePicker newDate=new DatePicker(java.time.LocalDate.now().plusDays(1));TextField newTime=new TextField("09:00");Button reschedule=new Button("Reschedule booked appointment");reschedule.setOnAction(e->run(()->{var start=java.time.LocalDateTime.of(newDate.getValue(),java.time.LocalTime.parse(newTime.getText())).atZone(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime();show(api.put("/api/appointments/"+Long.parseLong(appointmentId.getText())+"/reschedule",java.util.Map.of("startsAt",start.toString())));}));
        return new VBox(12,new Label("Appointment desk"),patient,doctor,new HBox(8,date,time),reason,visitType,new HBox(8,book,refresh),appointmentId,new HBox(8,checkIn,cancel,noShow),statusReason,statusHistory,new HBox(8,newDate,newTime,reschedule),new Label("No-show can be recorded after the scheduled end time. Status changes are attributed and kept in the visit history."));
    }
    private VBox pharmacy() {
        TextField code=new TextField();code.setPromptText("Medicine code");TextField name=new TextField();name.setPromptText("Generic name");TextField strength=new TextField();strength.setPromptText("Strength (optional)");TextField unit=new TextField("tablet");unit.setPromptText("Stock unit");TextField price=new TextField("0.00");price.setPromptText("Sale price in INR");TextField reorder=new TextField("0");reorder.setPromptText("Reorder alert quantity");
        Button add=new Button("Add medicine");add.setOnAction(e->run(()->show(api.post("/api/inventory/medicines",java.util.Map.of("code",code.getText(),"genericName",name.getText(),"brandName","","strength",strength.getText(),"dosageForm","","unit",unit.getText(),"reorderLevel",Integer.parseInt(reorder.getText()),"salePrice",new java.math.BigDecimal(price.getText()))))));
        TextField medicineId=new TextField();medicineId.setPromptText("Medicine ID");TextField batch=new TextField();batch.setPromptText("Batch number");TextField expiry=new TextField();expiry.setPromptText("Expiry date YYYY-MM-DD");TextField quantity=new TextField();quantity.setPromptText("Quantity received");TextField purchase=new TextField("0.00");purchase.setPromptText("Purchase price per unit");
        Button receive=new Button("Receive batch");receive.setOnAction(e->run(()->show(api.post("/api/inventory/receipts",java.util.Map.of("medicineId",Long.parseLong(medicineId.getText()),"batchNumber",batch.getText(),"expiryDate",expiry.getText(),"supplier","","purchasePrice",new java.math.BigDecimal(purchase.getText()),"quantity",Integer.parseInt(quantity.getText()))))));
        TextField returnBatch=new TextField();returnBatch.setPromptText("Batch ID to return to supplier");TextField returnQuantity=new TextField();returnQuantity.setPromptText("Quantity to return");TextField returnReason=new TextField();returnReason.setPromptText("Supplier return reason");Button returnStock=new Button("Return stock to supplier");returnStock.setOnAction(e->run(()->show(api.post("/api/inventory/supplier-returns",java.util.Map.of("batchId",Long.parseLong(returnBatch.getText()),"quantity",Integer.parseInt(returnQuantity.getText()),"reason",returnReason.getText())))));
        Button refresh=new Button("Refresh stock");refresh.setOnAction(e->load("/api/inventory/medicines","Medicine inventory"));Button alerts=new Button("Stock alerts");alerts.setOnAction(e->load("/api/inventory/alerts","Reorder and expiry alerts"));Button movements=new Button("Stock movement history");movements.setOnAction(e->load("/api/inventory/movements","Stock movement history"));
        TextField prescriptionEncounter=new TextField();prescriptionEncounter.setPromptText("Encounter ID to find its prescription");Button findRx=new Button("Show prescription items");findRx.setOnAction(e->load("/api/encounters/"+Long.parseLong(prescriptionEncounter.getText())+"/prescriptions","Prescription items"));
        TextField prescriptionItem=new TextField();prescriptionItem.setPromptText("Prescription item ID");TextField dispenseQty=new TextField();dispenseQty.setPromptText("Quantity to dispense");Button dispense=new Button("Dispense by earliest expiry");dispense.setOnAction(e->run(()->show(api.post("/api/prescription-items/"+Long.parseLong(prescriptionItem.getText())+"/dispense",java.util.Map.of("quantity",Integer.parseInt(dispenseQty.getText()))))));
        Button substitutionHistory=new Button("Review substitution approval history");substitutionHistory.setOnAction(e->run(()->load("/api/prescription-items/"+Long.parseLong(prescriptionItem.getText())+"/substitution-history","Prescription substitution history")));
        return new VBox(10,new Label("Pharmacy stock"),new Label("Medicine catalog · INR sale price per stock unit"),code,name,strength,unit,price,reorder,add,new Separator(),new Label("Receive a supplier batch"),medicineId,batch,expiry,quantity,purchase,receive,new Separator(),new Label("Return clinic stock to the supplier · patient-returned medicines must never be restocked"),returnBatch,returnQuantity,returnReason,returnStock,new Separator(),new Label("Dispense only after the doctor finalizes the visit. Complete dispensing before billing; invoicing locks further dispensing."),prescriptionEncounter,findRx,prescriptionItem,dispenseQty,new HBox(8,dispense,substitutionHistory),new HBox(8,refresh,alerts,movements));
    }
    private VBox consultations() {
        TextField appointment=new TextField();appointment.setPromptText("Checked-in appointment ID");TextField complaint=new TextField();complaint.setPromptText("Presenting complaint");TextField diagnosis=new TextField();diagnosis.setPromptText("Diagnosis");TextArea notes=new TextArea();notes.setPromptText("Clinical notes");notes.setPrefRowCount(4);TextField bp=new TextField();bp.setPromptText("Blood pressure");TextField pulse=new TextField();pulse.setPromptText("Pulse per minute");TextField temperature=new TextField();temperature.setPromptText("Temperature °C");TextField weight=new TextField();weight.setPromptText("Weight kg");TextField allergies=new TextField();allergies.setPromptText("Allergies");TextField currentMeds=new TextField();currentMeds.setPromptText("Current medicines");
        Button start=new Button("Start consultation");start.setOnAction(e->run(()->{var body=new java.util.LinkedHashMap<String,Object>();body.put("appointmentId",Long.parseLong(appointment.getText()));body.put("complaint",complaint.getText());body.put("diagnosisText",diagnosis.getText());body.put("clinicalNotes",notes.getText());body.put("bloodPressure",bp.getText());if(!pulse.getText().isBlank())body.put("pulsePerMinute",Integer.parseInt(pulse.getText()));if(!temperature.getText().isBlank())body.put("temperatureCelsius",new java.math.BigDecimal(temperature.getText()));if(!weight.getText().isBlank())body.put("weightKg",new java.math.BigDecimal(weight.getText()));body.put("allergies",allergies.getText());body.put("currentMedicines",currentMeds.getText());show(api.post("/api/encounters",body));}));
        TextField encounterId=new TextField();encounterId.setPromptText("Encounter ID");boolean[] recordLoaded={false};String[] loadedEncounterId={""};Button record=new Button("Load medical record");record.setOnAction(e->run(()->api.get("/api/encounters/"+Long.parseLong(encounterId.getText().trim())).whenComplete((text,error)->Platform.runLater(()->{if(error!=null){status.setText("The encounter could not be loaded.");results.setText(friendlyError(error.getMessage()));}else try{JsonNode current=json.readTree(text);complaint.setText(current.path("complaint").asText(""));diagnosis.setText(current.path("diagnosisText").asText(""));bp.setText(current.path("bloodPressure").asText(""));pulse.setText(current.path("pulsePerMinute").asText(""));temperature.setText(current.path("temperatureCelsius").asText(""));weight.setText(current.path("weightKg").asText(""));allergies.setText(current.path("allergies").asText(""));currentMeds.setText(current.path("currentMedicines").asText(""));notes.setText(current.path("clinicalNotes").asText(""));recordLoaded[0]=true;loadedEncounterId[0]=encounterId.getText().trim();show(text);status.setText("Record loaded. Review the current values before saving changes.");}catch(Exception parseError){status.setText("The encounter response could not be read.");}}))));
        TextField historyPatient=new TextField();historyPatient.setPromptText("Patient number for visit history");Button patientHistory=new Button("Review patient visit history");patientHistory.setOnAction(e->run(()->load("/api/patients/"+java.net.URLEncoder.encode(historyPatient.getText().trim(),java.nio.charset.StandardCharsets.UTF_8)+"/encounters","Patient visit history")));
        Button saveOpen=new Button("Save changes to open encounter");saveOpen.setOnAction(e->run(()->{if(!recordLoaded[0]||!loadedEncounterId[0].equals(encounterId.getText().trim())){status.setText("Load the selected encounter first so existing values are preserved.");return;}var body=new java.util.LinkedHashMap<String,Object>();body.put("complaint",complaint.getText());body.put("diagnosisText",diagnosis.getText());body.put("bloodPressure",bp.getText());body.put("allergies",allergies.getText());body.put("currentMedicines",currentMeds.getText());body.put("clinicalNotes",notes.getText());body.put("pulsePerMinute",pulse.getText().isBlank()?null:Integer.parseInt(pulse.getText()));body.put("temperatureCelsius",temperature.getText().isBlank()?null:new java.math.BigDecimal(temperature.getText()));body.put("weightKg",weight.getText().isBlank()?null:new java.math.BigDecimal(weight.getText()));show(api.put("/api/encounters/"+Long.parseLong(encounterId.getText().trim()),body),"Open encounter saved.",()->{});}));
        Button finalize=new Button("Finalize encounter");finalize.setOnAction(e->run(()->show(api.post("/api/encounters/"+Long.parseLong(encounterId.getText())+"/finalize",java.util.Map.of()),"Record finalized and appointment completed.",()->{})));
        TextField medId=new TextField();medId.setPromptText("Medicine ID");TextField dosage=new TextField();dosage.setPromptText("Dose");TextField frequency=new TextField();frequency.setPromptText("Frequency");TextField qty=new TextField();qty.setPromptText("Quantity");Button prescribe=new Button("Add prescription");prescribe.setOnAction(e->run(()->show(api.post("/api/prescriptions",java.util.Map.of("encounterId",Long.parseLong(encounterId.getText()),"items",java.util.List.of(java.util.Map.of("medicineId",Long.parseLong(medId.getText()),"dosage",dosage.getText(),"route","ORAL","frequency",frequency.getText(),"quantity",Integer.parseInt(qty.getText()),"instructions","")))))));
        TextField substitutionItem=new TextField();substitutionItem.setPromptText("Prescription item ID");TextField substituteMedicine=new TextField();substituteMedicine.setPromptText("Replacement medicine ID");TextField substitutionReason=new TextField();substitutionReason.setPromptText("Clinical reason for substitution");Button approveSubstitution=new Button("Approve medicine substitution");approveSubstitution.setOnAction(e->run(()->show(api.post("/api/prescription-items/"+Long.parseLong(substitutionItem.getText())+"/substitution-approvals",java.util.Map.of("substituteMedicineId",Long.parseLong(substituteMedicine.getText()),"reason",substitutionReason.getText())))));
        Button addenda=new Button("Open corrections/addenda");addenda.setOnAction(e->run(()->load("/api/encounters/"+Long.parseLong(encounterId.getText())+"/addenda","Record addenda")));
        TextField correctionReason=new TextField();correctionReason.setPromptText("Reason for addendum");TextArea correctionText=new TextArea();correctionText.setPromptText("Append a correction to the finalized medical record");correctionText.setPrefRowCount(3);Button appendAddendum=new Button("Append correction");appendAddendum.setOnAction(e->run(()->show(api.post("/api/encounters/"+Long.parseLong(encounterId.getText())+"/addenda",java.util.Map.of("correctionReason",correctionReason.getText(),"addendumText",correctionText.getText())),"Correction appended to the finalized record.",()->{})));
        return new VBox(10,new Label("Consultation"),new Label("Start only after the appointment is checked in."),appointment,complaint,diagnosis,new HBox(8,bp,pulse),new HBox(8,temperature,weight),allergies,currentMeds,notes,start,new Separator(),encounterId,new HBox(8,record,saveOpen),new HBox(8,finalize,addenda),historyPatient,patientHistory,new Separator(),new Label("Prescription"),medId,dosage,frequency,qty,prescribe,new Label("Substitution requires clinician approval before dispensing; record the clinical reason."),substitutionItem,substituteMedicine,substitutionReason,approveSubstitution,new Separator(),new Label("Corrections to a finalized record"),correctionReason,correctionText,appendAddendum);
    }
    private VBox billing() {
        TextField serviceCode=new TextField();serviceCode.setPromptText("Service code");TextField serviceName=new TextField();serviceName.setPromptText("Service description");TextField servicePrice=new TextField("0.00");servicePrice.setPromptText("Price in INR");Button addService=new Button("Add billable service");
        addService.setOnAction(e->run(()->show(api.post("/api/billing/services",java.util.Map.of("code",serviceCode.getText(),"description",serviceName.getText(),"unitPrice",new java.math.BigDecimal(servicePrice.getText()))))));
        TextField encounter=new TextField();encounter.setPromptText("Finalized encounter ID");TextField serviceIds=new TextField();serviceIds.setPromptText("Service IDs, comma separated (blank if none)");Button invoice=new Button("Create invoice");invoice.setOnAction(e->run(()->{java.util.List<Long> ids=serviceIds.getText().isBlank()?java.util.List.of():java.util.Arrays.stream(serviceIds.getText().split(",")).map(String::trim).map(Long::parseLong).toList();show(api.post("/api/billing/invoices",java.util.Map.of("encounterId",Long.parseLong(encounter.getText()),"serviceIds",ids)));}));
        TextField invoiceId=new TextField();invoiceId.setPromptText("Invoice ID");Button invoiceDetails=new Button("View itemized invoice and receipts");invoiceDetails.setOnAction(e->run(()->load("/api/billing/invoices/"+Long.parseLong(invoiceId.getText().trim()),"Invoice details and receipts")));TextField amount=new TextField();amount.setPromptText("Amount in INR");ComboBox<String> method=new ComboBox<>(FXCollections.observableArrayList("CASH","UPI","CARD","BANK_TRANSFER","OTHER"));method.getSelectionModel().selectFirst();Button pay=new Button("Record payment");pay.setOnAction(e->run(()->show(api.post("/api/billing/invoices/"+Long.parseLong(invoiceId.getText())+"/payments",java.util.Map.of("amount",new java.math.BigDecimal(amount.getText()),"method",method.getValue())))));
        Button services=new Button("View billable services");services.setOnAction(e->load("/api/billing/services","Billable services"));Button invoices=new Button("Recent invoices");invoices.setOnAction(e->load("/api/billing/invoices","Recent invoices"));
        TextField paymentId=new TextField();paymentId.setPromptText("Payment ID to refund");TextField refundAmount=new TextField();refundAmount.setPromptText("Refund amount in INR");TextField refundReason=new TextField();refundReason.setPromptText("Refund reason");Button refund=new Button("Record refund");refund.setOnAction(e->run(()->show(api.post("/api/billing/payments/"+Long.parseLong(paymentId.getText())+"/refund",java.util.Map.of("amount",new java.math.BigDecimal(refundAmount.getText()),"reason",refundReason.getText())))));
        TextField voidReason=new TextField();voidReason.setPromptText("Reason to void the invoice");Button voidInvoice=new Button("Void invoice");voidInvoice.setOnAction(e->run(()->show(api.post("/api/billing/invoices/"+Long.parseLong(invoiceId.getText())+"/void",java.util.Map.of("reason",voidReason.getText())))));
        return new VBox(9,new Label("Billing · amounts in Indian rupees"),new Label("Dispense available medicines before invoicing. The invoice records dispensed quantities and prevents later dispensing on that visit."),serviceCode,serviceName,servicePrice,new HBox(8,addService,services),new Separator(),encounter,serviceIds,invoice,new Separator(),new HBox(8,invoiceId,invoiceDetails),amount,method,pay,new HBox(8,invoices),new Separator(),new Label("Refunds and voids require a recorded reason"),paymentId,refundAmount,refundReason,refund,voidReason,voidInvoice);
    }
    private VBox staff() {
        TextField username=new TextField();username.setPromptText("Username");TextField display=new TextField();display.setPromptText("Staff display name");PasswordField password=new PasswordField();password.setPromptText("Temporary password (12+ characters)");ComboBox<String> role=new ComboBox<>(FXCollections.observableArrayList("RECEPTIONIST","DOCTOR","PHARMACIST","BILLING","ADMIN"));role.getSelectionModel().selectFirst();
        Button createStaff=new Button("Create staff account");createStaff.setOnAction(e->run(()->{var body=java.util.Map.of("username",username.getText(),"displayName",display.getText(),"password",password.getText(),"role",role.getValue());show(api.post("/api/admin/users",body));}));Button listStaff=new Button("List staff");listStaff.setOnAction(e->load("/api/admin/users","Staff accounts"));
        TextField staffId=new TextField();staffId.setPromptText("Staff account ID for doctor");TextField specialty=new TextField();specialty.setPromptText("Medical specialty");Button createDoctor=new Button("Add doctor profile");createDoctor.setOnAction(e->run(()->show(api.post("/api/doctors",java.util.Map.of("staffUserId",Long.parseLong(staffId.getText()),"specialty",specialty.getText(),"appointmentMinutes",20)))));Button listDoctors=new Button("List doctors");listDoctors.setOnAction(e->load("/api/doctors","Doctors"));
        TextField doctorId=new TextField();doctorId.setPromptText("Doctor ID");ComboBox<String> weekday=new ComboBox<>(FXCollections.observableArrayList("1 Monday","2 Tuesday","3 Wednesday","4 Thursday","5 Friday","6 Saturday","7 Sunday"));weekday.getSelectionModel().selectFirst();TextField from=new TextField("09:00");TextField to=new TextField("17:00");Button addHours=new Button("Add weekly clinic hours");addHours.setOnAction(e->run(()->show(api.post("/api/doctors/"+Long.parseLong(doctorId.getText())+"/schedule",java.util.Map.of("dayOfWeek",Integer.parseInt(weekday.getValue().substring(0,1)),"startsAt",from.getText()+":00","endsAt",to.getText()+":00")))));
        Button viewHours=new Button("View weekly clinic hours");viewHours.setOnAction(e->run(()->load("/api/doctors/"+Long.parseLong(doctorId.getText())+"/schedule","Doctor clinic hours")));
        TextField userId=new TextField();userId.setPromptText("Staff account ID");ComboBox<String> accountStatus=new ComboBox<>(FXCollections.observableArrayList("Active","Inactive"));accountStatus.getSelectionModel().selectFirst();Button setAccountStatus=new Button("Update staff access");setAccountStatus.setOnAction(e->run(()->show(api.sendPatch("/api/admin/users/"+Long.parseLong(userId.getText())+"/active",java.util.Map.of("active","Active".equals(accountStatus.getValue()))))));Button auditLog=new Button("Recent audit events");auditLog.setOnAction(e->load("/api/admin/audit","Recent audit events"));Button report=new Button("Today's summary");report.setOnAction(e->load("/api/admin/reports/daily","Daily summary"));
        TextField resetId=new TextField();resetId.setPromptText("Staff account ID for password reset");PasswordField temporaryPassword=new PasswordField();temporaryPassword.setPromptText("Temporary password (12+ characters)");Button reset=new Button("Reset staff password");reset.setOnAction(e->run(()->show(api.post("/api/admin/users/"+Long.parseLong(resetId.getText())+"/password",java.util.Map.of("newPassword",temporaryPassword.getText())))));
        TextField mergeSource=new TextField();mergeSource.setPromptText("Source patient record ID");TextField mergeTarget=new TextField();mergeTarget.setPromptText("Surviving patient record ID");TextField mergeReason=new TextField();mergeReason.setPromptText("Reason these records are the same person");Button mergePatients=new Button("Review and merge patient records");mergePatients.setOnAction(e->{Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Appointments, encounters and invoices will move to the surviving record. The source record will remain inactive with a reason in merge history.",ButtonType.CANCEL,ButtonType.OK);confirm.setHeaderText("Confirm patient record merge");if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK)run(()->show(api.post("/api/admin/patients/"+Long.parseLong(mergeSource.getText())+"/merge",java.util.Map.of("targetPatientId",Long.parseLong(mergeTarget.getText()),"reason",mergeReason.getText()))));});
        TextField mergeHistoryId=new TextField();mergeHistoryId.setPromptText("Patient record ID");Button mergeHistory=new Button("Review patient merge history");mergeHistory.setOnAction(e->run(()->load("/api/admin/patients/"+Long.parseLong(mergeHistoryId.getText())+"/merge-history","Patient merge history")));
        return new VBox(10,new Label("Staff accounts · administrator access"),username,display,password,role,new HBox(8,createStaff,listStaff),userId,new HBox(8,accountStatus,setAccountStatus,auditLog,report),resetId,temporaryPassword,reset,new Separator(),new Label("Doctor profiles"),staffId,specialty,new HBox(8,createDoctor,listDoctors),new Separator(),new Label("Weekly clinic hours · 24-hour clock"),doctorId,weekday,new HBox(8,from,to),new HBox(8,addHours,viewHours),new Separator(),new Label("Patient identity review · confirm both IDs and record a reason"),mergeSource,mergeTarget,mergeReason,mergePatients,mergeHistoryId,mergeHistory);
    }
    private void load(String path,String heading) { run(() -> { status.setText(heading); show(api.get(path)); }); }
    private void run(Runnable action) { try { action.run(); } catch(Exception e) { status.setText("Action could not be completed. Check your access and connection."); results.setText(friendlyError(e.getMessage())); } }
    private void show(String text) { try { JsonNode node=json.readTree(text); results.setText(formatResult(node)); } catch(Exception e) { results.setText(text); } }
    private void show(CompletableFuture<String> response) { complete(response,null,()->{}); }
    private void show(CompletableFuture<String> response,String success,Runnable afterSuccess) { complete(response,success,afterSuccess); }
    private void complete(CompletableFuture<String> response,String success,Runnable afterSuccess) { response.whenComplete((text,error)->Platform.runLater(()->{if(error!=null){status.setText("The request could not be completed. Check your access and connection.");results.setText(friendlyError(error.getMessage()));}else{show(text);if(success!=null)status.setText(success);afterSuccess.run();}})); }
    private String formatResult(JsonNode node) {
        if(node==null||node.isNull()) return "No records found.";
        if(node.isArray()) {
            if(node.isEmpty()) return "No matching records.";
            StringBuilder out=new StringBuilder();int number=1;
            for(JsonNode row:node) { if(number>100) {out.append("\nShowing the first 100 records.");break;} if(number>1)out.append("\n\n");out.append(number++).append(". ").append(formatObject(row)); }
            return out.toString();
        }
        if(node.isObject()) return formatObject(node);
        return node.asText();
    }
    private String formatObject(JsonNode node) {
        if(!node.isObject()) return node.asText();
        StringBuilder out=new StringBuilder();
        node.fields().forEachRemaining(entry->{JsonNode value=entry.getValue();if(value==null||value.isNull()||value.isEmpty())return;if(out.length()>0)out.append("  ·  ");out.append(friendlyLabel(entry.getKey())).append(": ").append(value.isContainerNode()?formatResult(value):value.asText());});
        return out.length()==0?"Record":out.toString();
    }
    private String friendlyLabel(String key) { return java.util.Arrays.stream(key.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' ').split("\\s+")).filter(part->!part.isBlank()).map(part->part.substring(0,1).toUpperCase(java.util.Locale.ROOT)+part.substring(1)).collect(java.util.stream.Collectors.joining(" ")); }
    private String friendlyError(String detail) {
        if(detail==null||detail.isBlank()) return "Please check the values and try again.";
        try { JsonNode error=json.readTree(detail).path("error"); if(!error.isMissingNode()) return error.asText(); } catch(Exception ignored) { }
        return "Please check the values and try again.";
    }
    public static void main(String[] args) { launch(args); }
}

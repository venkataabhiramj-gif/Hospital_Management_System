package com.hospital.management.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private BorderPane workspaceRoot;
    private VBox pageArea;
    private VBox navigation;
    private Timeline dashboardRefresh;

    @Override public void start(Stage stage) {
        primaryStage=stage;
        stage.setTitle("ApexCare ClinicDesk \u2014 Memorial Hospital Clinical System");
        stage.setMinWidth(920); stage.setMinHeight(640);
        Scene scene = new Scene(login(stage), 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/clinic.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private VBox login(Stage stage) {
        TextField host = new TextField("http://localhost:8088"); host.setPromptText("e.g. http://localhost:8088");
        TextField username = new TextField(); username.setPromptText("Staff username (e.g. admin or demo.doctor)");
        PasswordField password = new PasswordField(); password.setPromptText("Account password");
        Label title = new Label("ApexCare Memorial Hospital"); title.getStyleClass().add("login-title");
        Label hint = new Label("Clinical Workstation \u00B7 Facility 01"); hint.getStyleClass().add("muted");
        Button connect = new Button("Sign in to Clinical Desk"); connect.setDefaultButton(true); connect.getStyleClass().add("btn-primary");
        connect.setMaxWidth(Double.MAX_VALUE);
        Label message = new Label(); message.getStyleClass().add("login-error");

        connect.setOnAction(e -> {
            String serviceAddress = host.getText(), user = username.getText().trim(), secret = password.getText();
            connect.setDisable(true);
            message.setText("Authenticating with clinic server\u2026");
            api.login(serviceAddress, user, secret).whenComplete((response, error) -> Platform.runLater(() -> {
                connect.setDisable(false);
                if (error != null) {
                    message.setText(loginFailure(error));
                } else {
                    try {
                        JsonNode profile = json.readTree(response);
                        activeUsername = profile.path("username").asText(user);
                        activeName = profile.path("displayName").asText(user);
                        activeRole = profile.path("role").asText("STAFF");
                        status.setText("Connected to clinic server \u00B7 Session active.");
                        stage.getScene().setRoot(workspace());
                    } catch (Exception parseError) {
                        message.setText("The server did not return a valid staff profile.");
                    }
                }
            }));
        });

        // Quick 1-Click Demo Profiles Row
        Label demoLabel = new Label("1-Click Instant Demo Credentials:"); demoLabel.getStyleClass().add("demo-label");
        Button btnAdmin = new Button("Admin"); btnAdmin.getStyleClass().add("demo-btn-chip");
        btnAdmin.setOnAction(e -> { username.setText("admin"); password.setText("DemoAdminPass123!"); });

        Button btnDoctor = new Button("Dr. Menon"); btnDoctor.getStyleClass().add("demo-btn-chip");
        btnDoctor.setOnAction(e -> { username.setText("demo.doctor"); password.setText("DemoStaffPass123!"); });

        Button btnReception = new Button("Reception"); btnReception.getStyleClass().add("demo-btn-chip");
        btnReception.setOnAction(e -> { username.setText("demo.reception"); password.setText("DemoStaffPass123!"); });

        Button btnPharmacy = new Button("Pharmacy"); btnPharmacy.getStyleClass().add("demo-btn-chip");
        btnPharmacy.setOnAction(e -> { username.setText("demo.pharmacy"); password.setText("DemoStaffPass123!"); });

        HBox demoRow = new HBox(6, btnAdmin, btnDoctor, btnReception, btnPharmacy);
        demoRow.setAlignment(javafx.geometry.Pos.CENTER);

        VBox box = new VBox(12, title, hint, new Label("Server Address"), host, new Label("Staff Username"), username, new Label("Password"), password, connect, message, new Separator(), demoLabel, demoRow);
        box.getStyleClass().add("login-card"); box.setMaxWidth(460);
        StackPane root = new StackPane(box); root.getStyleClass().add("login-background"); return new VBox(root);
    }

    private BorderPane workspace() {
        VBox brandBox = new VBox(2);
        Label brand = new Label("ApexCare Memorial Hospital"); brand.getStyleClass().add("brand");
        Label subBrand = new Label("ClinicDesk Outpatient System \u00B7 Facility 01"); subBrand.getStyleClass().add("muted");
        brandBox.getChildren().addAll(brand, subBrand);
        Label account = new Label(activeName + "  \u00B7  " + activeRole.replace('_',' ')); account.getStyleClass().add("muted");
        Button changePassword=new Button("Change password");changePassword.setOnAction(e->changePassword());
        Button signOut=new Button("Sign out");signOut.setOnAction(e->{if(dashboardRefresh!=null)dashboardRefresh.stop();api.clearCredentials();activeName="";activeUsername="";activeRole="";primaryStage.getScene().setRoot(login(primaryStage));});
        HBox header = new HBox(16,brandBox,new Region(),account,changePassword,signOut); HBox.setHgrow(header.getChildren().get(1),Priority.ALWAYS); header.setPadding(new Insets(18,28,18,28)); header.getStyleClass().add("topbar");
        navigation = new VBox(7); navigation.setPadding(new Insets(22,14,18,14)); navigation.getStyleClass().add("sidebar"); navigation.setPrefWidth(218);
        Label navTitle = new Label("CLINIC WORKSPACE"); navTitle.getStyleClass().add("nav-caption"); navigation.getChildren().add(navTitle);
        pageArea = new VBox(18); pageArea.setPadding(new Insets(30,34,26,34)); pageArea.getStyleClass().add("page-area");
        ScrollPane pageScroll=new ScrollPane(pageArea);pageScroll.setFitToWidth(true);pageScroll.getStyleClass().add("page-scroll");
        BorderPane body=new BorderPane();body.setLeft(navigation);body.setCenter(pageScroll);body.getStyleClass().add("app-body");
        results.setEditable(false); results.setWrapText(true); results.setPrefRowCount(3); results.setMaxHeight(94); results.getStyleClass().add("result-box");
        status.getStyleClass().add("status-line");
        VBox feedback=new VBox(5,status,results);feedback.setPadding(new Insets(10,28,18,28));feedback.getStyleClass().add("feedback");
        workspaceRoot=new BorderPane();workspaceRoot.setTop(header);workspaceRoot.setCenter(body);workspaceRoot.setBottom(feedback);workspaceRoot.getStyleClass().add("app-root");
        addNavigation("Overview",()->overview());
        if (hasRole("ADMIN", "RECEPTIONIST", "BILLING")) addNavigation("Patients",()->patients());
        if (hasRole("ADMIN", "RECEPTIONIST", "DOCTOR")) addNavigation("Appointments",()->appointments());
        if (hasRole("ADMIN", "DOCTOR")) addNavigation("Consultations",()->consultations());
        if (hasRole("ADMIN", "PHARMACIST")) addNavigation("Pharmacy",()->pharmacy());
        if (hasRole("ADMIN", "BILLING")) addNavigation("Billing",()->billing());
        if (hasRole("ADMIN")) addNavigation("Staff & doctors",()->staff());
        navigate("Overview",overview());
        return workspaceRoot;
    }

    private void addNavigation(String title,java.util.function.Supplier<javafx.scene.Node> content) {
        Button item=new Button(title);item.getStyleClass().add("nav-button");item.setMaxWidth(Double.MAX_VALUE);item.setOnAction(e->navigate(title,content.get()));navigation.getChildren().add(item);
    }
    private void navigate(String title,javafx.scene.Node content) {
        if(pageArea==null)return;
        if(!"Overview".equals(title)&&dashboardRefresh!=null)dashboardRefresh.stop();
        Label heading=new Label(title);heading.getStyleClass().add("page-title");
        VBox surface=new VBox(18,heading,content);surface.getStyleClass().add("page-card");
        pageArea.getChildren().setAll(surface);VBox.setVgrow(surface,Priority.ALWAYS);
        navigation.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast).forEach(b->b.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),b.getText().equals(title)));
    }
    private boolean hasRole(String... allowed) { return java.util.Arrays.asList(allowed).contains(activeRole); }
    private String loginFailure(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String detail = cause.getMessage();
        if (detail != null && !detail.isBlank() && !detail.contains("Use HTTPS")) {
            return detail;
        }
        return "Sign-in failed. Please verify the server address, username, and password.";
    }
    private void changePassword() {
        PasswordField next=new PasswordField();next.setPromptText("New password (12-72 UTF-8 bytes)");PasswordField confirm=new PasswordField();confirm.setPromptText("Confirm new password");
        GridPane fields=new GridPane();fields.setVgap(10);fields.setHgap(10);fields.addRow(0,new Label("New password"),next);fields.addRow(1,new Label("Confirm"),confirm);
        Dialog<ButtonType> dialog=new Dialog<>();dialog.initOwner(primaryStage);dialog.setTitle("Change password");dialog.setHeaderText("Choose a long password you do not use elsewhere.");dialog.getDialogPane().setContent(fields);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button->{if(!next.getText().equals(confirm.getText())||next.getText().length()<12){status.setText("Passwords must match and contain at least 12 characters.");return;}String value=next.getText();status.setText("Changing password\u2026");api.put("/api/me/password",java.util.Map.of("newPassword",value)).whenComplete((result,error)->Platform.runLater(()->{if(error!=null){status.setText("Password could not be changed.");results.setText(friendlyError(error.getMessage()));}else{api.setCredentials(activeUsername,value);status.setText("Password changed. The Windows app is signed in with the new password.");show(result);}}));});
    }
    private VBox overview() {
        Label greeting = new Label("Good day, " + activeName); greeting.getStyleClass().add("section-title");
        Label note = new Label("Today's appointments refresh every minute. Stock warnings flag low quantities and medicines nearing expiry."); note.getStyleClass().add("muted");
        VBox page = new VBox(18,greeting,note);
        HBox shortcuts = new HBox(10); shortcuts.getStyleClass().add("action-row");
        if (hasRole("ADMIN", "RECEPTIONIST", "BILLING")) {
            Button patients = new Button("Find patients"); patients.getStyleClass().add("secondary-button"); patients.setOnAction(e -> navigate("Patients",patients())); shortcuts.getChildren().add(patients);
        }
        if (hasRole("ADMIN", "RECEPTIONIST", "DOCTOR")) {
            Button appointments = new Button("Open appointment desk"); appointments.setOnAction(e -> navigate("Appointments",appointments())); shortcuts.getChildren().add(appointments);
        }
        if (hasRole("ADMIN", "PHARMACIST")) {
            Button medicines = new Button("Review medicine stock"); medicines.getStyleClass().add("secondary-button"); medicines.setOnAction(e -> navigate("Pharmacy",pharmacy())); shortcuts.getChildren().add(medicines);
        }
        page.getChildren().add(shortcuts);
        GridPane metrics=new GridPane();metrics.setHgap(14);metrics.setVgap(14);metrics.getStyleClass().add("metrics-grid");
        Label scheduled=valueLabel("\u2014"),checkedIn=valueLabel("\u2014"),completed=valueLabel("\u2014"),stockAlerts=valueLabel("\u2014"),collections=valueLabel("\u2014");
        metrics.add(metric("Appointments today",scheduled,"Scheduled visits"),0,0);metrics.add(metric("Checked in",checkedIn,"Patients in the clinic"),1,0);metrics.add(metric("Completed visits",completed,"Consultations finished"),2,0);
        if(hasRole("ADMIN","PHARMACIST"))metrics.add(metric("Stock alerts",stockAlerts,"Low stock or near expiry"),0,1);
        if(hasRole("ADMIN"))metrics.add(metric("Collected today",collections,"Net payments \u00B7 INR"),1,1);
        for(javafx.scene.Node child:metrics.getChildren())if(child instanceof VBox)GridPane.setHgrow(child,Priority.ALWAYS);
        Label scheduleTitle=new Label("Today's appointments");scheduleTitle.getStyleClass().add("section-title");
        VBox appointmentRows=new VBox(8);appointmentRows.getStyleClass().add("appointment-list");
        Button refresh=new Button("Refresh dashboard");refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e->refreshOverview(scheduled,checkedIn,completed,stockAlerts,collections,appointmentRows));
        page.getChildren().addAll(metrics,new HBox(12,scheduleTitle,new Region(),refresh));HBox titleRow=(HBox)page.getChildren().get(page.getChildren().size()-1);HBox.setHgrow(titleRow.getChildren().get(1),Priority.ALWAYS);
        page.getChildren().add(appointmentRows);
        refreshOverview(scheduled,checkedIn,completed,stockAlerts,collections,appointmentRows);
        if(dashboardRefresh!=null)dashboardRefresh.stop();
        dashboardRefresh=new Timeline(new KeyFrame(Duration.minutes(1),e->refreshOverview(scheduled,checkedIn,completed,stockAlerts,collections,appointmentRows)));
        dashboardRefresh.setCycleCount(Timeline.INDEFINITE);dashboardRefresh.play();
        return page;
    }
    private Label valueLabel(String value){Label label=new Label(value);label.getStyleClass().add("metric-value");return label;}
    private VBox metric(String title,Label value,String description){Label heading=new Label(title);heading.getStyleClass().add("metric-heading");Label note=new Label(description);note.getStyleClass().add("muted");VBox card=new VBox(7,heading,value,note);card.getStyleClass().add("metric-card");return card;}
    private VBox section(String title,String description){Label heading=new Label(title);heading.getStyleClass().add("section-title");Label note=new Label(description);note.getStyleClass().add("muted");VBox card=new VBox(13,heading,note);card.getStyleClass().add("form-section");return card;}
    private VBox labelled(String title,javafx.scene.Node control){Label label=new Label(title);label.getStyleClass().add("field-label");VBox field=new VBox(6,label,control);field.getStyleClass().add("labelled-field");return field;}
    private GridPane formGrid(javafx.scene.Node... fields){GridPane grid=new GridPane();grid.setHgap(16);grid.setVgap(14);for(int i=0;i<2;i++){ColumnConstraints column=new ColumnConstraints();column.setPercentWidth(50);column.setHgrow(Priority.ALWAYS);grid.getColumnConstraints().add(column);}for(int i=0;i<fields.length;i++){int col=i%2,row=i/2;grid.add(fields[i],col,row);GridPane.setHgrow(fields[i],Priority.ALWAYS);if(fields[i] instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}return grid;}
    private void refreshOverview(Label scheduled,Label checkedIn,Label completed,Label stockAlerts,Label collections,VBox appointmentRows){
        String today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
        if(hasRole("ADMIN","RECEPTIONIST","DOCTOR"))api.get("/api/appointments?date="+today).whenComplete((text,error)->Platform.runLater(()->{
            if(error!=null){scheduled.setText("\u2014");appointmentRows.getChildren().setAll(new Label("Appointments are unavailable. Check the clinic connection."));return;}
            try{JsonNode rows=json.readTree(text);int waiting=0,done=0;appointmentRows.getChildren().clear();
                for(JsonNode row:rows){String statusText=row.path("status").asText("UNKNOWN");if("CHECKED_IN".equals(statusText))waiting++;if("COMPLETED".equals(statusText))done++;
                    HBox line=new HBox(14);line.getStyleClass().add("appointment-row");Label time=new Label(row.path("starts_at").asText("").replace('T',' '));time.getStyleClass().add("appointment-time");Label patient=new Label(row.path("patient_name").asText("Patient"));patient.getStyleClass().add("appointment-patient");Label doctor=new Label(row.path("doctor_name").asText(""));doctor.getStyleClass().add("muted");Label state=new Label(statusText.replace('_',' '));state.getStyleClass().add("status-pill");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);line.getChildren().addAll(time,patient,doctor,spacer,state);appointmentRows.getChildren().add(line);
                }
                scheduled.setText(Integer.toString(rows.size()));checkedIn.setText(Integer.toString(waiting));completed.setText(Integer.toString(done));
                if(rows.isEmpty())appointmentRows.getChildren().add(new Label("No appointments are booked for today."));
            }catch(Exception parseError){appointmentRows.getChildren().setAll(new Label("Today's appointment list could not be displayed."));}
        }));
        if(hasRole("ADMIN"))api.get("/api/admin/reports/daily").whenComplete((text,error)->Platform.runLater(()->{if(error==null)try{JsonNode report=json.readTree(text);completed.setText(report.path("appointments").path("completed").asText(completed.getText()));collections.setText("\u20B9"+report.path("netPaymentsReceived").asText("0.00"));}catch(Exception ignored){}}));
        if(hasRole("ADMIN","PHARMACIST"))api.get("/api/inventory/alerts").whenComplete((text,error)->Platform.runLater(()->{if(error==null)try{JsonNode alerts=json.readTree(text);stockAlerts.setText(Integer.toString(alerts.path("lowStock").size()+alerts.path("expiringWithin30Days").size()));}catch(Exception ignored){}}));
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
            VBox registration=section("Register a patient","Search for possible duplicates before saving a new record.");
            registration.getChildren().addAll(formGrid(labelled("Full name",name),labelled("Mobile number",phone),labelled("Date of birth",dob)),checkMatches,reviewedMatches,reviewReason,save);
            VBox correction=section("Correct patient details","Search first and enter the complete corrected details. A blank date or phone clears it.");
            correction.getChildren().addAll(formGrid(labelled("Patient record ID",patientId),labelled("Correct full name",correctedName),labelled("Correct mobile number",correctedPhone),labelled("Correct date of birth",correctedDob)),checkCorrectionMatches,correctionReviewed,duplicateCorrectionReason,correctionReason,correct);
            page.getChildren().addAll(registration,correction);
        }
        Button refresh = new Button("Search patients"); TextField search = new TextField(); search.setPromptText("Name, patient number, phone or date of birth (YYYY-MM-DD)");
        refresh.setOnAction(e -> load("/api/patients?q="+java.net.URLEncoder.encode(search.getText(),java.nio.charset.StandardCharsets.UTF_8),"Patient directory"));
        VBox searchCard=section("Find an existing patient","Search by name, patient number, phone number, or date of birth.");searchCard.getChildren().addAll(formGrid(labelled("Patient search",search)),refresh);page.getChildren().add(searchCard);
        if (hasRole("ADMIN", "DOCTOR")) {
            TextField patientNumber=new TextField();patientNumber.setPromptText("Patient number for visit history");Button history=new Button("Open visit history");history.setOnAction(e->load("/api/patients/"+java.net.URLEncoder.encode(patientNumber.getText().trim(),java.nio.charset.StandardCharsets.UTF_8)+"/encounters","Patient visit history"));
            VBox visitHistory=section("Visit history","Doctors and administrators can review a patient's prior encounters.");visitHistory.getChildren().addAll(formGrid(labelled("Patient number",patientNumber)),history);page.getChildren().add(visitHistory);
        }
        if (hasRole("ADMIN")) {
            TextField patientId=new TextField();patientId.setPromptText("Patient record ID for demographic history");Button history=new Button("View demographic corrections");history.setOnAction(e->run(()->load("/api/patients/"+Long.parseLong(patientId.getText().trim())+"/demographic-history","Demographic correction history")));
            Button duplicateHistory=new Button("View possible duplicate reviews");duplicateHistory.setOnAction(e->run(()->load("/api/patients/"+Long.parseLong(patientId.getText().trim())+"/duplicate-review","Possible duplicate review history")));
            VBox reviewHistory=section("Record review history","Review demographic corrections and duplicate identity decisions.");reviewHistory.getChildren().addAll(formGrid(labelled("Patient record ID",patientId)),new HBox(10,history,duplicateHistory));page.getChildren().add(reviewHistory);
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
        VBox booking=section("Book an outpatient visit","Choose an existing patient, a doctor and an open clinic time.");
        booking.getChildren().addAll(formGrid(labelled("Patient number",patient),labelled("Doctor ID",doctor),labelled("Visit date",date),labelled("Start time \u00B7 24-hour clock",time),labelled("Visit type",visitType)),labelled("Visit reason \u00B7 optional",reason),new HBox(10,book,refresh));
        VBox manage=section("Manage a booked visit","Check in arrivals, review status history, or record a reason when cancelling or marking no-show.");
        manage.getChildren().addAll(formGrid(labelled("Appointment ID",appointmentId),labelled("Status reason",statusReason)),new HBox(10,checkIn,cancel,noShow,statusHistory),formGrid(labelled("New date",newDate),labelled("New start time",newTime)),reschedule,new Label("No-show can only be recorded after the scheduled visit has ended."));
        return new VBox(16,booking,manage);
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
        VBox catalogue=section("Medicine catalogue","Use generic names and define the reorder level for each stock unit.");catalogue.getChildren().addAll(formGrid(labelled("Medicine code",code),labelled("Generic name",name),labelled("Strength",strength),labelled("Stock unit",unit),labelled("Sale price \u00B7 INR",price),labelled("Reorder alert quantity",reorder)),new HBox(10,add,refresh,alerts,movements));
        VBox receiving=section("Receive a supplier batch","Stock is tracked by batch and expiry date.");receiving.getChildren().addAll(formGrid(labelled("Medicine ID",medicineId),labelled("Batch number",batch),labelled("Expiry date \u00B7 YYYY-MM-DD",expiry),labelled("Quantity received",quantity),labelled("Purchase price per unit \u00B7 INR",purchase)),receive);
        VBox returns=section("Supplier returns","Returned stock is removed from available clinic inventory.");returns.getChildren().addAll(formGrid(labelled("Batch ID",returnBatch),labelled("Quantity to return",returnQuantity),labelled("Reason",returnReason)),returnStock,new Label("Patient-returned medicines must never be restocked."));
        VBox dispensing=section("Prescriptions and dispensing","Dispense only after the doctor finalizes the visit, and finish before billing.");dispensing.getChildren().addAll(formGrid(labelled("Encounter ID",prescriptionEncounter),labelled("Prescription item ID",prescriptionItem),labelled("Quantity to dispense",dispenseQty)),new HBox(10,findRx,dispense,substitutionHistory));
        return new VBox(16,catalogue,receiving,returns,dispensing);
    }
    private VBox consultations() {
        TextField appointment=new TextField();appointment.setPromptText("Checked-in appointment ID");TextField complaint=new TextField();complaint.setPromptText("Presenting complaint");TextField diagnosis=new TextField();diagnosis.setPromptText("Diagnosis");TextArea notes=new TextArea();notes.setPromptText("Clinical notes");notes.setPrefRowCount(4);TextField bp=new TextField();bp.setPromptText("Blood pressure");TextField pulse=new TextField();pulse.setPromptText("Pulse per minute");TextField temperature=new TextField();temperature.setPromptText("Temperature \u00B0C");TextField weight=new TextField();weight.setPromptText("Weight kg");TextField allergies=new TextField();allergies.setPromptText("Allergies");TextField currentMeds=new TextField();currentMeds.setPromptText("Current medicines");
        Button start=new Button("Start consultation");start.setOnAction(e->run(()->{var body=new java.util.LinkedHashMap<String,Object>();body.put("appointmentId",Long.parseLong(appointment.getText()));body.put("complaint",complaint.getText());body.put("diagnosisText",diagnosis.getText());body.put("clinicalNotes",notes.getText());body.put("bloodPressure",bp.getText());if(!pulse.getText().isBlank())body.put("pulsePerMinute",Integer.parseInt(pulse.getText()));if(!temperature.getText().isBlank())body.put("temperatureCelsius",new java.math.BigDecimal(temperature.getText()));if(!weight.getText().isBlank())body.put("weightKg",new java.math.BigDecimal(weight.getText()));body.put("allergies",allergies.getText());body.put("currentMedicines",currentMeds.getText());show(api.post("/api/encounters",body));}));
        TextField encounterId=new TextField();encounterId.setPromptText("Encounter ID");boolean[] recordLoaded={false};String[] loadedEncounterId={""};Button record=new Button("Load medical record");record.setOnAction(e->run(()->api.get("/api/encounters/"+Long.parseLong(encounterId.getText().trim())).whenComplete((text,error)->Platform.runLater(()->{if(error!=null){status.setText("The encounter could not be loaded.");results.setText(friendlyError(error.getMessage()));}else try{JsonNode current=json.readTree(text);complaint.setText(current.path("complaint").asText(""));diagnosis.setText(current.path("diagnosisText").asText(""));bp.setText(current.path("bloodPressure").asText(""));pulse.setText(current.path("pulsePerMinute").asText(""));temperature.setText(current.path("temperatureCelsius").asText(""));weight.setText(current.path("weightKg").asText(""));allergies.setText(current.path("allergies").asText(""));currentMeds.setText(current.path("currentMedicines").asText(""));notes.setText(current.path("clinicalNotes").asText(""));recordLoaded[0]=true;loadedEncounterId[0]=encounterId.getText().trim();show(text);status.setText("Record loaded. Review the current values before saving changes.");}catch(Exception parseError){status.setText("The encounter response could not be read.");}}))));
        TextField historyPatient=new TextField();historyPatient.setPromptText("Patient number for visit history");Button patientHistory=new Button("Review patient visit history");patientHistory.setOnAction(e->run(()->load("/api/patients/"+java.net.URLEncoder.encode(historyPatient.getText().trim(),java.nio.charset.StandardCharsets.UTF_8)+"/encounters","Patient visit history")));
        Button saveOpen=new Button("Save changes to open encounter");saveOpen.setOnAction(e->run(()->{if(!recordLoaded[0]||!loadedEncounterId[0].equals(encounterId.getText().trim())){status.setText("Load the selected encounter first so existing values are preserved.");return;}var body=new java.util.LinkedHashMap<String,Object>();body.put("complaint",complaint.getText());body.put("diagnosisText",diagnosis.getText());body.put("bloodPressure",bp.getText());body.put("allergies",allergies.getText());body.put("currentMedicines",currentMeds.getText());body.put("clinicalNotes",notes.getText());body.put("pulsePerMinute",pulse.getText().isBlank()?null:Integer.parseInt(pulse.getText()));body.put("temperatureCelsius",temperature.getText().isBlank()?null:new java.math.BigDecimal(temperature.getText()));body.put("weightKg",weight.getText().isBlank()?null:new java.math.BigDecimal(weight.getText()));show(api.put("/api/encounters/"+Long.parseLong(encounterId.getText().trim()),body),"Open encounter saved.",()->{});}));
        Button finalize=new Button("Finalize encounter");finalize.setOnAction(e->run(()->show(api.post("/api/encounters/"+Long.parseLong(encounterId.getText())+"/finalize",java.util.Map.of()),"Record finalized and appointment completed.",()->{})));
        TextField medId=new TextField();medId.setPromptText("Medicine ID");TextField dosage=new TextField();dosage.setPromptText("Dose");TextField frequency=new TextField();frequency.setPromptText("Frequency");TextField qty=new TextField();qty.setPromptText("Quantity");Button prescribe=new Button("Add prescription");prescribe.setOnAction(e->run(()->show(api.post("/api/prescriptions",java.util.Map.of("encounterId",Long.parseLong(encounterId.getText()),"items",java.util.List.of(java.util.Map.of("medicineId",Long.parseLong(medId.getText()),"dosage",dosage.getText(),"route","ORAL","frequency",frequency.getText(),"quantity",Integer.parseInt(qty.getText()),"instructions","")))))));
        TextField substitutionItem=new TextField();substitutionItem.setPromptText("Prescription item ID");TextField substituteMedicine=new TextField();substituteMedicine.setPromptText("Replacement medicine ID");TextField substitutionReason=new TextField();substitutionReason.setPromptText("Clinical reason for substitution");Button approveSubstitution=new Button("Approve medicine substitution");approveSubstitution.setOnAction(e->run(()->show(api.post("/api/prescription-items/"+Long.parseLong(substitutionItem.getText())+"/substitution-approvals",java.util.Map.of("substituteMedicineId",Long.parseLong(substituteMedicine.getText()),"reason",substitutionReason.getText())))));
        Button addenda=new Button("Open corrections/addenda");addenda.setOnAction(e->run(()->load("/api/encounters/"+Long.parseLong(encounterId.getText())+"/addenda","Record addenda")));
        TextField correctionReason=new TextField();correctionReason.setPromptText("Reason for addendum");TextArea correctionText=new TextArea();correctionText.setPromptText("Append a correction to the finalized medical record");correctionText.setPrefRowCount(3);Button appendAddendum=new Button("Append correction");appendAddendum.setOnAction(e->run(()->show(api.post("/api/encounters/"+Long.parseLong(encounterId.getText())+"/addenda",java.util.Map.of("correctionReason",correctionReason.getText(),"addendumText",correctionText.getText())),"Correction appended to the finalized record.",()->{})));
        VBox consultation=section("Start a consultation","The appointment must be checked in before clinical notes can be opened.");consultation.getChildren().addAll(formGrid(labelled("Checked-in appointment ID",appointment),labelled("Presenting complaint",complaint),labelled("Diagnosis",diagnosis),labelled("Blood pressure",bp),labelled("Pulse per minute",pulse),labelled("Temperature \u00B7 \u00B0C",temperature),labelled("Weight \u00B7 kg",weight),labelled("Allergies",allergies),labelled("Current medicines",currentMeds)),labelled("Clinical notes",notes),start);
        VBox recordCard=section("Review or update a visit record","Load an existing encounter before saving changes. Finalized records are immutable; add a correction instead.");recordCard.getChildren().addAll(formGrid(labelled("Encounter ID",encounterId),labelled("Patient number for visit history",historyPatient)),new HBox(10,record,saveOpen,finalize,addenda,patientHistory));
        VBox prescription=section("Prescription","Enter the medicine catalogue ID and the prescribed directions.");prescription.getChildren().addAll(formGrid(labelled("Medicine ID",medId),labelled("Dose",dosage),labelled("Frequency",frequency),labelled("Quantity",qty)),prescribe);
        VBox substitution=section("Clinician-approved substitution","Record the clinical reason before pharmacy dispensing.");substitution.getChildren().addAll(formGrid(labelled("Prescription item ID",substitutionItem),labelled("Replacement medicine ID",substituteMedicine),labelled("Reason",substitutionReason)),approveSubstitution);
        VBox correction=section("Append a correction","Use an addendum to preserve the finalized record and its history.");correction.getChildren().addAll(formGrid(labelled("Reason for addendum",correctionReason)),correctionText,appendAddendum);
        return new VBox(16,consultation,recordCard,prescription,substitution,correction);
    }
    private VBox billing() {
        TextField serviceCode=new TextField();serviceCode.setPromptText("Service code");TextField serviceName=new TextField();serviceName.setPromptText("Service description");TextField servicePrice=new TextField("0.00");servicePrice.setPromptText("Price in INR");Button addService=new Button("Add billable service");
        addService.setOnAction(e->run(()->show(api.post("/api/billing/services",java.util.Map.of("code",serviceCode.getText(),"description",serviceName.getText(),"unitPrice",new java.math.BigDecimal(servicePrice.getText()))))));
        TextField encounter=new TextField();encounter.setPromptText("Finalized encounter ID");TextField serviceIds=new TextField();serviceIds.setPromptText("Service IDs, comma separated (blank if none)");Button invoice=new Button("Create invoice");invoice.setOnAction(e->run(()->{java.util.List<Long> ids=serviceIds.getText().isBlank()?java.util.List.of():java.util.Arrays.stream(serviceIds.getText().split(",")).map(String::trim).map(Long::parseLong).toList();show(api.post("/api/billing/invoices",java.util.Map.of("encounterId",Long.parseLong(encounter.getText()),"serviceIds",ids)));}));
        TextField invoiceId=new TextField();invoiceId.setPromptText("Invoice ID");Button invoiceDetails=new Button("View itemized invoice and receipts");invoiceDetails.setOnAction(e->run(()->load("/api/billing/invoices/"+Long.parseLong(invoiceId.getText().trim()),"Invoice details and receipts")));TextField amount=new TextField();amount.setPromptText("Amount in INR");ComboBox<String> method=new ComboBox<>(FXCollections.observableArrayList("CASH","UPI","CARD","BANK_TRANSFER","OTHER"));method.getSelectionModel().selectFirst();Button pay=new Button("Record payment");pay.setOnAction(e->run(()->show(api.post("/api/billing/invoices/"+Long.parseLong(invoiceId.getText())+"/payments",java.util.Map.of("amount",new java.math.BigDecimal(amount.getText()),"method",method.getValue())))));
        Button services=new Button("View billable services");services.setOnAction(e->load("/api/billing/services","Billable services"));Button invoices=new Button("Recent invoices");invoices.setOnAction(e->load("/api/billing/invoices","Recent invoices"));
        TextField paymentId=new TextField();paymentId.setPromptText("Payment ID to refund");TextField refundAmount=new TextField();refundAmount.setPromptText("Refund amount in INR");TextField refundReason=new TextField();refundReason.setPromptText("Refund reason");Button refund=new Button("Record refund");refund.setOnAction(e->run(()->show(api.post("/api/billing/payments/"+Long.parseLong(paymentId.getText())+"/refund",java.util.Map.of("amount",new java.math.BigDecimal(refundAmount.getText()),"reason",refundReason.getText())))));
        TextField voidReason=new TextField();voidReason.setPromptText("Reason to void the invoice");Button voidInvoice=new Button("Void invoice");voidInvoice.setOnAction(e->run(()->show(api.post("/api/billing/invoices/"+Long.parseLong(invoiceId.getText())+"/void",java.util.Map.of("reason",voidReason.getText())))));
        VBox catalogue=section("Billable services","Maintain the clinic service list and prices in Indian rupees.");catalogue.getChildren().addAll(formGrid(labelled("Service code",serviceCode),labelled("Service description",serviceName),labelled("Price \u00B7 INR",servicePrice)),new HBox(10,addService,services));
        VBox invoiceCard=section("Create an invoice","Dispense medicines first. An invoice locks any further dispensing for its visit.");invoiceCard.getChildren().addAll(formGrid(labelled("Finalized encounter ID",encounter),labelled("Service IDs \u00B7 comma separated",serviceIds)),invoice);
        VBox payment=section("Record a payment","Payments cannot exceed the outstanding invoice balance.");payment.getChildren().addAll(formGrid(labelled("Invoice ID",invoiceId),labelled("Amount \u00B7 INR",amount),labelled("Payment method",method)),new HBox(10,pay,invoiceDetails,invoices));
        VBox adjustments=section("Refunds and invoice corrections","Refunds and invoice voids require a reason and are kept in the record.");adjustments.getChildren().addAll(formGrid(labelled("Payment ID to refund",paymentId),labelled("Refund amount \u00B7 INR",refundAmount),labelled("Refund reason",refundReason),labelled("Reason to void invoice",voidReason)),new HBox(10,refund,voidInvoice));
        return new VBox(16,catalogue,invoiceCard,payment,adjustments);
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
        VBox accounts=section("Staff accounts","Use individual accounts and assign each person the access their role needs.");accounts.getChildren().addAll(formGrid(labelled("Username",username),labelled("Display name",display),labelled("Temporary password",password),labelled("Role",role)),new HBox(10,createStaff,listStaff),formGrid(labelled("Staff account ID",userId),labelled("Account access",accountStatus)),new HBox(10,setAccountStatus,auditLog,report),formGrid(labelled("Account ID for password reset",resetId),labelled("New temporary password",temporaryPassword)),reset);
        VBox doctors=section("Doctor profiles","Link a doctor profile to an existing staff account.");doctors.getChildren().addAll(formGrid(labelled("Doctor staff account ID",staffId),labelled("Medical specialty",specialty)),new HBox(10,createDoctor,listDoctors));
        VBox schedules=section("Weekly clinic hours","Use a 24-hour clock. Appointment slots are checked against the doctor's schedule.");schedules.getChildren().addAll(formGrid(labelled("Doctor ID",doctorId),labelled("Weekday",weekday),labelled("Opens at",from),labelled("Closes at",to)),new HBox(10,addHours,viewHours));
        VBox patientReview=section("Patient identity review","Confirm both record IDs and give a reason. Merges can be reviewed later.");patientReview.getChildren().addAll(formGrid(labelled("Source patient ID",mergeSource),labelled("Surviving patient ID",mergeTarget),labelled("Merge reason",mergeReason),labelled("Patient ID for merge history",mergeHistoryId)),new HBox(10,mergePatients,mergeHistory));
        return new VBox(16,accounts,doctors,schedules,patientReview);
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
        node.fields().forEachRemaining(entry->{JsonNode value=entry.getValue();if(value==null||value.isNull()||value.isEmpty())return;if(out.length()>0)out.append("  \u00B7  ");out.append(friendlyLabel(entry.getKey())).append(": ").append(value.isContainerNode()?formatResult(value):value.asText());});
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

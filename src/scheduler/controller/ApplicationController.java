package scheduler.controller;


import static scheduler.model.MaintainRecords.getManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import javax.xml.ws.Provider;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import scheduler.Controller;
import scheduler.model.DatabaseConnection;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor;

/** Class that extends Application. This class loads all the FXML loader files and controllers, sets individual stages or scenes, and coordinates
 * removal of old views v.s. new. It will also call all the necessary model queries and set the data or builders for each controller accordingly.
 * All localization and resource bundles will also be passed in before each loader initializes. This class also has the threads necessary for 
 * background tasks and processes and all essential instance and static fields are available through getter and setter methods for all packages.
 * The reminder service is initialized in the subclass 'Launch' which has the main() method.
 * Many of the FX buttons and controls are also handled here with the corresponding methods and FX id's.
 * @author dkell40@wgu.edu
*/
public abstract class ApplicationController extends Application implements Controller{

    /**JavaFX variables*/
    @FXML
    private static VBox root;

    @FXML
    private VBox calendarVbox;

    @FXML
    private VBox scheduleVbox;

    @FXML
    private AnchorPane rightAnchor;

    protected static VBox appVbox;
    /**protected instance variables used throughout the application*/
    protected MonthlyController calendarController;
    protected ReportsViewController reportScheduleController;
    protected WeeklyView weeklyController;

    /**protected static variables used throughout the application*/
    protected static ApplicationController mainApplication;
    protected static String USER;
    protected static String consultant;
    protected static Stage appStage;
    protected static boolean loginIsValidated;
    protected static final String color = "light-blue";
    protected static ResourceBundle location;

    /**protected static ResultSetMonitor variables used throughout the application*/
    private static ResultSetMonitor appointmentSet;
    private static ResultSetMonitor userSet;
    private static ResultSetMonitor schedulesSet;
    private static ResultSetMonitor regionalSet;
    private static ResultSetMonitor userReminderSet;
    private static ResultSetMonitor typeSet;
    private static ResultSetMonitor customerSet;
    
    /**protected static Thread variables used throughout the application*/
    protected static Thread apptThread;
    protected static Thread regionalThread;
    protected static Thread userReminderThread;
    protected static Thread typeThread;
    protected static Thread userThread;
    protected static Thread customerThread;

    /**protected static Reminder Scheduler*/
    protected static ScheduledExecutorService reminder;
    
    protected ApplicationController (){
        location = ResourceBundle.getBundle("LANGUAGE");
    }

    /**
     * Sets the Locale manually within the application to English.
     */
    @FXML
    private void handleOnActionSelectLanguageEnglish(){
        Locale.setDefault(new Locale("en", "US"));
        location = null;location = ResourceBundle.getBundle("LANGUAGE");
        start(appStage);
    }
    /**
     * Sets the Locale manually within the application to French.
     */
    @FXML
    private void handleOnActionSelectLanguageFrench(){
        Locale.setDefault(new Locale("fr", "FR"));
        location = null;location = ResourceBundle.getBundle("LANGUAGE");
        start(appStage);
    }

    /**
     * Sets the Locale manually within the application to Spanish.
     */
    @FXML
    private void handleOnActionSelectLanguageSpanish(){
        Locale.setDefault(new Locale("es", "ES"));
        location = null;location = ResourceBundle.getBundle("LANGUAGE");
        start(appStage);
    }

    /**
     * Sets the Locale manually within the application to auto detect.
     */
    @FXML
    private void handleOnActionSelectLanguageDefault(){
        String defaultLanguage = System.getProperty("user.language");
        String defaultCountry = System.getProperty("user.country");
        Locale.setDefault(new Locale(defaultLanguage, defaultCountry));
        location = null;location = ResourceBundle.getBundle("LANGUAGE");
        start(appStage);
    }

    /** Opens a new window to handle appointment data.
     * @return the controller for the appointment query.
     */
    @FXML
    private MaintainApptController handleMaintainAppointments(){
        MaintainApptController mac = null;
        if(customerSet == null)MaintainRecords.queryCustomer();
        if(loginIsValidated){
            FXMLLoader loader =new FXMLLoader(getClass().getResource("/scheduler/view/MaintainAppointments.fxml"));
            loader.setResources(location);
            Stage stage = getNewWindow(loader, true);mac = loader.getController();mac.setMaintAppStage(stage);
            stage.show();
        }  
        return mac;
    }
    
    /** Opens a new window to handle customer data.
     * @return the controller for the customer query.
     */
    @FXML
    private MaintainCustomersController handleMaintainCustomers(){
        MaintainCustomersController mac = null;
        if(loginIsValidated){
            FXMLLoader loader =new FXMLLoader(getClass().getResource("/scheduler/view/MaintainCustomers.fxml"));
            loader.setResources(location);
            Stage stage = getNewWindow(loader, true);mac = loader.getController();mac.setMaintAppStage(stage);
            stage.show();
        }  
        return mac;
    }

    /** Selects the action to take given the table name.
     * @param controller the controller to pass
     * @param id the id to pass
     */
    public void maintainRecords(Controller controller, String Id){
        if(!USER.equals(consultant)){
            showAlert("You cannot edit another consultant's data!");
        return;}
        String table = controller.getResultSetMonitor().getTableName();
        String message = "Cannot update "+table+" in this area";
        switch(table){
        case "appointment" : handleMaintainAppointments().setModel(controller, Id);break;
        case "customer" : handleMaintainCustomers().setModel(controller, Id);break;
        default : showAlert(message);break;
        } 
    }

    /** 
     * @param controller the controller to pass
     */
    public static void showAlert(String message){
        showLambdaAlertDialog((a) -> { a.setAlertType(Alert.AlertType.INFORMATION);a.setHeaderText(message);
        });
    }
    
    /** This is used for the purposes of passing in a lambda Alert expression.
     * @param show the functional interface to invoke.
     */
    public static void showLambdaAlertDialog(Consumer<Alert> show){
        Function<Alert, Alert> alert = (a) -> {show.accept(a); a.showAndWait();return a;};
        alert.apply(new Alert(null));
    }

    /** This is used for the purposes of passing in a lambda Popup expression.
     * @param function the functional interface to invoke.
     * @param window the window to add to the pop-up.
     */
    public static void showLambdaPopupDialog(BiFunction<Popup, Window, Boolean> function, Window window){
        Function<Popup, Popup> popup = (p) -> {p.setUserData(null); function.apply(p, window);p.show(window);return p;};
        popup.apply(new Popup());
    }

    /** This is used for the purposes of passing in a lambda expression.
     * @param builder the functional interface to invoke.
     */
    public static void buildContentsAsLambda(Provider<ComboBoxContainer> builder) {
        builder.invoke(new ComboBoxContainer());  
    }

    /** Kicks of the application from JavaFx.
     * @param primaryStage the primaryStage to set
     */
    public abstract void start(Stage primaryStage);

    /** 
     * This method reinitializes start() which checks the login is valid.
     */
    @FXML
    public void handleOnActionConnect(){
        if(!loginIsValidated)
        start(appStage);
    }

    /** 
     * Clears all crucial variables and sets a blank screen with
     * limited menu options.
     */
    @FXML
    public void handleOnActionDisconnect(){
        getAuditLog("logged out", Level.INFO, "USER: "+USER);
        CalendarView.setGlobalDate(LocalDate.now());
        USER = null;
        consultant = null;
        appointmentSet = null;
        userSet = null;
        schedulesSet = null;
        regionalSet = null;
        userReminderSet = null;
        typeSet = null;
        customerSet = null;
        loginIsValidated = false;
        if(reminder != null && !reminder.isTerminated())
            reminder.shutdownNow();
        setDisconnectedScene();
    }

    /** 
     * Because the connection is not handled in try with resources,
     * it can be left open somewhere in the application. It the java 
     * virtual machine is still open after exit within the system processes,
     * this could lead to multiple connections left open during testing.
     */
    @FXML
    public void handleOnActionExit(){
        DatabaseConnection.close();
        appStage.close();
    }

    /** 
     * This method calls the lambda FXML loader for the disconnected stage,
     * scene and controller.
     */
    public void setDisconnectedScene() {
        root = (VBox) loadFXMLNode((ldr, n) -> { return ldr.getController();  } ,"/scheduler/view/AppView.fxml"); 
        Scene disconnectedScene = new Scene(root);
        disconnectedScene.getStylesheets().add(ApplicationController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
        appStage.setScene(disconnectedScene);
        appStage.show();
    }

    /**This method calls the lambda FXML loader for the disconnected stage,
     * scene and controller for the login window.
     * @return loginIsValidated if login is successful
     */
    protected boolean getUserLogin(){
        if(LogInController.getLogin() == null){
            if(loginIsValidated == false){
                Stage logInStage = new Stage();

                logInStage.setOnCloseRequest(e -> {LogInController.setLogin(null);});

                Parent logInForm = (Parent) loadFXMLNode((ldr, n) -> 
                {ldr.setResources(location); LogInController logIn = ldr.getController(); 

                logIn.setMainApplication(this, logInStage); return logIn; } ,"/scheduler/view/LogIn.fxml");
                Scene logInScene = new Scene(logInForm, 300, 175);
                logInScene.getStylesheets().add(LogInController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
                logInStage.setTitle("Login Scheduler");
                logInStage.setScene(logInScene);
                logInStage.showAndWait();
            }
        }
        return loginIsValidated;

    }

    /** 
     * This is a menu selection within the FXML application and 
     * will fire an external system browser to handle the HTML file.
     */
    @FXML
    private void handleMenuOpenReadMe() throws IOException{
        File file = new File("README.html");
        Desktop.getDesktop().open(file);
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewCustomerTable(){ 
        showTable(MaintainRecords.queryCustomer() , false, true); 
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewUserTable(){ 
        showTable(getManager(null, "*", "", "user"), false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewAddressTable(){ 
        showTable(getManager(null, "*", "", "address"), true, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewCityTable(){ 
        showTable(getManager(null, "*", "", "city"), false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewCountryTable(){ 
        showTable(getManager(null, "*", "", "country"), false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewSchedules(){ 
        schedulesSet.setTableName("Schedules for each consultant");
        showTable(schedulesSet, false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleLocationStatistics(){ 
        regionalSet.setTableName("Regional Statistics for consultant's Customers");
        showTable(regionalSet, false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleTypeStatistics(){ 
        String date = parseSQLDate(CalendarView.getGlobalDate().toString());
        typeSet = null;
        MaintainRecords.queryTypeStatistics(consultant, date);
        typeSet.setTableName("Types per Month");
        showTable(typeSet, false, true);  
    }

    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewAppointmentTable(){ 
        showTable(appointmentSet, false, true);  
    }
    
    /** This is a menu selection within the FXML application and will
     * call a show TableView windows with configuration parameters of TableView
     * 'setEditable' and TableView 'shouldShow' for the given query.
     */
    @FXML
    private void handleViewReminderTable(){ 
        userReminderSet = null;
        showTable(MaintainRecords.queryUserReminders(consultant) , false, true); 
    }

    /** This is the loader for the SimpleTableController class which is a database resultSet 
     * wrapper for model data and can be used for multiple queries. It is a reusable component class.
     */
    public Stage showTable(ResultSetMonitor manager, boolean editable, boolean shouldShow){
        Stage stage = null;
        if(loginIsValidated){
            FXMLLoader simpleLoader = new FXMLLoader(getClass().getResource("/scheduler/view/SimpleTableView.fxml"));
            simpleLoader.setResources(location);
            stage = getNewWindow(simpleLoader, shouldShow);

            if(manager.getObservableDataList().size() > 0){
                SimpleTableController simpleController = simpleLoader.getController();
                simpleController.setStage(stage);
                simpleController.getTable().setEditable(editable);
                simpleController.setResultSetMonitor(manager);
                simpleController.setTableMaxSize(536, 800);simpleController.setTableMinSize(536, 300);
                simpleController.initialize();
                simpleController.renderTable();}
        }
        return stage;
    }

    /** 
     * To eliminate redundancy within the application this will create a new stage 
     * and window for each FXML loader that is passed in and will set show() based on the 
     * 'shouldShow' variable.
     *  @param loader the loader to load
     * @param shouldShow the scene to show
     * 
     */
    private static Stage getNewWindow(FXMLLoader loader, boolean shouldShow){
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        Scene scene = null;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setScene(scene);
        if(shouldShow)
        stage.show();
        return stage;
    }

    /** 
     * Loads the left VBox in the main application with the ReportScheduleView FXML
     * controller and sets the necessary model data inside for reporting view and 
     * graph information. This controller will switch the scenes between calendars for 
     * each consultant.
     */
    protected void loadConsultantSchedules(){

        Node node  =  (VBox) loadFXMLNode((ldr, n) -> { return reportScheduleController = ldr.getController(); } ,"/scheduler/view/ReportScheduleView.fxml"); 
        mainApplication.scheduleVbox.getChildren().add(node);

        reportScheduleController.setUser(consultant);

        join(getLocationThread());

        reportScheduleController.setLocations(regionalSet);

        runThread("loadConsultantSchedules()", () -> {
            reportScheduleController.setManager(userSet);
            reportScheduleController.setConsultants();
        });
        setLocationThread(null);

        loadTypesPerMonth();       

    }

    /** 
     * Loads the right VBox in the main application with the ReportScheduleView VBOX
     * controller and sets the necessary model data inside for reporting types and 
     * legend information derived from the description column.
     */
    protected void loadTypesPerMonth(){
        typeSet = null;
        mainApplication.getCalendarController();
        TypesPerMonth types = new TypesPerMonth(MaintainRecords.queryTypeStatistics
                (consultant, parseSQLDate(CalendarView.getGlobalDate().toString()) ));

        types.render();
        if(!mainApplication.rightAnchor.getChildren().isEmpty())
            mainApplication.rightAnchor.getChildren().remove(0);
        mainApplication.rightAnchor.getChildren().add(types.getVbox());
    }

    /** Loading an FXML controller, loader, and various resources can take up
     * a lot of space for each instance within the code and a centralized version is
     * embedded in this method with a functional interface as a parameter for ease of lambda expressions
     * when creating a new FXML instance. Repeated loader methods and configuration is redundant and
     * should be placed here while specific configuration is called within the lambda expression.
     * @return node the node to add to a VBox
     */
    protected Node loadFXMLNode(BiFunction<FXMLLoader, Controller, Controller> controller, String path){
        Node node = null;
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        loader.setResources(location);
        try {  node = loader.load();   
        loader.setResources(location);
        } 
        catch (IOException e) {  e.printStackTrace();}
        controller.apply(loader, loader.getController()); 
        return node;
    }

    /** Loading an FXML controller, loader, and various resources can take up
     * a lot of space for each instance within the code and a centralized version is
     * embedded in this method with a functional interface as a parameter for ease of lambda expressions
     * when creating a new FXML instance. Repeated loader methods and configuration is redundant and
     * should be placed here while specific configuration is called within the lambda expression.
     * @param function the functional interface used for lambda expressions
     * @param path the FXML path to set
     * @return controller the controller to get
     */
    protected Controller loadFXMLController(BiFunction<FXMLLoader, Controller, Controller> function, String path){
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        loader.setResources(location);
        try {  loader.load();  
        loader.setResources(location);
        } 
        catch (IOException e) {  e.printStackTrace();}
        return function.apply(loader, loader.getController()); 

    }

    /** This is the main application FXML loader and is set immediately after the main method and before all instance variables
     * are initialized.
     * @return appRoot the VBox to get
     */
    protected VBox setAppController(){
        VBox appRoot  =  (VBox) loadFXMLNode((ldr, n) -> { return mainApplication = ldr.getController();  } ,"/scheduler/view/AppView.fxml"); 
        return appRoot;
    }

    /** As there are two scenes that are switched within the application, this scene is the counter
     * part to the disconnected scene. This method, however, sets the necessary queries for the calendar
     * and loads  the rest of the main components within the application while continuing initialization.
     * 
     */
    protected void setMainScene(){ 
        root = appVbox;
        runThread("continueLoadDatabase()", () -> {continueLoadDatabase();});

        mainApplication.calendarVbox.getChildren().add(getMonthlyCalendar(USER, null));
        calendarController.weeklyVbox.getChildren().add(getWeeklyCalendar(USER, color));
        calendarController.setWeeklyController(weeklyController);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("setMainScene()-loadConsultantSchedules()");
                loadConsultantSchedules();
            }
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ApplicationController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
        appStage.setScene(scene);
        appStage.show();

    }

    /** The default 'logged in user' had a specified color set when switching between consultants
     * while each consultant gets a random color at application start. This method compares the parameter
     * consultant to the main application user and sets the color accordingly.
     * @param user the consultant or user to compare USER to 
     * @return result the color result
     */
    public static String getColor(String user) {
        String result = user.equals(USER) ? color : Fill.getRandom().color();
        return result;
    }

    /** This method is set after setMainScene() and will fire another thread to finish loading
     * queries that are not essential till a future time, not to be confused with the class <Future>.
     */
    private void continueLoadDatabase() {

        runThread("THREAD: continueLoadDatabase()", () -> {
            ApplicationController.customerThread = Thread.currentThread();
            MaintainRecords.queryCustomer();;
        });
        MaintainRecords.queryConsultantSchedules();

    }

    /** 
     * This will refresh the calendars but as a static call for other classes.
     */
    public static void refresh(String consultant){
        mainApplication.refreshCalendars(consultant);
    }

    /** 
     * This will refresh the calendars for the instance and will poll a query.
     * Although this technique is depreciated, it is used for the purposes of study and
     * fixed an error in the code.
     */
    public void refreshCalendars(String consultant){

        mainApplication.calendarVbox.getChildren().remove(0);
        mainApplication.scheduleVbox.getChildren().remove(0);

        while(appointmentSet == null){
            poll();
        }

        mainApplication.calendarVbox.getChildren().add(getMonthlyCalendar(consultant, null));
        calendarController.weeklyVbox.getChildren().add(getWeeklyCalendar(consultant, getColor(consultant)));
        calendarController.setWeeklyController(weeklyController);
        loadConsultantSchedules();

    }

    /** Creates an FXML loader and controller for the weekly calendar and joins a previous FX thread
     * that called the database for a query  of appointments. Although asynchronous I/O could be used, it was
     * actually faster in this scenario to use one time threads also assuming that the end user has more than 
     * one processor. Additionally, the CPU I/O from Executors was avoided as the operation on the query is a network I/O
     * process.
     * @return node the Weekly node to get
     */
    private Node getWeeklyCalendar(String consultant, String color){
        Node node =  (VBox) loadFXMLNode((ldr, n) -> { return weeklyController = ldr.getController();  } ,"/scheduler/view/WeeklyView.fxml"); 


        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("monthly-weekly");
                join(getApptThread());
                weeklyController.setCssColor(color);
                weeklyController.setResultSetMonitor(appointmentSet);
                weeklyController.renderCalendar(CalendarView.getGlobalDate());
                setApptThread(null);
            }
        });

        return node;
    }

    /** Creates an FXML loader and controller for the monthly calendar and joins a previous thread
     * that called the database for a query  of appointments. Although asynchronous I/O could be used, it was
     * actually faster in this scenario to use one time threads also assuming that the end user has more than 
     * one processor. Additionally, the CPU I/O from Executors was avoided as the operation on the query is a network I/O
     * process.
     * @return node the calendar node to get
     */
    private Node getMonthlyCalendar(String consultant, String color){
        Node node =  (VBox) loadFXMLNode((ldr, n) -> { return calendarController = ldr.getController(); } ,"/scheduler/view/CalendarView.fxml"); 
        join(getApptThread());
        calendarController.setResultSetMonitor(appointmentSet);
        calendarController.renderCalendar(CalendarView.getGlobalDate());
        return node;
    }

    /** 
     * @return customerSet the customer ResultSet Monitor to get
     */
    public static ResultSetMonitor getCustomerSet() {
        return customerSet;
    }

    /** 
     * @param customerSet the customer ResultSet Monitor to set 
     */
    public static void setCustomerSet(ResultSetMonitor customerSet) {
        ApplicationController.customerSet = customerSet;
    }

    /** 
     * @return appointmentSet the appointment ResultSet Monitor to get
     */
    public static ResultSetMonitor getAppointmentSet() {
        return appointmentSet;
    }

    /** 
     * @param appointmentSet the appointment ResultSet Monitor to set 
     */
    public static void setAppointmentSet(ResultSetMonitor appointmentSet) {
        ApplicationController.appointmentSet = appointmentSet;
    }

    /** 
     * @return userSet the user ResultSet Monitor to get 
     */
    public static ResultSetMonitor getUserSet() {
        return userSet;
    }

    /** 
     * @param userSet the user ResultSet Monitor to set  
     */
    public static void setUserSet(ResultSetMonitor userSet) {
        ApplicationController.userSet = userSet;
    }

    /** 
     * @return schedulesSet the schedules ResultSet Monitor to get 
     */
    public static ResultSetMonitor getScheduleSet() {
        return schedulesSet;
    }

    /** 
     * @param schedulesSet the schedulesSet ResultSet Monitor to set  
     */
    public static void setScheduleSet(ResultSetMonitor schedulesSet) {
        ApplicationController.schedulesSet = schedulesSet;
    }

    /** 
     * @return calendarController the calendarController ResultSet Monitor to get 
     */
    public MonthlyController getCalendarController() {
        return calendarController;
    }

    /** 
     * @return regionalSet the regionalSet ResultSet Monitor to get  
     */
    public static ResultSetMonitor getRegionalSet() {
        return regionalSet;
    }

    /** 
     * @param regionalSet the regionalSet ResultSet Monitor to set  
     */
    public static void setRegionalSet(ResultSetMonitor locationSet) {
        ApplicationController.regionalSet = locationSet;
    }

    /**
     * @return userReminderSet the userReminderSet ResultSet Monitor to get  
     */
    public static ResultSetMonitor getUserReminderSet() {
        return userReminderSet;
    }


    /**
     * @param userReminderSet the userReminderSet to set
     */
    public static void setUserReminderSet(ResultSetMonitor userReminderSet) {
        ApplicationController.userReminderSet = userReminderSet;
    }

    /** 
     * @return appointmentSet the appointmentSet ResultSet Monitor to get  
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return appointmentSet;
    }

    /** 
     * @param appointmentSet the appointmentSet to set
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel) {
        ApplicationController.appointmentSet = resultSetModel;
    }
    
    /** 
     * @return mainApplication the mainApplication ResultSet Monitor to get  
     */
    public static ApplicationController getMainApplication() {
        return mainApplication;
    }

    /** 
     * @param loginIsValidated the loginIsValidated to set
     */
    public static void setLoginIsValidated(boolean loginIsValidated) {
        ApplicationController.loginIsValidated = loginIsValidated;
    }

    /** 
     * @return loginIsValidated the loginIsValidated to get  
     */
    public static boolean isLoginValidated() {
        return loginIsValidated;
    }

    /** 
     * @return appStage the appStage to get   
     */
    public static Stage getAppStage() {
        return appStage;
    }

    /** 
     * @return USER the USER to get   
     */
    public static String getUser() {
        return USER;
    }

    /** 
     * @param USER the USER to set 
     */
    public static void setUser(String user) {
        ApplicationController.USER = user;
    }

    /** 
     * @param consultant the consultant to set 
     */
    public static String getConsultant() {
        return consultant;
    }

    /** 
     * @param consultant the consultant to set 
     */
    public static void setConsultant(String consultant) {
        ApplicationController.consultant = consultant;
    }

    /**
     * @return the apptThread
     */
    public static Thread getApptThread() {
        return apptThread;
    }

    /**
     * @param apptThread the apptThread to set
     */
    public static void setApptThread(Thread appThread) {
        ApplicationController.apptThread = appThread;
    }

    /**
     * @return the regionalThread
     */
    public static Thread getLocationThread() {
        return regionalThread;
    }

    /**
     * @param regionalThread the regionalThread to set
     */
    public static void setLocationThread(Thread locationThread) {
        ApplicationController.regionalThread = locationThread;
    }

    /**
     * @return the userReminderThread
     */
    public static Thread getUserReminderThread() {
        return userReminderThread;
    }

    /**
     * @param userReminderThread the userReminderThread to set
     */
    public static void setUserReminderThread(Thread userReminderThread) {
        ApplicationController.userReminderThread = userReminderThread;
    }

    /**
     * @return the typeSet
     */
    public static ResultSetMonitor getTypeSet() {
        return typeSet;
    }

    /**
     * @param typeSet the typeSet to set
     */
    public static void setTypeSet(ResultSetMonitor typeSet) {
        ApplicationController.typeSet = typeSet;
    }

    /**
     * @return the location
     */
    public static ResourceBundle getResources() {
        return location;
    }


}



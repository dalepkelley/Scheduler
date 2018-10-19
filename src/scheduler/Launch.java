package scheduler;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import scheduler.controller.ApplicationController;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Class that subclasses the main controller which extends Application.
 * The main() method resides here and is sub-classed to allocate Locale and reminders in one area.
* @author dkell40@wgu.edu
*/
public class Launch extends ApplicationController implements DateTimeHelper{

    
    /** main method for the scheduler.
     * Locale area for testing purposes included.
     */
    public static void main(String[] args) {

        String defaultLanguage = System.getProperty("user.language");
        String defaultCountry = System.getProperty("user.country");
        Locale.setDefault(new Locale(defaultLanguage, defaultCountry));
        //Locale.setDefault(new Locale("fr", "FR"));
        //java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Paris"));
        //Locale.setDefault(new Locale("es", "ES"));
        //java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/London"));
        //Locale.setDefault(new Locale("en", "US"));

        launch(args);
    }


    /** 
     * Implements the abstract method in the Java Application class.
     */
    @Override
    public void start(Stage primaryStage) 
    
    {
        appStage = appStage == null ? primaryStage : appStage;
        appStage.setTitle("My Application");
        appStage.setOnCloseRequest(e -> {  
            getAuditLog("logged out", Level.INFO, "USER: "+USER);
        });
        appVbox = setAppController();
        setDisconnectedScene();

        runThread("Launch-queryUserSchedules()", () -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            MaintainRecords.queryUserSchedules();
        });

        if(getUserLogin()){

            runThread("GlobalVariables-queryAppointment()", () -> {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                ApplicationController.setApptThread(Thread.currentThread());
                MaintainRecords.queryAppointment(consultant);
            });

            runThread("GlobalVariables-queryLocationStatistics()", () -> {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                ApplicationController.setLocationThread(Thread.currentThread());
                MaintainRecords.queryRegionalStatistics(consultant);
            });

            queueReminders(MaintainRecords.queryUserReminders(consultant));
            setMainScene();
        }
    }
    
    /**This will queue all the relevant reminders from the results of the model data and 
     * schedule the reminder service to run them.
     * @param resultSetMonitor the resultSetMonitor for Reminder data retrieval
     */
    public static void queueReminders(ResultSetMonitor resultSetMonitor){
            if(reminder != null && !reminder.isTerminated()){
                reminder.shutdownNow();
            }
            reminder = Executors.newScheduledThreadPool(1);
            

            resultSetMonitor.getObservableDataList().forEach((model) -> {
                LocalDate date = model.getDate(2);
            if (date.equals(LocalDate.now())) {
                setReminder(model);
            }
        });
        
    }

    /**Sets a particular model record to be queued up if it falls within todays date.
     * It will also check to see if a reminder window has already started before application run
     * and will offset the remaining time to the next snooze increment. 
     * @param model the model data for a particular reminder
     */
    public static void setReminder(ModelDataType model){

        if(between(LocalDateTime.now() ,model.getTime(2) ,model.getTime(7) ))
        {
            alreadyStarted(model);
        }else{
            long offset = (model.getTime(2).minusSeconds(LocalTime.now().toSecondOfDay()).toSecondOfDay());
            addReminder(model, offset);
        }
    }

    /** Checks whether a particular instance of a LocaldateTime value is within start/end times.
     * @param thisDayAtTime the current dateTime usually within an iterator.
     * @param start the start marker for comparison
     * @param ends the end marker for comparison
     */
    public static boolean between(LocalDateTime thisDayAtTime, LocalTime start, LocalTime end ){
        LocalTime thisTime =  thisDayAtTime.toLocalTime();
        return thisTime.equals(start) || (thisTime.isAfter(start) && thisTime.isBefore(end));
    }

    /**Sets a particular reminder record to be displayed. This method starts the recursive snooze
     * addReminder() method and will also check to see if a reminder window has already started before 
     * the application finishes loading. It will also set the offset for the next snooze increment when added. 
     * @param model the data for a particular reminder
     */
    private static void alreadyStarted(ModelDataType model) {

        LocalTime timeNow = LocalDateTime.now().toLocalTime();
        Integer snoozeIncrement = new Integer(model.get(4)) * 60;
        LocalTime start = model.getTime(7);
        String description = model.get(5);
        String customerName = model.get(6);
        Integer duration = start.minusSeconds(timeNow.toSecondOfDay()).toSecondOfDay();
        Integer offset = duration%snoozeIncrement;



        addReminder(model, offset);

        String message = "You have a(n) " + description + " with " + customerName
                + " at " + start.format(amPmformat) ;
        Platform.runLater(() -> {showLambdaAlertDialog((a) -> { a.setTitle("Reminder");a.setAlertType(Alert.AlertType.INFORMATION);a.setHeaderText(message);});});


    }

    /** This is the beginning of a recursive snooze check and lasts up to the start time is reached.
    * @param model the data for a particular reminder
    * @param offset the offset between snooze if started in between snooze markers.
    */
    private static void addReminder(ModelDataType model, long offset){

        LocalTime start = model.getTime(7);
        String description = model.get(5);
        String customerName = model.get(6);
        int snoozeIncrement = new Integer(model.get(4));


        reminder.schedule(() -> withSnooze(description, customerName, start, snoozeIncrement), offset, TimeUnit.SECONDS);

    }

    /** This method is recursive and will display the same message until start is reached
     * and will display a new message at start.
    * @param description the model data
    * @param customerName the model data
    * @param start the start date and time
    * @param snoozeIncrement the duration for each snooze
    */
    private static void withSnooze(String description, String customerName, LocalTime start, long snoozeIncrement){
        String message = "You have a(n) " + description + " with " + customerName
                + " right now!" ;
        if(LocalTime.now().isBefore(start)){
            reminder.schedule(() -> withSnooze(description, customerName, start, snoozeIncrement), snoozeIncrement, TimeUnit.MINUTES);
            showReminder(description, customerName, start);
        }
        else
            Platform.runLater(() -> {showLambdaAlertDialog((a) -> { a.setTitle("Reminder");a.setAlertType(Alert.AlertType.INFORMATION);a.setHeaderText(message);});}); 
    }
    
    /** The alert or pop-up to show for the reminder messages.
    * @param description the model data
    * @param customerName the model data
    * @param start the model data
    */
    private static void showReminder(String description, String customerName, LocalTime start){
        String message = "You have a(n) " + description + " with " + customerName
                + " at " + start.format(amPmformat) ;
        Platform.runLater(() -> {showLambdaAlertDialog((a) -> { a.setTitle("Reminder");a.setAlertType(Alert.AlertType.INFORMATION);a.setHeaderText(message);});});

    }
    
    /** Inherited from the Controller class and is not used because this
     * sub-class also extends the Application class which inherits start().
    */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }
    
    /** Inherited from the Controller class and is not used because it cannot be made
     * abstract in this scenario.
    */
    @Override
    public void refresh(){
        
    }


}


package scheduler.controller;

import static scheduler.controller.ApplicationController.buildContentsAsLambda;
import static scheduler.controller.ApplicationController.showLambdaAlertDialog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import scheduler.Controller;
import scheduler.Launch;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ExceptionControls;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** This controller has functionality to input data into various field types and
 * can query the underlying data model with the data to insert or update. Builders 
 * are used to get the multi-value data for each ComboBox and reporting area is one
 * exists.
 * @author dkell40@wgu.edu
 */
public class MaintainApptController extends ExceptionControls implements Controller{

    @FXML
    private VBox root;
    @FXML
    private TextField titleField; 
    @FXML
    private TextField locationField;
    @FXML
    private TextField contactField;
    @FXML
    private TextField urlField;
    @FXML
    private ComboBox<Container> customerBox; 
    @FXML
    private ComboBox<Container> descriptionTypeBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<Container> startTimeBox;
    @FXML
    private ComboBox<Container> endTimeBox ;
    @FXML
    private TextArea customerDetails;

    @FXML
    private ComboBox<Container> durationBox;
    @FXML
    private ComboBox<Container> snooseBox ;
    @FXML
    private VBox reminderBox;

    private static ArrayList<String> appointment;

    private static String[] reminder;
    private ListViewEvents reminders;
    
    private ResultSetMonitor resultSetMonitor;
    private Controller controller;
    private ModelDataType model; 
    private Stage stage;
    
    private long appointmentId = -1;
    private String[] appointmentColumns;




    /** 
     * @param maintAppStage the maintAppStage to set
     */
    public MaintainApptController setMaintAppStage(Stage maintAppStage) {
        this.stage = maintAppStage;
        return this;
    }

    /** 
     * @FXML item to handle adding a new appointment and /or reminder record to the database given
     * the fields. The data is validated by the super class ExceptionControls.
     */
    @FXML
    public void handleAddRecord(){

        assert ApplicationController.getAppointmentSet() != null : "getAppointmentSet is null LINE: 90 "+ this.getClass().getSimpleName(); 

        try {
            String createDate = LocalDateTime.now(ZoneOffset.UTC).toString();
            String date = checkOverlappingAppts(validate("date","Start time",datePicker,startTimeBox), model);
            LocalDateTime start = parseSQLDateTime(date);
            String end = checkOverlappingAppts(validateAfter(start.toString(), validate("date","End time",datePicker,endTimeBox)), model);
            String type =  validateNullComboBox("Type", descriptionTypeBox) ;
            
            appointment = new ArrayList<String>();
            reminder = new String[9];


            parse_((data)-> { data.add(getAppointmentKey()); }, appointment );//appointmentId
            parse_((data)-> { data.add(validateNullComboBox("Customer", customerBox)); }, appointment );//customerId
            parse_((data)-> { data.add(checkInvalidOrNull("Title", titleField.getText())); }, appointment );//title
            parse_((data)-> { data.add(type); }, appointment );//description
            parse_((data)-> { data.add(checkInvalidOrNull("Location", locationField.getText())); }, appointment );//location
            parse_((data)-> { data.add(checkInvalidOrNull("Contact", contactField.getText())); }, appointment );//contact
            parse_((data)-> { data.add(checkInvalidOrNullURL("URL", urlField.getText())); }, appointment );//url       
            parse_((data)-> { data.add(setUTC(start.toString())); } , appointment);//start
            parse_((data)-> { data.add(setUTC(end)); }, appointment );//end    
            parse_((data)-> { data.add(createDate); }, appointment );//createDate
            parse_((data)-> { data.add(ApplicationController.getUser()); }, appointment );//createBy
            parse_((data)-> { data.add(SERVER_TIMESTAMP_NOW_UTC); }, appointment );//lastUpdate
            parse_((data)-> { data.add(ApplicationController.getUser()); }, appointment );//lastUpdateBy
            
            recordsMaintain((database) -> {getAppointmentId(getSQLCommand(appointment, database, "appointment", model, appointmentColumns)); });

            
            if(!durationBox.getSelectionModel().isEmpty()){
                
                Long duration = new Long(getText(durationBox));
                LocalDateTime reminderdate = start.minusMinutes(duration);

                parse((data)-> { data[0] = ("-1"); }, reminder ); //reminderId
                parse((data)-> { data[1] = (getRemindercol()); }, reminder ); //remindercol
                parse((data)-> { data[2] = (setUTC(reminderdate.toString())); }, reminder ); //reminderdate
                parse((data)-> { data[3] = (ApplicationController.getUser()); }, reminder ); //reminder.createdBy
                parse((data)-> { data[4] = (validateNullComboBox("snoose",snooseBox)); }, reminder ); //snoozeIncrement
                parse((data)-> { data[5] = (type); }, reminder ); //description
                parse((data)-> { data[6] = (customerBox.getSelectionModel().getSelectedItem().getDescription()); }, reminder ); //customerName, 
                parse((data)-> { data[7] = (start.toString()); }, reminder ); //start
                parse((data)-> { data[8] = (""+appointmentId); }, reminder );  //appointmentId

                assert ApplicationController.getUserReminderSet() != null 
                        : "getUserReminderSet() is null LINE: 129 "+ this.getClass().getSimpleName();

                recordsMaintain((database) -> {getSQLReminder(database, reminder); });

                parse((data)-> { data[2] = (reminderdate.toString()); }, reminder ); //remindercol
                
                addNewReminder(reminder);
            }

            setThreads(ApplicationController.getUser());
            ApplicationController.getMainApplication().refreshCalendars(ApplicationController.getConsultant());
            clearFields();stage.close();

        } catch (IllegalArgumentException e) {
            showLambdaAlertDialog((a) -> { a.setAlertType(Alert.AlertType.WARNING);a.setHeaderText(e.getMessage());});
        } 

    }
    
    /** Based on the sql data, a ModelDataType is built and passed to the 
     * reminder queue as it did at application start-up.
     * @param sql the sql data to set
     */
    private void addNewReminder(String[] sql){
        ModelDataType model = new ModelDataType(sql);
        Launch.setReminder(model);

        assert ApplicationController.getUserReminderSet() != null 
                : ".getUserReminderSet() is null LINE: 245 "+ this.getClass().getSimpleName();
        
        ApplicationController.getUserReminderSet().getObservableDataList().add(model);
        
    }
    
    /** This method clears the list view and creates a new one in which
     * the reminders can be displayed in the appointment view FXML. The list is
     * filtered by the search and leaves the observable list intact.
     * @param search the search to set
     * @return listView the listView to get
     */
    private ListView<Container> getListViewEvents(String search){ 
        join(ApplicationController.getUserReminderThread());
        reminders = new ListViewEvents(null, false)
                .getListView(ApplicationController.getUserReminderSet());
        reminderBox.getChildren().removeAll(reminderBox.getChildren());
        reminders.filterBy((m, c) -> { 
          
            if (m.get(8).equals((search+appointmentId))) {
                c.add(new Container(m.get(0) , m.get(1), m));
            }
            return !c.isEmpty(); 
        });
        reminders.renderListView().getListView().setMaxSize(250, 125);
        
        reminderBox.getChildren().add(reminders.getListView());
        return reminders.getListView();
    }

    /** Parses the remindercol string format.
     * @return remindercol the remindercol data to get
     */
    private String getRemindercol(){
       return getDescription(durationBox)+" before start - Snooze every "+getText(snooseBox)+" min";
    }

    /** Ensures not null either way.
     * @param row sets the row for comparison
     * @return  row gets a non null row
     */
    private String[] getAppointmentId(String[] row){
        if(row != null)appointmentId = new Long(row[0]); 
        return row;
    }

    /** Used for lambda expressions.
     * @param database the function to set
     */
    private void recordsMaintain(Consumer<MaintainRecords> database){
        database.accept(new MaintainRecords());
    }

    /** Used for lambda expressions.
     * @param row the row to set
     * @param database the database to set
     * @param table the table to set
     * @param model the model to set
     * @param columns... the array to set
     * @return data[] the completed data[] after database handles
     */
    private String[] getSQLCommand(ArrayList<String> row, MaintainRecords database, String table, ModelDataType model, String... columns){
        return model != null ? database.updateTable(table, row.toArray(new String[row.size()]), columns) 
                : database.insertIntoTable(table, row.toArray(new String[row.size()])) ;   
    }
    
    /** Gets Appointment Key
     * @return model the model to get
     */
    private String getAppointmentKey(){
        if(model != null)return model.get(0);
        else return null;
    }

    /** 
     * @param database the database to set
     * @param row the row to set
     * @return row the competed row to get after insertion
     */
    private String[] getSQLReminder(MaintainRecords database, String... row){
        return database.insertIntoReminder(row);   
    }

    /** Inserts the row as a Lambda expression.
     * @param statement the functional interface to set
     * @param row the row to set
     */
    private static void parse(Consumer<String[]> statement, String ... row){
        statement.accept(row);    
    }
    
    /** Inserts the row as a Lambda expression.
     * @param statement the functional interface to set
     * @param row the row to set
     */
    private static void parse_(Consumer<ArrayList<String>> statement, ArrayList<String> row){
        statement.accept(row);    
    }

    /** 
     * Sets all fields to null
     */
    @FXML
    private void clearFields(){
        model = null;
        Arrays.asList(datePicker, startTimeBox, endTimeBox, 
                descriptionTypeBox, customerBox).forEach((comboBoxBase) -> { comboBoxBase.setValue(null);});
        Arrays.asList(titleField, locationField, 
                contactField, urlField, customerDetails).forEach((field) -> { field.clear();});
        getListViewEvents("no data");
    }

    /** 
     * @return stage the stage to get
     */
    public Stage getMaintAppStage() { return stage; }
    
    /** 
     * based on model data, populates the fields with their respective data.
     */
    private void setAccessibleText(){
        assert (model != null) : "model is null LINE: 141 "+ this.getClass().getSimpleName();

        if(model != null) {

            titleField.setText(model.get(3));

            locationField.setText(model.get(10));
            contactField.setText(model.get(5));
            urlField.setText(model.get(13));   
            datePicker.setValue(model.getDate(1));

            select(startTimeBox, model.getTimeStrAmPm(1));
            select(endTimeBox, model.getTimeStrAmPm(2));
            select(customerBox, model.get(7));
            select(descriptionTypeBox, model.get(4));
          
            getListViewEvents("");

        }  
    }

    /** Selects based on copy of data.
     * @param b the CsomboBox to set
     * @param data the data to select
     */
    private void select(ComboBox<Container> b, String data){
        b.getItems().forEach((c) -> {if(c.toString().equals(data)) 
            b.getSelectionModel().select(c);  });
    }

    /** Gets a list from a file for the type field.
     * @return obsList the obsList to get
     */
    public ObservableList<ModelDataType> getTypesFromFile(){
        ObservableList<ModelDataType> types = FXCollections.observableArrayList();
        String line = "";
        try (BufferedReader in = new BufferedReader(new FileReader("Types.txt"));)
        {  while((line = in.readLine()) != null){
            types.add(new ModelDataType(line, line));
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return types;
    }

    /** Gets a list from a start and end frame.
     * @return obsList the obsList to get
     */
    public ObservableList<ModelDataType> setTimeFrames(){
        ObservableList<ModelDataType> times = FXCollections.observableArrayList();
        for (LocalTime startTime = start; ! startTime.isAfter(end);startTime = startTime.plus(fifteenMinuteIncrement)) {
            times.add(new ModelDataType(startTime.toString(), startTime.format(amPmformat))); 
        }
        return times;
    }

    /** Gets a list from the given data.
     * @return obsList the obsList to get
     */
    public ObservableList<ModelDataType> setReminderBefore(){
        ObservableList<ModelDataType> duration = FXCollections.observableArrayList();
        duration.add(new ModelDataType("60", "1 hour"));
        duration.add(new ModelDataType("30", "30 Minutes"));
        duration.add(new ModelDataType("15", "15 Minutes"));
        return duration; 
    }

    /** Gets a list from the given data.
     * @return obsList the obsList to get
     */
    public ObservableList<ModelDataType> setSnooze(){
        ObservableList<ModelDataType> snoose = FXCollections.observableArrayList();
        snoose.add(new ModelDataType("5", "Five Minutes"));
        snoose.add(new ModelDataType("3", "Three Minutes"));
        snoose.add(new ModelDataType("1", "One Minute"));
        return snoose; 
    }

    /** 
     * @return model the model to get
     */
    public ModelDataType getModel() {
        return model;
    }

    /** Actually sets multiple variables that were not able to initialize
     * upon construction of this class object.s
     * @param controller the controller to set
     * @param id the id to set
     * @return this the instance to get for method chaining
     */
    public MaintainApptController setModel(Controller controller, String id) {
        this.controller = controller;
        resultSetMonitor = controller.getResultSetMonitor();
        
        ResultSetMonitor appointmentSet = MaintainRecords.getRecordById("appointment",id);
        appointmentColumns = appointmentSet.getColumnHeaderNames();
        
        resultSetMonitor.getObservableDataList().forEach(m -> {if(m.get(0).equals(id)) model=m;});
        
        appointmentId = new Long(model.getId());
        setAccessibleText();
        return this;
    }
    
    /** 
     * Refreshes the controller with new data,
     */
    @Override
    public void refresh(){
        if(controller != null)
            controller.refresh();
        else ApplicationController.getCustomerSet().refresh();

    }
    
    /** 
     * @return resultSetMonitor the resultSetMonitor to get
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return resultSetMonitor;
    }

    /** 
     * @param resultSetMonitor the resultSetMonitor to set
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel) {
        this.resultSetMonitor = resultSetModel;
    }
    
    /**Initializes instance or static variables and uses a lambda builder
     * for the bloated ComboBox class.
     * @param url the url to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {  
        reminderBox.getStyleClass().add("calendar-box-style");
        
        buildContentsAsLambda( (b) -> {b.setComboBox(startTimeBox).initialize(setTimeFrames(), null); return b;} );
        buildContentsAsLambda( (b) -> {b.setComboBox(endTimeBox).initialize(setTimeFrames(), null); return b;} );
        buildContentsAsLambda( (b) -> {b.setReport(customerDetails).setComboBox(customerBox).initialize(null, ApplicationController.getCustomerSet()); return b;} );
        buildContentsAsLambda( (b) -> {b.setComboBox(descriptionTypeBox).initialize(getTypesFromFile(), null); return b;} );

        assert (durationBox != null) : "durationBox is null LINE: 158 "+ this.getClass().getSimpleName();
        assert (setReminderBefore() != null) : "setReminderBefore() is null LINE: 159 "+ this.getClass().getSimpleName();

        buildContentsAsLambda( (b) -> {b.setComboBox(durationBox).initialize(setReminderBefore(), null); return b;} );
        buildContentsAsLambda( (b) -> {b.setComboBox(snooseBox).initialize(setSnooze(), null); return b;} );

    }
}

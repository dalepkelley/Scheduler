
package scheduler.controller;

import static scheduler.controller.ApplicationController.buildContentsAsLambda;
import static scheduler.controller.ApplicationController.showLambdaAlertDialog;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import scheduler.Controller;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ExceptionControls;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;

public class MaintainCustomersController extends ExceptionControls implements Controller{

    /**private @FXML instance variables used throughout the class*/
    @FXML
    private VBox root;
    @FXML
    private TextField customerNameField; 
    @FXML
    private TextField addressField;
    @FXML
    private TextField address2Field;
    @FXML
    private TextField postalCodeField;
    @FXML
    private TextField phoneField; 
    @FXML
    private ComboBox<Container> cityBox; 
    @FXML
    private TextArea cityDetails;

    /**private static variables used throughout the class*/
    private static ArrayList<String> address;
    private static ArrayList<String> customer;

    /**private instance variables used throughout the class*/
    private ResultSetMonitor refreshMonitor;
    private Controller controller;
    private ModelDataType addressModel; 
    private ModelDataType customerModel; 
    private String[] addressColumns;
    private String[] customerColumns;
    private Stage stage;
    private Long addressId;



    /** 
     * @FXML Handles adding a customer record upon fire event of a button. 
     */
    @FXML
    public void handleAddRecord(){

        try {

            final String createDate = LocalDateTime.now(ZoneOffset.UTC).toString();
            address = new ArrayList<String>();
            customer = new ArrayList<String>();

            parse((data)-> { data.add(getAddressKey()); }, address );//addressId
            parse((data)-> { data.add(checkInvalidOrNull("address", addressField.getText())); }, address );//address
            parse((data)-> { data.add(checkInvalid("address2", address2Field.getText())); }, address );//address2
            parse((data)-> { data.add(validateNullComboBox("city", cityBox)); }, address );//cityId
            parse((data)-> { data.add(checkInvalidOrNull("postalCode", postalCodeField.getText())); }, address );//postalCode
            parse((data)-> { data.add(checkInvalidOrNull("phone", phoneField.getText())); }, address );//phone
            parse((data)-> { data.add(createDate); }, address );//createDate
            parse((data)-> { data.add(ApplicationController.getUser()); }, address );//createBy
            parse((data)-> { data.add(SERVER_TIMESTAMP_NOW_UTC); }, address );//lastUpdate
            parse((data)-> { data.add(ApplicationController.getUser()); }, address );//lastUpdateBy


            recordsMaintain((database) -> {getAddressId(getSQLCommand(address, database, "address", addressModel, addressColumns)); });


            parse((data)-> { data.add(getCustomerKey()); } , customer);//customerId
            parse((data)-> { data.add(checkInvalidOrNull("address2", customerNameField.getText())); }, customer );//customerName
            parse((data)-> { data.add(addressId.toString()); } , customer); //addressId
            parse((data)-> { data.add(ACTIVE); } , customer); //active
            parse((data)-> { data.add(createDate); } , customer); //createDate
            parse((data)-> { data.add(ApplicationController.getUser()); } , customer); //createdBy
            parse((data)-> { data.add(SERVER_TIMESTAMP_NOW_UTC); } , customer); //lastUpdate
            parse((data)-> { data.add(ApplicationController.getUser()); } , customer); //lastUpdateBy

            recordsMaintain((database) -> {getSQLCommand(customer, database, "customer", customerModel, customerColumns);});

            refresh();
            
            clearFields();stage.close();

        } catch (IllegalArgumentException e) {
            showLambdaAlertDialog((a) -> { a.setAlertType(Alert.AlertType.WARNING);a.setHeaderText(e.getMessage());});
        }

    }

    /** Sets the ID and returns.
     * @param row the row to set
     * @return row the row to get
     */
    private String[] getAddressId(String[] row){
        if(row != null)addressId = new Long(row[0]); 
        return row;
    }

    /** Functional interface method.
     * @param database the database to set
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

    /** Gets Address Key
     * @return model the model to get
     */
    private String getAddressKey(){
        if(addressModel != null)return addressModel.get(0);
        else return null;
    }
    
    /** Gets Customer Key
     * @return model the model to get
     */
    private String getCustomerKey(){
        if(customerModel != null)return customerModel.get(0);
        else return null;
    }
    
    /** Inserts the row as a Lambda expression.
     * @param statement the functional interface to set
     * @param row the row to set
     */
    private static void parse(Consumer<ArrayList<String>> statement, ArrayList<String> row){
        statement.accept(row);    
    }

    /** 
     * Sets all fields to null
     */
    @FXML
    private void clearFields(){
        addressModel = null; cityBox.setValue(null);
        customerModel = null;
        Arrays.asList(customerNameField, addressField, 
                address2Field, postalCodeField, cityDetails, phoneField).forEach((field) -> { field.clear();});

    }

    /** 
     * based on model data, populates the fields with their respective data.
     */
    private void setAccessibleText(){
        if(addressModel != null) {

            addressField.setText(addressModel.get(1));
            address2Field.setText(addressModel.get(2));
            postalCodeField.setText(addressModel.get(4));   
            phoneField.setText(addressModel.get(5));
        }  

        if(customerModel != null) {
            customerNameField.setText(customerModel.get(1));
        } 


    }

    /** Selects based on copy of data.
     * @param b the CsomboBox to set
     * @param data the data to select
     */
    private void select(ComboBox<Container> b, String data){
        b.getItems().forEach((c) -> { if(c.getId().toString().equals(data)) 
            b.getSelectionModel().select(c);  });
    }

    /** 
     * @return model the model to get
     */
    public ModelDataType getModel() {
        return customerModel;
    }
    
    /** 
     * @return stage the stage to get
     */
    public Stage getMaintAppStage() {  return stage; }

    /** 
     * @param stage the stage to set
     */
    public MaintainCustomersController setMaintAppStage(Stage maintAppStage) {
        this.stage = maintAppStage;
        return this;
    }

    /** Actually sets multiple variables that were not able to initialize
     * upon construction of this class object.s
     * @param controller the controller to set
     * @param id the id to set
     * @return this the instance to get for method chaining
     */
    public MaintainCustomersController setModel(Controller controller, String id) {
        this.controller = controller;
        refreshMonitor = controller.getResultSetMonitor();
        
        ResultSetMonitor customerSet = MaintainRecords.getRecordById("customer",id);
        customerColumns = customerSet.getColumnHeaderNames();
        
        this.customerModel = customerSet.getObservableDataList().get(0);

        addressId = new Long(customerModel.get(2));
        ResultSetMonitor addressSet = MaintainRecords.getRecordById("address",addressId.toString());
        addressColumns = addressSet.getColumnHeaderNames();

        this.addressModel = addressSet.getObservableDataList().get(0);
        select(cityBox, addressModel.get(3));


        setAccessibleText();
        return this;
    }
    
    /**Initializes instance or static variables and uses a lambda builder
     * for the bloated ComboBox class.
     * @param URL the URL to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {  
        buildContentsAsLambda( (b) -> {b.setReport(cityDetails).setComboBox(cityBox).initialize(null
                , MaintainRecords.getManager(ApplicationController.getConsultant(), "cityId, city, country", "", "city", "country")); return b;} );
    }
    
    /** 
     * Refreshes the controller with new data,
     */
    @Override
    public void refresh(){
        if(controller != null) controller.refresh();
        else ApplicationController.getCustomerSet().refresh();
    }

    /** 
     * @return stage the stage to get
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return refreshMonitor;
    }

    /** 
     * @return stage the stage to get
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel) {
        this.refreshMonitor = resultSetModel;
    }

}

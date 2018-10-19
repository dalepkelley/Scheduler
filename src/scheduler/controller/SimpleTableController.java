package scheduler.controller;

import static scheduler.model.MaintainRecords.deleteSQLRecord;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import scheduler.Controller;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** This is a database wrapper class for viewing table information in a TableView and is contingent upon 
 * getting the proper MetaData for column names, length, and observable list elements. The TableView can 
 * populate any query where a ResultSet was returned and will grow or shrink to the data. The models DataType 
 * that was generated also needs to be in some type of array as all elements are collected through loops like
 * 'while', 'do while' and mainly 'for'. The purpose of this class and others like ResultSetMonitor or 
 * especially MaintainRecords was to utilize reusable components for less redundancy while compacting the code.
 * 
 * Please note that the table view, when rendered, still has functionality to sort each row by the column name header.
 * 
 * @author dkell40@wgu.edu
*/
public class SimpleTableController implements Controller{

    @FXML
    private VBox vbox;

    @FXML
    private TableView<ModelDataType> table;

    private static MenuItem menuDelete;
    private static MenuItem menuModify;
    public Boolean isGloballyDeleting;
    private TableColumn<ModelDataType, String>[] collumns;
    private Integer colLength;
    private ResultSetMonitor resultSetMonitor;
    private Stage stage;

    /** 
     * Default Constructor 
     */
    public void maintain(){
        ApplicationController.getMainApplication().maintainRecords(this, getModel().get(0));
    }

    /** 
     * based off the table name in the resultSet MetaData, the method
     * will either show a message stating 'cannot delete' or delete the record.
     */
    public void switchAction(){
        String message = "Cannot delete "+resultSetMonitor.getTableName()+" record in this area";
        switch(resultSetMonitor.getTableName()){
        case "appointment" : delete();break;
        case "customer" : delete();break;
        default : ApplicationController.showAlert(message);break;
        } 
    }

    /** 
     * Determines how to delete the record from the selection model.
     */
    private boolean delete(){
        return deleteSQLRecord(resultSetMonitor
                .getTableName(), getDatabaseId())? deleteTableLRecord() : false;
    }

    /** Removes a record from the list and refreshed the application to reflect the changes.
     * @return success the success of the list removal.
     */
    private boolean deleteTableLRecord(){
        assert (getModel() != null) : "getModel() is null LINE:58"+ this.getClass().getSimpleName();

        boolean result = !resultSetMonitor.getObservableDataList().remove(getModel());

        setThreads(ApplicationController.getUser());
        ApplicationController.getMainApplication().refreshCalendars(ApplicationController.getUser());
        return result;
    }

    /** 
     * Sets Action and handlers for certain mouse click events for the menu.
     */
    public void setActions(){
        ContextMenu rightClickMenu = new ContextMenu();                   
        menuDelete = new MenuItem("Delete");menuDelete.setOnAction((e) -> {switchAction();});
        menuModify = new MenuItem("Modify");menuModify.setOnAction((e) -> {maintain();});
        rightClickMenu.getItems().addAll(menuDelete, menuModify);

        table.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent t) -> {
            if(t.getButton() == MouseButton.SECONDARY){
                rightClickMenu.show(table , t.getScreenX() , t.getScreenY());
            }
            if(t.getButton().equals(MouseButton.PRIMARY) && t.getClickCount() == 2){
                assert getModel() != null : "getModel() is null LINE: 78 "+ this.getClass().getSimpleName();
                maintain();
            }
        });
        table.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!table.isFocused()){
                table.getSelectionModel().clearSelection();  
            }
        });
    }

    /** 
     * Renders the table populating cells with data from the ResultSetMonitor Observable list and sets 
     * the column names according to the MetaData. The ModelDataType has SimpleObject variables for the
     * Factory class in each cell.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void renderTable(){
        setActions();
        collumns = new TableColumn[colLength];
        for (int i = 0; i < colLength; i++){
            final int f = i;
            collumns[i] = new TableColumn(resultSetMonitor.getColumnHeaderNames()[i]);
            collumns[0].setVisible(false);
            collumns[i].setCellValueFactory(cellData -> cellData.getValue().getSimpleString(f));   
            collumns[i].setMinWidth(75);
        }
        table.getColumns().addAll(collumns);
    }

    /** Sets the size and returns the instance for method chaining.
     * @param height the height to set
     * @param width the width to set
     * @return controller the controller to get
     */
    public SimpleTableController setTableMaxSize(int height, int width){
        table.setMaxHeight(height);table.setMaxWidth(width);
        return this;
    }

    /** Sets the size and returns the instance for method chaining.
     * @param height the height to set
     * @param width the width to set
     * @return controller the controller to get
     */
    public SimpleTableController setTableMinSize(int height, int width){
        table.setMinHeight(height);table.setMinWidth(width);
        return this;
    }

    /** 
     * @return table the table to get  
     */
    public TableView<ModelDataType> getTable() {
        return table;
    }

    /** 
     * @return DatabaseId the DatabaseId to get  
     */
    private Integer getDatabaseId(){
        return new Integer(getModel().get(0));
    }

    /** 
     * @return Model the Model to get  
     */
    private ModelDataType getModel(){
        return table.getSelectionModel().getSelectedItem();
    }

    /** 
     * @return stage the stage to get  
     */
    public Stage getStage() {
        return stage;
    }

    /** 
     * @param stage the stage to set  
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /** 
     * Overloaded initialize method
     */
    public void initialize(){

        colLength = resultSetMonitor.getObservableDataList().size() > 0 
                ? resultSetMonitor.getObservableDataList().get(0).getData().length : 0 ;
                table.setPlaceholder(new Label(""));
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                table.setItems(resultSetMonitor.getObservableDataList());
    }
    
    /** 
     * Refreshes the data in the ResultSet by a re-query and sets the new
     * TableView data.
     */
    @Override
    public void refresh(){
        resultSetMonitor.refresh();
        table.setItems(resultSetMonitor.getObservableDataList());
    }

    /**Initializes instance or static variables.
     * @param URL the URL to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) { 
        vbox.getStylesheets().add(ApplicationController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
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


} 
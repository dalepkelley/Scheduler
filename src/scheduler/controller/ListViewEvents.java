package scheduler.controller;

import static scheduler.Launch.queueReminders;
import static scheduler.model.MaintainRecords.deleteSQLRecord;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import scheduler.Controller;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Class that has more functionality of the ListView class and can filter observableArrayList
 * elements with greater flexibility . Extra functions include reporting more details of the 
 * ListView to a TextArea or Pop-up and can filter data without a re-query of the underlying
 * data model to improve performance.
 * @author dkell40@wgu.edu
 */
public class ListViewEvents implements Controller
{

    private final ObservableList<Container> obsContainerList = FXCollections.observableArrayList();
    private final boolean isUserRecord = ApplicationController.getUser().equals(ApplicationController.getConsultant());
    protected TextArea report;

    private ListView<Container> listView;
    private ResultSetMonitor resultSetMonitor;
    private Boolean isGloballyDeleting;
    private Boolean refresh;
    private ContextMenu rightClickMenu;
    private Stage stage;
    private PopupWindow popup;

    /** Overloaded constructor
     * @return event the event to initialize
     */
    ListViewEvents (TextArea event){ this(event, true); }

    /** Overloaded constructor
     * @return event the event to initialize
     * @return refresh the refresh configuration to initialize
     */
    ListViewEvents (TextArea event, boolean refresh){
        this.report = event != null ? event: new TextArea();
        this.refresh = refresh;
    }

    /** deletes from multiple areas.
     * @return successful removal of the container and ListView objects.
     */
    private boolean deleteListViewRecord(boolean refresh){
        assert ApplicationController.getUserReminderSet() != null 
                : "getUserReminderSet() is null LINE: 57 "+ this.getClass().getSimpleName();
        resultSetMonitor.getObservableDataList().remove(getSelection().getModel());report.clear();
        if(refresh){setThreads(ApplicationController.getUser());
        ApplicationController.getMainApplication().refreshCalendars(ApplicationController.getUser());}
        queueReminders(ApplicationController.getUserReminderSet());
        listView.getItems().forEach(c -> {if(c.getId().toString().equals(getDatabaseId())) listView.getItems().remove(c);});
        return !obsContainerList.remove(getSelection());
    }

    /** Sets the ActionEvent to handle per menu item.
     * @param item the menu item to select
     */
    public EventHandler<ActionEvent> onSelectMenuItem(MenuItem item) {

        return (ActionEvent event) -> {
            switch(item.getText().toLowerCase()){
            case "delete" : setIsGloballyDeleting(deleteSQLRecord(resultSetMonitor
                    .getTableName(), getDatabaseId()) ? deleteListViewRecord(refresh) : false);break;
            case "modify" : ApplicationController.getMainApplication().maintainRecords(this, getModel().get(0));break;
            }
        };
    }

    /** Shows the contents of the menu.
     * @param t the item to set in the Event
     */
    public void menuShow(MouseEvent t){
        rightClickMenu.show(listView , t.getScreenX() , t.getScreenY());
    }

    /** 
     * 
     */
    public void editRecord(){
        if(isUserRecord && getSelection() != null)
            ApplicationController.getMainApplication().maintainRecords(this, getModel().get(0));

    }

    /** gets details from a container object.
     * @return reportDetails for the selection
     */
    public String report(){
        final Container[] container = new Container[1];
        container[0] = (Container)getSelection();
        if(listView.getSelectionModel().getSelectedIndex() != -1 && container[0] != null){
            return reportDetailsForEach(container[0]).getText();
        }
        return null; 
    }

    /** Fires the actions to set eternally.
     * @return this the instance to get for method chaining
     */
    public ListViewEvents renderListView(){
        setActions();
        return this;
    }

    /** Filters the ListView with all objects intact.
     * @return this the instance to get for method chaining
     */
    public ListViewEvents filterBy(BiFunction<ModelDataType,ObservableList<Container>,Boolean> function){    
        resultSetMonitor.getObservableDataList().forEach((model) -> {
            function.apply(model,obsContainerList);   
        });
        return this;
    }

    /** 
     * Sets the menu item action when an event property has occurred.
     */
    public void setActions(){
        rightClickMenu = new ContextMenu();
        listView = new ListView<>(obsContainerList);

        MenuItem menuDelete = new MenuItem("Delete");menuDelete.setOnAction(onSelectMenuItem(menuDelete));
        MenuItem menuModify = new MenuItem("Modify");menuModify.setOnAction(onSelectMenuItem(menuModify));
        rightClickMenu.getItems().addAll(menuDelete, menuModify);

        listView.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent t) -> {
            if(t.getButton() == MouseButton.SECONDARY){
                if(isUserRecord)menuShow(t);
            }
            if(t.getButton().equals(MouseButton.PRIMARY) && t.getClickCount() == 2){
                if(isUserRecord)editRecord();
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
            report();
            if(popup != null)popup.hide();
            if(getSelection() != null)
                showPopup(listView, o.getValue().getModel(), getSelectionId());

        });

        listView.getFocusModel().focusedItemProperty().addListener((observable, oldValue, newValue) -> {
            listView.getSelectionModel().select(listView.getFocusModel().getFocusedItem());

        });

        listView.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!listView.isFocused()){
                if(listView.getSelectionModel().isSelected(-1) || listView.getSelectionModel().isEmpty()) {
                } else {
                    listView.getSelectionModel().select(-1);
                }
            }
        });  

    }

    /** Sets the menu item action when an event property has occurred for 
     * the pop-up window.
     * @param node the node to set
     * @param model the model to set
     * @param id the id to set
     * @return pop-up the window to show
     */
    public PopupWindow showPopup(Node node, ModelDataType model, int id){

        Function<Tooltip, Tooltip> tool = (t) -> {t.setMinWidth(300);
        t.setMaxWidth(300);t.setWrapText(true);return t;};
        Function<PopupWindow, PopupWindow> window = (p) -> {return p;};
        Tooltip tooltip = tool.apply(new Tooltip());
        popup = window.apply(tooltip);


        node.setOnMouseClicked((MouseEvent mouseEvent) -> {
            if(!popup.isShowing()){
                tooltip.setText(report());
                if(!tooltip.getText().equals(""))
                    popup.show(node, mouseEvent.getScreenX()-popup.getWidth()/2, mouseEvent.getScreenY()-popup.getHeight());
                popup.autoFixProperty();
            }
        });

        node.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!node.isFocused() || getSelectionId() != id)
                popup.hide(); 
        });  

        return popup;

    }

    /** Forces a selection based on a model that was given externally.
     * @param model the model to set
     */
    public void selectModel(ModelDataType model){
        final Container[] container = new Container[1];
        if(model != null)
            obsContainerList.forEach((c) -> { 
                container[0] = c.getModel().equals(model) ? container[0] = c: container[0] ;
                listView.getSelectionModel().select(c);
            });
        else
            listView.getSelectionModel().select(0);
    }

    /** Based on the contents of the container text, the details are
     * then inserted into a TextArea for viewing.
     * @param container the container to set
     * @return textArea the textArea that has the details of the report
     */
    public TextArea reportDetailsForEach(Container container){
        report.clear();
        SimpleStringProperty[] data = container.getModel().getData();
        String cell;
        for (int i = 1; i < data.length; i++) {
            cell = leftJustify(data[i].get(), (data[i].length().intValue()+1));
            report.appendText(cell+"\n");
        }
        report.appendText("\n");
        return report;

    }

    /** Depending on the spaces in the line, the text is formatted 
     * for visual purposes.
      * @param s the s to set
      * @param n the n to set
     * @return string the parsed string to get
     */
    public static String leftJustify(String s, int n) {
        if (s.length() <= n) n++;
        return String.format("%1$-" + n + "s", s);
    }

    /** 
     * @return listView the listView to get
     */
    public ListView<Container> getListView() {
        return listView;
    }

    /** 
     * @param listView the listView to set
     */
    public void setListView(ListView<Container> listView) {
        this.listView = listView;
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
     * @return id the sql id to get
     */
    private Integer getDatabaseId(){
        return new Integer(getSelection().getModel().get(0));
    }

    /** 
     * @return index the Integer to get 
     */
    private Integer getSelectionId(){
        return listView.getSelectionModel().getSelectedIndex();
    }

    /** 
     * @return container the container to get 
     */
    private Container getSelection(){
        return listView.getSelectionModel().getSelectedItem();
    }

    /** 
     * @return model the ModelDataType to get
     */
    private ModelDataType getModel(){
        return getSelection().getModel();
    }

    /** 
     * @return report the TextArea to get
     */
    public TextArea getReport() {
        return report;
    }

    /**
     * @param resultSetMonitor - oriented class with Meta-data, Model data, Observable list
     * and SQL commands in a Monitor wrapper.
     * @return the instance of this class
     */
    public ListViewEvents getListView(ResultSetMonitor resultSetMonitor) {
        this.resultSetMonitor = resultSetMonitor;
        return this;
    }

    /**
     * @return the isGloballyDeleting
     */
    public Boolean getIsGloballyDeleting() {
        return isGloballyDeleting;
    }

    /**
     * @param isGloballyDeleting the isGloballyDeleting to set
     */
    public void setIsGloballyDeleting(Boolean isGloballyDeleting) {
        this.isGloballyDeleting = isGloballyDeleting;
    }

    /**
     * @return resultSetMonitor the resultSetMonitor to get
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return resultSetMonitor;
    }

    /**
     * @param isGloballyDeleting the isGloballyDeleting to set
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetMonitor) {
        this.resultSetMonitor = resultSetMonitor;
    }

    /**
     * Refreshes the ResultSet by sending the same query internally
     */
    @Override
    public void refresh(){
        resultSetMonitor.refresh();
    }

    /**
     * Overrides from Application class
     * @param location the location to set
     * @param resources the resources to set
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        report.getStylesheets().add(ApplicationController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
    }


}
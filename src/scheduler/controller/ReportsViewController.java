package scheduler.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import scheduler.Controller;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;

public class ReportsViewController extends CalendarView implements Controller{

    @FXML
    private VBox root;

    @FXML
    private Label user;
    


    @FXML
    private CheckBox type;

    @FXML
    private CheckBox reminders;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private MenuButton userCalendar;
    
    @FXML
    private PieChart pieChart;

    private ObservableList<PieChart.Data> pie;
    private ArrayList<String> consultants;
    private ResultSetMonitor scheduleSet;
    private ResultSetMonitor locationSet;
    private Map<String, Double> locationData;
    private Label consultant = new Label();

    
    /** 
     * @param scheduleSet the scheduleSet to get
     * @return scheduleSet the scheduleSet to get  
     */
    public void setConsultants(){
        consultants = new ArrayList<String>();

        scheduleSet.getObservableDataList().forEach((model) -> { 
            if(!consultants.contains(model.get(1)))
                consultants.add(model.get(1));
        });

        consultants.forEach((person) -> { 
            MenuItem item = new MenuItem(person);
            item.setOnAction(setActionEvents(item));
            userCalendar.getItems().add(item) ; 

        });
    }

    /** Parses the data to a readable array for the chart.
     * @param locationSet the locationSet to get 
     */
    public void setLocations(ResultSetMonitor locationSet){
        assert locationSet != null : "locationSet is null LINE: 102 "+ getClass().getSimpleName();

        pieChart.setAnimated(true);

        this.locationSet =locationSet;
        locationData = new HashMap<String, Double>();

        this.locationSet.getObservableDataList().forEach( m -> {
            locationData.put(m.get(2), new Double(m.get(3)));
        });

        setData(locationData);
    }

    /** Parses the data to a readable array for the Pie chart.
     * @param map the map to get 
     */
    public void setData(Map<String, Double> map) {
        if(pie == null)
            pie = FXCollections.observableArrayList();
        else
            map.forEach((key, value) -> pie.remove(key));

        map.forEach((key, value) -> pie.add(new PieChart.Data(key, value)));
        if(!pieChart.getData().isEmpty())
            pieChart.getData().clear();
        pieChart.setData(pie);
        pieChart.setAnimated(false);

    }

    /** This method will run all the necessary query threads and set the 
     * consultant for each accordingly.
     * @param consultant the consultant to get 
     * @return EventHandler the EventHandler to get  
     */
    private EventHandler<ActionEvent> setActionEvents(MenuItem consultant){
        return (ActionEvent event) -> {
            setThreads(consultant.getText());

            ApplicationController.setConsultant(consultant.getText());
            
            this.consultant.setText(consultant.getText());

            assert ApplicationController.getMainApplication() != null 
                    : "getMainApplication() is null LINE: 136 "+getClass().getSimpleName();

            join(ApplicationController.getApptThread());
            ApplicationController.setApptThread(null);
            ApplicationController.refresh(consultant.getText());

        };

    }
    
    /** 
     * @return scheduleSet the scheduleSet to get
     */
    public ResultSetMonitor getResultSetModel() {
        return scheduleSet;
    }

    /** 
     * @param  scheduleSet the scheduleSet to set
     */
    public void setManager(ResultSetMonitor resultSetModel) {
        this.scheduleSet = resultSetModel;
    }

    /** 
     * @param consultant the consultant to get 
     */
    public void setUser(String consultant) {
        this.consultant.setText(consultant);
    }
    
    /** Sets the user upon initialization.
     * @param location the location to get 
     * @param resources the resources to get 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.user.setText(ApplicationController.getUser());
        this.consultant.setText(ApplicationController.getConsultant());
    }  
    
    /** 
     * Inherited from controller class.
     */
    @Override
    public void refresh(){
        initialize(null, null);
    }

    /** 
     * @return locationSet the locationSet to get  
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return locationSet;
    }

    /** 
     * @param resultSetModel the locationSet to get 
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel) {
        this.locationSet = resultSetModel;
    }

}


class TypesPerMonth {

    ResultSetMonitor resultSetMonitor;
    VBox vbox;
    /**
     * @return VBox the VBox to get
     */
    public VBox getVbox() {
        return vbox;
    }

    /** 
     * Default constructor
     */
    public TypesPerMonth(){
    }
    
    /** 
     * Overloaded constructor
     * @param resultSetMonitor the resultSetMonitor to set 
     */
    public TypesPerMonth(ResultSetMonitor resultSetMonitor){
        this.resultSetMonitor = resultSetMonitor;
        sort(2);
    }

    /** Creates a new Legend from a bar chart and only displays the Legend.
     * @return VBox the chart to get  
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public VBox render() {

        CategoryAxis xAxis    = new CategoryAxis();
        xAxis.setLabel("# Appt Types");
        NumberAxis yAxis = new NumberAxis();

        StackedBarChart chart = new StackedBarChart(xAxis, yAxis);

        
        chart.setLegendSide(Side.BOTTOM);

        Node legend = chart.getChildrenUnmodifiable().get(2);


        for (ModelDataType model : resultSetMonitor.getObservableDataList())
        {
            XYChart.Series dataSeries1 = new XYChart.Series();
            dataSeries1.setName((model.get(2) + " " + model.get(3)));
            LocalDate date = LocalDate.parse(model.getDateStr(4));
            String month = date.getMonth().name();
            Integer year = date.getYear();

            dataSeries1.getData().add(new XYChart.Data(month + " " + year, new Integer(model.get(3))));
            chart.getData().add(dataSeries1);

        }
        Text text = new Text("Appointment Types - Monthly");
        
        vbox = new VBox(text, legend);
        vbox.setMaxHeight(800);
        vbox.setMaxWidth(200);
        return vbox;
    }

    /** Sorts an Iterator list.
     * @param fieldIndex the fieldIndex to get 
     */
    public void sort(int fieldIndex) {
        resultSetMonitor.getObservableDataList().sort((a, b) -> a.get(fieldIndex).compareTo(b.get(fieldIndex)));  
    }
    

}
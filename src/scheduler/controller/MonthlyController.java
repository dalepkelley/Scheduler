package scheduler.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import scheduler.Controller;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Extension of the CalendarView class and has functionality to set the monthly
 * view according to the ResultSet. It is straight forward and synchronizes with 
 * WeeklyView which also extends CalendarView controller.
 * @author dkell40@wgu.edu
*/
public class MonthlyController extends CalendarView{


    @FXML
    private VBox rootCalendar;
    @FXML
    public VBox weeklyVbox;
    @FXML
    private GridPane calendar;
    @FXML
    private GridPane calendarLabel;
    @FXML
    private Label monthYearTitle;

    private ArrayList<AnchorPane> calendarArray = new ArrayList<>(42);
    private int current;
    private ResultSetMonitor resultSetMonitor;
    private boolean isLoaded;
    private WeeklyView weeklyController;
    private LocalDate last;

    /** Propagates down one calendar view month.
     * @param event the event to set
     */
    @FXML
    private void decrementView(ActionEvent event)  {
        setGlobalDate(getGlobalDate().minusMonths(1));refresh();
    } 

    /** Propagates up one calendar view month.
     * @param event the event to set
     */
    @FXML
    private void incrementView(ActionEvent event)  {
        setGlobalDate(getGlobalDate().plusMonths(1));refresh();
    }

    /** 
     * Refreshes the monthly view from the FXML standpoint when switching Tabs.
     */
    @FXML
    public void setMonthly(){
        refresh();
    }
    
    /** 
     * Refreshes the weekly view from the FXML standpoint when switching Tabs.
     */
    @FXML
    public void setWeekly(){
        weeklyController.refresh();
    }
    
    /** Draws the calendar events, creates new instances and adds all relevant
     * calendar data for the view.
     * @param today the today to set
     */
    public void renderCalendar(LocalDate today)  {
        consultant.setText(ApplicationController.getConsultant());
        last = today;
        LocalDate date = LocalDate.of(today.getYear(), today.getMonthValue(), 1);

        while (!date.getDayOfWeek().toString().equals("SUNDAY") ) {
            date = date.minusDays(1);
        }

        for (AnchorPane thisDay : calendarArray) {
            final LocalDate finalDate = date;
            Text dayNumber = new Text(String.valueOf(date.getDayOfMonth()));
            Integer thisMonth = new Integer(date.getMonthValue());
            remove(thisDay);anchor(dayNumber, 0.0, 0.0);

            if(!(thisMonth < current || thisMonth > current)){
                thisDay.getChildren().add(dayNumber);

                ListViewEvents dayCell = new ListViewEvents(null)
                        .getListView(resultSetMonitor);
                dayCell.filterBy((m, c) -> { 

                    if (m.getDateStr(START).contains(finalDate.toString()  )) {
                        c.add(new Container(m.getId() , m.getTime(START).format(
                                amPmformat)+" - "+m.get(3), m));
                    }
                    return !c.isEmpty(); 
                });
                dayCell.renderListView().getListView().setMaxSize(110, 70);

                for(ModelDataType model : resultSetMonitor.getObservableDataList()){

                    if(model.getDateTimeStr(START).contains(date.toString())){

                        anchor(dayCell.getListView(), 13.0, 1.0);
                        addTo(thisDay, dayCell.getListView());break;
                    }
                }
            }
            date = date.plusDays(1);
        }
        monthYearTitle.setText(today.getMonth()+" "+today.getYear());
        isLoaded = true;
    }

    /** Configuration for pane.
     * @param  child the child to set
     * @param  top the top to set
     * @param  left the left to set
     */
    private void anchor(Node child, Double top, Double left) {
        AnchorPane.setTopAnchor(child, top);AnchorPane.setLeftAnchor(child, left);
    }

    /** Refreshes the pane.
     * @param pane the pane to set
     */
    private void remove(AnchorPane pane){
        if(!pane.getChildren().isEmpty())
            pane.getChildren().removeAll(pane.getChildren()); 
    }

    /** Adds a pane to calendar.
     * @param  pane the pane to set
     * @param  node the node to set
     */
    private void addTo(AnchorPane pane, ListView<Container> node){
        pane.getChildren().add(node);
    }


    /** 
     * @return calendarArray the calendarArray to set  
     */
    public ArrayList<AnchorPane> getDaysOfCalendar() {
        return calendarArray;
    }

    /** 
     * @param  daysOfCalendar the calendarArray to set
     */
    public void setDaysOfCalendar(ArrayList<AnchorPane> daysOfCalendar) {
        this.calendarArray = daysOfCalendar;
    }

    /**
     * @param weeklyController the weeklyController to set
     */
    public void setWeeklyController(WeeklyView weeklyController) {
        this.weeklyController = weeklyController;
    }
    
    /**
     * @return resultSetModel the resultSetModel to get
     */
    @Override
    public ResultSetMonitor getResultSetMonitor() {
        return resultSetMonitor;
    }

    /**
     * @param resultSetModel the resultSetModel to set
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel) {
        this.resultSetMonitor = resultSetModel;
    }

    /**
     * Refreshes the calendar with a new current value.
     */
    @Override
    public void refresh(){

        if(isLoaded){
            current = getGlobalDate().getMonthValue();
            checkNewMonth(last);
            renderCalendar(getGlobalDate());
        }
    }

    /**Initializes instance or static variables and sets the calendar objects.
     * @param URL the URL to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int week = 0; week < 6; week++) {
            for (int days = 0; days < 7; days++) {
                AnchorPane day = new AnchorPane();
                calendar.add(day,days,week);
                day.getStyleClass().add("calendar-box-style");
                calendarArray.add(day);
            }
        }
        current = getGlobalDate().getMonthValue();

    }

}

/** 
 * 
 */
abstract class CalendarView  implements Controller{
    @FXML
    protected Label consultant;
    
    public static final int START = 1;
    public static final int END = 2;
    private static LocalDate globalDate = LocalDate.now();

    /** 
     * @param  location the location to set
     * @param  resources the resources to set 
     */
    public abstract void initialize(URL location, ResourceBundle resources);

    /** This method will show each type of appointment per month but is rendered after
     * the calendar has been shown. This is only a split second and is not noticeable to 
     * the end user. all reporting data is set in the ReportsViewController.
     * @param  currentYearMonth the currentYearMonth to set
     */
    public void checkNewMonth(LocalDate currentYearMonth){

        if (currentYearMonth.getMonth().getValue() != globalDate.getMonth().getValue()){

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("CalendarView-loadTypesPerMonth()");
                    ApplicationController.getMainApplication().loadTypesPerMonth();
                }
            });

        }
    }

    /**
     * @return globalDate the globalDate to get
     */
    public static synchronized LocalDate getGlobalDate() {
        return globalDate;
    }
    /**
     * @param globalDate the globalDate to set
     */
    public static synchronized void setGlobalDate(LocalDate globalDate) {
        CalendarView.globalDate = globalDate;
    }



}
package scheduler.controller;


import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Extension of the CalendarView class and has functionality to set the weekly
 * view according to the ResultSet. It is straight forward and synchronizes with 
 * MonthlyView which also extends CalendarView controller.
 * @author dkell40@wgu.edu
*/
public class WeeklyView extends CalendarView{

    @FXML
    private VBox root;  
    @FXML
    private VBox weeklyVbox;
    @FXML
    private GridPane header;
    @FXML
    private GridPane weekdays;
    @FXML
    private Label monthYearTitle;

    private GridPane weeklyCalendar;
    private ScrollPane scroller;
    private ResultSetMonitor resultSetMonitor;
    private String cssColor;
    private boolean isLoaded;
    private LocalDate last;
    
    
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

    /** Propagates down one calendar view week.
     * @param event the event to set
     */
    @FXML
    private void decrementWeek(ActionEvent event) {
        setGlobalDate(getGlobalDate().minusWeeks(1));refresh();
    } 

    /** Propagates up one calendar view week.
     * @param event the event to set
     */
    @FXML
    private void incrementWeek(ActionEvent event) {
        setGlobalDate(getGlobalDate().plusWeeks(1)); refresh();
    }
    
    /** Filters the list by model object.
     * @param event the event to set
     * @param model the model to set
     */
    private void filterView(ListViewEvents event, ModelDataType model){
        event.filterBy((m, c) -> { 
            if (m.equals(model) ){ 
                c.add(new Container(m.getId() , m.get(4), m));
            }
            return !c.isEmpty(); 
        });
    }

    /** This will render the calendar and add day ListViews to certain panes 
     * where a criteria has been met with the matching of data DateTime values and day
     * cell propagation. It basically sorts the corresponding records to their respective 
     * dates and time frames.
     * @param today the today to set 
     */
    public void renderCalendar(LocalDate today){
        consultant.setText(ApplicationController.getConsultant());
        last = today;
    	clear();weeklyCalendar = new GridPane();weeklyCalendar.snappedRightInset();
    	scroller = new ScrollPane(weeklyCalendar);
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1) ;
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        for (LocalDate date = startOfWeek; ! date.isAfter(endOfWeek); date = date.plusDays(1)) {

            int slotIndex = 1 ;
            for (LocalDateTime datAtTime = date.atTime(firstStartTime); 
                    ! datAtTime.isAfter(date.atTime(lastEndTime));datAtTime = datAtTime.plus(fifteenMinuteIncrement)) {
                final LocalDateTime thisDayAtTime = datAtTime;
                final int finalSlot = slotIndex;
                
                
                DayCell day = new DayCell(thisDayAtTime, fifteenMinuteIncrement, null, cssColor);
                weeklyCalendar.add(day.getView(), day.getDayOfWeek().getValue(), slotIndex);
                resultSetMonitor.getObservableDataList().forEach((model) -> {

                    if(isOnSchedule(model, thisDayAtTime)){
                    	
                        ListViewEvents listView = new ListViewEvents(null).getListView(resultSetMonitor);
                        runThread("Weekly-filter()", () -> { filterView(listView, model);listView.renderListView().getListView(); });
                        day.getView().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, true);
                        day.setListView(listView);
                        
                        
                        if(isOnScheduledTime(thisDayAtTime, model.getTime(START))){
                            Text text = new Text(" "+model.get(4));
                            text.getStyleClass().addAll("default");
                            
                            runThread("THREAD: Weekly-day.showPopupTooltip()", () -> 
                            { day.showPopupTooltip(text, model); });

                            GridPane.setHalignment(text, HPos.CENTER);
                            weeklyCalendar.add(text,  day.getDayOfWeek().getValue(), finalSlot);
                        }
                    }else{
                        
                    }
                });

                Label time = datAtTime.getMinute() >= 15 && datAtTime.getMinute() <= 45 
                        ? new Label("") : new Label(datAtTime.format(amPmformat));
                time.getStyleClass().add("time-cell");
                GridPane.setHalignment(time, HPos.RIGHT);
                weeklyCalendar.add(time, 0, slotIndex);
                slotIndex++ ;
            }
   
            Text dayNumber = new Text(date.format(dayFormatter));
            GridPane.setHalignment(dayNumber, HPos.CENTER);
            weekdays.add(dayNumber, date.getDayOfWeek().getValue(), 0);

        }
        weeklyVbox.getChildren().add(scroller);
        monthYearTitle.setText(today.getMonth()+" "+today.getYear());
        isLoaded = true;
    }

    /** Checks to see whether the record falls within a given criteria.s
     * @param model the model to set 
     * @param localdateTime the localdateTime to set 
     * @return onScheduleSuccess the onScheduleSuccess to get  
     */
    public boolean isOnSchedule(ModelDataType model, LocalDateTime localdateTime){
    	return model.getDate(START).equals(localdateTime.toLocalDate())  
    			&& isBetween(localdateTime, model.getTime(START), model.getTime(END));
    }
    
    /** Checks to see whether the record falls within a given criteria.s
     * @param thisDayAtTime the thisDayAtTime to set 
     * @param start the start to set 
     * @return onScheduleSuccess the onScheduleSuccess to get  
     */
    public boolean isOnScheduledTime(LocalDateTime thisDayAtTime, LocalTime start){
        LocalTime thisTime =  thisDayAtTime.toLocalTime();
        return thisTime.equals(start);
    }
    
    /** 
     * @param cssColor the cssColor to set  
     */
    public void setCssColor(String cssColor) {
        this.cssColor = cssColor;
    }

    /** 
     * Clears all entries in the calendar day cells.
     */
    private void clear(){
        weeklyVbox.getChildren().clear();weekdays.getChildren().clear();
    }
    
    /** 
     * Moves the calendar position according to the specified date
     */
    @Override
    public void refresh(){
        if(isLoaded)checkNewMonth(last);
        renderCalendar(getGlobalDate());  
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
    public void setResultSetMonitor(ResultSetMonitor resultSetMonitor) {
        this.resultSetMonitor = resultSetMonitor;
    }

    /**Initializes instance or static variables.
     * @param URL the URL to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {       
        
    }


}
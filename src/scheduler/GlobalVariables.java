package scheduler;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import scheduler.controller.ApplicationController;
import scheduler.controller.ListViewEvents;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Interface with DateTimeHelper variables, methods and then some.
*
* @author dkell40@wgu.edu
*/
public interface GlobalVariables extends DateTimeHelper{
    /**
     * Stage field variable global to most classes in the application.
     */
    Stage appStage  = ApplicationController.getAppStage();

    /**
     * @param thread - performs a Thread join and waits for that thread to complete
     */
    public default void join(Thread thread){
        if(thread != null && thread.isAlive())
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }
    
    /** THis runs re-occurring threads necessary for performance.
     * @param consultant is passed to each query in the threads.
     */
    public default void setThreads(String consultant){
        ApplicationController.setAppointmentSet(null);
        runThread("GlobalVariables-queryAppointment()", () -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            ApplicationController.setApptThread(Thread.currentThread());
            MaintainRecords.queryAppointment(consultant);
        });

        ApplicationController.setRegionalSet(null);
        runThread("GlobalVariables-queryLocationStatistics()", () -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            ApplicationController.setLocationThread(Thread.currentThread());
            MaintainRecords.queryRegionalStatistics(consultant);
        });

    }
    /** Method to be used with Lambda as a parameter
     * @param name - name of thread for debug.
     * @param task - predefined job  before run() or start() is invoked
     */
    public default void runThread(String name, Runnable task){
        Thread thread = new Thread(task);
        thread.setName(name);
        thread.start();

    }
    /** Out-dated technique for waiting on resources with
     * multiple threads.
     */
    public default void poll(){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

/** Interface date and time static variables that are global to the application.
* Includes static and default methods to parse and validate Java DateTime objects and strings.
* @author dkell40@wgu.edu
*/
interface DateTimeHelper{

    /**
     * Field variables global to most classes in the application.
     */
    DateTimeFormatter militaryFormat = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter amPmformat = DateTimeFormatter.ofPattern("h:mm a");
    DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MMM d");
    DateTimeFormatter slashFormatter = DateTimeFormatter.ofPattern("M-d-yyyy");
    LocalTime firstStartTime = LocalTime.parse((LocalTime.of(7, 0)).toString(), militaryFormat);
    LocalTime start = LocalTime.parse((LocalTime.of(5, 0)).toString());
    Duration fifteenMinuteIncrement = Duration.ofMinutes(15);
    LocalTime lastEndTime  = LocalTime.parse((LocalTime.of(23, 30)).toString(), militaryFormat);
    LocalTime end  = LocalTime.parse((LocalTime.of(23, 30)).toString());
    String SERVER_TIMESTAMP_NOW_UTC = null;
    String ACTIVE = "1";

    /**
     * @param dateTime parses String date/time data into LocalDateTime format
     */
    public default LocalDateTime parseSQLDateTime(String dateTime){
        String newTime = dateTime.replace(' ', 'T');
        switch(dateTime.length()){
        case 19 : return LocalDateTime.parse(newTime.substring(0, 19));
        default : return LocalDateTime.parse(newTime);
        }
    }

    /**
     * @param date parses String date/time data into UTC LocalDateTime format
     */
    public default String setUTC(String date){
        LocalDateTime input = parseSQLDateTime(date);
        return input.atZone(ZoneOffset.systemDefault()).withZoneSameInstant
                (ZoneId.of("UTC")).toLocalDateTime().toString();
    }

    /**
     * @param dateTime parses String date/time into a sub-string format
     */
    public default String parseSQLDate(String dateTime){
        return dateTime.substring(0, 7);
    }

    /** Checks whether a particular instance of a LocaldateTime value is within start/end times.
     * @param thisDayAtTime 
     * @param start 
     * @param end 
     */
    public default boolean isBetween(LocalDateTime thisDayAtTime, LocalTime start, LocalTime end ){
        LocalTime thisTime =  thisDayAtTime.toLocalTime();
        return thisTime.equals(start) || (thisTime.isAfter(start) && thisTime.isBefore(end));
    }

    /** This class is used in the Weekly calendar class to create slices of the
     * calendar and includes the model wrapped in the List View for ease of data access.
     * 
    * @author dkell40@wgu.edu
    */
    public static class DayCell {

        private ListViewEvents event;
        private final LocalDateTime start ;
        private final Region view ;
        private ContextMenu rightClickMenu;

        /** 
         * DayCell constructor, creates a fraction of the Weekly calendar
         */
        public DayCell(LocalDateTime start, Duration duration, ListViewEvents event, String color) {
            this.start = start ;
            view = new Region();
            view.setMinSize(100, 10);
            rightClickMenu = new ContextMenu();
            view.getStyleClass().addAll("weekly-view", color);           
            this.event = event;

        }

        /**
         * @return the event
         */
        public ListViewEvents getListView() {
            return event;
        }
        /**
         * @param listView the listView to set
         */
        public void setListView(ListViewEvents listView) {
            this.event = listView;setHandlers();
        }
        /**
         * @return the DayOfWeek
         */
        public DayOfWeek getDayOfWeek() {
            return start.getDayOfWeek() ;
        }
        /**
         * @return the view
         */
        public Node getView() {
            return view;
        }

        /**
         * @param color the color to set for each Region in the weekly calendar.
         * @param h the height to set for each Region in the weekly calendar.
         * @param w the width to set for each Region in the weekly calendar.
         */
        public void setColor(String color, int h, int w){
            view.setMaxSize(h, w);
            view.getStyleClass().forEach( o -> {  view.getStyleClass().remove(o);});
            view.getStyleClass().addAll("weekly-view", color);
        }

        /**
         * All handlers for mouse click events such as Left click, right click
         * , double click and mouse-over are handled here.
         * This works in conjunction with ListViewEvents class methods.
         */
        private void setHandlers(){

            final boolean result = ApplicationController.getUser().equals(ApplicationController.getConsultant());
            view.addEventHandler(MouseEvent.MOUSE_CLICKED, (final MouseEvent t) -> {

                if(event != null && t.getButton() == MouseButton.SECONDARY){
                    event.selectModel(null);event.report();
                    if(result)rightClickMenu.show( view , t.getScreenX() , t.getScreenY());
                }
                if(event != null && t.getButton().equals(MouseButton.PRIMARY) && t.getClickCount() == 2){
                    event.editRecord();
                }
                if(event != null && t.getButton().equals(MouseButton.PRIMARY) && t.getClickCount() == 1){
                    event.selectModel(null);event.report();
                }
            });

            showPopupTooltip(view, null);

            MenuItem menuDelete = new MenuItem("Delete");menuDelete.setOnAction(event.onSelectMenuItem(menuDelete));
            MenuItem menuModify = new MenuItem("Modify");menuModify.setOnAction(event.onSelectMenuItem(menuModify));
            rightClickMenu.getItems().addAll(menuDelete, menuModify);
        }

        /** Enables a Popup subclass of Tooltip for calendar mouse events.
         * @param node the node to set
         * @param model the model to set
         */
        public void showPopupTooltip(Node node, ModelDataType model){  

            Function<Tooltip, Tooltip> tool = (t) -> {t.getStyleClass().add("calendar-box-style");
            t.setMinWidth(300);t.setMaxWidth(300);t.setWrapText(true);return t;};
            Function<PopupWindow, PopupWindow> window = (t) -> {return t;};
            Tooltip tooltip = tool.apply(new Tooltip());
            PopupWindow popup = window.apply(tooltip);

            node.setOnMouseMoved((MouseEvent mouseEvent) -> {
                if(!popup.isShowing()){
                    event.selectModel(model);tooltip.setText(event.report());

                    popup.show(node, mouseEvent.getScreenX()-popup.getWidth()/2, mouseEvent.getScreenY()-popup.getHeight());
                    popup.autoFixProperty();
                }
            });  
            node.setOnMouseExited((MouseEvent event) -> {
                popup.hide();
            });
        }


    }
}

package scheduler.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import scheduler.Controller;
import scheduler.controller.ApplicationController;
import scheduler.controller.ComboBoxContainer.Container;
import scheduler.model.ResultSetMonitor.ModelDataType;

public abstract class ExceptionControls implements Controller{
    
    ResourceBundle language = ApplicationController.getResources();
    

    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String checkInvalidOrNull (String fieldName, String input)  throws IllegalArgumentException{
        
        assert input == null: fieldName+" null";
        assert input.equals(""): fieldName + " " + language.getString("beEmpty");
        if(input.equals(""))
            throw new IllegalArgumentException(fieldName + " " + language.getString("beEmpty"));
        if(input.equals(" "))
            throw new IllegalArgumentException(fieldName + " " + language.getString("passNoSpaces"));
        input = checkInvalid(fieldName, input);
        return input;
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String checkInvalid (String fieldName, String input)  throws IllegalArgumentException{

        assert input == null: fieldName+" null";
        Character[] invalidSQLinject = {'*','?','=','#','$'
                ,'%', '@', '(', ')', '{','}',';'};
        String message = fieldName + " " + language.getString("noContain");
        
        for (int i=0;i<invalidSQLinject.length-1;i++){
            if(input.contains(invalidSQLinject[i].toString()) )
                throw new IllegalArgumentException(message + invalidSQLinject[i]);    
        }
        return input;
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String checkInvalidOrNullURL (String fieldName, String input)  throws IllegalArgumentException{
        if(input == null || input.equals(""))
            return""; //can be empty and not a required field
        try{
            URL url = new URL(input);
            return url.toExternalForm();
        }catch (MalformedURLException e) {
            throw new IllegalArgumentException(language.getString("invalid") +fieldName + ". " + e.getMessage());
        }
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validate(String name, String boxName, DatePicker picker, ComboBox<Container> box)  throws IllegalArgumentException{
        assert picker.getValue() != null: name+" null";
        assert box.getValue() != null: boxName+" null";
        return validateBusinessDate(validateNullDatePicker(name,picker))
                +" "+validateBusinessHours(validateNullComboBox(boxName,box));
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validateNullDatePicker(String fieldName, DatePicker picker){
        assert picker == null: fieldName+" null";
        if(picker.getValue() == null)
            throw new IllegalArgumentException(language.getString("validateNullComboBox") + " "+fieldName); 
        return picker.getValue().toString();
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validateNullComboBox(String fieldName, ComboBox<Container> box)  throws IllegalArgumentException{
        assert box == null: fieldName+" null";
        if(box.getSelectionModel().isEmpty())
            throw new IllegalArgumentException(fieldName + " " + language.getString("validateNullDatePicker")); 
        return box.getSelectionModel().getSelectedItem().getId().toString();
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String getText(ComboBox<Container> box)  throws IllegalArgumentException{
        assert box == null: "box null";
        return box.getSelectionModel().getSelectedItem().getId().toString();
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String getDescription(ComboBox<Container> box)  throws IllegalArgumentException{
        assert box == null: "box null";
        return box.getSelectionModel().getSelectedItem().getDescription();
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public boolean isInvalidOrNullPassword (String fieldName, String password) throws IllegalArgumentException{
        assert password == null: fieldName+" null";
        if(password.equals(""))
            throw new IllegalArgumentException(fieldName + " " + language.getString("beEmpty"));
        if ((password.length() < 3)){
            throw new IllegalArgumentException(language.getString("passFourChar"));
        }
        if(password.contains(" ")){
            throw new IllegalArgumentException(language.getString("passNoSpaces"));
        }
        return false;        
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public boolean isInvalidUser (String userName) throws IllegalArgumentException{
        assert userName == null: "User is null";
        boolean result = true;
        String message = language.getString("userNameField");
        switch(userName){
        case "root":throw new IllegalArgumentException(message + userName);
        case "admin":throw new IllegalArgumentException(message + userName);
        case "administrator":throw new IllegalArgumentException(message + userName);
        default : result = false;
        }
        if ((userName.length() < 3)){
            throw new IllegalArgumentException(language.getString("lessThanChar"));
        }
        String res = "";
        for(ModelDataType m : ApplicationController.getUserSet().getObservableDataList())
            if(  (m.get(1).equals(userName)) ) res = userName;
        
        if(res != userName)
        throw new IllegalArgumentException(language.getString("invalidUser"));
        
        return result;      
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validateBusinessHours(String hour)  throws IllegalArgumentException{
        assert hour == null: "hour is null";
        LocalTime businessStart = LocalTime.parse("08:00");
        LocalTime businessEnd = LocalTime.parse("17:00");
        LocalTime time = LocalTime.parse(hour);
        if(time.isBefore(businessStart)|| time.isAfter(businessEnd))
            throw new IllegalArgumentException(language.getString("validateBusinessHours"));
        return hour;
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validateBusinessDate(String date)  throws IllegalArgumentException{
        assert date == null: "date is null";
        LocalDate businessDay = LocalDate.parse(date);
        if(businessDay.getDayOfWeek().name().equals("SATURDAY")
                ||businessDay.getDayOfWeek().name().equals("SUNDAY")) 
            throw new IllegalArgumentException(language.getString("validateBusinessDate"));
        return date;
    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String validateAfter(String start, String end)  throws IllegalArgumentException{
        assert start == null: "start is null";
        assert end == null: "end is null";
    	LocalDateTime startTime = parseSQLDateTime(start);
    	LocalDateTime endTime = parseSQLDateTime(end);
    	if(endTime.toLocalTime().equals(startTime.toLocalTime()) 
    			|| endTime.toLocalTime().isBefore(startTime.toLocalTime()))
    		throw new IllegalArgumentException(language.getString("validateAfter"));
		return end;

    }
    
    /** 
     * @param event the event to set 
     * @return event the event to set  
     */
    public String checkOverlappingAppts(String dateTime, ModelDataType modelToUpdate)  throws IllegalArgumentException{
        assert dateTime == null: "start is null";
        LocalDateTime fields = parseSQLDateTime(dateTime);
        for(ModelDataType model : ApplicationController.getAppointmentSet().getObservableDataList()){
            if (model.equals(modelToUpdate))continue;
            LocalDateTime sqlDateStart = parseSQLDateTime(model.get(1));
            LocalDateTime sqlDateEnd = parseSQLDateTime(model.get(2));
            LocalTime modelStart = sqlDateStart.toLocalTime();
            LocalTime modelEnd = sqlDateEnd.toLocalTime();
            LocalDate modelDate = sqlDateStart.toLocalDate();
            
            if(modelDate.equals(fields.toLocalDate()) && isBetween(fields, modelStart, modelEnd))
                throw new IllegalArgumentException(language.getString("checkOverlappingAppts"));  
        }
        return dateTime;
    }
    

}


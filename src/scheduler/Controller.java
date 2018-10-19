package scheduler;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javafx.fxml.Initializable;
import scheduler.model.ResultSetMonitor;
/** Interface for all Controller type classes and then some.
*
* @author dkell40@wgu.edu
*/
public interface Controller extends Initializable, GlobalVariables{
    
    /**
     * Audit Logger for "AuditLog.txt"
     */
    Logger logger = Logger.getLogger("AuditLog");
    
    /**
     * refreshes both calendars
     */
    public void refresh();
    
    /**This is a multi-purpose ResultSet oriented class with Meta-data, Model data, Observable list
     * and SQL commands in a Monitor wrapper.
     * @return the resultSetMonitor
     */
    public ResultSetMonitor getResultSetMonitor();
    
    /**This is a multi-purpose ResultSet oriented class with Meta-data, Model data, Observable list
     * and SQL commands in a Monitor wrapper.
     * @param resultSetMonitor the resultSetMonitor to set
     */
    public void setResultSetMonitor(ResultSetMonitor resultSetModel);

    /**
     * @param message the logger message to set
     * @param level the logger Level (FINEST to WARNING) to set
     * @param name the logger USER to append to message
     */
    public default void getAuditLog(String message, Level level, String name){
        try {

            FileHandler handler =  new File("AuditLog.txt").isFile() ? new FileHandler("AuditLog.txt", true) : new FileHandler("AuditLog.txt");
            logger.setUseParentHandlers(false);
            String timestamp = LocalDateTime.now().atZone(ZoneOffset.systemDefault()).withZoneSameInstant
                    (ZoneId.of("UTC")).toLocalDateTime().toString();

            SimpleFormatter simpleFormatter = new SimpleFormatter();
            handler.setFormatter(simpleFormatter);  
            logger.addHandler(handler);
            logger.log(level, message + " UTC_TIMESTAMP: ["+timestamp+"] "+name);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }  

    }


    public enum Fill {

        /**
         * Enumeration paratemets
         */
        LIGHT_PURPLE("light-purple"),
        LIGHT_AQUA("light-aqua"),
        LIGHT_TEAL("light-teal"),
        LIGHT_GREEN("light-green"),
        LIGHT_ORANGE("light-orange"),
        LIGHT_SHERBERT("light-sherbert"),
        LIGHT_PINK("light-pink"),
        LIGHT_LAVENDAR("light-lavender");

        /**
         * color field variable
         */
        private String color;

        /**
         * Constructor
         * @param color value as a strings
         */
        Fill(String color) {
            this.color = color;
        }
        /**
         * @return the Fill class color at random.
         */
        public static Fill getRandom() {
            return values()[(int) (Math.random() * values().length)];
        }

        /**
         * @return final String representation of the color.
         */
        public String color() {
            return color;
        }
    }
}


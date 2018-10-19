package scheduler.model;

import static scheduler.controller.ApplicationController.showLambdaAlertDialog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javafx.scene.control.Alert;
import scheduler.GlobalVariables;
import scheduler.controller.ApplicationController;
/** database helper class for Connection to java database driver. Also stores
 * the database connection variables.
 *
 * @author dkell40@wgu.edu
 */
public class DatabaseConnection implements GlobalVariables{

    private static final String USERNAME = "U03O74";
    private static final String PASSWORD = "53688033732";

    private static Connection conn;
    private static DatabaseConnection dconn;
    private static boolean isConnected = false;

    /** 
     * Default Constructor 
     */
    public DatabaseConnection() {
        DatabaseConnection.dconn = this;openDBConnection();
    }  

    /** 
     * @return Connection the Connection to get  
     */
    private final synchronized Connection openDBConnection() {

        String config = "?ssl=false";
        String db = "U03O74";
        String url = "jdbc:mysql://52.206.157.109:3306/" + db + config;  
        
        try  {
            conn=DriverManager.getConnection(url,USERNAME,PASSWORD);
            isConnected = !conn.isClosed();
        } catch (SQLException e) {

            showLambdaAlertDialog((a) -> { a.setAlertType(Alert.AlertType.ERROR);
            a.setHeaderText(e.getCause().getMessage());});
            
            switch(e.getSQLState()){
            case "08001" : setDisconnectedScene();break;
            default : break;
            }
        }
        return conn;
    }

    /** 
     * Sets main application variables.
     */
    public void setDisconnectedScene(){
        ApplicationController.setLoginIsValidated(false);
        ApplicationController.getMainApplication().setDisconnectedScene();
    }

    /** 
     * @return Connection the Connection to get  
     */
    public static Connection getActiveConnection() {
        return conn;
    }

    /** 
     * @return dconn the DatabaseConnection to get  
     */
    public static DatabaseConnection getCurrentInstance(){
        return dconn;
    }

    /** 
     * @return isConnected the isConnected to get  
     */
    public static boolean isConnected() {
        return isConnected;
    }

    /** NOT AutoCloseable for consecutive ResultSet queries
     * @return isConnected the isConnected to get  
     */
    public final synchronized static boolean close(){
        try {
            if(conn != null && !conn.isClosed())
                conn.close();
            dconn = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isConnected = false;
    }


}

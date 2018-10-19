
package scheduler.controller;

import static scheduler.controller.ApplicationController.showLambdaAlertDialog;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import scheduler.Controller;
import scheduler.model.DatabaseConnection;
import scheduler.model.ExceptionControls;
import scheduler.model.MaintainRecords;
import scheduler.model.ResultSetMonitor;
/** This is a logIn controller which includes functionality to determine the location
 * and translate log-in and error control messages into multiple languages. Functionality is imported 
 * through the FXML loader by the setResources() method and extends ExceptionControls for the reporting
 * of error messages.
 * @author dkell40@wgu.edu
 */
public class LogInController extends ExceptionControls implements Controller{

    @FXML
    private Label label;
    @FXML 
    private Button logIn;
    @FXML 
    private Button cancel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField userField;

    private static LogInController login;
    private static Stage loginStage;
    private static ResourceBundle language = ApplicationController.getResources();
    private static ResultSetMonitor resultSetModel;
    /** validation is done by methods from super class ExceptionControls. The validation can 
     * also change the specific message languages and are in the LANGUAGE_.properties files.
     */
    @FXML
    private void onActionLogInHandler() {

        try {
            /*This class extends ExceptionControls and uses 3 mechanisms*/
            /*Try/multi-catch, *Assertions, throws IllegalArgumentException with if/else*/
            isInvalidUser(checkInvalidOrNull("userField", userField.getText()));
            isInvalidOrNullPassword("passwordField", passwordField.getText());

            ResultSetMonitor mgr = MaintainRecords.queryLogin(userField.getText());
            queryUserPass(mgr); //<-check mechanisms

        }catch (IllegalArgumentException | AssertionError i){

            getAuditLog(i.getMessage(), Level.WARNING,"USER: "+userField.getText());

            showLambdaAlertDialog((a) -> { a.setAlertType(Alert.AlertType.WARNING);a.setHeaderText(i.getMessage());});  

        }catch (Exception e) {

        }
    }

    /**The password to be verified containing letters, numbers and
     * minimum length of 4 characters. Also cannot be blank in the field.
     * @param mgr the ResultSet to compare the user password input to
     */
    private void queryUserPass(ResultSetMonitor mgr) throws IllegalArgumentException{
        assert mgr.getObservableDataList() == null: language.getString("invalidUserPass")+" USER: "+userField.getText();
        if(mgr.getObservableDataList().isEmpty()){
            passwordField.clear();
            getAuditLog(language.getString("invalidUserPass"), Level.WARNING, " USER: "+userField.getText());
            throw new IllegalArgumentException(language.getString("invalidUserPass"));
        }
        else
            if(mgr.getObservableDataList().get(0).get(0).equals(passwordField.getText())){
                onActionContinue();
            }else{
                passwordField.clear();
                getAuditLog(language.getString("invalidPass"), Level.WARNING, " USER: "+userField.getText());
                throw new IllegalArgumentException(language.getString("invalidPass") + " USER: "+userField.getText());
            }
    }

    /**
     * @return login the login to get
     */
    public static LogInController getLogin() {
        return login;
    }

    /**
     * @FXML cancels this login instance and closes the stage
     */
    @FXML
    private void onActionCancelHandler(){
        if(DatabaseConnection.isConnected())
            ApplicationController.setLoginIsValidated(false);
        login = null;loginStage.close();
    }

    /**
     * After login verification, main application variables are set and this
     * stage is closed.
     */
    private void onActionContinue(){

        getAuditLog(" Login success", Level.INFO, "USER: "+userField.getText());

        String user = userField.getText();
        ApplicationController.setUser(user);
        ApplicationController.setLoginIsValidated(true);
        ApplicationController.setConsultant(user);
        setLogin(null);
        loginStage.close();
    }

    /**
     * @param mainApplication the mainApplication to set
     * @param stage the stage to set
     */
    public void setMainApplication(ApplicationController mainApplication, Stage stage){
        LogInController.loginStage = stage;
        loginStage.initModality(Modality.APPLICATION_MODAL);

    }



    /**
     * @param login the login to set 
     */
    public static void setLogin(LogInController login) {
        LogInController.login = login;
    }

    /**
     * @param loginStage the loginStage to set
     */
    public Stage getLoginStage() {
        return loginStage;
    }

    /**Initializes instance or static variables
     * @param url the url to set 
     * @param resourceBundle the resourceBundle to set 
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LogInController.setLogin(LogInController.getLogin() == null ? this : LogInController.getLogin());  
        userField.setText("");
    }

    /**
     * re-initializes instance or static variables
     */
    @Override
    public void refresh(){
        initialize(null, null);
    }

    /**
     * @return resultSetModel the resultSetModel to get
     */
    @Override
    public ResultSetMonitor getResultSetMonitor(){
        return resultSetModel;
    }

    /**
     * @param resultSetModel the resultSetModel to set
     */
    @Override
    public void setResultSetMonitor(ResultSetMonitor resultSetModel){
        LogInController.resultSetModel = resultSetModel;
    }

}

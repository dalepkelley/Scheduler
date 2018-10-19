package scheduler.model;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import scheduler.GlobalVariables;
import scheduler.controller.ApplicationController;
/** SQL wrapper class to easily query SQL statements through Java.
 * This class utilizes generatedKeys() from a single PreparedStatement method
 * to return the primary key back to the data array that was passed as a parameter
 * to each method as a return to each method for insert and update. There are two 
 * delete functions where one sets a column to active status or not using integers.
 * @author dkell40@wgu.edu
 */
public final class MaintainRecords implements GlobalVariables{
    
    private final String USER;

    public MaintainRecords(){
        USER = ApplicationController.getUser();
    }
    
    /** This method is used in all methods to get the PreparedStatement.
     * @param query the String to query for all SQL statements.
     */
    final synchronized static PreparedStatement getStatement(final String query) throws SQLException{
        if(DatabaseConnection.isConnected() == false){new DatabaseConnection();}
        MaintainRecords.printSQLStatements(query);
        return DatabaseConnection.getActiveConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    }
    /** Gets the ID or Primary Key (String) as a result of the method getStatement() using
     * arguments 'Statement.RETURN_GENERATED_KEYS'
     * @param preState the statement that has the key(s)
     * @return the new key as a String
     */
    final synchronized static String getNewKey(PreparedStatement preState) throws SQLException{
        Long pk = new Long(-1);
        try (ResultSet generatedKeys = preState.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                pk = generatedKeys.getLong(1);
            }
        }
        return pk.toString();
    }
    
    private int offset(int i){
        return i+1;
    }

    /** SQL insert for all tables
     * @param table the table name
     * @param data the raw data to insert into prepared statement
     * @return the row that was actually updated or if not return null
     */
    public final synchronized String[] insertIntoTable(final String table, final String... data ) {
        String values = "";
        for (int i = 0; i < data.length; i++){
            values = i == 0 ? values+"?" : values+", ?" ;
        }
        final String query = "INSERT INTO "+table+" VALUES ("+values+");";
        
        try (final PreparedStatement preState = getStatement(query))  {

            for (int i = 0; i < data.length; i++){
                preState.setString(offset(i), data[i]);
            }
            preState.executeUpdate();
            
            data[0]= getNewKey(preState);


        } catch (SQLException e){
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());

        } finally{
            DatabaseConnection.close();
        }
        return data;
    }
    
    /** SQL update for all tables
     * @param table the table name to add to prepared statement
     * @param data the raw data to add to prepared statement
     * @param columns the column names to add to prepared statement
     * @return the row that was actually updated or if not return null
     */
    public final synchronized String[] updateTable(final String table, final String[] data, final String... columns ) {
       
        String values = "";
        for (int i = 1; i < columns.length; i++){
            values = i == 1 ? values+columns[i]+"=?" : values+", "+columns[i]+"=?";
        }
        
        final String query = "UPDATE "+table+" SET "+values+" WHERE "+columns[0]+" = ?";

        try (final PreparedStatement preState = getStatement(query)) {

            for (int i = 1; i < data.length; i++){
                preState.setString(i, data[i]);
            }
            preState.setString(data.length, data[0]);
            
            preState.executeUpdate();
            

        } catch (SQLException e){
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());

        } finally{
            DatabaseConnection.close();
        }

        return data;
    }
    
    /**
     * 
     * @param data address address2 postalCode phone
     * @return the row number that was actually updated or if not return -1
     */
    public final synchronized String[] insertIntoReminder(final String... sql ) {//5
        String[] newRow = new String[8];

        final String query = "INSERT INTO reminder VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (final PreparedStatement preState = getStatement(query)) {

            preState.setString(1, null);/**reminderId*/
            preState.setString(2, sql[2]);newRow[1]= sql[2];//reminderDate  //get start
            preState.setString(3, sql[4]);newRow[2]= sql[4];//snoozeIncrement
            preState.setString(4, "1");newRow[3]= "1";/**snoozeIncrementTypeId*/
            preState.setString(5, sql[8]);newRow[4]= sql[8];//appointmentId
            preState.setString(6, USER);newRow[5]= USER;/**createdBy*/
            preState.setString(7, LocalDateTime.now(ZoneOffset.UTC).toString());
            newRow[6] = LocalDateTime.now(ZoneOffset.UTC).toString();
            preState.setString(8, sql[1]);newRow[7]= sql[1];/**reminderCol*/        

            preState.executeUpdate();

            newRow[0]= getNewKey(preState);
            sql[0] =newRow[0];

        } catch (SQLException e){
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());

            return newRow = null;

        } finally{
            DatabaseConnection.close();
        }
        assert newRow != null : "newRow is null LINE: 245 "+ this.getClass().getSimpleName();
        return newRow;
    }

  
    /** Selects whether to set a record inactive or permanently delete.
     * based on the table name.
     * @param table the table to delete
     * @param rowNum the rowNum to delete 
     */
    public final synchronized static boolean deleteSQLRecord(final String table, final Integer rowNum){
        switch (table){
        case "customer":  return setActiveSQLRecord(table, rowNum, 0);
        case "user":  return setActiveSQLRecord(table, rowNum, 0);
        default : return permaDeleteSQLRecord(table, rowNum);
        }     
    }

    /**
     * 
     * @param table
     * @param rowNum
     * @return the row number that was actually updated or if not return -1
     */
    public final synchronized static boolean permaDeleteSQLRecord(final String table, final Integer rowNum ) {
        boolean isSuccessful = false; 
        final String query = "DELETE FROM "+ table+" WHERE "+table+"Id = ?";

        /** interface Statement AutoCloseable*/
        try ( final PreparedStatement preState = getStatement(query)) {

            preState.setInt(1, rowNum);
            isSuccessful = preState.executeUpdate() != 0;

        } catch (SQLException e){
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            /**logger.log(Level.SEVERE, "unexpected error " + ex.getMessage(), ex);*/
            /**Loggers are thread safe and static as shown above, see CH9 client-server*/

        }finally{
            DatabaseConnection.close();
        }

        return isSuccessful;
    }

    /** Wrapper method for all full update table command.
     * @param table the table to set
     * @param rowNum the rowNum to set
     * @param active the active to set
     */
    public final synchronized static boolean setActiveSQLRecord(final String table, final Integer rowNum, final Integer active ) {
        boolean isSuccessful = false; 
        final String query = "UPDATE "+ table+" SET active = ? WHERE "+table+"Id = ?";

        /** interface Statement AutoCloseable*/
        try ( final PreparedStatement preState = getStatement(query)) {

            preState.setInt(1, active);
            preState.setInt(2, rowNum);
            isSuccessful = preState.executeUpdate() != 0;

        } catch (SQLException e){
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());

        }finally{
            DatabaseConnection.close();
        }
        return isSuccessful;
    }

    /** Method makes sure null is not returned.
     * @param consultant the consultant to compare
     * @return consultant the consultant to get
     */
    private final static String getUser(final String consultant){
        if(consultant == null)
            return ApplicationController.getUser(); 
        return consultant;
    }

    /**  SQL query Appointment results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query appointments.
     */
    public final static ResultSetMonitor queryAppointment(final String consultant){
        String filter = "appointmentId, start, end, title, description, contact, phone, "
                + "customerName, address, city, location, postalCode, country, url, appointment.createdBy";
        if(ApplicationController.getAppointmentSet() == null || consultant != ApplicationController.getUser())
            ApplicationController.setAppointmentSet(getManager(consultant, filter, "", "appointment", "customer", "address", "city", "country"));
        return ApplicationController.getAppointmentSet();
    }

    /**   SQL query Customer results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query customer table.
     */
    public final static ResultSetMonitor queryCustomer(){
        final String filter = "customerId, customerName, address, city, postalCode, country, phone";
        if(ApplicationController.getCustomerSet() == null)
            ApplicationController.setCustomerSet(getManager(null, filter, "", "customer", "address", "city", "country"));
        return ApplicationController.getCustomerSet();
    }

    /**  SQL query Consultant results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query consultant schedules.
     */
    public final static ResultSetMonitor queryConsultantSchedules() { //getScheduleSet
        if(ApplicationController.getScheduleSet() == null)
            ApplicationController.setScheduleSet(getResultSetManager("SELECT appointmentID, userName,"
                    + " title, description, start, active FROM appointment LEFT JOIN user ON appointment.createdby "
                    + "= user.userName" + whereActive(" ", "user") + " ORDER BY createdby"));
        return ApplicationController.getScheduleSet(); 
    }

    /**  SQL query regional results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query regional statistics.
     */
    public static ResultSetMonitor queryRegionalStatistics(String consultant) {
        final String filter = "appointmentId, appointment.createdBy, city, COUNT(city) AS regionCount";
        final String orderBy = " GROUP BY city";
        if(ApplicationController.getRegionalSet() == null)
            ApplicationController.setRegionalSet(getManager(consultant, filter, orderBy, 
                    "appointment", "customer", "address", "city", "country"));
        return ApplicationController.getRegionalSet();  //GROUP BY location
    }

    /**  SQL query reminder results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query user reminders.
     */
    public final static ResultSetMonitor queryUserReminders(String consultant){
        final String filter = "reminderId, remindercol, reminderDate, reminder.createdBy, snoozeIncrement, description, customerName, start, reminder.appointmentId";
        if(ApplicationController.getUserReminderSet() == null)
            ApplicationController.setUserReminderSet(getManager(consultant, filter, "", "reminder", "appointment", "customer"));
        return ApplicationController.getUserReminderSet();

    }

    /**  SQL query statistic results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query type statistics.
     */
    public static ResultSetMonitor queryTypeStatistics(String consultant, String yearMonth) {  
        final String query = "SELECT appointmentId, createdBy, description, COUNT(description) AS type, start FROM "
                + "( SELECT * FROM appointment WHERE start LIKE '" + yearMonth + "%' ) sub WHERE createdBy ='"+consultant+"' GROUP BY description;";
        if(ApplicationController.getTypeSet() == null)
            ApplicationController.setTypeSet(getResultSetManager( query ));
        return ApplicationController.getTypeSet();
    } 

    /**  SQL query user results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query user schedules.
     */
    public final static ResultSetMonitor queryUserSchedules() { 
        if(ApplicationController.getUserSet() == null)
            ApplicationController.setUserSet(getManager(null, "userId, userName", "", "user"));
        return ApplicationController.getUserSet(); 
    }
    
    /**  SQL query row results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query by Id.
     */
    public final static ResultSetMonitor getRecordById(String table, String Id) { //userSet
        return getResultSetManager("SELECT * FROM "+table+" WHERE "+table+"Id="+Id+";");
    }

    /** SQL query login results.
     * @return the ResultSet, MetaData, ModelData, and list inside ResultSetMonitor for the
     * specified query password from user.
     */
    public final static ResultSetMonitor queryLogin(String user) {
        final String query = "SELECT password FROM user WHERE userName='"+user+"' AND active = '1';";
        return getResultSetManager( query );
    }

    /** 
     * @param prints the statements to console for debug purposes.
     */
    final static void printSQLStatements(final String s){
        System.out.println(s);
    }

    /** Selects whether to use WHERE or AND clause in an SQL statement and
     * depending on the table.
     * @param clause the clause to set
     * @param consultant the consultant to set
     * @param table the table to set
     */
    private final static String whereUser(String clause, final String consultant, final String table) {
        if(table.equals("appointment") || table.equals("reminder")){
            clause = whereAnd(clause) + table+".createdBy = '"+getUser(consultant)+"'";}
        return clause;
    }

    /** Finds records base on active column set as '1'.
     * @param clause the clause to set
     * @param table_s each table to set
     * @return clause gets the result of the clause
     */
    private final static String whereActive(String clause, final String... table_s){
        for(String table : table_s){
            if(table.equals("customer") || table.equals("user"))
                clause = whereAnd(clause) + table+".active = 1"; }
        return clause;
    }

    /** Method determines whether to add WHERE or AND in SQL clause.
     * @param clause sets the clause
     * @return  statement gets the full statement result
     */
    private final static String whereAnd(final String clause){
        final StringBuilder statement = new StringBuilder(clause);
        if(clause.contains("WHERE")) statement.append(" AND ");
        else statement.append("WHERE ");
        return statement.toString();
    }

    /** Method finishes building query string and creates a new ResultSetMonitor object.
     * @param consultant the consultant to set
     * @param filter the filter to set
     * @param orderBy the orderBy to set
     * @param table_s the table_s to set
     * @return  
     */
    public final synchronized static ResultSetMonitor getManager(final String consultant, final String filter, final String orderBy, String... table_s){
        final String where = whereUser(whereActive(" ", table_s), consultant, table_s[0]) + orderBy;
        try(ResultSetMonitor resultSetMgr = new ResultSetMonitor(where, filter, table_s)){
            return resultSetMgr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Creates a new ResultSetMonitor object.
     * @param query the query to set
     * @return resultSetMgr to get
     */
    public final synchronized static ResultSetMonitor getResultSetManager(final String query){
        try(final ResultSetMonitor resultSetMgr = new ResultSetMonitor(query)){
            return resultSetMgr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }



}

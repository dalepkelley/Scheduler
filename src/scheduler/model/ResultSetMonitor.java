package scheduler.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import scheduler.GlobalVariables;


/** This is a wrapper class for retrieving SQL SELECT queries from the database.
 * Queries can be parsed using methods for ease of SQL commands. For instance, if multiple tables have a 
 * propagation and can transcend down a relative relational path, this class can easily build the query
 * string needed for long SQL statements.
 * @author dkell40@wgu.edu
 *
 */
public final class ResultSetMonitor implements Callable<ResultSetMonitor>, AutoCloseable {

    private String[] colHeaderNames;
    private String tableNameMeta;
    private final String query;
    private ResultSetMetaData rsMetaData;
    private AtomicInteger numOfColsMeta;
    private ObservableList<ModelDataType> observableDataList = 
            FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    /** 
     * Overloaded Constructor
     * @param query the query to set   
     */
    public ResultSetMonitor (final String query){ 
        this(query, false);
    }

    /** 
     * Overloaded Constructor
     * @param query the query to set   
     * @param executor the executor boolean configuration to set   
     */
    public ResultSetMonitor (final String query, boolean executor){ 
        this.query = query;
        executor = executor == false ? processSQL() : true ;
    }


    /**
     * Overloaded Constructor
     * @param where the where string fragment to set 
     * @param selectColumns the column name fragments to set 
     * @param joinTables the table name fragments to set  
     */
    public ResultSetMonitor (final String where, final String selectColumns, final String... joinTables) {
        String statement = getJoinIfMultiTable(selectColumns, joinTables).toString();
        this.query = statement+where;processSQL();
    }

    /** 
     * This will re-query the ResultSet and set new data.
     */
    public final synchronized void refresh(){
        observableDataList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        processSQL();
    }

    /** 
     * @return callEventSuccess the callEventSuccess to get  
     */
    public final synchronized boolean processSQL(){
        return call() != null;
    }

    /** Creates a localized DateTime string.
     * @param input the input to set
     * @return localizedDateTime the localizedDateTime to get
     */
    public final synchronized String getLocalized(LocalDateTime input){
        return input.atZone(ZoneOffset.UTC).withZoneSameInstant
                (ZoneId.systemDefault()).toLocalDateTime().toString();
    }

    /** Method overrides from the Callable<> interface and can be used with an
     * Executor service but not as a both a monitor and Future at the same time.
     * @return resultSetMonitor the resultSetMonitor to get
     */
    @Override
    public final synchronized ResultSetMonitor call() {

        /** try with resources AutoCloseable */
        try (   PreparedStatement statement = MaintainRecords.getStatement(query);
                ResultSet rs = statement.executeQuery(); )
        {   

            rsMetaData = (ResultSetMetaData) rs.getMetaData();
            numOfColsMeta = new AtomicInteger(rsMetaData.getColumnCount());
            tableNameMeta = rsMetaData.getTableName(1); 
            setColumnHeaderNames(rs);

            while (rs.next()) {


                final String[] buff = new String[numOfColsMeta.get()];

                for (int i = 0;i<numOfColsMeta.get();i++){

                    switch(colHeaderNames[i]){
                    case "start" : buff[i] =  getLocalized(rs.getTimestamp(colHeaderNames[i]).toLocalDateTime());break;
                    case "end" :  buff[i] =  getLocalized(rs.getTimestamp(colHeaderNames[i]).toLocalDateTime());break;
                    case "reminderDate" :  buff[i] =  getLocalized(rs.getTimestamp(colHeaderNames[i]).toLocalDateTime());break;
                    default : buff[i] =  rs.getString(colHeaderNames[i]);
                    }

                }
                observableDataList.add(new ModelDataType(buff));

            }

        } catch (SQLException se) { 
            System.out.println(se);

        }finally{
            this.close();
        }

        return this;
    }

    /** Joins tables based off the parameters using the function leftJoin()
     * and inserts the fragment back into the statement.
     * @param columns the columns to set
     * @param joinTables the tables to join
     * @return query the query to get
     */
    public final synchronized StringBuilder getJoinIfMultiTable(final String columns, final String... joinTables){   
        final StringBuilder query = new StringBuilder("SELECT "+columns+" FROM "+joinTables[0]);
        for(int i =1;i<joinTables.length;i++){
            query.append(leftJoin(joinTables[i-1], joinTables[i]));
        }
        return query;
    }

    /** Adds a WHERE clause to the statement based on certain criteria.
     * @param statement the statement to set
     * @return query the query string to get
     */
    public final synchronized StringBuilder addWhereClause(final StringBuilder statement){   
        final StringBuilder query = statement.append( "WHERE ");
        return query;
    }

    /** Sets column name MetaData into this class before ResultSet is closed.
     * @param MetaData the MetaData to set
     */
    private final synchronized void setColumnHeaderNames(ResultSet resultset) throws SQLException{
        colHeaderNames =  colHeaderNames == null ? new String[numOfColsMeta.get()] : colHeaderNames ;
        for (int i = 1; i <= colHeaderNames.length;i++){
            colHeaderNames[i-1] = resultset.getMetaData().getColumnName(i);
        }
    }

    /** 
     * @return colHeaderNames the colHeaderNames to get
     */
    public final String[] getColumnHeaderNames(){
        return colHeaderNames;
    }

    /** Adds table left join string fragments to the query for each table pair given.
     * @param a the table to set
     * @param b the table to set
     * @return query the query to get
     */
    private final synchronized String leftJoin(final String a, final String b){
        return " LEFT JOIN " + b + " ON " +a+"."+b+"ID = " +b+"."+b+"ID";
    }

    /** 
     * @return observableDataList the observableDataList to get
     */
    public final ObservableList<ModelDataType> getObservableDataList() {
        return observableDataList;
    }

    /** 
     * @return tableNameMeta the tableNameMeta to get
     */
    public final String getTableName(){
        return tableNameMeta;
    }

    /** 
     * @param table the table to set
     */
    public final void setTableName(String table){
        tableNameMeta = table;
    }

    /** This is a wrapper class for the model data that is returned from the
     * ResultSet. This data is parsed in a SimpleObjectProperty type array
     * and only includes String types. This is convenient for using loops but
     * the string data has to be converted to other data types like LocalDateTime
     * in this class alone to reduce the amount of code redundancy and overhead 
     * within the application.
     * @author dkell40@wgu.edu
     *
     */
    public static final class ModelDataType implements GlobalVariables{

        private final SimpleStringProperty[] data;

        /** default Constructor
         * @param data the data to set 
         */
        public ModelDataType(String... data) {
            this.data = new SimpleStringProperty[data.length];
            for (int i = 0; i < data.length; i++)
                this.data[i]=new SimpleStringProperty(data[i]); 
        }

        /** 
         * @return id the id to get as an int
         */
        public final int getId(){
            return new Integer(data[0].get());
        }

        /** 
         * @return data the data at index to get as a string
         */
        public final String get(int i) {
            return data[i].get();
        }

        /** 
         * @return data the data at index to get  
         */
        public final SimpleStringProperty getSimpleString(int i) {
            return data[i];
        }

        /** 
         * @return data the data at index to get as a LocalDateTime
         */
        public final LocalDateTime getDateTime(int i) {
            return parseSQLDateTime(get(i));
        }

        /** 
         * @return data the data at index to get as a LocalDateTime String 
         */
        public final String getDateTimeStr(int i) {
            return getDateTime(i).toString();
        }

        /** 
         * @return data the data at index to get as a LocalDate
         */
        public final LocalDate getDate(int i) {;
        return getDateTime(i).toLocalDate();
        }

        /** 
         * @return data the data at index to get as a formatted Date string
         */
        public final String getDateSlash(int i) {
            return getDate(i).format(slashFormatter);
        }

        /** 
         * @return data the data at index to get as a Date string
         */
        public final String getDateStr(int i) {
            return getDate(i).toString();
        }

        /** 
         * @return data the data at index to get as a LocalTime 
         */
        public final LocalTime getTime(int i) {
            return getDateTime(i).toLocalTime();
        }

        /** 
         * @return data the data at index to get as a LocalTime String 
         */
        public final String getTimeStr(int i) {
            return getTime(i).toString();
        }

        /** 
         * @return data the data at index to get as a formatted LocalTime String 
         */
        public final String getTimeStrAmPm(int i) {
            return getTime(i).format(amPmformat);
        }

        /** 
         * @return data the data array to get
         */
        public final SimpleStringProperty[] getData() {
            return data;
        }

    }

    /** 
     * Closes the database connection.
     */
    @Override
    public final synchronized void close(){
        DatabaseConnection.close();

    }




}

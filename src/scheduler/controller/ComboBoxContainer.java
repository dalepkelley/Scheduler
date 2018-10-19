package scheduler.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import scheduler.model.ResultSetMonitor;
import scheduler.model.ResultSetMonitor.ModelDataType;
/** Class that has more functionality of the ComboBox class and can filter observableArrayList
 * elements with greater flexibility as an extension of the ListViewevents class. Extra functions include 
 * reporting more details of either the ComboBox or ListView to a TextArea or Pop-up.
 * @author dkell40@wgu.edu
 */
public class ComboBoxContainer extends ListViewEvents
{
    private ComboBox<Container> comboBox;
    private ObservableList<Container> containerList;

    /** 
     * Default constructor
     */
    ComboBoxContainer() {this(null); }

    /** 
     * Overloaded Constructor
     */
    ComboBoxContainer(TextArea event) {super(event);}

    /** 
     * @param event the event text area to set
     */
    public ComboBoxContainer setReport(TextArea event) {
        report = event; return this;
    }

    /** The container list is also set and is part of the ComboBox.
     * @param comboBox the comboBox to set
     */
    public ComboBoxContainer setComboBox(ComboBox<Container> comboBox) {
        this.comboBox = comboBox;
        comboBox.getStylesheets().add(ApplicationController.class.getResource("/scheduler/view/GUI.css").toExternalForm());
        containerList = FXCollections.observableArrayList();
        return this;
    }

    /** The container list is also set and is part of the ComboBox. 
     * @return comboBox the comboBox to get
     */
    public ComboBox<Container> initialize(ObservableList<ModelDataType> modelList, ResultSetMonitor rsm){

        if(modelList != null)  
            modelList.forEach((dataType) -> { 
                containerList.add(new Container(dataType.get(0), dataType.get(1) , dataType));
            });
        else 
            rsm.getObservableDataList().forEach((dataType) -> { 
                containerList.add(new Container(dataType.get(0), dataType.get(1) , dataType));
                sort(-1);
            });

        comboBox.setItems(containerList);

        comboBox.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> { 
            Container container = (Container)comboBox.getSelectionModel().selectedItemProperty().get();
            if(comboBox.getSelectionModel().getSelectedIndex() != -1 && container != null){
                reportDetailsForEach(container);
            }
        });
        return comboBox;
    }

    /** 
     * Sorts a given type list.
     */
    public void sort(int fieldIndex) {
        containerList.sort((a, b) -> a.getDescription().compareTo(b.getDescription()));  
    }

    /**The Container class has key and value pairs that can have multiple values within each ComboBox or ListView
     * or both.
     */
    public static class Container
    {

        private Object id;
        private String description;
        private ModelDataType model;
        private ListViewEvents event;

        /** Only constructor for class.
         * @param id the id to get
         * @param eventDescription the eventDescription to get
         * @param data the data to get 
         */
        public Container(Object id, String eventDescription, ModelDataType data)
        {
            this.id = id;
            this.description = eventDescription;
            this.model = data;
        }

        /** 
         * @return  id hte id to get
         */
        public Object getId() { return id; }

        /** 
         * @return description the description to get
         */
        public String getDescription(){ return description; }

        /** 
         * @return model the model to get
         */
        public ModelDataType getModel() { return model; }

        /** 
         * @param model the model to set
         */
        public void setModel(ModelDataType model) { this.model = model; }

        /** 
         * @return event the event to get  
         */
        public ListViewEvents getEvent() { return event; }

        /** 
         * @param event the event to set
         */
        public void setEvent(ListViewEvents event) { this.event = event; }
        
        /** Overrides from Object.
         * @return description the description to get  
         */
        @Override
        public String toString(){ return description; }

    }


}
public class Waiter extends Employee{
    /**
     *The waiter class defines the waiter specific set of functionalities.
     * This includes changing table statuses, placing orders, and closing tickets.
     * The assignedTables parameter is an array which contains all the tables a waiter is responsible for.
     * The size of this array is 28, as this is the total number of tables in the restaurant (since we are not including the bar table).
     */
    Table[] assignedTables;

    /**
     * Default no-arg constructor for a new waiter object
     */
    public Waiter() {
        super();
        assignedTables = new Table[28];
    }

    /**
     * Overloaded constructor for waiter object with partial information
     * @param name waiter name
     * @param newUN waiter username
     * @param newPW waiter password
     * @param tables waiter's assigned tables
     */
    public Waiter(String name, String newUN, String newPW, Table[] tables){
        super(name, newUN, newPW);
        this.assignedTables = tables;
    }

    /**
     * Overloaded constructor for a waiter object with full information given
     * This is intended for use by manager to manually create an employee
     * @param name waiter name
     * @param newUN waiter username
     * @param newPW waiter password
     * @param newRate waiter hourly pay rate
     * @param tables waiter's assigned tables
     */
    public Waiter(String name, String newUN, String newPW, double newRate, Table[] tables){
        super(name, newUN, newPW, "waiter", newRate);
        this.assignedTables = tables;
    }

    /**
     * getter method for assigned tables array
     * @return returns the current employee assigned table array
     */
    public Table[] getAssignedTables(){
        return this.assignedTables;
    }

    /**
     * Setter method for assigned tables array
     * @param newTables contains new set of assigned tables
     */
    public void setAssignedTables(Table[] newTables){
        this.assignedTables = newTables;
    }
}

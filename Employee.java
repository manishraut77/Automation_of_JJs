public class Employee {
    /**
     * The employee class defines the basic set of information and functions for an employee.
     * This includes features like creating an account, signing in to the system, signing out, and clocking in/out.
     */

    private String name;
    private String username;
    private String password;
    static int empID = 0;
    String role;
    private double hourlyRate;
    public boolean isClockedIn;

    /**
     * Default no-arg constructor for an employee object
     */
    public Employee() {
        this.name = "";
        this.username = "";
        this.password = "";
        empID++;
        this.empID = empID;
        this.role = "";
        this.hourlyRate = 0;
        this.isClockedIn = false;
    }

    /**
     * Overloaded constructor for creating an employee object with partial data
     * @param newName contains new employee object name
     * @param newUN contains new employee username
     * @param newPW contains new employee password
     */
    public Employee(String newName, String newUN, String newPW){
        this.name = newName;
        this.username = newUN;
        this.password = newPW;
        empID++;
        this.empID = empID;
    }

    /**
     * Overloaded constructor for an employee object with full data, intended primarily for use by managers
     * @param newName contains new employee name
     * @param newUN contains new employee username
     * @param newPW contains new employee password
     * @param newRole contains new employee role
     * @param newRate contains new employee hourly rate
     */
    public Employee(String newName, String newUN, String newPW, String newRole, double newRate) {
        this.name = newName;
        this.username = newUN;
        this.password = newPW;
        empID++;
        this.empID = empID++;
        this.role = newRole;
        this.hourlyRate = newRate;
        this.isClockedIn = false;
    }

    /**
     * Getter method for the name data field.
     * @return returns a string containing the employee name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter method for name parameter.
     * @param newName newName is the new name for the employee
     */
    public void setName(String newName){
        this.name = newName;
    }

    /**
     * Getter method for the username data field.
     * @return returns a string containing the username
     */
    private String getUsername(){
        return this.username;
    }

    /**
     * Setter method for the username data field.
     * @param newUN newUN is the new username for the employee
     */
    public void setUsername(String newUN){
        this.username = newUN;
    }

    /**
     * Getter method for the password data field.
     * @return returns a string containing employee password
     */
    private String getPassword(){
        return this.password;
    }

    /**
     * Setter method for the password data field.
     * @param newPW newPW is the new employee password
     */
    public void setPassword(String newPW){
        this.password = newPW;
    }

    /**
     * Getter method for employee ID.
     * @return returns an int containing employee ID
     */
    public int getEmpID() {
        return this.empID;
    }

    /**
     * Setter for employee ID.
     * @param newID newID is the new employee ID
     */
    public void setEmpID(int newID){
        this.empID = newID;
    }

    /**
     * Getter method for employee role.
     * @return returns a string containing employee role
     */
    public String getRole(){
        return this.role;
    }

    /**
     * Setter method for role method.
     * @param newRole newRole is the new role for the employee
     */
    public void setRole(String newRole){
        this.role = newRole;
    }

    /**
     * Getter method for the hourly rate data field.
     * @return Returns a double containing current hourly rate
     */
    public double getHourlyRate(){
        return this.hourlyRate;
    }

    /**
     * Setter method for hourly rate
     * @param newRate newRate is the new hourly rate for the employee
     */
    public void setHourlyRate(double newRate){
        this.hourlyRate = newRate;
    }

    /**
     * Getter method for clocked in flag
     * @return returns current value for isClockedIn
     */
    public boolean getClockIn(){
        return this.isClockedIn;
    }

    /**
     * Setter method for clocked in flag
     * @param newClockedIn new clocked in status
     */
    public void setClockedIn(boolean newClockedIn){
        this.isClockedIn = newClockedIn;
    }

    /**
     * Sign in method -
     */
    public void signIn(){

    }

    /**
     * Sign out method
     */
    public void signOut(){

    }

    /**
     * Clock in method
     */
    public void clockIn(){
        setClockedIn(true);
    }

    /**
     * Clock out method
     */
    public void clockOut(){
        setClockedIn(false);
    }
}

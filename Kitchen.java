public class Kitchen {
    /**
     * The kitchen class outlines the basic structure and function of the kitchen.
     * This includes displaying tickets for kitchen staff and marking orders from the queue finished.
     * The order queue is implemented as an arraylist as it can be dynamically sized
     */

    java.util.ArrayList<Ticket> orderQueue = new java.util.ArrayList<>();

    /**
     * Overloaded constructor for a new kitchen object
     * @param newQueue provides the arraylist
     */
    public Kitchen(java.util.ArrayList<Ticket> newQueue) {
        this.orderQueue = newQueue;
    }
}

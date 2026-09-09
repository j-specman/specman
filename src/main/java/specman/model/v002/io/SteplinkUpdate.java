package specman.model.v002.io;

public class SteplinkUpdate {
    public final String location;
    public final String oldNumber;
    public final String newNumber;

    public SteplinkUpdate(String location, String oldNumber, String newNumber) {
        this.location = location;
        this.oldNumber = oldNumber;
        this.newNumber = newNumber;
    }
}

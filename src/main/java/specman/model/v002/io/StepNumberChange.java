package specman.model.v002.io;

public class StepNumberChange {
    public final String oldNumber;
    public final String newNumber;
    public final String stepType;
    public final String plainText;

    public StepNumberChange(String oldNumber, String newNumber, String stepType, String plainText) {
        this.oldNumber = oldNumber;
        this.newNumber = newNumber;
        this.stepType = stepType;
        this.plainText = plainText;
    }
}

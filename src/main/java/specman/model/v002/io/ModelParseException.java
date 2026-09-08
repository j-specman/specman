package specman.model.v002.io;

public class ModelParseException extends Exception {
    public ModelParseException(String message) {
        super(message);
    }

    public ModelParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

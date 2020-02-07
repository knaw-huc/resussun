package nl.knaw.huc.resussun.timbuctoo;

public class TimbuctooException extends Exception {
    public TimbuctooException(String message) {
        super(message);
    }

    public TimbuctooException(String message, Throwable cause) {
        super(message, cause);
    }
}

package exception;

public class NoAccountFoundException extends RuntimeException {

    public NoAccountFoundException(String message) {
        super(message);
    }
}

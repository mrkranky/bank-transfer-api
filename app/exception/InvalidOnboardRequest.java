package exception;

public class InvalidOnboardRequest extends RuntimeException {

    public InvalidOnboardRequest(String message) {
        super(message);
    }
}

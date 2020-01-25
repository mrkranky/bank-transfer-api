package exception;

public class InvalidTransferRequest extends RuntimeException {
    public InvalidTransferRequest(String message) {
        super(message);
    }
}

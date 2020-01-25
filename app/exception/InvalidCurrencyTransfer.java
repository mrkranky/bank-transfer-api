package exception;

public class InvalidCurrencyTransfer extends RuntimeException {
    public InvalidCurrencyTransfer(String message) {
        super(message);
    }
}

package common.model.exception;

import java.util.ArrayList;
import java.util.List;

public class DataValidationException extends Exception {
    private List<String> errors = new ArrayList<>();

    public DataValidationException(String message) {
        super(message);
    }

    public DataValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() { return errors; }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public String getFormattedErrors() {
        return String.join("; ", errors);
    }
}
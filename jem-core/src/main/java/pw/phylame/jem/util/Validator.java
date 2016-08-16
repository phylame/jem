package pw.phylame.jem.util;

public interface Validator {
    void validate(CharSequence name, Object value) throws RuntimeException;
}

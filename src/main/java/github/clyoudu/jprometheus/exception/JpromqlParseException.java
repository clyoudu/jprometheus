package github.clyoudu.jprometheus.exception;

/**
 * @author leichen
 */
public class JpromqlParseException extends JprometheusException {

    public JpromqlParseException(String msg) {
        super(msg);
    }

    public JpromqlParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

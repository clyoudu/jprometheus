package github.clyoudu.jprometheus.exception;

/**
 * @author leichen
 */
public class JprometheusException extends RuntimeException {

    public JprometheusException(String msg) {
        super(msg);
    }

    public JprometheusException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

package github.clyoudu.jprometheus.promql.parser;

import github.clyoudu.jprometheus.exception.JpromqlParseException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * @author leichen
 */
public class JpromqlErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
        String msg, RecognitionException e) {
        String message = "line " + line + ":" + charPositionInLine + " " + msg;
        throw new JpromqlParseException(message);
    }
}

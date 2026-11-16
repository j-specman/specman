package classman.model.v001.io;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/** Remembers the first syntax error of the lexer or parser it is attached to. */
class ClassDiagramSyntaxErrorCollector extends BaseErrorListener {
    private String firstError;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        if (firstError == null) {
            firstError = "line " + line + ":" + charPositionInLine + " " + msg;
        }
    }

    boolean hasError() {
        return firstError != null;
    }

    String firstError() {
        return firstError;
    }
}

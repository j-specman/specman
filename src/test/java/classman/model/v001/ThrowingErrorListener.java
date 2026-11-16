package classman.model.v001;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.BitSet;

/** Turns every syntax error and every grammar ambiguity into an exception, so a test fails at the first one. */
public class ThrowingErrorListener extends BaseErrorListener {

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                          String msg, RecognitionException e) {
    throw new IllegalStateException("line " + line + ":" + charPositionInLine + " " + msg);
  }

  @Override
  public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
                              BitSet ambigAlts, org.antlr.v4.runtime.atn.ATNConfigSet configs) {
    throw new IllegalStateException("ambiguity in rule "
        + recognizer.getRuleNames()[dfa.atnStartState.ruleIndex] + " at token " + startIndex + ".." + stopIndex);
  }
}

package specman.model.v002.io;

import org.antlr.v4.runtime.Token;

import java.util.IdentityHashMap;
import java.util.Map;

/** Remembers where in the text of a model file its elements have been written, so that problems found later in the
 * model can be reported as "line L:C", like the syntax errors are. The positions are those of the first token of an
 * element, with the same numbering as ANTLR uses: lines start at 1, columns at 0. Elements are identified by instance.
 * A model read from another format than the text format has no positions at all. */
public final class ModelSourcePositions {
  private static final String NO_POSITION = "";

  private final Map<Object, String> positions = new IdentityHashMap<>();

  void put(Object element, Token firstToken) {
    positions.put(element, describe(firstToken));
  }

  /** @return "line L:C" of the element, or an empty string if its position is unknown */
  public String of(Object element) {
    return positions.getOrDefault(element, NO_POSITION);
  }

  static String describe(Token token) {
    return "line " + token.getLine() + ":" + token.getCharPositionInLine();
  }
}

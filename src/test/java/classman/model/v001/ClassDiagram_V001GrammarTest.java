package classman.model.v001;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassDiagram_V001GrammarTest {

  private static ClassDiagram_V001Parser parserFor(String source) {
    ClassDiagram_V001Lexer lexer = new ClassDiagram_V001Lexer(CharStreams.fromString(source));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ThrowingErrorListener());
    ClassDiagram_V001Parser parser = new ClassDiagram_V001Parser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new ThrowingErrorListener());
    parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    return parser;
  }

  private static ParseTree parse(String source) {
    return parserFor(source).classDiagram();
  }

  private static String example(String name) throws IOException {
    try (InputStream in = ClassDiagram_V001GrammarTest.class.getResourceAsStream(name + ".classdiagram")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static int countIn(String source, int rule) {
    return Trees.findAllRuleNodes(parse(source), rule).size();
  }

  /** The minimal example without any at() parses: two classes and one relation. */
  @Test
  void minimalDiagram() throws IOException {
    String source = example("hello");
    assertEquals(2, countIn(source, ClassDiagram_V001Parser.RULE_classDecl));
    assertEquals(1, countIn(source, ClassDiagram_V001Parser.RULE_relation));
  }

  /** The benchmark diagram from the manual parses completely. */
  @Test
  void benchmarkDiagram() throws IOException {
    String source = example("benchmark");
    assertEquals(11, countIn(source, ClassDiagram_V001Parser.RULE_classDecl));
    assertEquals(17, countIn(source, ClassDiagram_V001Parser.RULE_relation));
    assertEquals(6, countIn(source, ClassDiagram_V001Parser.RULE_assocClass));
    assertEquals(2, countIn(source, ClassDiagram_V001Parser.RULE_note));
  }

  /** Odd but valid constructs: quoted names, negative coordinates, comments, tagged values, all relation types. */
  @Test
  void edgeCaseSyntax() throws IOException {
    parse(example("edgecases"));
  }

  /** A brace followed by "word =" opens tagged values, any other brace opens the class body. */
  @Test
  void taggedValuesVersusBody() {
    String source = "classdiagram { class A {k=1} class B {x} class C {k=1} {x} }";
    assertEquals(2, countIn(source, ClassDiagram_V001Parser.RULE_taggedValues));
    assertEquals(2, countIn(source, ClassDiagram_V001Parser.RULE_classBody));
  }

  /** An extends relation takes no multiplicities, so this is a syntax error. */
  @Test
  void extendsMultiplicityRejected() {
    assertThrows(IllegalStateException.class, () -> parse("classdiagram { extends A 1 -> * B }"));
  }

  /** Keywords are fine as attribute names and types, but a class named like a keyword needs backticks. */
  @Test
  void keywordsAsNames() {
    parse("classdiagram { class A { width : left } }");
    assertThrows(IllegalStateException.class, () -> parse("classdiagram { class left { } }"));
  }
}

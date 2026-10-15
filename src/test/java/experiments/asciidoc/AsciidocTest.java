package experiments.asciidoc;

import org.asciidoctor.Asciidoctor;
import experiments.NoSuiteTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

@NoSuiteTest
public class AsciidocTest {

  @Test
  void test() {
    Asciidoctor asciidoctor = Asciidoctor.Factory.create();
    String output = asciidoctor.convert("== Hello _Baeldung_!\n" +
        "This is a list of\n\n" +
        "* one\n" +
        "* two\n" +
        "* three\n\n" +
        "items.\n\n" +
        "here is some [.deleted]#deleted text# styled by a custom role,\n" +
        "\n" +
        "\n" +
        "[cols=\"3,5\"]\n" +
        "|===\n" +
        "|*admin*|(Connection String des Admin-Users nutzen)\n" +
        "|*create*|(Connection String des Admin-Users nutzen)\n" +
        "|===\n"
      , new HashMap<>());
    System.out.println(output);
  }
}

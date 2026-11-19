package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;

/** Resolves the change marks of a diagram source and returns the canonical text, so tests compare readable sources. */
final class ResolvedDiagrams {
  private static final ClassDiagramParser_V001 PARSER = new ClassDiagramParser_V001();
  private static final ClassDiagramSerializer_V001 SERIALIZER = new ClassDiagramSerializer_V001();
  private static final ChangeResolver_V001 RESOLVER = new ChangeResolver_V001();

  private ResolvedDiagrams() {
  }

  static String canonical(String elements) throws ClassDiagramParseException {
    return SERIALIZER.serialize(parse(elements));
  }

  static String accepted(String elements, String changeSet) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    RESOLVER.accept(model, changeSet);
    return SERIALIZER.serialize(model);
  }

  static String rejected(String elements, String changeSet) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    RESOLVER.reject(model, changeSet);
    return SERIALIZER.serialize(model);
  }

  private static ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return PARSER.parse("classdiagram {\n" + elements + "\n}");
  }
}

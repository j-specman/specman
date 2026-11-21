package classman.model.v001.change;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;

/** Resolves the change marks of a diagram source and returns the canonical text, so tests compare readable sources. */
final class ResolvedDiagrams {
  private static final ClassDiagramParser_V001 PARSER = new ClassDiagramParser_V001();
  private static final ClassDiagramSerializer_V001 SERIALIZER = new ClassDiagramSerializer_V001();
  private static final ChangeResolver_V001 RESOLVER = new ChangeResolver_V001();
  private static final ChangeSetRenamer_V001 RENAMER = new ChangeSetRenamer_V001();

  private ResolvedDiagrams() {
  }

  static String canonical(String elements) throws ClassDiagramParseException {
    return SERIALIZER.serialize(parse(elements));
  }

  /** @param source a whole diagram source, not only its elements */
  static String canonicalWhole(String source) throws ClassDiagramParseException {
    return SERIALIZER.serialize(PARSER.parse(source));
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

  /** The diagram without the element with the ID and what hangs on it. */
  static String deleted(String elements, String id) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    new ElementDeleter_V001().delete(model, elementWithId(model, id));
    return SERIALIZER.serialize(model);
  }

  /** The diagram after the change set deleted the element with the ID. */
  static String deleted(String elements, String id, String changeSet) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    new ElementDeleter_V001().delete(model, elementWithId(model, id), changeSet);
    return SERIALIZER.serialize(model);
  }

  private static AbstractDiagramElementModel_V001 elementWithId(ClassDiagramModel_V001 model, String id) {
    for (AbstractDiagramElementModel_V001 element : model.elements) {
      if (id.equals(element.id)) {
        return element;
      }
      if (element instanceof AssociationRelationModel_V001 association && association.assocClass != null
          && id.equals(association.assocClass.id)) {
        return association.assocClass;
      }
    }
    throw new AssertionError("no element " + id);
  }

  static String renamed(String elements, String from, String to) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = parse(elements);
    RENAMER.rename(model, from, to);
    return SERIALIZER.serialize(model);
  }

  /** @param source a whole diagram source, not only its elements */
  static String renamedWhole(String source, String from, String to) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = PARSER.parse(source);
    RENAMER.rename(model, from, to);
    return SERIALIZER.serialize(model);
  }

  private static ClassDiagramModel_V001 parse(String elements) throws ClassDiagramParseException {
    return PARSER.parse("classdiagram {\n" + elements + "\n}");
  }
}

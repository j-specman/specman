package specman.ops;

import specman.editarea.image.classman.LinkedClassmanDuplicates;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.visitor.LinkedClassmanAreaCollector;

import java.io.File;

/** What the operations which link a Classman diagram to a file the user chose have in common. */
abstract class AbstractClassmanLinkOp extends AbstractSpecmanOp {

  AbstractClassmanLinkOp(SpecmanOpContext context) {
    super(context);
  }

  /** @return what the file has, if a diagram can link it: It has to have a class diagram, and no other diagram of the
   * document may link it. Otherwise null, and the user got told why. */
  ClassmanLinkResolution resolveLinkable(File file) {
    ClassmanLinkResolution resolution = new ClassmanLinkResolver().resolve(file);
    if (!resolution.isOk()) {
      fehler("msg.classman.file.invalid", file.getName(), resolution.problemText());
      return null;
    }
    if (LinkedClassmanDuplicates.isLinkedBy(file, LinkedClassmanAreaCollector.areasOf(context::traverseDiagram))) {
      fehler("msg.classman.file.linked.already", file.getName());
      return null;
    }
    return resolution;
  }
}

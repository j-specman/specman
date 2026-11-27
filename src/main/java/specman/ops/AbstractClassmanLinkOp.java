package specman.ops;

import specman.editarea.image.classman.LinkedClassmanDuplicates;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.visitor.LinkedClassmanAreaCollector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** What the operations which link a Classman diagram to a file the user chose have in common. */
abstract class AbstractClassmanLinkOp extends AbstractSpecmanOp {

  AbstractClassmanLinkOp(SpecmanOpContext context) {
    super(context);
  }

  /** @return what the file has, if a new diagram can link it, see {@link #resolveLinkable(File, LinkedClassmanImageEditArea)} */
  ClassmanLinkResolution resolveLinkable(File file) {
    return resolveLinkable(file, null);
  }

  /** @param relinked the diagram which is to link another file, null for a new diagram. It doesn't count as one which
   *                  links its file already, so it may take the file it has with another link.
   * @return what the file has, if the diagram can link it: It has to have a class diagram, and no other diagram of the
   * document may link it. Otherwise null, and the user got told why. */
  ClassmanLinkResolution resolveLinkable(File file, LinkedClassmanImageEditArea relinked) {
    ClassmanLinkResolution resolution = new ClassmanLinkResolver().resolve(file);
    if (!resolution.isOk()) {
      fehler("msg.classman.file.invalid", file.getName(), resolution.problemText());
      return null;
    }
    List<LinkedClassmanImageEditArea> others =
      new ArrayList<>(LinkedClassmanAreaCollector.areasOf(context::traverseDiagram));
    others.remove(relinked);
    if (LinkedClassmanDuplicates.isLinkedBy(file, others)) {
      fehler("msg.classman.file.linked.already", file.getName());
      return null;
    }
    return resolution;
  }
}

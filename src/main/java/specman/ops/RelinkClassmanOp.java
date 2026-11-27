package specman.ops;

import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.LinkAliases;
import specman.link.LinkLocation;
import specman.link.classman.ClassmanLink;
import specman.link.classman.ClassmanLinkResolution;
import specman.undo.props.UDBL;

import java.io.File;

/** Repairs the link of a diagram which is shown as a placeholder. */
public class RelinkClassmanOp extends AbstractClassmanLinkOp {

  public RelinkClassmanOp(SpecmanOpContext context) {
    super(context);
  }

  /** A link by an alias which leads nowhere is repaired by saying which file the alias stands for, which keeps the link as
   * it is. Any other link, and the link of a duplicate, is replaced by a new one. */
  public void relink(LinkedClassmanImageEditArea diagram) {
    if (diagram.relinkableAlias() != null) {
      relinkAlias(diagram, diagram.relinkableAlias());
    }
    else {
      relinkByNewLink(diagram);
    }
  }

  /** The alias is nobody's or stands for a file that isn't there any more. The user is asked which file it stands for, and
   * the answer is remembered, so the other links with the alias work again too. A file which can't be linked is refused,
   * and the user is told why. */
  private void relinkAlias(LinkedClassmanImageEditArea diagram, String alias) {
    File chosen = context.chooseClassmanFileForAlias(alias);
    if (chosen != null && resolveLinkable(chosen) != null) {
      LinkAliases.reassign(chosen, alias);
      diagram.resolveAgain();
    }
  }

  /** The user chooses the file and how the new link names it, like for a new diagram. The alias which the link had can't
   * be taken over: for a duplicate it would take the file away from the other diagram, and a path has none. The link is
   * replaced in one undo step. */
  private void relinkByNewLink(LinkedClassmanImageEditArea diagram) {
    File chosen = context.chooseClassmanFile();
    if (chosen == null) {
      return;
    }
    ClassmanLinkResolution resolution = resolveLinkable(chosen);
    if (resolution == null) {
      return;
    }
    LinkLocation location = context.askClassmanLocation(chosen, resolution.diagramId(), resolution.diagramName());
    if (location != null) {
      LinkAliases.remember(chosen, location);
      UDBL.setLinkUDBL(diagram, ClassmanLink.of(location, resolution.diagramId(), resolution.diagramName()));
    }
  }
}

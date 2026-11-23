package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.StyleProperty_V001;

import java.util.EnumSet;
import java.util.Set;

/** What the format painter has picked up: the style of an element, which it gives to other elements as far as they
 * have the same parts. A part which one of them hasn't, like the fill of a relation, is neither taken nor given. */
final class FormatPainter {
  private ElementStyleModel_V001 style = new ElementStyleModel_V001();
  private Set<StyleProperty_V001> properties = Set.of();

  /** Takes the style of the element, which must have one. */
  void pickUp(AbstractDiagramElementModel_V001 source) {
    style = source.style.copy();
    properties = source.styleProperties();
  }

  /** @return the style which the target gets: its own, with the parts it has in common with the element which was
   * picked up replaced by those of that element, so that a standard part takes a style back, too */
  ElementStyleModel_V001 styleFor(AbstractDiagramElementModel_V001 target) {
    Set<StyleProperty_V001> common = EnumSet.noneOf(StyleProperty_V001.class);
    common.addAll(properties);
    common.retainAll(target.styleProperties());
    return target.style.with(style, common);
  }
}

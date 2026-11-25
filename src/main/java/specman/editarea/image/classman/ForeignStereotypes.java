package specman.editarea.image.classman;

import classman.model.v001.change.StereotypeReferences_V001;

import java.util.LinkedHashSet;
import java.util.Set;

import static specman.Specman.editor;

/** Sources of diagrams which come from outside, from another document or from a file, may use stereotypes which refer to
 * meta tag configurations this document doesn't have. They are dropped. */
public final class ForeignStereotypes {
  private static final StereotypeReferences_V001 STEREOTYPES = new StereotypeReferences_V001();

  private ForeignStereotypes() {}

  /** @param dropped collects the names of the configurations which this document doesn't have
   * @return the source without the stereotypes which refer to them, the given source itself if there are none */
  public static String withoutUnknownConfigs(String source, Set<String> dropped) {
    Set<String> unknown = new LinkedHashSet<>(STEREOTYPES.configsUsedIn(source));
    unknown.removeIf(name -> editor().findMetaTagConfig(name) != null);
    dropped.addAll(unknown);
    return unknown.isEmpty() ? source : STEREOTYPES.withoutConfigs(source, unknown);
  }
}

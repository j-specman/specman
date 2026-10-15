package specman.undo;

import specman.EditException;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigRegistry;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

public class UndoableMetaTagConfigEdited extends AbstractUndoableInteraction {
  private final MetaTagConfigRegistry registry;
  private final MetaTagConfig oldConfig;
  private final MetaTagConfig newConfig;

  public UndoableMetaTagConfigEdited(MetaTagConfigRegistry registry, MetaTagConfig oldConfig, MetaTagConfig newConfig) {
    this.registry = registry;
    this.oldConfig = oldConfig;
    this.newConfig = newConfig;
  }

  @Override
  protected void undoEdit() throws EditException {
    registry.replace(newConfig.getName(), oldConfig);
    editor().showToast(i18n("undo.metatag.config.edited.undo", oldConfig.getName()));
  }

  @Override
  protected void redoEdit() throws EditException {
    registry.replace(oldConfig.getName(), newConfig);
    editor().showToast(i18n("undo.metatag.config.edited.redo", newConfig.getName()));
  }
}

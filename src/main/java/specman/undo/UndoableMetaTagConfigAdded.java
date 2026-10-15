package specman.undo;

import specman.EditException;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigRegistry;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

public class UndoableMetaTagConfigAdded extends AbstractUndoableInteraction {
  private final MetaTagConfigRegistry registry;
  private final MetaTagConfig config;

  public UndoableMetaTagConfigAdded(MetaTagConfigRegistry registry, MetaTagConfig config) {
    this.registry = registry;
    this.config = config;
  }

  @Override
  protected void undoEdit() throws EditException {
    registry.remove(config.getName());
    editor().showToast(i18n("undo.metatag.config.added.undo", config.getName()));
  }

  @Override
  protected void redoEdit() throws EditException {
    registry.add(config);
    editor().showToast(i18n("undo.metatag.config.added.redo", config.getName()));
  }
}

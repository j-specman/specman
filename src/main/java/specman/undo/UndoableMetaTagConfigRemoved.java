package specman.undo;

import specman.EditException;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigRegistry;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

public class UndoableMetaTagConfigRemoved extends AbstractUndoableInteraction {
  private final MetaTagConfigRegistry registry;
  private final MetaTagConfig config;
  private final int index;

  /** @param index the position the configuration had in the list before it was removed */
  public UndoableMetaTagConfigRemoved(MetaTagConfigRegistry registry, MetaTagConfig config, int index) {
    this.registry = registry;
    this.config = config;
    this.index = index;
  }

  @Override
  protected void undoEdit() throws EditException {
    registry.add(index, config);
    editor().showToast(i18n("undo.metatag.config.removed.undo", config.getName()));
  }

  @Override
  protected void redoEdit() throws EditException {
    registry.remove(config.getName());
    editor().showToast(i18n("undo.metatag.config.removed.redo", config.getName()));
  }
}

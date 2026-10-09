package specman.undo;

import specman.EditException;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigRegistry;

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
  }

  @Override
  protected void redoEdit() throws EditException {
    registry.replace(oldConfig.getName(), newConfig);
  }
}

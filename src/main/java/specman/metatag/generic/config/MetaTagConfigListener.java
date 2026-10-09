package specman.metatag.generic.config;

/** Gets informed by the {@link MetaTagConfigRegistry} when a configuration was edited. */
public interface MetaTagConfigListener {
  void configReplaced(MetaTagConfig oldConfig, MetaTagConfig newConfig);
}

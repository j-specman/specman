package specman.metatag.generic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The meta tag configurations of a diagram, in the order they were created. */
public class MetaTagConfigRegistry {
  private final Map<String, MetaTagConfig> configsByName = new LinkedHashMap<>();
  private final List<MetaTagConfigListener> listeners = new ArrayList<>();

  public List<MetaTagConfig> all() {
    return Collections.unmodifiableList(new ArrayList<>(configsByName.values()));
  }

  public MetaTagConfig get(String name) {
    return configsByName.get(name);
  }

  public boolean contains(String name) {
    return configsByName.containsKey(name);
  }

  /** @throws IllegalArgumentException if a configuration of that name already exists */
  public void add(MetaTagConfig config) {
    if (contains(config.getName())) {
      throw new IllegalArgumentException("Meta tag configuration '" + config.getName() + "' already exists");
    }
    configsByName.put(config.getName(), config);
  }

  /** Replaces the configuration named oldName by the given one, which may carry a different
   * name. Keeps the position in the list and notifies the listeners.
   * @throws IllegalArgumentException if there is no configuration named oldName, or the new name
   * is already taken by another configuration */
  public void replace(String oldName, MetaTagConfig newConfig) {
    MetaTagConfig oldConfig = configsByName.get(oldName);
    if (oldConfig == null) {
      throw new IllegalArgumentException("Unknown meta tag configuration '" + oldName + "'");
    }
    if (!oldName.equals(newConfig.getName()) && contains(newConfig.getName())) {
      throw new IllegalArgumentException("Meta tag configuration '" + newConfig.getName() + "' already exists");
    }
    Map<String, MetaTagConfig> reordered = new LinkedHashMap<>();
    configsByName.forEach((name, config) -> {
      if (name.equals(oldName)) {
        reordered.put(newConfig.getName(), newConfig);
      }
      else {
        reordered.put(name, config);
      }
    });
    configsByName.clear();
    configsByName.putAll(reordered);
    new ArrayList<>(listeners).forEach(listener -> listener.configReplaced(oldConfig, newConfig));
  }

  public void remove(String name) {
    configsByName.remove(name);
  }

  /** Forgets all configurations and listeners. Meant for replacing the whole diagram: with its
   * steps, all panels registered as listeners are gone, too, and must not be kept alive by the
   * registry. */
  public void clear() {
    configsByName.clear();
    listeners.clear();
  }

  public void addListener(MetaTagConfigListener listener) {
    listeners.add(listener);
  }

  public void removeListener(MetaTagConfigListener listener) {
    listeners.remove(listener);
  }
}

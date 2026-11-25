package specman.cli.sanitize;

import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.MetaTagConfigModel_V002;
import specman.model.v002.MetaTagModel_V002;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks the meta tag configurations of a model and the tags the steps have been assigned. */
public class MetaTagValidator extends AbstractModelValidator {

  private final DiagramModel_V002 model;

  public MetaTagValidator(DiagramModel_V002 model) {
    super("Invalid meta tags");
    this.model = model;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    Map<String, MetaTagConfigModel_V002> configsByName = checkConfigurations(errors);
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      checkTagsOfStep(step, configsByName, errors);
    }
  }

  /** @return the configurations by their name, the errors of the configurations are added to the specified list. */
  private Map<String, MetaTagConfigModel_V002> checkConfigurations(List<String> errors) {
    Map<String, MetaTagConfigModel_V002> configsByName = new LinkedHashMap<>();
    for (MetaTagConfigModel_V002 config : model.metaTagConfigs) {
      if (configsByName.containsKey(config.name)) {
        errors.add(configError(config, "is defined more than once"));
      }
      configsByName.put(config.name, config);
      if (showsNeitherLabelNorIcon(config)) {
        errors.add(configError(config, "has neither label nor icon"));
      }
    }
    return configsByName;
  }

  private static boolean showsNeitherLabelNorIcon(MetaTagConfigModel_V002 config) {
    return isEmpty(config.label) && isEmpty(config.iconSvg);
  }

  private static boolean hasUnallowedFreetext(MetaTagModel_V002 tag, MetaTagConfigModel_V002 config) {
    return !config.freetextAllowed && isNotEmpty(tag.freetext);
  }

  private static void checkTagsOfStep(AbstractStepModel_V002 step, Map<String, MetaTagConfigModel_V002> configsByName,
                                      List<String> errors) {
    String stepLabel = describe(step);
    Set<String> assignedConfigNames = new HashSet<>();
    for (MetaTagModel_V002 tag : step.tags) {
      if (!assignedConfigNames.add(tag.configName)) {
        errors.add(tagError(stepLabel, tag, "is assigned more than once"));
      }
      checkTagAgainstConfiguration(stepLabel, tag, configsByName.get(tag.configName), errors);
    }
  }

  private static void checkTagAgainstConfiguration(String stepLabel, MetaTagModel_V002 tag,
                                                   MetaTagConfigModel_V002 config, List<String> errors) {
    if (config == null) {
      errors.add(tagError(stepLabel, tag, "has no configuration"));
    }
    else if (hasUnallowedFreetext(tag, config)) {
      errors.add(tagError(stepLabel, tag, "has freetext but its configuration does not allow freetext"));
    }
  }

  private static String configError(MetaTagConfigModel_V002 config, String problem) {
    return "Meta tag configuration '" + config.name + "' " + problem;
  }

  private static String tagError(String stepLabel, MetaTagModel_V002 tag, String problem) {
    return stepLabel + ": tag '" + tag.configName + "' " + problem;
  }
}

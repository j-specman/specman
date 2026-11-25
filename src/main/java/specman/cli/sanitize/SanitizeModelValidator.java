package specman.cli.sanitize;

import classman.model.v001.change.StereotypeReferences_V001;
import specman.cli.AbstractCLO;
import specman.cli.CLIException;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.MetaTagConfigModel_V002;
import specman.model.v002.MetaTagModel_V002;
import specman.model.v002.io.TextAreaTraversal_V002;
import specman.model.v002.io.TextAreaTraversal_V002.Site;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks the properties of a model which can't be repaired automatically by the sanitizer.
 * Each check collects all violations it finds and reports them together as one {@link CLIException}.
 */
final class SanitizeModelValidator {

  private static final String UNKNOWN_STEP_NUMBER = "?";
  private static final StereotypeReferences_V001 STEREOTYPES = new StereotypeReferences_V001();

  private SanitizeModelValidator() {
  }

  static void assertNoDuplicateIds(DiagramModel_V002 model) throws CLIException {
    AbstractCLO.assertNoErrors("Duplicate step IDs", describeDuplicateIds(model));
  }

  static void assertValidMetaTags(DiagramModel_V002 model) throws CLIException {
    List<String> errors = new ArrayList<>();
    Map<String, MetaTagConfigModel_V002> configsByName = checkConfigurations(model, errors);
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      checkTagsOfStep(step, configsByName, errors);
    }
    checkStereotypesOfDiagrams(model, configsByName.keySet(), errors);
    AbstractCLO.assertNoErrors("Invalid meta tags", errors);
  }

  private static List<String> describeDuplicateIds(DiagramModel_V002 model) {
    List<String> descriptions = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : collectStepNumbersById(model).entrySet()) {
      if (entry.getValue().size() > 1) {
        descriptions.add("ID " + entry.getKey() + " used by steps: " + entry.getValue());
      }
    }
    return descriptions;
  }

  private static Map<String, List<String>> collectStepNumbersById(DiagramModel_V002 model) {
    Map<String, List<String>> stepNumbersById = new LinkedHashMap<>();
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      stepNumbersById.computeIfAbsent(step.id, id -> new ArrayList<>()).add(displayedStepNumber(step));
    }
    return stepNumbersById;
  }

  /** @return the configurations by their name, the errors of the configurations are added to the specified list. */
  private static Map<String, MetaTagConfigModel_V002> checkConfigurations(DiagramModel_V002 model, List<String> errors) {
    Map<String, MetaTagConfigModel_V002> configsByName = new LinkedHashMap<>();
    for (MetaTagConfigModel_V002 config : model.metaTagConfigs) {
      if (configsByName.put(config.name, config) != null) {
        errors.add(configError(config, "is defined more than once"));
      }
      if (!showsLabelOrIcon(config)) {
        errors.add(configError(config, "has neither label nor icon"));
      }
    }
    return configsByName;
  }

  private static boolean showsLabelOrIcon(MetaTagConfigModel_V002 config) {
    return isNotEmpty(config.label) || isNotEmpty(config.iconSvg);
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
    else if (!config.freetextAllowed && isNotEmpty(tag.freetext)) {
      errors.add(tagError(stepLabel, tag, "has freetext but its configuration does not allow freetext"));
    }
  }

  /** The stereotype of a class in a class diagram may refer to a configuration, like a tag at a step does. The old text
   * of a change mark is no use of it, it may refer to a configuration which has gone. */
  private static void checkStereotypesOfDiagrams(DiagramModel_V002 model, Set<String> configNames, List<String> errors) {
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> {
      for (AbstractEditAreaModel_V002 area : content.areas) {
        if (area instanceof ClassmanImageEditAreaModel_V002 diagram) {
          checkStereotypesOfDiagram(diagram.source, site, configNames, errors);
        }
      }
    });
  }

  private static void checkStereotypesOfDiagram(String source, Site site, Set<String> configNames, List<String> errors) {
    for (String configName : STEREOTYPES.configsUsedIn(source)) {
      if (!configNames.contains(configName)) {
        for (String className : STEREOTYPES.classesUsing(source, configName)) {
          errors.add(describe(site) + ": class '" + className + "' has a stereotype of the tag '" + configName
            + "' which has no configuration");
        }
      }
    }
  }

  private static String configError(MetaTagConfigModel_V002 config, String problem) {
    return "Meta tag configuration '" + config.name + "' " + problem;
  }

  private static String tagError(String stepLabel, MetaTagModel_V002 tag, String problem) {
    return stepLabel + ": tag '" + tag.configName + "' " + problem;
  }

  /** @return e.g. "Step 1.2 (a1b2c3d4) text", or "intro" */
  static String describe(Site site) {
    return site.step() != null ? describe(site.step()) + " " + site.part() : site.part();
  }

  /** @return e.g. "Step 1.2 (a1b2c3d4)" */
  static String describe(AbstractStepModel_V002 step) {
    return "Step " + displayedStepNumber(step) + " (" + step.id + ")";
  }

  private static String displayedStepNumber(AbstractStepModel_V002 step) {
    return step.stepNumber != null ? step.stepNumber : UNKNOWN_STEP_NUMBER;
  }

  private static boolean isNotEmpty(String text) {
    return text != null && !text.isEmpty();
  }
}

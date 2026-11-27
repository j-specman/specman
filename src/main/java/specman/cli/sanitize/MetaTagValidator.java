package specman.cli.sanitize;

import classman.model.v001.change.StereotypeReferences_V001;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;
import specman.model.v002.MetaTagConfigModel_V002;
import specman.model.v002.MetaTagModel_V002;
import specman.model.v002.io.TextAreaTraversal_V002;
import specman.model.v002.io.TextAreaTraversal_V002.Site;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks the meta tag configurations of a model and the tags the steps have been assigned. */
public class MetaTagValidator extends AbstractModelValidator {

  private static final StereotypeReferences_V001 STEREOTYPES = new StereotypeReferences_V001();

  private final DiagramModel_V002 model;
  private final ClassmanLinkResolver resolver;

  /** @param resolver follows the links of linked diagrams, relative to the document */
  public MetaTagValidator(DiagramModel_V002 model, ClassmanLinkResolver resolver) {
    super("Invalid meta tags");
    this.model = model;
    this.resolver = resolver;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    Map<String, MetaTagConfigModel_V002> configsByName = checkConfigurations(errors);
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      checkTagsOfStep(step, configsByName, errors);
    }
    checkStereotypesOfDiagrams(configsByName.keySet(), errors);
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

  /** The stereotype of a class in a class diagram may refer to a configuration, like a tag at a step does. The old text
   * of a change mark is no use of it, it may refer to a configuration which has gone. A linked diagram is checked as
   * the file has it, if the link leads to a diagram. */
  private void checkStereotypesOfDiagrams(Set<String> configNames, List<String> errors) {
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> {
      for (AbstractEditAreaModel_V002 area : content.areas) {
        if (area instanceof ClassmanImageEditAreaModel_V002 diagram) {
          checkStereotypesOfDiagram(diagram.source, site, configNames, errors);
        }
        else if (area instanceof LinkedClassmanImageEditAreaModel_V002 linked) {
          ClassmanLinkResolution resolution = resolver.resolve(linked.link);
          if (resolution.isOk()) {
            checkStereotypesOfDiagram(resolution.source(), site, configNames, errors);
          }
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
}

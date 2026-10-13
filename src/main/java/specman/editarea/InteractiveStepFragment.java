package specman.editarea;

import specman.metatag.stepnumberlabel.StepnumberLabel;

/** Tag Interface für grafische Bestandteile eines Schritts, die Ziel einer
 * Interaktion sind, und über die man den betreffenden Schritt im Diagramm
 * suchen kann. Aktuell sind dies zum einen die {@link TextEditArea}s und
 * zum anderen die {@link StepnumberLabel}. Letztere sind relevant, weil
 * der User über diese per Drag&Drop einen Schritt verschieben kann. */
public interface InteractiveStepFragment {
  /** Dient nur einer Debug-Ausgabe im Falle eines fehlenden Lookups von Schritten. */
  String getText();
}
package specman.link.suggestion;

import specman.link.LinkAliases;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.io.FilenameUtils.getBaseName;

/** Diese Klasse versucht aus bestehenden Zuordnungen von Aliassen zu Dateinamen einen Alias für eine neue Datei vorzuschlagen.
 * Im Einzelnen passiert Folgendes:
 * <ul>
 * <li>Untersucht werden nur die Zuordnungen, die sich auf Dateien im gleichen Verzeichnis beziehen. Die Annahme ist, dass nur
 * die Specs innerhalb eines Verzeichnisses verlässlich einem gemeinsamen Namensschema folgen, aus denen sich sinnvoll ein Vorschlag
 * ableiten lässt</li>
 * <li>Für alle diese Zuordnungen versucht das System einen Vorschlag abzuleiten</li>
 * <li>In die Vorschlagsermittlung geht nur der Basename der Datei, also weder Pfad noch Extension</li>
 * <li>Alle ermittelten Vorschläge gehen als AliasMatch (zugrunde liegende Zuordnung und abgeleiteter Vorschlag) in ein Scoring ein,
 * welcher der beste ist</li>
 * <li>Stand Okt. 2026 ist das Scoring trivial: Es gewinnt der Vorschlag, der auf den längsten Alias zurückgeht - bei mehreren Kandidaten der erstbeste</li>
 * </ul>
 * Ein Vorschlag wird wie folgt ermittelt:
 * <ul>
 *   <li>Der Alias wird in typisierte Zeichen zerlegt (siehe Klassen AliasCharacter). Da Aliasse nur aus den Zeichen
 *     A-Z, a-z, 0-9, "-" und "_" bestehen dürfen, gibt es genau drei Typen: Buchstabe (implizit nur ASCII-Buchstaben, ein "ä" oder
 *     "ß" im Dateinamen ist also kein Buchstabe im Sinne dieser Klasse), Ziffer und Trenner. "-" und "_" sind beide vom Typ Trenner
 *     und gelten als gleichwertig, ganz ähnlich wie Groß- und Kleinbuchstaben</li>
 *   <li>Die zugeordnete Datei wird von links nach rechts nach dem Vorkommen der Zeichen mit ihren Positionen durchsucht.
 *     Buchstaben case-insensitive, Trenner passen auf "-" und auf "_". Es kommt eine Folge von AliasCharacters mit den Indices
 *     im Dateinamen heraus</li>
 *   <li>Sind nicht alle Zeichen enthalten, ergibt sich aus der Zuordnung kein Vorschlag</li>
 *   <li>Anschließend erfolgt eine Untersuchung der neuen Datei, ob an allen Positionen der Zeichentyp aus der Datei der
 *     Zuordnung zu finden ist. Ist der Dateiname zu kurz oder passt ein Zeichentyp nicht, ergibt sich aus der Zuordnung kein Vorschlag</li>
 * </ul>
 * Der beste Vorschlag kommt zur Anzeige, durchläuft aber noch eine abschließende Umwandlung, sodass alle enthaltenen
 * Buchstaben in der Groß-/Kleinschreibung und alle Trenner in der Form ("-" oder "_") dem Alias entsprechen, aus dem der
 * Vorschlag hervorgegangen ist.
 * <br>
 * Beispiele:
 * <ul>
 *   <li>Alias "uc720" für Datei mit Basename "BALI UC-720 Kunde speichern" ergibt einen Vorschlag: Case-insensitive betrachtet,
 *     finden sich alle Zeichen des Alias "uc720" im Dateinamen wieder an Index 5, 6, 8, 9, 10</li>
 *   <li>Eine Datei mit Basename "BALI UC-315 Bilanzen speichern" hat an den passenden Positionen die passenden Zeichentypen:
 *     "U, C, 3, 1, 5"</li>
 *   <li>Ist uc720 der beste Vorschlag, erfolgt eine Umwandlung der Zeichenfolge "U, C, 3, 1, 5" in "u, c, 3, 1, 5"</li>
 *   <li>Eine Datei mit Basename "testusecase" passt nicht, weil sie zwar genügend Zeichen hat (Index bis 10),
 *    aber an Index 8, 9 und 10 stehen keine Ziffern</li>
 *   <li>Alias "uc-720" für Datei "BALI UC_720 Kunde speichern": Der Trenner an Index 7 passt auf den Alias-Trenner "-".
 *     Für "BALI UC-315 Bilanzen speichern" ergibt sich "UC-315" und für "BALI UC_315 Bilanzen speichern" "UC_315", beides wird
 *     bei der abschließenden Umwandlung zu "uc-315"</li>
 * </ul>
 * Die Bevorzugung der Vorschläge nach Länge des Alias beruht auf typischen Konventionen z.B. der Art "BALI UC-200-02 ..." für
 * Secondary Usecases. Geht es also grade um einen Vorschlag für einen solchen Secondary und ist ein solcher schon mal mit einem Alias
 * versehen worden, dann wird dessen Aliasmuster bevorzugt.
 * <br>
 * Die Positionen im Dateinamen sind bewusst starr. Bei einem ungünstigen Namensschema mit unterschiedlich langen Nummern
 * (z.B. "UC-730" und "UC-1350" statt "UC-0730" und "UC-1350") führt das beim ersten Mal zu einem falschen Vorschlag:
 * Aus dem Alias "uc730" ergibt sich für "BALI UC-1350 ..." der Vorschlag "uc135" statt "uc1350". Das ist akzeptiert,
 * denn der Vorschlag wird im Dialog nur vorbelegt und vom Nutzer korrigiert. Der korrigierte Alias "uc1350" ist länger als
 * "uc730" und wird deshalb bei der nächsten Datei mit vierstelliger Nummer bevorzugt, das Problem heilt sich also selbst.
 * Bei einer dreistelligen Nummer scheitert "uc1350" an der Typprüfung, sodass weiterhin "uc730" greift.*/
public class AliasSuggestion {
  private final List<LinkAliases.Entry> entries;

  public AliasSuggestion(List<LinkAliases.Entry> entries) {
    this.entries = entries;
  }

  /** @return the suggested alias, null if no existing alias is a suitable example for the file */
  public String suggest(File file) {
    List<AliasMatch> matches = collectMatches(file);
    AliasMatch best = pickBestMatch(matches);
    return best != null ? best.suggestion() : null;
  }

  private List<AliasMatch> collectMatches(File file) {
    String baseName = getBaseName(file.getName());
    return entriesInDirectoryOf(file)
      .stream()
      .map(entry -> matchOf(entry, baseName))
      .filter(Objects::nonNull)
      .toList();
  }

  /** @return the match with the highest score, the first one of several with the same score, null if there are no matches */
  private AliasMatch pickBestMatch(List<AliasMatch> matches) {
    return matches
      .stream()
      .max(Comparator.comparingInt(AliasMatch::score))
      .orElse(null);
  }

  private List<LinkAliases.Entry> entriesInDirectoryOf(File file) {
    return entries.stream()
      .filter(entry -> Objects.equals(entry.file().getParentFile(), file.getParentFile()))
      .toList();
  }

  private AliasMatch matchOf(LinkAliases.Entry entry, String baseName) {
    List<AliasCharacter> aliasCharacters = locateInExample(entry);
    if (aliasCharacters == null || !allTypesMatchedIn(aliasCharacters, baseName)) {
      return null;
    }
    return new AliasMatch(entry, aliasCharacters, baseName);
  }

  private boolean allTypesMatchedIn(List<AliasCharacter> aliasCharacters, String fileBaseName) {
    return aliasCharacters.stream().allMatch(aliasCharacter -> aliasCharacter.isTypeMatchedIn(fileBaseName));
  }

  /** @return the characters of the alias with their indices in the example's base name, null if they can't all be found */
  private List<AliasCharacter> locateInExample(LinkAliases.Entry entry) {
    return AliasCharacter.locateAll(entry.alias(), getBaseName(entry.file().getName()));
  }
}

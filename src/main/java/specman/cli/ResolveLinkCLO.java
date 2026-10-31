package specman.cli;

import specman.link.resolver.LinkResolution;
import specman.link.resolver.LinkResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/**
 * Checks where Specman links lead to, see {@link LinkResolver}. The links are the arguments behind the operation name, or
 * the lines of stdin if there are none, which allows to feed it from other tools via pipes.
 * <p>
 * The output has exactly one line for each link, in the order of the links, whatever they look like:
 * <pre>
 *   STATUS TAB file TAB detail
 * </pre>
 * The status is OK, SANITIZED (the link's step number was wrong) or ERROR. The file is the diagram file the link leads to,
 * empty if it doesn't lead to one. The detail is the complete link with the step's current number for OK and SANITIZED, and
 * an explanation for ERROR. The lines end with a plain line feed on all platforms and are encoded in UTF-8.
 * <p>
 * Exit code 0 = no line is an ERROR, 1 = at least one is.
 */
public class ResolveLinkCLO extends AbstractCLO {
  /** Separates the status, file and detail in each line of the output. */
  public static final String FIELD_SEPARATOR = "\t";
  public static final String LINE_END = "\n";

  private final List<String> linksFromArguments;

  ResolveLinkCLO(String[] args) {
    this.linksFromArguments = Arrays.asList(args).subList(1, args.length);
  }

  int run() {
    PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
    LinkResolver resolver = new LinkResolver();
    boolean anyError = false;
    for (String link : requestedLinks()) {
      LinkResolution resolution = resolver.resolve(link);
      out.print(resolution.toLine(FIELD_SEPARATOR) + LINE_END);
      anyError |= resolution.isError();
    }
    out.flush();
    return anyError ? EXIT_ERROR : EXIT_SUCCESS;
  }

  private List<String> requestedLinks() {
    return linksFromArguments.isEmpty() ? readLinksFromStdin() : linksFromArguments;
  }

  private List<String> readLinksFromStdin() {
    List<String> links = new ArrayList<>();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
      String line = reader.readLine();
      while (line != null) {
        links.add(line);
        line = reader.readLine();
      }
    }
    catch (IOException e) {
      throw error("Links can't be read from stdin: " + e.getMessage());
    }
    return links;
  }
}

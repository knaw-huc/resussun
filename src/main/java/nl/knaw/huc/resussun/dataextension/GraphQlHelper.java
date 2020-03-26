package nl.knaw.huc.resussun.dataextension;

public class GraphQlHelper {
  // see: http://spec.graphql.org/June2018/#sec-Appendix-Grammar-Summary.Lexical-Tokens
  public static String escapeGraphQl(String label) {

    if (label.substring(0, 1).matches("[^_A-Za-z]")) {
      label = label.replaceFirst("[^_A-Za-z]", "_");
    }

    return label.replaceAll("\\W", "_");
  }
}

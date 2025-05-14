package prova.stdcla.jdk;

import java.awt.Color;

/**
 * seguito dal sito <a href=
 * "https://advancedweb.hu/a-categorized-list-of-all-java-and-jvm-features-since-jdk-8-to-21"
 * target="_blank">advanced web</a> }
 * <p>
 * vedi anche <a href="https://www.java.com/releases/matrix/">mappa dei JDK</a>
 */
public class PrJdkFeatures {

  private record Punto(int x, int y) {
  }

  private record ColoredPoint(Punto p, Color col) {
  }

  private enum Giorno {
    lunedi, martedi, mercoledi, giovedi, venerdi, sabato, domenica
  }

  public PrJdkFeatures() {
    //
  }

  public static void main(String[] args) {
    var app = new PrJdkFeatures();
    app.doTheJob();
  }

  private void doTheJob() {
    feature01(2, 12, Long.parseLong("19570723"), "Claudio");
    feature02();
    feature03();
    feature04();
    feature05();
  }

  /**
   * Pattern Matching for switch supporting type patterns and guarded patterns
   * JDK 21 (Preview in JDK 20 JDK 19 JDK 18 JDK 17)
   *
   * @param po
   */
  private void feature01(Object... po) {
    for (Object o : po) {
      String formatted = switch (o) {
        case Integer i when i > 10 -> String.format("a large Integer %d", i);
        case Integer i -> String.format("a small Integer %d", i);
        case Long l -> String.format("a Long %d", l);
        default -> o.toString();
      };
      System.out.println(formatted);
    }
  }

  /**
   * Record Patterns for switch and instanceof to deconstruct complex nested
   * structures
   */
  private void feature02() {
    Punto p = new Punto(5, 8);
    ColoredPoint r = new ColoredPoint(p, new Color(246));
    if (r instanceof ColoredPoint(Punto(int x, int y), Color c)) {
      System.out.printf("punto( %d, %d ) color=%s\n", x, y, c);
    }
  }

  /**
   * preview in JDK 21
   */
  private void feature03() {
    // Set<Integer> s = new HashSet<Integer>();
    // var _ = s.add(33);
  }

  /**
   * Long multi line Text Blocks
   */
  private void feature04() {
    String html = """
        <html>
            <body>
                <p>Hello, world</p>
            </body>
        </html>
        """;
    System.out.println("PrJdkFeatures.feature04()");
    System.out.println(html);
  }

  /**
   * Switch expression
   */
  private void feature05() {
    Giorno gg = Giorno.martedi;
    String sz = switch (gg) {
      case lunedi, giovedi -> "Giornata lunga";
      case sabato, domenica -> "Riposo";
      default -> "giornata corta";
    };
    System.out.printf("feature05(%s = %s)\n", gg.toString(), sz);
  }
}

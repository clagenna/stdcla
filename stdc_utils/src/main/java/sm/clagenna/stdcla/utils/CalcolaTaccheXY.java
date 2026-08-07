package sm.clagenna.stdcla.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * (Claude) 2026-07-15 Calcola le posizioni delle tacche su un asse di ascisse,
 * mappando un intervallo di valori double su un intervallo di pixel di un
 * JavaFX Pane.
 *
 * I valori delle tacche intermedie vengono arrotondati a "numeri belli" (1, 2 o
 * 5 seguiti da zeri, es: 1000, 120, 0.2, 5000...) secondo il classico algoritmo
 * di Paul Heckbert ("Nice Numbers for Graph Labels"). I punti vMinX e vMaxX
 * vengono sempre inclusi con il loro valore esatto (non arrotondato).
 */
public class CalcolaTaccheXY {

  /**
   * Rappresenta una singola tacca: valore nel dominio (vX) e posizione
   * corrispondente in pixel (pixelX).
   */
//  public record PosTacca(double value, double pixel) {
//  }

  /**
   * Calcola la lista delle tacche da disegnare.
   *
   * @param vMin
   *          valore minimo del dominio
   * @param vMax
   *          valore massimo del dominio
   * @param pixMin
   *          pixel corrispondente a vMinX
   * @param pixMax
   *          pixel corrispondente a vMaxX
   * @param qtaTacche
   *          numero indicativo di tacche desiderate (incluse le estremità). Il
   *          numero effettivo di tacche generate può differire leggermente
   *          perché i valori vengono arrotondati a "numeri belli".
   * @return lista di PosTacca, non ordinata per costruzione ma già in ordine
   *         crescente/decrescente coerente con vMinX/vMaxX; essendo una List<>
   *         può comunque essere riordinata a piacere.
   */
  public static List<PosTacca> calcolaTacche(double vMin, double vMax //
      , double pixMin, double pixMax //
      , int qtaTacche) {
    List<PosTacca> result = new ArrayList<>();

    // Caso degenere: nessun intervallo utile o richiesta insensata
    if (vMax == vMin || qtaTacche < 2) {
      result.add(new PosTacca(vMin, pixMin));
      if (vMax != vMin) {
        result.add(new PosTacca(vMax, pixMax));
      }
      return result;
    }

    // Lavoriamo sempre con min < max, poi rimappiamo su vMinX/vMaxX reali
    double domMin = Math.min(vMin, vMax);
    double domMax = Math.max(vMin, vMax);

    double range = niceNum(domMax - domMin, false);
    double step = niceNum(range / (qtaTacche - 1), true);

    double niceMin = Math.ceil(domMin / step) * step;
    double niceMax = Math.floor(domMax / step) * step;

    // Punto iniziale esatto (non arrotondato)
    result.add(new PosTacca(vMin, mapToPixel(vMin, vMin, vMax, pixMin, pixMax)));

    // Tolleranza per evitare tacche duplicate troppo vicine agli estremi
    double eps = step * 1e-6;

    for (double v = niceMin; v <= niceMax + eps; v += step) {
      if (v > domMin + eps && v < domMax - eps) {
        double vRounded = roundToStep(v, step);
        double pixel = mapToPixel(vRounded, vMin, vMax, pixMin, pixMax);
        result.add(new PosTacca(vRounded, pixel));
      }
    }

    // Punto finale esatto (non arrotondato)
    result.add(new PosTacca(vMax, mapToPixel(vMax, vMin, vMax, pixMin, pixMax)));

    return result;
  }

  /** Mappa linearmente un valore del dominio in un pixel. */
  private static double mapToPixel(double v, double vMinX, double vMaxX, double pixMinX, double pixMaxX) {
    return pixMinX + (v - vMinX) / (vMaxX - vMinX) * (pixMaxX - pixMinX);
  }

  /**
   * Algoritmo di Heckbert per trovare un "numero bello" vicino a range. Se
   * round=true arrotonda al più vicino tra {1,2,5,10}*10^exp, se round=false
   * arrotonda per eccesso (usato per calcolare il range).
   */
  private static double niceNum(double range, boolean round) {
    double exponent = Math.floor(Math.log10(range));
    double fraction = range / Math.pow(10, exponent);
    double niceFraction;

    if (round) {
      if (fraction < 1.5)
        niceFraction = 1;
      else if (fraction < 3)
        niceFraction = 2;
      else if (fraction < 7)
        niceFraction = 5;
      else
        niceFraction = 10;
    } else {
      if (fraction <= 1)
        niceFraction = 1;
      else if (fraction <= 2)
        niceFraction = 2;
      else if (fraction <= 5)
        niceFraction = 5;
      else
        niceFraction = 10;
    }

    return niceFraction * Math.pow(10, exponent);
  }

  /**
   * Arrotonda v al numero di decimali coerente con lo step, per evitare errori
   * di arrotondamento in virgola mobile (es: 0.1 + 0.2 = 0.30000000004).
   */
  private static double roundToStep(double v, double step) {
    int decimals = Math.max(0, -(int) Math.floor(Math.log10(step)) + 1);
    double factor = Math.pow(10, decimals);
    return Math.round(v * factor) / factor;
  }

  // ------------------- Esempio di utilizzo -------------------
  public static void main(String[] args) {
    double vMinY = -122.3;
    double vMaxY = 183.72;
    double pY1 = 50;
    double pY2 = 850;
    final int qtaTacche = 50;
    List<PosTacca> tacche = calcolaTacche( vMinY, vMaxY, pY2, pY1 /* , pY2 */, qtaTacche);
    PosTacca tk = null;
    //    tacche.forEach(t -> {
    //      double diffPix = 0;
    //      if (tk != null)
    //        diffPix = t.pixelX() - tk.pixelX();
    //      System.out.printf("x=%-8s pixX=%6s\n", Utils.formatDouble(t.vX()), Utils.formatLong((long) t.pixelX()));
    //      tk = t;
    //    });

    for (PosTacca t : tacche) {
      double diffPix = 0;
      if (tk != null)
        diffPix = t.getPixel() - tk.getPixel();
      System.out.printf("x=%-8s pixX=%6s, \tDiff=%s\n", Utils.formatDouble(t.getValue()), Utils.formatLong((long) t.getPixel()),
          Utils.formatDouble(diffPix));
      tk = t;
    }

    System.out.printf("Qta tacche generate: %d\n", tacche.size());
    System.out.println("---");

    List<PosTacca> tacche2 = calcolaTacche(0.05, 0.42, 50, 850, qtaTacche);
    tacche2.forEach(t -> System.out.printf("x=%-8s pixX=%6s\n", Utils.formatDouble(t.getValue()), Utils.formatLong((long) t.getPixel())));
    System.out.printf("Qta tacche generate: %d\n", tacche.size());
  }
}

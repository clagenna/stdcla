package sm.clagenna.stdcla.utils;

import lombok.Data;

/**
 * Valore intrinseco (di solito l'ascisse e/o ordinata) con la sua conversione in
 * posizione sull'asse di una "tacca" in un grafico.
 */
@Data
public class PosTacca {

  private double value;
  private double pixel;
  private Object rifObj;

  public PosTacca(double value, double pixel) {
    this.value = value;
    this.pixel = pixel;
    this.rifObj = null;
  }

  public PosTacca(double value, double pixel, Object rifObj) {
    this.value = value;
    this.pixel = pixel;
    this.rifObj = rifObj;
  }

  public String toString() {
    return String.format("PosTacca [value=%.2f, pixel=%.2f, rifObj=%s]", value, pixel, rifObj);
  }
}

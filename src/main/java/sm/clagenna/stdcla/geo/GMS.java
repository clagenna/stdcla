package sm.clagenna.stdcla.geo;

import java.util.Locale;

public record GMS(String nsow, int grad, int min, double seco) {
  
  @Override
  public String toString() {
    return String.format(Locale.US, "%d°%02d'%2.4f\"%s", grad, min, seco, nsow);
  }
}
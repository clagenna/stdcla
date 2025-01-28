package prova.stdcla.utils;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

public class ProvaCurrency {

  public ProvaCurrency() {
    //
  }

  @Test
  public void provalo() {
    Map<Locale, Currency> curcys = getAllCurrencies();

    for (Locale cu : curcys.keySet()) {
      System.out.printf("provalo(%s=%s)\n", cu.getDisplayCountry() , curcys.get(cu).toString());
    }
  }

  public Map<Locale, Currency> getAllCurrencies() {
    Map<Locale, Currency> toret = new HashMap<Locale, Currency>();
    Locale[] locs = Locale.getAvailableLocales();
    for (Locale loc : locs) {
      try {
        Currency currency = Currency.getInstance(loc);
        if (currency != null)
          toret.put(loc, currency);
      } catch (Exception exc) {
        // exc.printStackTrace();
      }
    }
    return toret;
  }
}

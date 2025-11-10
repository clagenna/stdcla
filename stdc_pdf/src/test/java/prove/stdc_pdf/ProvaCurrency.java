package prove.stdc_pdf;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.junit.Test;

public class ProvaCurrency {

  @Test
  public void doit() {
    String[] szCy = { "234,56", "1.234,56", //
        "2,345.78", "88.23" };
    NumberFormat fmtIta = NumberFormat.getCurrencyInstance(Locale.ITALY);
    NumberFormat fmtUs = NumberFormat.getCurrencyInstance(Locale.US);
    Number nu;
    String sz;
    Currency cITA = Currency.getInstance(Locale.ITALY);
    String curITA = cITA.getSymbol();
    //    System.out.printf("%s\n", fmtIta.format(123456.34f));
    //    System.out.printf("%s\n", fmtUs.format(123456.34f));
    //    try {
    //      String pro ="3.456,34 €";
    //      nu = fmtIta.parse(pro);
    //      curITA = pro.substring(8);
    //      pro = "987.654,33" + curITA;
    //      nu = fmtIta.parse(pro);
    //          
    //    } catch (ParseException e) {
    //      e.printStackTrace();
    //    }
    curITA = " €"; // €
    for (String cy : szCy) {
      try {
        sz = cy + curITA;
        nu = fmtIta.parse(sz);
        System.out.printf("ITA: %12s %s\n", cy, nu.toString());
      } catch (Exception e) {
        System.err.println("ITA:" + e.getMessage());
      }
      try {
        sz = "$" + cy;
        nu = fmtUs.parse(sz);
        System.out.printf("USA: %12s %s\n", cy, nu.toString());
      } catch (Exception e) {
        System.err.println("USA" + e.getMessage());
      }
    }
  }
}

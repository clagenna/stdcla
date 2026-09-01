package prova.utils;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class ProvaParseDouble {

  List<String> testValues;

  @SuppressWarnings("unused")
  @Test
  public void testParseDouble() {
    testValues = new ArrayList<>();
    testValues.addAll(Arrays.asList("123.45", "1,234.56", "1.234,56", "1 234,56", "1'234.56", "invalid"));

    byte[] arr = new byte[] { 43, 0, 49, 0, 55, 0, 57, 0, 44, 0, 57, 0, 55, 0, -3, -1 };
    String sz_16 = new String(arr, StandardCharsets.UTF_16); // -- bad, non riconosce i caratteri 
    String sz_16BE = new String(arr, StandardCharsets.UTF_16BE); //--- bad, non riconosce il BOM
    String sz_16LE = new String(arr, StandardCharsets.UTF_16LE); // Ok! ->  "+179,97ü" == UTF_16LE 
    String sz_8 = new String(arr, StandardCharsets.UTF_8); // -- bad, non riconosce gli zeri intermedi

    arr = new byte[] { 0, 43, 0, 49, 0, 55, 0, 57, 0, 44, 0, 57, 0, 55, -3, -1 };
    String sz2_16 = new String(arr, StandardCharsets.UTF_16); // Ok! -> +179,97. ma non riconosce il BOM 
    String sz2_16BE = new String(arr, StandardCharsets.UTF_16BE); // Ok! -> +179,97. ma non riconosce il BOM 
    String sz2_16LE = new String(arr, StandardCharsets.UTF_16LE); // -- bad, non riconosce gli zeri intermedi
    String sz2_8 = new String(arr, StandardCharsets.UTF_8); // -- bad, " + 1 7 9 , 9 7 ??" non riconosce gli zeri intermedi

    testValues.add(0, sz_16LE);
    for (String value : testValues) {
      try {
        Double parsedValue = parseDouble(value);
        System.out.println("double Val '" + value + "' = " + (null == parsedValue ? "null" : parsedValue));
      } catch (NumberFormatException e) {
        System.out.println("Failed to parse '" + value + "': " + e.getMessage());
      }
    }
  }

  public Double parseDouble(String p_sz) {
    Double ii = null;
    Locale locale = Locale.getDefault();
    if (null == p_sz)
      return ii;
    // String cleaned = p_sz.replaceAll("\\p{C}", "").trim(); !! non funziona (UNICODE!)
    String cleaned = p_sz.replaceAll("[^\\x00-\\x7F]", "").trim(); // tolgo i caratteri non ASCII !
    String clean2 = cleaned.replaceAll("[^0-9.,+-]", ""); // tolgo i caratteri non numerici
    if (clean2.length() != cleaned.length()) {
      return ii;
    }
    // ---------------------------------------------------
    // questo perche' dagli USA mi arrivano double della forma "-9,99" ?!?
    // devo sovrascrivere il tipo di formatter
    int nv = cleaned.length() - cleaned.lastIndexOf(",");
    if (nv == 3)
      locale = Locale.ITALY;
    // ---------------------------------------------------
    // Se ho 2 decimali dopo il punto, allora sono in formato USA
    nv = cleaned.length() - cleaned.lastIndexOf(".");
    if (nv == 3)
      locale = Locale.US;
    // ---------------------------------------------------
    NumberFormat fmt = NumberFormat.getInstance(locale);
    try {
      // cambiare i "." e "," a priori è un arbitrio, va chiamata la Utils.setLocale()
      // String sz = psz.trim().replace(S_Group_Sep, "").replace(S_Decimal_Sep, ".");
      double mult = 1.0;
      if (cleaned.length() > 1) {
        // verifico se il primo carattere è un segno, in tal caso lo tolgo e lo salvo per moltiplicare il risultato
        String strt = cleaned.substring(0, 1);
        if (strt.equals("-") || strt.equals("+")) {
          mult = strt.equals("-") ? -1.0 : 1.0;
          cleaned = cleaned.substring(1);
        }
        // verifico se il primo carattere è una virgola, in tal caso lo tolgo aggiungo zero all'inizio
        strt = cleaned.substring(0, 1);
        if (strt.equals(","))
          cleaned = "0" + cleaned;
        ii = fmt.parse(cleaned).doubleValue() * mult;
      }
    } catch (NumberFormatException | ParseException ex) {
      //
    }
    return ii;
  }

}

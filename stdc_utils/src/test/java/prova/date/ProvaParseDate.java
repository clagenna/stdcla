package prova.date;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class ProvaParseDate {
  private static final String[] s_aszTest = { //
      "2024-06-01T12:34:56.789Z" //
      , "2024-06-01T12:34:56.789" //
      , "2024-06-01T12:34:56Z" //
      , "2024-06-01T12:34:56" //
      , "2024-06-01T12:34" //
      , "2024-06-01T12" //
      , "2024-06-01" //
      , "2024-06-01 12:34:56.789" //
      , "2024-06-01 12:34:56" //
      , "2024-06-01 12:34" //
      , "2024-06-01 12" //
      , "2024_06_01 15:32:26" //
      , "2024_06_01" //
      , "24-06-01" //
      , "2024-06" //
      , "20240601 123456" //
      , "20240601 1234" //
      , "20240601" //
      , "23/07/2024 12:34:56.765" //
      , "23/07/2024 12:34:56" //
      , "23/07/2024 12:34" //
      , "23/07/2024" //
      , "23/07/57 23:30" //
      , "23/07/24" //
  };

  static final String[] szPattDateY4MDHMSs = { // 2024-06-01T12:34:56.789Z
      ".*" //
      , "(\\d{4})[-_](\\d{2})" // anno, mese (obbligatori)
      , "(?:[-_](\\d{2})" // giorno (facc)
      /**/, "(?:[T ](\\d{2})" // ore
      /*--*/, "(?::(\\d{2})" // minuti
      /*---*/, "(?::(\\d{2})" // secondi
      /*----*/, "(?:\\.(\\d{1,9}))?" // frazione di secondo
      /*---*/, ")?" // chiusura secondi
      /*--*/, ")?" // chiusura minuti
      /**/, ")?" // chiusura ore
      , ")?" // chiusura giorno
      , "Z?.*$" // resto stringa
  };

  static final String[] szPattDateBY4MDHMSs = { // 20240601 123456.456
      ".*" //
      , "(\\d{4})(\\d{2})(\\d{2})" // anno, mese, giorno (obbligatori)
      /**/, "(?:[ ](\\d{2})" // ore
      /*--*/, "(?:(\\d{2})" // minuti
      /*---*/, "(?:(\\d{2})" // secondi
      /*----*/, "(?:\\.(\\d{1,9}))?" // frazione di secondo
      /*---*/, ")?" // chiusura secondi
      /*--*/, ")?" // chiusura minuti
      /**/, ")?" // chiusura ore
      , "Z?.*$" // resto stringa
  };

  static final String[] szPattDateY2MDHMSs = { // 24-12-31 13:34:56.789
      ".*" //
      , "(\\d{2})[-_](\\d{2})" // anno, mese 
      , "[-_](\\d{2})" // giorno 
      /**/, "(?:[ ](\\d{2})" // ore
      /*--*/, "(?::(\\d{2})" // minuti
      /*---*/, "(?::(\\d{2})" // secondi
      /*----*/, "(?:\\.(\\d{1,9}))?" // frazione di secondo
      /*---*/, ")?" // chiusura secondi
      /*--*/, ")?" // chiusura minuti
      /**/, ")?" // chiusura ore 
      , ".*$" // resto stringa
  };

  static final String[] szPattDateDMY4HMSs = { // 23/07/2024 12:34:56.789
      ".*" //
      , "(\\d{2})[-_/](\\d{2})[-_/](\\d{2,4})" // giorno, mese, anno{4} (obbligatori)
      , "(?:[ ](\\d{2}):(\\d{2})" // ore, minuti
      /**/, "(?::(\\d{2})" // secondi
      /*--*/, "(?:\\.(\\d{1,9}))?" // frazione di secondo
      /**/, ")?" // chiusura secondi
      , ")?" // chiusura ore minuti
      , ".*$" // resto stringa
  };

  static final String[]              szPattDateDMY2HMSs = {                         // 23/07/24 12:34:56.789
      ".*"                                                                          //
      , "(\\d{2})[/](\\d{2})[/](\\d{2})"                                            // giorno, mese, anno{2} (obbligatori)
      , "(?:[ ](\\d{2}):(\\d{2})"                                                   // ore, minuti
      /**/, "(?::(\\d{2})"                                                          // secondi
      /*--*/, "(?:\\.(\\d{1,9}))?"                                                  // frazione di secondo
      /**/, ")?"                                                                    // chiusura secondi
      , ")?"                                                                        // chiusura ore minuti
      , ".*$"                                                                       // resto stringa
  };
  @SuppressWarnings("unused")
  private static final LocalDateTime s_dtMin;
  private static final int           s_iYear2Pivot;
  private static final Pattern[]     s_arrPatt;
  static {
    s_dtMin = LocalDateTime.parse("1861:01:01 00:00:00", ParseData.s_fmtDtExif);
    // 16/08/2025 : dtMax assumo non possa essere superiore ad oggi !!!
    //  s_dtMax = LocalDateTime.parse("2050:12:31 23:59:59", s_fmtDtExif);
    // s_dtMax = LocalDateTime.now();
    // 80 anni prima dell'anno corrente
    s_iYear2Pivot = (LocalDateTime.now().getYear() + 20) % 100;

    s_arrPatt = new Pattern[] { // 
        Pattern.compile(String.join("", szPattDateY4MDHMSs)) // 0
        , Pattern.compile(String.join("", szPattDateBY4MDHMSs)) // 1
        , Pattern.compile(String.join("", szPattDateY2MDHMSs)) // 2
        , null // 3 (marmotta) inverto le date con gg/mm/yyyy 
        , Pattern.compile(String.join("", szPattDateDMY4HMSs)) // 4
        , Pattern.compile(String.join("", szPattDateDMY2HMSs)) // 5
    };
  }

  public static void main(String[] args) {
    //    Pattern p = Pattern.compile(String.join("", szPattDateDMY4HMSs));
    //    Matcher m = p.matcher("23/07/24");
    //    boolean b = m.matches();
    //    if (b) {
    //      for (int i = 0; i < m.groupCount(); i++) {
    //        String szVal = m.group(i + 1);
    //        System.out.printf("i = %d\t val = %s\n", i, szVal);
    //      }
    //    } 
    compDate("23/07/57 07:12");
    @SuppressWarnings("unused")
    LocalDateTime d1 = ProvaParseDate.guess("23/07/57");
    for (String szDt : s_aszTest)
      ProvaParseDate.compDate(szDt);
  }

  private static void compDate(String szDt) {
    LocalDateTime d1 = ProvaParseDate.guess(szDt);
    LocalDateTime d2 = ParseData.parseData(szDt);
    LocalDateTime d3 = ParseData.guessData(szDt);
    if ( !Utils.isValueEq(d1,
        d2) /* || !Utils.isValueEq(d1, d3) || !Utils.isValueEq(d2, d3) */) {
      System.err.printf("src = %20s\t d1 = %20s\t d2 = %20s\t d3 = %s\n", //
          szDt, ParseData.formatDate(d1), ParseData.formatDate(d2), ParseData.formatDate(d3));
    } else
      System.out.printf("src = %20s\t d1 = %20s\t d2 = %20s\t d3 = %s\n", //
          szDt, ParseData.formatDate(d1), ParseData.formatDate(d2), ParseData.formatDate(d3));
  }

  private static LocalDateTime guess(String szDt) {
    LocalDateTime d = null;
    int[] arrDt = { 2999, 1, 1, 0, 0, 0, 0 };
    boolean bDMY = false;
    for (Pattern p : s_arrPatt) {
      if (null == p) { // segnale di inversione delle date con gg/mm/yyyy
        bDMY = true;
        continue;
      }
      Matcher m = p.matcher(szDt);
      if ( !m.matches())
        continue;
      for (int i = 0; i < m.groupCount(); i++) {
        String szVal = m.group(i + 1);
        if (null == szVal)
          continue;
        if ( !bDMY) { // qui il formato e' yyyy/mm/dd
          arrDt[i] = Integer.parseInt(szVal);
          // se l'anno e' a due cifre lo trasformo in anno a 4 cifre
          if (i == 0 && arrDt[i] < 100) {
            arrDt[i] += 2000;
          }
        } else {
          // qui il formato e' gg/mm/yyyy
          switch (i) {
            case 0: // giorno
              arrDt[2] = Integer.parseInt(szVal);
              break;
            case 1: // mese
              arrDt[1] = Integer.parseInt(szVal);
              break;
            case 2: // anno
              arrDt[0] = Integer.parseInt(szVal);
              // se l'anno e' a due cifre lo trasformo in anno a 4 cifre
              if (arrDt[0] < 100) {
                if (arrDt[0] < s_iYear2Pivot)
                  arrDt[0] += 2000;
                else
                  arrDt[0] += 1900;
              }
              break;
            default:
              arrDt[i] = Integer.parseInt(szVal);
              break;
          }
        }
      }
      try {
        d = LocalDateTime.of(//
            arrDt[0] //
            , arrDt[1] //
            , arrDt[2] //
            , arrDt[3] //
            , arrDt[4] //
            , arrDt[5] //
            , arrDt[6] * 1000000 //
        );
      } catch (Exception e) {
        System.err.printf("src = %20s\t d1 = %20s\t d2 = %20s\t d3 = %s\n", //
            szDt, ParseData.formatDate(d), " ",
            " " /* ParseData.formatDate(d3) */);
      }
      break;
    }
    return d;
  }

}

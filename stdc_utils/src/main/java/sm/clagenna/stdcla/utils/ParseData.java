package sm.clagenna.stdcla.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

public class ParseData {

  public static final SimpleDateFormat s_fmt       = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  public static SimpleDateFormat       s_fmtDtDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter      s_fmtTs     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  public static DateTimeFormatter      s_fmtOra    = DateTimeFormatter.ofPattern("HH:mm:ss");
  public static DateTimeFormatter      s_fmtTsT    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  public static DateTimeFormatter      s_fmtDtExif = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
  public static DateTimeFormatter      s_fmtDtFile = DateTimeFormatter.ofPattern("'f'yyyyMMdd'_'HHmmss");
  public static DateTimeFormatter      s_fmtY4MD   = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
  public static DateTimeFormatter      s_fmtPY4M   = DateTimeFormatter.ofPattern("yyyy.MM").withZone(ZoneId.systemDefault());

  public static int[]                s_qtaDays = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365 };
  private static final LocalDateTime s_dtMin;
  private static final int           s_iYear2Pivot;
  private static final LocalDateTime s_dtMax;
  @Getter
  private static String              lastErr   = null;
  @Setter
  private static boolean             showErr   = false;

  static {
    s_dtMin = LocalDateTime.parse("1861:01:01 00:00:00", s_fmtDtExif);
    // 16/08/2025 : dtMax assumo non possa essere superiore ad oggi !!!
    //  s_dtMax = LocalDateTime.parse("2050:12:31 23:59:59", s_fmtDtExif);
    s_dtMax = LocalDateTime.now();
    s_iYear2Pivot = (s_dtMax.getYear() + 20) % 100; // 100 anni prima dell'anno corrente
  }

  //  private static DateTimeFormatter[] s_arrpat = { //
  //      s_fmtDtExif, // 00
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), // 01
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"), // 01
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // 02
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"), // 02
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"), // 03
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"), // 04
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 05
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 06
  //      DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 07
  //      DateTimeFormatter.ofPattern("yyyy_MM_dd"), // 08
  //      DateTimeFormatter.ofPattern("yy-MM-dd"), // 09
  //      DateTimeFormatter.ofPattern("yyyy-MM"), // 10
  //
  //      DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"), // 11
  //      DateTimeFormatter.ofPattern("yyyyMMdd HHmm"), // 12
  //      DateTimeFormatter.ofPattern("yyyyMMdd"), // 13
  //
  //      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"), // 14
  //      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"), // 15
  //      DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 16
  //
  //      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), // 17
  //      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"), // 18
  //      DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 19
  //
  //  };

  private static final String[] szPattDateY4MDHMSs = { // 2024-06-01T12:34:56.789Z
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

  private static final String[] szPattDateBY4MDHMSs = { // 20240601 123456.456
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

  private static final String[] szPattDateY2MDHMSs = { // 24-12-31 13:34:56.789
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

  private static final String[] szPattDateDMY4HMSs = { // 23/07/2024 12:34:56.789
      ".*" //
      , "(\\d{2})[-_/](\\d{2})[-_/](\\d{2,4})" // giorno, mese, anno{4} (obbligatori)
      , "(?:[ ](\\d{2}):(\\d{2})" // ore, minuti
      /**/, "(?::(\\d{2})" // secondi
      /*--*/, "(?:\\.(\\d{1,9}))?" // frazione di secondo
      /**/, ")?" // chiusura secondi
      , ")?" // chiusura ore minuti
      , ".*$" // resto stringa
  };
  //
  //  private static final String[] szPattDateDMY2HMSs = { // 23/07/24 12:34:56.789
  //      ".*" //
  //      , "(\\d{2})[-_/](\\d{2})[-_/](\\d{2})" // giorno, mese, anno{2} (obbligatori)
  //      , "(?:[ ](\\d{2}):(\\d{2})" // ore, minuti
  //      , "(?::(\\d{2})" // secondi
  //      , "(?:\\.(\\d{1,9}))?" // frazione di secondo
  //      , ")?" // chiusura secondi
  //      , ")?" // chiusura ore minuti
  //      , ".*$" // resto stringa
  //  };

  static final Pattern[] s_arrPatt = { //
      Pattern.compile(String.join("", szPattDateY4MDHMSs)) // 0
      , Pattern.compile(String.join("", szPattDateBY4MDHMSs)) // 1
      , Pattern.compile(String.join("", szPattDateY2MDHMSs)) // 2
      , null // 3 inverto le date con gg/mm/yyyy
      , Pattern.compile(String.join("", szPattDateDMY4HMSs)) // 3
      // , Pattern.compile(String.join("", szPattDateDMY2HMSs)) // 4
  };

  //  private static Pattern[] s_arrpatGuess = { //
  //      //               .* yyyy-MM-dd?*hh.mm.ss ?*
  //      Pattern.compile(".*([0-9]{4})[\\-_.:]([0-9]{2})[\\-_.:]([0-9]{2}).*([0-9]{2})[\\-_.:]([0-9]{2})[\\-_.:]([0-9]{2}).*"), //
  //      //               .* yyyyMMdd?hhmmss .*
  //      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).([0-9]{2})([0-9]{2})([0-9]{2}).*"), //
  //      //               .* yyyy-MM-dd?*hh.mm ?*
  //      Pattern.compile(".*([0-9]{4})-([0-9]{2})-([0-9]{2}).*([0-9]{2})\\.([0-9]{2})\\.*"), //
  //      //               .* yyyy-MM-dd ?*
  //      Pattern.compile(".*([0-9]{4})-([0-9]{2})-([0-9]{2})\\.*"), //
  //      //               .* yyyy_MM_dd ?*
  //      Pattern.compile(".*([0-9]{4})_([0-9]{2})_([0-9]{2})\\.*"), //
  //      //               .* yy-MM-dd ?*
  //      Pattern.compile(".*([0-9]{2})-([0-9]{2})-([0-9]{2})\\.*"), //
  //      //               .* yyyy-MM ?*
  //      Pattern.compile(".*([0-9]{4})-([0-9]{2})\\.*"), //
  //
  //      //               .* yyyyMMdd?hhmm .*
  //      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).([0-9]{2})([0-9]{2}).*"), //
  //      //               .* yyyyMMdd .*
  //      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).*"), //
  //      //               .* yyyyMM .*
  //      Pattern.compile(".*([0-9]{4})([0-9]{2}).*"), //
  //
  //      //               .* yyyy .*
  //      Pattern.compile(".*([0-9]{4}).*"), //
  //  };

  public ParseData() {
    //
  }

  public static LocalDateTime parseData(String p_sz) {
    return ParseData.guessData(p_sz);
  }

  public static LocalDateTime guessData(String szDt) {
    LocalDateTime d = null;
    lastErr = null;
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
      lastErr = String.format("Err Data(1) = %s Exc = %s", //
          szDt, ParseData.formatDate(d), e.getMessage());
      if (showErr) {
        System.err.println(lastErr);
      }
    }
    // date passate oltre 1861-01-01 non le considero valide, date future oltre oggi le considero
    if (null == d || d.isBefore(s_dtMin) /* || d.isAfter(s_dtMax) */ ) {
      lastErr = String.format("Err Data(2) = %s Exc = %s, anteriore a %s\n", //
          szDt, ParseData.formatDate(d), ParseData.formatDate(s_dtMin));
      if (showErr)
        System.err.println(lastErr);
      return null;
    }
    return d;
  }

  //  public static LocalDateTime parseData(String p_sz) {
  //    LocalDateTime dtRet = null;
  //    if ( !Utils.isValue(p_sz))
  //      return null;
  //    @SuppressWarnings("unused") int k = 0;
  //    String szVal = p_sz.trim();
  //    // a volte c'e' 'T' ma mancano i secondi finali, allora li aggiungo
  //    if (szVal.contains("T") && szVal.length() == 16)
  //      szVal += ":00";
  //    // a volte la stringa contiene il separatore 'T' ma non la 'Z' finale !!!
  //    if (szVal.contains("T") && !szVal.endsWith("Z"))
  //      szVal += "Z";
  //    for (DateTimeFormatter pat : s_arrpat) {
  //      try {
  //        dtRet = LocalDateTime.parse(szVal, pat);
  //      } catch (DateTimeParseException e) {
  //        //
  //      }
  //      try {
  //        if (dtRet == null) {
  //          LocalDate ldt = LocalDate.parse(szVal, pat);
  //          if (ldt != null)
  //            dtRet = ldt.atStartOfDay();
  //        }
  //      } catch (DateTimeParseException e) {
  //        //
  //      }
  //
  //      k++;
  //      if (dtRet != null && dtRet.isAfter(s_dtMin) && dtRet.isBefore(s_dtMax))
  //        break;
  //      dtRet = null;
  //    }
  //    return dtRet;
  //  }

  //  public static LocalDateTime guessData(String p_sz) {
  //    LocalDateTime dtRet = null;
  //    if (p_sz == null)
  //      return null;
  //    String[] szFmtDt = { "2999", "01", "01", "00", "00", "00" };
  //
  //    for (Pattern pat : s_arrpatGuess) {
  //      Matcher mtch = pat.matcher(p_sz);
  //      if (mtch.find()) {
  //        for (int k = 0; k < mtch.groupCount(); k++)
  //          szFmtDt[k] = mtch.group(k + 1);
  //        String l_fmt = "%s:%s:%s %s:%s:%s";
  //        if (szFmtDt[0].length() == 2)
  //          l_fmt = "20%s:%s:%s %s:%s:%s";
  //        String szDt = String.format(l_fmt, (Object[]) szFmtDt);
  //        try {
  //          dtRet = LocalDateTime.parse(szDt, s_fmtDtExif);
  //        } catch (Exception e) {
  //          dtRet = null;
  //          // e.printStackTrace();
  //        }
  //        if (dtRet != null)
  //          if (dtRet.isAfter(s_dtMin) && dtRet.isBefore(s_dtMax))
  //            break;
  //        dtRet = null;
  //      }
  //    }
  //    return dtRet;
  //  }
  public OffsetDateTime parseOffsetDateTime(String p_sz, String p_ofs) {
    ZoneOffset offsZone = ZoneOffset.of(p_ofs);
    LocalDateTime dt = ParseData.parseData(p_sz);
    OffsetDateTime odt = OffsetDateTime.of(dt, offsZone);
    return odt;
  }

  public Long elapsedSeconds(String p_sz1, String p_sz2) {
    //    Long llRet = null;
    //    if (null == p_sz1 || p_sz1.length() < 2)
    //      return llRet;
    //    if (null == p_sz2 || p_sz2.length() < 2)
    //      return llRet;
    //    LocalDateTime ts1 = LocalDateTime.parse(p_sz1, s_fmtmY4MD_hms);
    //    LocalDateTime ts2 = LocalDateTime.parse(p_sz2, s_fmtmY4MD_hms);
    //    llRet = ChronoUnit.SECONDS.between(ts1, ts2);
    //    return llRet;
    return null;
  }

  /**
   * Ritorna un RecDeltaTime cercando di parse-are la stringa fornita che
   * rappresenta un delta di tempo nella forma:<br/>
   *
   * <pre>
   * [yyyy-mm-dd] [HH:mm[:ss]]
   * </pre>
   *
   * @param psz
   * @return
   */
  public static RecDeltaTime parseDeltaTime(String psz) {
    RecDeltaTime ret = new RecDeltaTime(psz);
    //    if (null == psz || psz.trim().length() < 2)
    //      return null;
    //    if (! ( psz.contains(":") || psz.contains("-") ) ) {
    //      // non mi pare sia formato DateTime, allora provo a interpretarlo come numero di secondi
    //      Long ll = null;
    //      try {
    //        ll = Utils.parseLong(psz);
    //      } catch (Exception e) {
    //        //
    //      }
    //      if (null != ll) {
    //        return new RecDeltaTime(psz, LocalDateTime.of(1970, 1, 1, 0, 0, 0).plusSeconds(ll), ll);
    //      }
    //    }
    //
    //
    //    int year = 1970; // Integer.parseInt(m.group(1));
    //    int month = 1; //  Integer.parseInt(m.group(2));
    //    int day = 1; //  Integer.parseInt(m.group(3));
    //    int hour = 0; //  Integer.parseInt(m.group(4));
    //    int minute = 0; //  Integer.parseInt(m.group(5));
    //    int second = 0; //  m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
    //    long unixEpoch = 0;
    //    // System.out.println("Provo " + psz);
    //    Matcher m = s_pattYY.matcher(psz.trim());
    //    boolean bMatch = m.matches();
    //    if (bMatch && m.groupCount() > 0) {
    //      year = Integer.parseInt(m.group(1));
    //      month = Integer.parseInt(m.group(2));
    //      day = Integer.parseInt(m.group(3));
    //      if (psz.length() > 10)
    //        psz = psz.substring(10).trim();
    //      else
    //        psz = "";
    //    }
    //    if (psz.length() > 0) {
    //      m = s_pattHMS.matcher(psz.trim());
    //      bMatch = m.matches();
    //      if (bMatch && m.groupCount() > 0) {
    //        hour = Integer.parseInt(m.group(1));
    //        minute = Integer.parseInt(m.group(2));
    //        second = Integer.parseInt(m.group(3));
    //        psz = "";
    //      }
    //    }
    //    if (psz.length() > 0) {
    //      m = s_pattHM.matcher(psz.trim());
    //      bMatch = m.matches();
    //      if (bMatch && m.groupCount() > 0) {
    //        hour = Integer.parseInt(m.group(1));
    //        minute = Integer.parseInt(m.group(2));
    //        psz = "";
    //      }
    //    }
    //    // LocalDate e LocalTime validano automaticamente i range
    //    LocalDate date = LocalDate.of(year, month, day);
    //    LocalTime time = LocalTime.of(hour, minute, second);
    //
    //    LocalDateTime ldt = LocalDateTime.of(date, time);
    //    unixEpoch = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
    //    ret = new RecDeltaTime(psz, ldt, unixEpoch);
    return ret;
  }

  public static String formatDate(java.util.Date p_ldt) {
    return ParseData.formatDate(ParseData.toLocalDateTime(p_ldt));
  }

  public static Object formatDate(LocalDate dtSolo) {
    return ParseData.formatDate(dtSolo.atStartOfDay());
  }

  public static String formatDate(LocalDateTime p_ldt) {
    String szRet = null;
    if (null == p_ldt)
      return szRet;
    try {
      szRet = s_fmtTs.format(p_ldt);
      szRet = szRet.replace(".000", "").replace(" 00:00:00", "");
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return szRet;
  }

  public static String formatOra(java.util.Date p_ldt) {
    return ParseData.formatOra(ParseData.toLocalDateTime(p_ldt));
  }

  public static String formatOra(LocalDateTime p_ldt) {
    String szRet = null;
    if (null == p_ldt)
      return szRet;
    try {
      szRet = s_fmtOra.format(p_ldt);
      szRet = szRet.replace(" 00:00:00", "");
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return szRet;
  }

  @SuppressWarnings("exports")
  public static Timestamp toTimestamp(LocalDateTime p_ldt) {
    if (null == p_ldt)
      return null;
    return Timestamp.valueOf(p_ldt);
  }

  /**
   * Converte un LocalDateTime in java.util.Date
   *
   * @param dt
   *          LocalDateTime da convertire
   * @return java.util.Date corrispondente
   */
  public static Date toDate(LocalDateTime dt) {
    Date dtRet = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
    return dtRet;
  }
  
  /**
   * Ritorna una stringa con la data attuale nel formato fYYYYMMDD_HHMMSS
   *
   * @return String con la data attuale nel formato fYYYYMMDD_HHMMSS
   */
  public static String toStringToday() {
    return s_fmtDtFile.format(LocalDateTime.now());
  }

  /**
   * Ritorna un LocalDateTime con l'orario azzerato (00:00:00) a partire da un
   * LocalDateTime fornito
   *
   * @param dt
   *          con LocalDateTime con orario non azzerato
   * @return LocalDateTime con orario azzerato (00:00:00)
   */
  public static LocalDateTime toLocalDateSolo(LocalDateTime dt) {
    if (null == dt)
      return null;
    LocalDate dtSolo = dt.toLocalDate();
    return dtSolo.atStartOfDay();
  }

  public static LocalDateTime toLocalDateTime(Object p_ldt) {
    if (null == p_ldt)
      return null;
    if (p_ldt instanceof LocalDateTime ldt)
      return ldt;
    if (p_ldt instanceof Timestamp tms)
      return ParseData.toLocalDateTime(tms);
    if (p_ldt instanceof java.sql.Date dt) {
      java.util.Date udt = new java.util.Date(dt.getTime());
      return udt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    if (p_ldt instanceof java.util.Date dt)
      return dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    return ParseData.parseData(p_ldt.toString());
  }

  @SuppressWarnings("exports")
  public static LocalDateTime toLocalDateTime(Timestamp p_ldt) {
    if (null == p_ldt)
      return null;
    return p_ldt.toLocalDateTime();
  }
}

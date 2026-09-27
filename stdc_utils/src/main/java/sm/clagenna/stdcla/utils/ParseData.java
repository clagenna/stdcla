package sm.clagenna.stdcla.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseData {

  public static final SimpleDateFormat s_fmt       = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  public static SimpleDateFormat       s_fmtDtDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter      s_fmtTs     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter      s_fmtOra    = DateTimeFormatter.ofPattern("HH:mm:ss");
  public static DateTimeFormatter      s_fmtTsT    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  public static DateTimeFormatter      s_fmtDtExif = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
  public static DateTimeFormatter      s_fmtDtFile = DateTimeFormatter.ofPattern("'f'yyyyMMdd'_'HHmmss");
  public static DateTimeFormatter      s_fmtY4MD   = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
  public static DateTimeFormatter      s_fmtPY4M   = DateTimeFormatter.ofPattern("yyyy.MM").withZone(ZoneId.systemDefault());

  public static int[]                s_qtaDays = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365 };
  private static final LocalDateTime s_dtMin;
  private static final LocalDateTime s_dtMax;

  static {
    s_dtMin = LocalDateTime.parse("1861:01:01 00:00:00", s_fmtDtExif);
    // 16/08/2025 : dtMax assumo non possa essere superiore ad oggi !!!
    //  s_dtMax = LocalDateTime.parse("2050:12:31 23:59:59", s_fmtDtExif);
    s_dtMax = LocalDateTime.now();

  }

  private static DateTimeFormatter[] s_arrpat = { //
      s_fmtDtExif, // 00
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), // 01
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"), // 01
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // 02
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"), // 02
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"), // 03
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"), // 04
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 05
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 06
      DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 07
      DateTimeFormatter.ofPattern("yyyy_MM_dd"), // 08
      DateTimeFormatter.ofPattern("yy-MM-dd"), // 09
      DateTimeFormatter.ofPattern("yyyy-MM"), // 10

      DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"), // 11
      DateTimeFormatter.ofPattern("yyyyMMdd HHmm"), // 12
      DateTimeFormatter.ofPattern("yyyyMMdd"), // 13

      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"), // 14
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"), // 15
      DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 16

      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), // 17
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"), // 18
      DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 19

  };

  private static Pattern[] s_arrpatGuess = { //
      //               .* yyyy-MM-dd?*hh.mm.ss ?*
      Pattern.compile(".*([0-9]{4})[\\-_.:]([0-9]{2})[\\-_.:]([0-9]{2}).*([0-9]{2})[\\-_.:]([0-9]{2})[\\-_.:]([0-9]{2}).*"), //
      //               .* yyyyMMdd?hhmmss .*
      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).([0-9]{2})([0-9]{2})([0-9]{2}).*"), //
      //               .* yyyy-MM-dd?*hh.mm ?*
      Pattern.compile(".*([0-9]{4})-([0-9]{2})-([0-9]{2}).*([0-9]{2})\\.([0-9]{2})\\.*"), //
      //               .* yyyy-MM-dd ?*
      Pattern.compile(".*([0-9]{4})-([0-9]{2})-([0-9]{2})\\.*"), //
      //               .* yyyy_MM_dd ?*
      Pattern.compile(".*([0-9]{4})_([0-9]{2})_([0-9]{2})\\.*"), //
      //               .* yy-MM-dd ?*
      Pattern.compile(".*([0-9]{2})-([0-9]{2})-([0-9]{2})\\.*"), //
      //               .* yyyy-MM ?*
      Pattern.compile(".*([0-9]{4})-([0-9]{2})\\.*"), //

      //               .* yyyyMMdd?hhmm .*
      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).([0-9]{2})([0-9]{2}).*"), //
      //               .* yyyyMMdd .*
      Pattern.compile(".*([0-9]{4})([0-9]{2})([0-9]{2}).*"), //
      //               .* yyyyMM .*
      Pattern.compile(".*([0-9]{4})([0-9]{2}).*"), //

      //               .* yyyy .*
      Pattern.compile(".*([0-9]{4}).*"), //
  };

  public ParseData() {
    //
  }

  public static LocalDateTime parseData(String p_sz) {
    LocalDateTime dtRet = null;
    if ( !Utils.isValue(p_sz))
      return null;
    @SuppressWarnings("unused") int k = 0;
    String szVal = p_sz.trim();
    for (DateTimeFormatter pat : s_arrpat) {
      try {
        dtRet = LocalDateTime.parse(szVal, pat);
      } catch (DateTimeParseException e) {
        //
      }
      try {
        if (dtRet == null) {
          LocalDate ldt = LocalDate.parse(szVal, pat);
          if (ldt != null)
            dtRet = ldt.atStartOfDay();
        }
      } catch (DateTimeParseException e) {
        //
      }

      k++;
      if (dtRet != null && dtRet.isAfter(s_dtMin) && dtRet.isBefore(s_dtMax))
        break;
      dtRet = null;
    }
    return dtRet;
  }

  public static LocalDateTime guessData(String p_sz) {
    LocalDateTime dtRet = null;
    if (p_sz == null)
      return null;
    String[] szFmtDt = { "2999", "01", "01", "06", "00", "00" };

    for (Pattern pat : s_arrpatGuess) {
      Matcher mtch = pat.matcher(p_sz);
      if (mtch.find()) {
        for (int k = 0; k < mtch.groupCount(); k++)
          szFmtDt[k] = mtch.group(k + 1);
        String l_fmt = "%s:%s:%s %s:%s:%s";
        if (szFmtDt[0].length() == 2)
          l_fmt = "20%s:%s:%s %s:%s:%s";
        String szDt = String.format(l_fmt, (Object[]) szFmtDt);
        try {
          dtRet = LocalDateTime.parse(szDt, s_fmtDtExif);
        } catch (Exception e) {
          dtRet = null;
          // e.printStackTrace();
        }
        if (dtRet != null)
          if (dtRet.isAfter(s_dtMin) && dtRet.isBefore(s_dtMax))
            break;
        dtRet = null;
      }
    }
    return dtRet;
  }

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

  public static String formatDate(LocalDateTime p_ldt) {
    String szRet = null;
    if (null == p_ldt)
      return szRet;
    try {
      szRet = s_fmtTs.format(p_ldt);
      szRet = szRet.replace(" 00:00:00", "");
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

  public static Date toDate(LocalDateTime dt) {
    Date dtRet = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
    return dtRet;
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

package sm.clagenna.stdcla.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseData {
  public static SimpleDateFormat  s_fmtDtDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter s_fmtTs     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter s_fmtDtExif = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
  public static DateTimeFormatter s_fmtDtFile = DateTimeFormatter.ofPattern("'f'yyyyMMdd'_'HHmmss");

  private static final LocalDateTime s_dtMin;
  private static final LocalDateTime s_dtMax;

  static {
    s_dtMin = LocalDateTime.parse("1861:01:01 00:00:00", s_fmtDtExif);
    s_dtMax = LocalDateTime.parse("2050:12:31 23:59:59", s_fmtDtExif);
  }

  private static DateTimeFormatter[] s_arrpat = { //
      s_fmtDtExif, // 00
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), // 01
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // 02
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 03
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 04
      DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 05
      DateTimeFormatter.ofPattern("yyyy_MM_dd"), // 05
      DateTimeFormatter.ofPattern("yy-MM-dd"), // 06
      DateTimeFormatter.ofPattern("yyyy-MM"), // 07

      DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"), // 08
      DateTimeFormatter.ofPattern("yyyyMMdd HHmm"), // 09
      DateTimeFormatter.ofPattern("yyyyMMdd"), // 10

      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"), // 11
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"), // 12
      DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 13

      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), // 14
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"), // 15
      DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 16

  };

  private static Pattern[] s_arrpatGuess = { //
      //               .* yyyy-MM-dd?*hh.mm.ss ?*
      Pattern.compile(".*([0-9]{4})[\\-_.]([0-9]{2})[\\-_.]([0-9]{2}).*([0-9]{2})[\\-_.]([0-9]{2})[\\-_.]([0-9]{2}).*"), //
      
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

  public LocalDateTime parseData(String p_sz) {
    LocalDateTime dtRet = null;
    if (p_sz == null)
      return null;
    @SuppressWarnings("unused")
    int k = 0;
    for (DateTimeFormatter pat : s_arrpat) {
      try {
        dtRet = LocalDateTime.parse(p_sz, pat);
      } catch (DateTimeParseException e) {
        //
      }
      try {
        if (dtRet == null) {
          LocalDate ldt = LocalDate.parse(p_sz, pat);
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

  public LocalDateTime guessData(String p_sz) {
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
    LocalDateTime dt = parseData(p_sz);
    OffsetDateTime odt = OffsetDateTime.of(dt, offsZone);
    return odt;
  }
}

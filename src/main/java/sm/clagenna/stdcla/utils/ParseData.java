package sm.clagenna.stdcla.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ParseData {
  public static SimpleDateFormat     s_fmtDtDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static DateTimeFormatter    s_fmtDtExif = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
  public static DateTimeFormatter    s_fmtDtFile = DateTimeFormatter.ofPattern("'f'yyyyMMdd'_'HHmmss");
  
  private static final LocalDateTime s_dtMin;
  private static final LocalDateTime s_dtMax;

  static {
    s_dtMin = LocalDateTime.parse("1861:01:01 00:00:00", s_fmtDtExif);
    s_dtMax = LocalDateTime.parse("2050:12:31 23:59:59", s_fmtDtExif);
  }

  private static DateTimeFormatter[] s_arrpat = { // 01
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), // 03
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // 03
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 02
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 04
      DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 05
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

  public LocalDateTime parseData(String p_sz) {
    LocalDateTime dtRet = null;
    if (p_sz == null)
      return null;
    @SuppressWarnings("unused") int k = 0;
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

}

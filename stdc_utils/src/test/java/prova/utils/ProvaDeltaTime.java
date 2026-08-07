package prova.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.RecDeltaTime;
import sm.clagenna.stdcla.utils.Utils;

public class ProvaDeltaTime {
  // Regex: data obbligatoria, ora obbligatoria, secondi opzionali
  //  private Pattern PATTERN;
  private Pattern s_pattYY;
  private Pattern s_pattHM;
  private Pattern s_pattHMS;

  ProvaDeltaTime() {
    //
  }

  public static void main(String[] args) {
    ProvaDeltaTime app = new ProvaDeltaTime();
    app.doTheJob();
  }

  @Test
  public void doTheJob() {
    //    String szPatt = //
    //        "^(?::(\\d{4})-(\\d{2})-(\\d{2}))?" + // yyyy-mm-dd
    //            "\\s*" + // separatore spazio
    //            "(\\d{2}):(\\d{2})" + // HH:mm
    //            "(?::(\\d{2}))?$"; // [:ss] opzionale

    //    String szPatt = "^(?:\\d{4}-\\d{2}-\\d{2})" // yyyy-mm-dd
    //        + "?(?:\\s+)?" // spazio opzionale
    //        + "(?:\\d{2}:\\d{2}" // HH:mm
    //        + "(?::\\d{2})?)?$"; // :ss Opzionale

    //    PATTERN = Pattern.compile(szPatt);

    s_pattYY = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2}).*"); // yyyy-mm-dd
    s_pattHMS = Pattern.compile(".*(\\d{2}):(\\d{2}):(\\d{2}).*"); // HH:mm:ss
    s_pattHM = Pattern.compile(".*(\\d{2}):(\\d{2}).*"); // HH:mm

    String[] arr = { //
        "1982-03-10" //
        , "2023-03-10 17:32:23" //
        , "2023-03-10 20:32" //
        , "17:33:12" //
        , "10:22" };
    for (String sz : arr) {
      RecDeltaTime rec = ParseData.parseDeltaTime(sz);
      System.out.printf("Da: %-30s a %s (%s)\n", sz, rec.getDuration().toString(), Utils.formatLong(rec.getDeltaSeconds()));
    }
  }

  public Long parseDeltaTime(String psz) {
    int year = 1970; // Integer.parseInt(m.group(1));
    int month = 1; //  Integer.parseInt(m.group(2));
    int day = 1; //  Integer.parseInt(m.group(3));
    int hour = 0; //  Integer.parseInt(m.group(4));
    int minute = 0; //  Integer.parseInt(m.group(5));
    int second = 0; //  m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
    long unixEpoch = 0;
    // System.out.println("Provo " + psz);
    Matcher m = s_pattYY.matcher(psz.trim());
    boolean bMatch = m.matches();
    if (bMatch && m.groupCount() > 0) {
      year = Integer.parseInt(m.group(1));
      month = Integer.parseInt(m.group(2));
      day = Integer.parseInt(m.group(3));
      if (psz.length() > 10)
        psz = psz.substring(10).trim();
      else
        psz = "";
    }
    if (psz.length() > 0) {
      m = s_pattHMS.matcher(psz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        hour = Integer.parseInt(m.group(1));
        minute = Integer.parseInt(m.group(2));
        second = Integer.parseInt(m.group(3));
        psz = "";
      }
    }
    if (psz.length() > 0) {
      m = s_pattHM.matcher(psz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        hour = Integer.parseInt(m.group(1));
        minute = Integer.parseInt(m.group(2));
        psz = "";
      }
    }
    // LocalDate e LocalTime validano automaticamente i range
    LocalDate date = LocalDate.of(year, month, day);
    LocalTime time = LocalTime.of(hour, minute, second);

    LocalDateTime ldt = LocalDateTime.of(date, time);
    unixEpoch = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
    return unixEpoch;
  }

}

package prova.date;

import java.time.LocalDateTime;

import org.junit.Test;

import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.RecDeltaTime;

public class ProvaDeltaTime {

  @Test
  public void test() {
    String[] szDts = new String[] { //
        "0:01:00", //  
        "-0:01:00", //  -60
        "1:00:00", //  -60
        "-1:00:00", //  -60
        "-1 0:00:00", //  -60
        "01-00 00:00", // yyyy-mm-dd HH:mm
        "-01-00 00:00", // yyyy-mm-dd HH:mm
        "01-00 01:00", // yyyy-mm-dd HH:mm
        "-01-00 01:00", // yyyy-mm-dd HH:mm
        "-1-02-01 00:13", // yyyy-mm-dd HH:mm
        "3:00:00", // +10.800
        "-0:27:00", // -1.620
        "-1:27", // -1.620
        "+0:00:11", // +11
        "+01-01 00:10:33", // mm-dd HH:mm:ss
        "+1 00:10:33", // dd HH:mm:ss
        "24351", //
        "-24321", //
        "24.351", //
        "-24.321" };
    LocalDateTime dtBase = LocalDateTime.of(2020, 6, 1, 0, 0, 0);
    // metti a null per il normale test
    String szDebug = "01-00 01:00";
    if ( null !=  szDebug ) {
        RecDeltaTime deltaTime = new RecDeltaTime();
        RecDeltaTime deltaTimeErr = new RecDeltaTime();
        try {
          deltaTime.parse(szDebug);
          deltaTimeErr.parse(String.valueOf(deltaTime.getDeltaSeconds()));
          System.out.printf("%24s -> %s\thms=%-20s\n", szDebug, deltaTime, deltaTime.toHMS());
          System.out.printf("%24s -> %s\thms=%-20s\n", szDebug, deltaTimeErr, deltaTimeErr.toHMS());
        } catch (Exception ex) {
          System.out.printf("%24s -> %-80s hms=%s\n", szDebug, "*ERR*", "*ERR*");
        }
        String szDelta = null != deltaTime ? deltaTime.toString() : "*null*";
        System.out.printf("%24s -> %s\thms=%-20s", szDebug, szDelta, deltaTime.toHMS());
        LocalDateTime dtNew = deltaTime.adjust(dtBase);
        System.out.printf("%24s + %-12s -> %s\n", ParseData.formatDate(dtBase), szDebug, ParseData.formatDate(dtNew));
    }
    // se arrivi qui, fai il test completo
    for (String szDt : szDts) {
      RecDeltaTime deltaTime = new RecDeltaTime();
      try {
        deltaTime.parse(szDt);
      } catch (Exception ex) {
        System.out.printf("%24s -> %-80s hms=%s\n", szDt, "*ERR*", "*ERR*");
        continue;
      }
      String szDelta = null != deltaTime ? deltaTime.toString() : "*null*";
      System.out.printf("%24s -> %s\thms=%-20s", szDt, szDelta, deltaTime.toHMS());
      LocalDateTime dtNew = deltaTime.adjust(dtBase);
      System.out.printf("%24s + %-12s -> %s\n", ParseData.formatDate(dtBase), szDt, ParseData.formatDate(dtNew));
    }
  }

}

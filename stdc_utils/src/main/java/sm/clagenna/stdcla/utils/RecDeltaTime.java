package sm.clagenna.stdcla.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;

/**
 * Rappresenta un Delta nel tempo tra dtAcquisizione e nuova in 3 formati
 * <ol>
 * <li>Stringa formattata</li>
 * <li>LocalDateTime</li>
 * <li>Unix Epoch (secondi dal 1/1/1970)</li>
 * </ol>
 */
@Data
public class RecDeltaTime {

  private static final Pattern s_pattYYMMDD;
  private static final Pattern s_pattMMDD;
  private static final Pattern s_pattDD;
  private static final Pattern s_pattHM;
  private static final Pattern s_pattHMS;
  // private static final LocalDateTime s_dtinizUX;
  // private static final LocalDateTime s_dtinizJX;

  private String deltaTime;
  private Long   deltaSeconds;

  private boolean  bNegative;
  private int      year;
  private int      month;
  private int      day;
  private int      hour;
  private int      minute;
  private int      second;
  private Period   period;
  private Duration duration;

  static {
    // s_dtinizUX = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
    // s_dtinizJX = LocalDateTime.of(0, 1, 1, 0, 0, 0);
    s_pattYYMMDD = Pattern.compile("^(\\d{1,4})-(\\d{2})-(\\d{2}).*"); // yyyy-mm-dd
    s_pattMMDD = Pattern.compile("^(\\d{1,2})-(\\d{2}).*"); // mm-dd
    s_pattDD = Pattern.compile("^(\\d{1,2}) (\\d{1,2}).*"); // 'dd HH:mm:ss'
    s_pattHMS = Pattern.compile(".*(\\d{1,2}):(\\d{2}):(\\d{2}).*"); // HH:mm:ss
    s_pattHM = Pattern.compile(".*(\\d{1,2}):(\\d{2}).*"); // HH:mm
  }

  public RecDeltaTime() {
    //
  }

  public RecDeltaTime(String szDeltaTime) {
    this.parse(szDeltaTime);
  }

  public RecDeltaTime parse(String pSz) {
    // System.out.println("RecDeltaTime.parse: " + pSz);
    year = 0;
    month = 0;
    day = 0;
    hour = 0;
    minute = 0;
    second = 0;
    deltaSeconds = 0L;

    if (null == pSz || pSz.trim().length() < 1)
      return null;
    String sz = pSz.trim();
    String szSign = "+";
    bNegative = false;
    setDeltaTime(pSz);
    if (sz.startsWith("+") || sz.startsWith("-")) {
      szSign = sz.substring(0, 1);
      bNegative = szSign.equals("-");
      sz = sz.substring(1).trim();
    }
    if ( ! (sz.contains(":") || sz.contains("-")))
      return parseDeltaNumeric(sz);
    return parseDeltaDateTime(sz);
  }

  private RecDeltaTime parseDeltaDateTime(String sz) {
    // provo a interpretarlo come formato DateTime
    // nella forma "[+/-][yyyy-[mm-[dd [[HH:mm]:ss]"
    // RecDeltaTime ret = null;

    // se contiene la data, estraggo anno(4), mese(2) e giorno(2) e lascio il resto in sz
    Matcher m = s_pattYYMMDD.matcher(sz.trim());
    boolean bMatch = m.matches();
    if (bMatch && m.groupCount() > 0) {
      String syy = m.group(1);
      String smm = m.group(2);
      String sgg = m.group(3);
      year = Integer.parseInt(syy);
      month = Integer.parseInt(smm);
      day = Integer.parseInt(sgg);
      int n = syy.length() + smm.length() + sgg.length() + 2;
      if (sz.length() > n)
        sz = sz.substring(n).trim();
      else
        sz = "";
    }
    if ( !bMatch) {
      // se non c'� la data completa, allora vedo se contiene mese(2) e giorno(2)
      m = s_pattMMDD.matcher(sz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        String smm = m.group(1);
        String sgg = m.group(2);
        month = Integer.parseInt(smm);
        day = Integer.parseInt(sgg);
        int nl = smm.length() + sgg.length() + 1;
        if (sz.length() > nl)
          sz = sz.substring(nl).trim();
        else
          sz = "";
      }
    }
    if ( !bMatch) {
      // se non c'� la data completa, allora vedo se contiene giorno(2)
      m = s_pattDD.matcher(sz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        String sgg = m.group(1);
        day = Integer.parseInt(sgg);
        if (sz.length() >= sgg.length() + 1)
          sz = sz.substring(sgg.length()).trim();
        else
          sz = "";
      }
    }
    // se � rimasto qualcosa, allora vedo se contiene l'ora, se si' estraggo ora(2), minuto(2) e secondo(2)
    bMatch = false;
    if ( !bMatch && sz.length() > 0) {
      m = s_pattHMS.matcher(sz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        hour = Integer.parseInt(m.group(1));
        minute = Integer.parseInt(m.group(2));
        second = Integer.parseInt(m.group(3));
        sz = "";
      }
    }
    if ( !bMatch && sz.length() > 0) {
      // se � rimasto qualcosa, allora vedo se contiene l'ora, se si' estraggo ora(2) e minuto(2)
      m = s_pattHM.matcher(sz.trim());
      bMatch = m.matches();
      if (bMatch && m.groupCount() > 0) {
        hour = Integer.parseInt(m.group(1));
        minute = Integer.parseInt(m.group(2));
        sz = "";
      }
    }

    deltaSeconds = year * 365L * 24L * 3600L //
        + ParseData.s_qtaDays[month] * 24L * 3600L //
        + day * 24L * 3600L //
        + hour * 3600L //
        + minute * 60L //
        + second;

    period = Period.of(year, month, day);
    duration = Duration.ofHours(hour).plusMinutes(minute).plusSeconds(second);

    return this;
  }

  /**
   * Se la stringa non contiene : o - allora provo a interpretarla come numero
   * di secondi
   *
   * @param pSz
   * @return
   */
  private RecDeltaTime parseDeltaNumeric(String pSz) {
    long ll = -1;
    try {
      ll = Utils.parseLong(pSz);
    } catch (Exception e) {
      //
    }
    if (ll < 0)
      throw new IllegalArgumentException(
          String.format("RecDeltaTime.parseDeltaNumeric: non riesco a interpretare '%s' come numero di secondi", pSz));
    deltaSeconds = ll;
    year = (int) (ll / (365L * 24L * 3600L)); // ottengo gl'anni
    ll -= year * (365L * 24L * 3600L); // tolgo gli anni
    month = -1;
    long lx = ll / (24 * 3600L); // ottengo i giorni rimanenti
    // vado alla ricerca del mese che contiene questi giorni rimanenti
    for (int i = 0; i < ParseData.s_qtaDays.length; i++) {
      if (ParseData.s_qtaDays[i] >= lx) {
        month = i;
        break;
      }
    }
    ll -= lx * 24L * 3600L; // tolgo i giorni
    hour = (int) (ll / 3600L); // ottengo le ore
    ll -= hour * 3600L; // tolgo le ore
    minute = (int) (ll / 60L); // ottengo i minuti
    ll -= minute * 60L; // tolgo i minuti
    second = (int) ll; // ottengo i secondi

    period = Period.of(year, month, day);
    duration = Duration.ofHours(hour).plusMinutes(minute).plusSeconds(second);

    return this;
  }

  /**
   * Somma (o sottrae) questo delta a {@code base} e restituisce il nuovo
   * LocalDateTime.
   */
  public LocalDateTime adjust(LocalDateTime base) {
    if (base == null)
      throw new IllegalArgumentException("base non pu� essere null");

    if (bNegative) {
      return base.minus(period).minus(duration);
    }
    return base.plus(period).plus(duration);
  }

  public String toHMS() {
    if (null == deltaSeconds)
      return null;
    String szSign = bNegative ? "-" : "";
    String szRetHH = String.format("%02d:%02d:%02d", hour, minute, second);
    if (szRetHH.equals("00:00:00"))
      szRetHH = "";
    else if (szRetHH.endsWith(":00"))
      szRetHH = szRetHH.substring(0, szRetHH.length() - 3);

    String szRetYY = String.format("%04d-%02d-%02d", year, month, day);
    if (szRetYY.equals("0000-00-00"))
      szRetYY = "";
    else if (szRetYY.startsWith("0000-00-"))
      szRetYY = szRetYY.substring(8);
    else if (szRetYY.startsWith("0000-"))
      szRetYY = szRetYY.substring(5);
    if (szRetHH.length() > 0 && szRetYY.length() > 0)
      szRetYY += " ";
    return szSign + szRetYY + szRetHH;
  }

  @Override
  public int hashCode() {
    if (null == deltaSeconds)
      return 0;
    return deltaSeconds.hashCode();
  }

  public boolean equals(RecDeltaTime other) {
    if (other == null)
      return false;
    if (this == other)
      return true;
    return this.deltaSeconds.equals(other.deltaSeconds);
  }
  
  public String toStringShort() {
    String szSign = bNegative ? "-" : "+";
    String szRet = String.format("%s%04d-%02d-%02d %02d:%02d:%02d", szSign, year, month, day, hour, minute, second);
    szRet = szRet.replaceAll(" 00:00:00$", "").replace("0000-00-00 ", "");;
    return szRet;
  }

  @Override
  public String toString() {
    String szSign = bNegative ? "-" : "+";
    String szRet = String.format("Y%dM%dD%d_H%dm%ds%d", year, month, day, hour, minute, second);
    String dur = Utils.formatLong(deltaSeconds);
    return String.format("%s\t%s%s (%s seconds)", deltaTime, szSign, szRet, dur);
  }

}

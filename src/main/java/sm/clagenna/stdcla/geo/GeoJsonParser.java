package sm.clagenna.stdcla.geo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.sys.TimerMeter;
import sm.clagenna.stdcla.utils.ParseData;

@Data
public class GeoJsonParser {
  private static final Logger    s_log        = LogManager.getLogger(GeoJsonParser.class);
  private static final String    CSZ_LAT      = "latitudeE7";
  private static final String    CSZ_LON      = "longitudeE7";
  private static final String    CSZ_ALT      = "altitude";
  private static final String    CSZ_TST      = "deviceTimestamp";
  private static final String    CSZ_FACT     = "formFactor";
  private static NumberFormat    intFmt       = NumberFormat.getNumberInstance(Locale.getDefault());
  private static final long      N_PRIMO      = 245591;
  private static final ParseData s_dataParser = new ParseData();

  private Path           jsonFile;
  private List<GeoCoord> listCoord;
  /** lowest time stamp perceived from data */
  private LocalDateTime  dtLow;
  /** highest time stamp perceived from data */
  private LocalDateTime  dtHigh;
  /** filtro data minima (se presente) sui dati da registrare */
  private LocalDateTime  dtMinFilter;
  /** filtro data massima (se presente) sui dati da registrare */
  private LocalDateTime  dtMaxFilter;
  /** riga corrente nel file JSON durante il parsing */
  private int            riga;
  private GeoCoord       geoCoord;
  private int            infoAdded;
  private TimerMeter m_tim;

  public GeoJsonParser() {
    dtMinFilter = LocalDateTime.MIN;
    dtMaxFilter = LocalDateTime.MAX;
  }

  public void setFilterDate(LocalDateTime p_min, LocalDateTime p_max) {
    setDtMinFilter(p_min);
    setDtMaxFilter(p_max);
  }

  public List<GeoCoord> parse(String p_file) {
    setJsonFile(Paths.get(p_file));
    dtLow = LocalDateTime.MAX;
    dtHigh = LocalDateTime.MIN;
    listCoord = new LinkedList<>();
    m_tim = new TimerMeter("Parse JSON");
    TimerMeter lTim = new TimerMeter("Parse JSON");
    infoAdded = 0;
    try (Stream<String> stre = Files.lines(jsonFile)) {
      stre.forEach(s -> analizzaRiga(s));
      sort();
    } catch (IOException e) {
      s_log.error("Errore durante parsing di {}, err={}", p_file, e.getMessage(), e);
      listCoord = null;
    }
    s_log.debug("JsonParser parse time={}", lTim.stop());
    return listCoord;

  }

  public void sort() {
    if (listCoord == null || listCoord.size() <= 1)
      return;
    Collections.sort(listCoord);
  }

  /**
   * Analizzo la struttura JSON:
   *
   * <pre>
   *       "formFactor": "PHONE",
   *       "timestamp": "2023-07-18T08:57:01.995Z"
   *    }, {
   *       "latitudeE7": 480805516,
   *       "longitudeE7": 26039380,
   *       "accuracy": 3200,
   *       "altitude": 135,
   *       "verticalAccuracy": 6,
   *       "source": "CELL",
   *       "deviceTag": 2008223542,
   *       "platformType": "ANDROID",
   *       "osLevel": 33,
   *       "serverTimestamp": "2023-07-18T09:08:51.780Z",
   *       "deviceTimestamp": "2023-07-18T09:08:51.444Z",
   *       "batteryCharging": false,
   *       "deviceDesignation": "UNKNOWN",
   *       "formFactor": "TABLET",
   *       "timestamp": "2023-07-18T08:57:47.361Z"
   *     }, {
   *       "latitudeE7": 480797083,
   *       "longitudeE7": 26154834,
   * </pre>
   *
   * @param p_s
   * @return
   */
  private Object analizzaRiga(String p_s) {
    if (riga++ % N_PRIMO == 0) {
      System.out.printf("Riga=%s time=%s\n", intFmt.format(riga), m_tim.stop());
      m_tim = new TimerMeter("Parse JSON");
    }

    if (p_s == null || !p_s.contains(":"))
      return null;
    double dd;
    String[] arr = p_s.split(":");
    if (arr.length < 2)
      return null;
    String lKey = arr[0].trim().replace("\"", "");
    String lVal = arr[1].trim().replace("\"", "").replace(",", "");
    if (lVal.contains("2023-07-08T13:09"))
      System.out.println("JsonParserStream.analizzaRiga()");

    for (int i = 2; i < arr.length; i++)
      lVal += ":" + arr[i].trim().replace("\"", "").replace(",", "");

    switch (lKey) {
      case CSZ_LAT:
        geoCoord = new GeoCoord(EGeoSrcCoord.google);
        dd = Double.parseDouble(lVal) / 10_000_000f;
        geoCoord.setLatitude(dd);
        infoAdded = 1;
        break;
      case CSZ_LON:
        dd = Double.parseDouble(lVal) / 10_000_000f;
        geoCoord.setLongitude(dd);
        infoAdded++;
        break;
      case CSZ_ALT:
        dd = Double.parseDouble(lVal);
        geoCoord.setAltitude(dd);
        infoAdded++;
        break;
      case CSZ_TST:
        LocalDateTime dt = s_dataParser.parseData(lVal);
        geoCoord.setTstamp(dt);
        infoAdded++;
        break;
      case CSZ_FACT:
        //        if ( !lVal.equals("PHONE"))
        //          break;
        if ( geoCoord.getTstamp() == null)
          return null;
        if (geoCoord.getTstamp().isAfter(dtMinFilter) && geoCoord.getTstamp().isBefore(dtMaxFilter))
          if (infoAdded >= 3) {
            listCoord.add(geoCoord);
            dt = geoCoord.getTstamp();
            dtLow = dtLow.isAfter(dt) ? dt : dtLow;
            dtHigh = dtHigh.isBefore(dt) ? dt : dtHigh;
          } else
            s_log.error("analizzaRiga incompleto, riga={}", riga);
        break;
    }
    return p_s;
  }

}

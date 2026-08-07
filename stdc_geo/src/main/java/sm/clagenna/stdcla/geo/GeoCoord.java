package sm.clagenna.stdcla.geo;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.utils.Utils;

/**
 * Classe per la gestione della posizione geografica di oggetti che hanno
 * caratteristiche geografiche (vedi {@link EGeoSrcCoord} ). Esso gestisce:
 * <ul>
 * <li>Il LocalDateTime della data di acquisizione</li>
 * <li>Longitudine</li>
 * <li>Latitudine</li>
 * <li>Altitudine</li>
 * <li>con {@link #getSrcGeo()} la provenienza delle informazioni</li>
 * </ul>
 */
@Data
public class GeoCoord implements Comparable<GeoCoord>, Serializable, Cloneable {
  private static final long   serialVersionUID = -6542631194264470411L;
  private static final Logger s_log            = LogManager.getLogger(GeoCoord.class);

  // private static final Logger      s_log     = LogManager.getLogger(GeoCoord.class);
  private static final GeoDistance s_geodist = new GeoDistance();

  private transient Path fotoFile;
  private transient Long fileSize;
  private LocalDateTime  tstampOld;
  private LocalDateTime  tstamp;
  private ZoneOffset     zoneOffset;
  private double         longitude;
  private double         latitude;
  private boolean        guessed;
  private double         altitude;
  private EGeoSrcCoord   srcGeo;

  public GeoCoord() {
    setLatitude(0);
    setLongitude(0);
    altitude = 0;
    setTstamp(LocalDateTime.now());
    setTstampOld(getTstamp());
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(double p_lat, double p_lon) {
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setTstamp(LocalDateTime.now());
    tstampOld = tstamp;
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon) {
    setTstamp(pdt);
    tstampOld = tstamp;
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon, double p_alt) {
    setTstamp(pdt);
    tstampOld = tstamp;
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(p_alt);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(EGeoSrcCoord p_v) {
    setSrcGeo(p_v);
  }

  //  public void setTstamp(LocalDateTime p_ts) {
  //    tstamp = p_ts;
  //  }

  public void setLongitude(double dbl) {
    longitude = dbl;
  }

  public void parseZoneOffset(String p_sz) {
    // default Italia
    zoneOffset = ZoneOffset.ofHours( +2);
    if (p_sz == null)
      return;
    setZoneOffset(ZoneOffset.of(p_sz));
  }

  /**
   * Parso i dati e li assegno a questa istanza. Se la data non è valida, allora
   * tstamp sarà null. Se latitudine o longitudine non sono valide, allora
   * saranno 0.
   * 
   * @param p_szDt
   *          la stringa della data da parsare
   * @param p_szLat
   *          la stringa della latitudine da parsare
   * @param p_szLon
   *          la stringa della longitudine da parsare
   * @return questa istanza di GeoCoord con i dati parsati
   */
  public GeoCoord parse(String p_szDt, String p_szLat, String p_szLon) {
    GeoFormatter fmt = new GeoFormatter();
    fmt.parseTStamp(this, p_szDt);
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    fmt.parseLatitude(this, p_szLat);
    fmt.parseLongitude(this, p_szLon);
    return this;
  }

  /**
   * Torna la distanza <b>geografica</b> tra le due foto in metri
   * 
   * @param p_b
   * @return
   */
  public double distanceInMetri(GeoCoord p_b) {
    if (p_b == null)
      return Double.MAX_VALUE;
    return s_geodist.calcDistance(latitude, longitude, p_b.getLatitude(), p_b.getLongitude());
  }

  public long distInSecs(GeoCoord p_o) {
    return ChronoUnit.SECONDS.between(getTstamp(), p_o.getTstamp());
  }

  public LocalDateTime addDelta(Long dlt) {
    if (null == dlt)
      return tstamp;
    if (null == tstampOld)
      tstampOld = tstamp;
    tstamp = tstampOld.plusSeconds(dlt);
    return tstamp;
  }

  public LocalDateTime getTstampOld() {
    if (null == tstampOld)
      return tstamp;
    return tstampOld;
  }

  public void assumeTStampOld() {
    if (null != tstampOld) {
      tstamp = tstampOld;
    }
  }

  //  non e' tstamp che andava modificata ! 
  //  ma dtAquisizione, che è quella che viene usata per il nome del file e per confrontare le coordinate con quelle di altre foto.
  public LocalDateTime getMainTstamp() {
    LocalDateTime lts = tstamp;
    return lts;
  }

  public static long getEpoch(LocalDateTime ts) {
    if (ts == null)
      return 0;
    ZonedDateTime zdt = ZonedDateTime.of(ts, ZoneId.systemDefault());
    return zdt.toInstant().toEpochMilli();
  }

  public long getEpoch() {
    return GeoCoord.getEpoch(tstamp);
  }

  /**
   * Verifica se l'istanza è completa, ovvero se ha un timestamp valido. Per
   * essere considerata completa, l'istanza deve avere un timestamp non null e
   * successivo a LocalDateTime.MIN. Non è necessario che abbia coordinate
   * geografiche valide, in quanto potrebbe essere utilizzata solo per
   * aggiornare il timestamp di un'altra istanza.
   *
   * @return
   */
  public boolean isComplete() {
    boolean bRet = true;
    bRet &= tstamp != null;
    if (bRet)
      bRet &= tstamp.isAfter(LocalDateTime.MIN);
    // devo poter cambiare solo il TStamp
    //    if (bRet)
    //      bRet &= longitude + latitude != 0;
    return bRet;
  }

  /**
   * Verifica se le coordinate sono cambiate rispetto ad un'altra istanza. Se
   * l'altra istanza è null o non è completa, allora si considera che le
   * coordinate sono cambiate.
   * 
   * @param p_altro
   *          l'altra istanza di GeoCoord da confrontare
   * @return true se le coordinate sono cambiate, false altrimenti
   */
  public boolean isChanged(GeoCoord p_altro) {
    boolean bRet = false;
    if (null == p_altro)
      return bRet;
    bRet = !isComplete();
    if ( !bRet)
      return bRet;
    bRet |= tstamp != null ? Utils.isChanged(tstamp, p_altro.getTstamp()) : false;
    if ( !bRet)
      bRet |= Utils.isChanged(longitude, p_altro.getLongitude());
    if ( !bRet)
      bRet |= Utils.isChanged(latitude, p_altro.getLatitude());
    if ( !bRet)
      bRet |= Utils.isChanged(altitude, p_altro.getAltitude());
    if ( !bRet)
      bRet |= Utils.isChanged(longitude, p_altro.getLongitude());
    if ( !bRet)
      bRet |= srcGeo != p_altro.getSrcGeo();
    if ( !bRet) {
      boolean ba = null == fotoFile;
      boolean bb = null == p_altro.getFotoFile();
      bRet |= ba ^ bb;
      if (bRet)
        return bRet;
      bRet |= !fotoFile.equals(p_altro.getFotoFile());
    }
    return bRet;
  }

  public boolean isTstampChanged() {
    boolean bRet = !isComplete();
    if ( !bRet)
      return bRet;
    return Utils.isChanged(tstamp, tstampOld);
  }

  public boolean isNeedRename() {
    if (null == fotoFile)
      return false;
    LocalDateTime locts = getMainTstamp();
    if (null == locts)
      return false;
    String szFile = fotoFile.getFileName().toString().toLowerCase();
    String szExt = null;
    int ndx = szFile.lastIndexOf(".");
    if (ndx > 0) {
      szExt = szFile.substring(ndx + 1);
      szFile = szFile.substring(0, ndx);
    }
    // Rimuovo eventuale suffisso di tipo "_1", "_2", etc. che viene aggiunto in caso di file con lo stesso nome
    ndx = szFile.lastIndexOf("_");
    // solo se ha un suffisso di questo tipo e se è più lungo di 14 caratteri, 
    // che è la lunghezza del nome del file senza estensione (es. "2024060112_121314_1")
    if (ndx > 14)
      szFile = szFile.substring(0, ndx);
    // ricompongo il nome del file con estensione, che è quello che devo confrontare con il nome che dovrebbe avere in base alla data di acquisizione
    szFile = String.format("%s.%s", szFile, (szExt != null ? szExt : ""));
    // passo a come dovrebbe essere il nome del file in base alla data di acquisizione
    String szNewName = GeoFormatter.createFileName(this);
    return !szFile.equals(szNewName);
  }

  public boolean isEmpty() {
    boolean bRet = false;
    bRet |= tstamp == null;
    if ( !bRet)
      bRet |= !tstamp.isAfter(LocalDateTime.MIN);
    if ( !bRet)
      bRet |= longitude * latitude == 0;
    return bRet;
  }

  public boolean isModifiedTStamp() {
    return tstampOld != null && !tstampOld.equals(tstamp);
  }

  @Override
  public String toString() {
    return GeoFormatter.format(this);
  }

  public String toStringSimple() {
    return GeoFormatter.formatSimple(this);
  }

  @Override
  public int hashCode() {
    return tstamp.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    boolean bRet = false;
    if (tstamp == null || obj == null || ! (obj instanceof GeoCoord))
      return bRet;
    GeoCoord geo = (GeoCoord) obj;
    if (geo.tstamp == null)
      return bRet;
    bRet = tstamp.equals(geo.tstamp);
    if (bRet)
      bRet &= latitude == geo.latitude;
    if (bRet)
      bRet &= longitude == geo.longitude;
    if (bRet)
      bRet &= altitude == geo.altitude;
    if (bRet) {
      bRet &= srcGeo == geo.srcGeo;
    }
    return bRet;
  }

  public boolean equalSolo(Object obj) {
    boolean bRet = false;
    if (tstamp == null || obj == null || ! (obj instanceof GeoCoord))
      return bRet;
    GeoCoord altro = (GeoCoord) obj;
    if (altro.getMainTstamp() == null)
      return bRet;
    bRet = getMainTstamp().equals(altro.getMainTstamp());
    if (bRet) {
      bRet &= srcGeo == altro.srcGeo;
    }
    return bRet;
  }

  @Override
  public int compareTo(GeoCoord p_o) {
    if (p_o == null || p_o.tstamp == null)
      return -1;
    if (tstamp == null)
      return 1;
    return tstamp.compareTo(p_o.tstamp);
  }

  public void update(GeoCoord other) {
    if (null == other)
      return;
    setAltitude(other.getAltitude());
    setLongitude(other.getLongitude());
    setLatitude(other.getLatitude());
    setGuessed(other.isGuessed());
    setFotoFile(other.getFotoFile());
  }

  public void setFotoFile(Path foFi) {
    fotoFile = foFi;
    if (null == fotoFile) {
      fileSize = null;
      return;
    }
    try {
      fileSize = Files.size(foFi);
    } catch (IOException e) {
      s_log.error("Error \"{}\" reading file size: {}", e.getMessage(), foFi.toString());
    }
  }

  public void assign(GeoCoord other) {
    if (null == other)
      return;
    update(other);
    tstamp = other.tstamp;
    if (null != other.tstampOld)
      tstamp = other.tstampOld;
    srcGeo = other.srcGeo;
  }

  public void assignMin(GeoCoord p_e) {
    if (p_e == null)
      return;
    if (p_e.getLongitude() != 0)
      longitude = p_e.getLongitude() < longitude ? p_e.getLongitude() : longitude;
    if (p_e.getLatitude() != 0)
      latitude = p_e.getLatitude() < latitude ? p_e.getLatitude() : latitude;
  }

  public void assignMax(GeoCoord p_e) {
    if (p_e == null)
      return;
    longitude = p_e.getLongitude() > longitude ? p_e.getLongitude() : longitude;
    latitude = p_e.getLatitude() > latitude ? p_e.getLatitude() : latitude;
  }

  public void altitudeAsDistance(GeoCoord p_prec) {
    if (hasLonLat())
      altitude = (int) distanceInMetri(p_prec);
  }

  public boolean hasLonLat() {
    return longitude * latitude != 0;
  }

  public boolean hasFotoFile() {
    return null != fotoFile;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    GeoCoord nw = new GeoCoord();
    nw.tstamp = tstamp;
    nw.altitude = altitude;
    nw.longitude = longitude;
    nw.latitude = latitude;
    nw.srcGeo = srcGeo;
    nw.guessed = guessed;
    nw.fotoFile = fotoFile;
    nw.fileSize = fileSize;
    return nw;
  }
}

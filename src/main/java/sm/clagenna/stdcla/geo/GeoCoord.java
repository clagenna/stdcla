package sm.clagenna.stdcla.geo;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.Data;
import sm.clagenna.stdcla.utils.Utils;

@Data
@SuppressWarnings("this-escape")
public class GeoCoord implements Comparable<GeoCoord>, Serializable, Cloneable {
  private static final long serialVersionUID = -6542631194264470411L;
  // private static final Logger      s_log     = LogManager.getLogger(GeoCoord.class);
  private static final GeoDistance s_geodist = new GeoDistance();

  private LocalDateTime tstamp;
  private ZoneOffset    zoneOffset;
  private double        longitude;
  private double        latitude;
  private boolean       guessed;
  private double        altitude;
  private EGeoSrcCoord  srcGeo;
  private Path          fotoFile;

  public GeoCoord() {
    setLatitude(0);
    setLongitude(0);
    altitude = 0;
    setTstamp(LocalDateTime.now());
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(double p_lat, double p_lon) {
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setTstamp(LocalDateTime.now());
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon) {
    setTstamp(pdt);
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon, double p_alt) {
    setTstamp(pdt);
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

  public GeoCoord parse(String p_szDt, String p_szLat, String p_szLon) {
    GeoFormatter fmt = new GeoFormatter();
    fmt.parseTStamp(this, p_szDt);
    setZoneOffset(GeoCoordFoto.s_zoneOffSet);
    fmt.parseLatitude(this, p_szLat);
    fmt.parseLongitude(this, p_szLon);
    return this;
  }

  public double distance(GeoCoord p_b) {
    if (p_b == null)
      return Double.MAX_VALUE;
    return s_geodist.calcDistance(latitude, longitude, p_b.getLatitude(), p_b.getLongitude());
  }

  public long distInSecs(GeoCoord p_o) {
    return ChronoUnit.SECONDS.between(getTstamp(), p_o.getTstamp());
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

  public boolean isComplete() {
    boolean bRet = true;
    bRet &= tstamp != null;
    if (bRet)
      bRet &= tstamp.isAfter(LocalDateTime.MIN);
    if (bRet)
      bRet &= longitude + latitude != 0;
    return bRet;
  }

  public boolean isChanged(GeoCoord p_updGeoFmt) {
    boolean bRet = false;
    if (null == p_updGeoFmt)
      return bRet;
    bRet = !isComplete();
    if ( !bRet)
      return bRet;
    bRet |= tstamp != null ? Utils.isChanged(tstamp, p_updGeoFmt.getTstamp()) : false;
    if ( !bRet)
      bRet |= Utils.isChanged(longitude, p_updGeoFmt.getLongitude());
    if ( !bRet)
      bRet |= Utils.isChanged(latitude, p_updGeoFmt.getLatitude());
    if ( !bRet)
      bRet |= Utils.isChanged(altitude, p_updGeoFmt.getAltitude());
    if ( !bRet)
      bRet |= Utils.isChanged(longitude, p_updGeoFmt.getLongitude());
    if ( !bRet)
      bRet |= srcGeo != p_updGeoFmt.getSrcGeo();
    if ( !bRet) {
      boolean ba = null == fotoFile;
      boolean bb = null == p_updGeoFmt.getFotoFile();
      bRet |= ba ^ bb;
      if (bRet)
        return bRet;
      bRet |= !fotoFile.equals(p_updGeoFmt.getFotoFile());
    }
    return bRet;
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
    GeoCoord geo = (GeoCoord) obj;
    if (geo.tstamp == null)
      return bRet;
    bRet = tstamp.equals(geo.tstamp);
    if (bRet) {
      bRet &= srcGeo == geo.srcGeo;
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
    if ( null == other )
      return;
    setAltitude(other.getAltitude());
    setLongitude(other.getLongitude());
    setLatitude(other.getLatitude());
    setGuessed(other.isGuessed());
    setFotoFile(other.getFotoFile());
  }

  public void assign(GeoCoord other) {
    if (null == other)
      return;
    update(other);
    tstamp = other.tstamp;
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
      altitude = (int) distance(p_prec);
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
    return nw;
  }
}

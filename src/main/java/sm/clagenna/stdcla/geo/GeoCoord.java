package sm.clagenna.stdcla.geo;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.Data;

@Data
public class GeoCoord implements Comparable<GeoCoord> {
  // private static final Logger      s_log     = LogManager.getLogger(GeoCoord.class);
  private static final GeoDistance s_geodist = new GeoDistance();
  private LocalDateTime            tstamp;
  private double                   longitude;
  private double                   latitude;
  private double                   altitude;
  private EGeoSrcCoord             srcGeo;
  private Path                     fotoFile;

  public GeoCoord() {
    setLatitude(0);
    setLongitude(0);
    setAltitude(0);
    setTstamp(LocalDateTime.now());
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(double p_lat, double p_lon) {
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setTstamp(LocalDateTime.now());
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon) {
    setTstamp(pdt);
    setLatitude(p_lat);
    setLongitude(p_lon);
    setAltitude(0);
    setSrcGeo(EGeoSrcCoord.track);
    setFotoFile(null);
  }

  public GeoCoord(LocalDateTime pdt, double p_lat, double p_lon, double p_alt) {
    setTstamp(pdt);
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

  public GeoCoord parse(String p_szDt, String p_szLat, String p_szLon) {
    GeoFormatter fmt = new GeoFormatter();
    fmt.parseTStamp(this, p_szDt);
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

  @Override
  public int compareTo(GeoCoord p_o) {
    if (p_o == null || p_o.tstamp == null)
      return -1;
    if (tstamp == null)
      return 1;
    return tstamp.compareTo(p_o.tstamp);
  }
}

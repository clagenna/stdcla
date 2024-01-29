package sm.clagenna.stdcla.geo.fromgoog;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.geo.EGeoSrcCoord;
import sm.clagenna.stdcla.geo.GeoCoord;
import sm.clagenna.stdcla.geo.GeoFormatter;

@Data
public class GoogPlaceVisit implements IGestGoog {
  private static final Logger s_log    = LogManager.getLogger(GoogPlaceVisit.class);
  private static GeoFormatter s_geofmt = new GeoFormatter();
  private IGeoCollector       m_coll;

  private GeoCoord       location;
  private GeoCoord       center;
  private List<GeoCoord> listChild;
  private List<GeoCoord> liRawPath;

  public GoogPlaceVisit() {
    open();
  }

  @Override
  public void open() {
    location = new GeoCoord(EGeoSrcCoord.google);
    center = new GeoCoord(EGeoSrcCoord.google);
    // indexed (childVist/simplifiedRawPath)
    listChild = new ArrayList<>();
    liRawPath = new ArrayList<>();
  }

  /**
   * Gestione dei seguenti tags:
   *
   * <pre>
   * placeVisit.location.latitudeE7/longitudeE7
   * placeVisit.duration.startTimestamp/endTimestamp
   *
   * placeVisit.centerLatE7/centerLngE7
   * placeVisit.childVisits.centerLatE7/centerLngE7
   * placeVisit.childVisits.duration.startTimestamp/endTimestamp
   * placeVisit.childVisits.location.latitudeE7/longitudeE7
   * placeVisit.simplifiedRawPath.points.latE7/lngE7
   * placeVisit.simplifiedRawPath.points.timestamp
   * --- questi sono scartati ----
   * placeVisit.childVisits.otherCandidateLocations.latitudeE7/longitudeE7
   * placeVisit.lastEditedTimestamp
   * placeVisit.otherCandidateLocations.latitudeE7
   * placeVisit.otherCandidateLocations.longitudeE7
   * </pre>
   */
  @Override
  public void gestRiga(String p_sz, Object val) {
    /*
     * placeVisit.location.latitudeE7=439506970
     * placeVisit.location.longitudeE7=124556129
     */
    if (p_sz.contains(".location.") || p_sz.contains("duration."))
      gestLocation(p_sz, val);
    if (p_sz.contains(".center"))
      gestCenter(p_sz, val);
    if (p_sz.contains(".childVisits"))
      gestChildVisit(p_sz, val);
    if (p_sz.contains(".simplifiedRawPath"))
      gestSimplified(p_sz, val);
  }

  /**
   * vedi :
   * <code>timelineObjects(19).placeVisit.childVisits(0).duration.startTimestamp=2023-07-03T08:14:38.511Z</code>
   *
   * @param p_sz
   * @param val
   */
  private void gestLocation(String p_sz, Object val) {
    double dbl;
    if (p_sz.contains("lati")) {
      dbl = latLon(val);
      location.setLatitude(dbl);
    } else if (p_sz.contains("longi")) {
      dbl = latLon(val);
      location.setLongitude(dbl);
    } else if (p_sz.contains(".duration.")) {
      LocalDateTime dt1 = null;
      if ( p_sz.contains(".end"))
        dt1 = location.getTstamp();
      s_geofmt.parseTStamp(location, val.toString());
      // c'era gia inizio ora ho la fine, faccio la media
      if ( dt1 != null) {
        LocalDateTime dt2 = location.getTstamp();
        var dif = dt1.until(dt2,ChronoUnit.SECONDS);
        location.setTstamp(dt1.plusSeconds(dif/2));
      }
    }
  }

  /**
   * vedi :
   * <code>timelineObjects(19).placeVisit.childVisits(0).centerLatE7=439504338</code>
   * oppure
   * <code>timelineObjects(19).placeVisit.childVisits(0).centerLngE7=124562787</code>
   *
   * @param p_sz
   * @param p_val
   */
  private void gestCenter(String p_sz, Object p_val) {
    double dbl = latLon(p_val);
    if (p_sz.contains("centerLatE7"))
      center.setLatitude(dbl);
    else
      center.setLongitude(dbl);
    // e' stato registrato prima
    if (location != null && location.getTstamp() != null)
      center.setTstamp(location.getTstamp());
  }

  /**
   * Per ora scarto <code>otherCandidateLocations</code>
   *
   * @param p_sz
   * @param p_val
   */
  private void gestChildVisit(String p_sz, Object val) {
    final String sz1 = ".childVisits(";
    int indx = indexChildVisit(p_sz, sz1);
    GeoCoord coo = null;
    if (listChild.size() > indx)
      coo = listChild.get(indx);
    if (coo == null) {
      coo = new GeoCoord(EGeoSrcCoord.google);
      listChild.add(indx, coo);
    }
    double dbl;
    if (p_sz.contains("latitude")) {
      dbl = latLon(val);
      coo.setLatitude(dbl);
    } else if (p_sz.contains("longitude")) {
      dbl = latLon(val);
      coo.setLongitude(dbl);
    } else if (p_sz.contains(".duration."))
      s_geofmt.parseTStamp(coo, val.toString());

  }

  private void gestSimplified(String p_sz, Object val) {
    int indx = indexChildVisit(p_sz, "points(");
    GeoCoord coo = null;
    if (liRawPath.size() > indx)
      coo = liRawPath.get(indx);
    if (coo == null) {
      coo = new GeoCoord(EGeoSrcCoord.google);
      liRawPath.add(indx, coo);
    }
    double dbl;
    if (p_sz.contains("latE7")) {
      dbl = latLon(val);
      coo.setLatitude(dbl);
    } else if (p_sz.contains("lngE7")) {
      dbl = latLon(val);
      coo.setLongitude(dbl);
    } else if (p_sz.contains(".timestamp"))
      s_geofmt.parseTStamp(coo, val.toString());
  }

  private int indexChildVisit(String p_sz, String sz1) {
    final String sz2 = ").";
    int n1 = p_sz.indexOf(sz1);
    if (n1 < 0) {
      s_log.error(sz1 + " senza indice 1 : {}", p_sz);
      return -1;
    }
    n1 += sz1.length();
    int n2 = p_sz.indexOf(sz2, n1);
    if (n2 < 0) {
      s_log.error(sz1 + " senza indice 2 : {}", p_sz);
      return -1;
    }
    String szIndx = p_sz.substring(n1, n2);
    return Integer.parseInt(szIndx);
  }

  private double latLon(Object p_v) {
    double dbl = -1;
    if (p_v == null) {
      s_log.error("Hai passato *NULL* !");
      return dbl;
    }
    if (p_v instanceof Integer oint)
      dbl = (double) oint / 10_000_000f;
    else if (p_v instanceof Double odbl)
      dbl = odbl / 10_000_000f;
    else if (p_v instanceof String sdbl)
      dbl = Double.parseDouble(sdbl) / 10_000_000f;
    else
      s_log.error("Non tratto il tipo {}", p_v.getClass().getSimpleName());
    return dbl;
  }

  @Override
  public void close() {
    saveAll();
  }

  private void saveAll() {
    if (m_coll == null) {
      s_log.warn("No geocoord collector");
      return;
    }
    m_coll.add(location);
    m_coll.add(center);
    for (GeoCoord geo : listChild)
      m_coll.add(geo);
    for (GeoCoord geo : liRawPath)
      m_coll.add(geo);
  }

  @Override
  public void setCollector(IGeoCollector p_col) {
    m_coll = p_col;
  }

}

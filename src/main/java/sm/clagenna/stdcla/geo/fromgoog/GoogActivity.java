package sm.clagenna.stdcla.geo.fromgoog;

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
public class GoogActivity implements IGestGoog {

  private static final Logger s_log    = LogManager.getLogger(GoogActivity.class);
  private static GeoFormatter s_geofmt = new GeoFormatter();

  private IGeoCollector  m_coll;
  private GeoCoord       startLocation;
  private List<GeoCoord> waypoints;
  private GeoCoord       endLocation;
  private GeoCoord       parking;

  public GoogActivity() {
    open();
  }

  @Override
  public void open() {
    startLocation = new GeoCoord(EGeoSrcCoord.google);
    endLocation = new GeoCoord(EGeoSrcCoord.google);
    parking = new GeoCoord(EGeoSrcCoord.google);
    waypoints = new ArrayList<>();
  }

  /**
   * <pre>
   *   startLocation.latitudeE7/longitudeE7
   *   endLocation.latitudeE7/longitudeE7
   *   duration.startTimestamp/endTimestamp
   *
   *   waypointPath.waypoints.latE7/lngE7
   *
   *   parkingEvent.location.latitudeE7/longitudeE7
   *   parkingEvent.timestamp
   *
   *   simplifiedRawPath.points.latE7/lngE7
   *   simplifiedRawPath.points.timestamp
   * </pre>
   */
  @Override
  public void gestRiga(String p_sz, Object val) {
    if (p_sz.contains(".startLocation."))
      gestStartLoc(p_sz, val);
    else if (p_sz.contains(".endLocation."))
      gestEndLoc(p_sz, val);
    else if (p_sz.contains(".duration."))
      gestDurationLoc(p_sz, val);
    else if (p_sz.contains(".parkingEvent."))
      gestParking(p_sz, val);
    else if (p_sz.contains(".waypointPath."))
      gestWayPoint(p_sz, val);
  }

  /**
   * gestisco:
   *
   * <pre>
   * timelineObjects(4).activitySegment.startLocation.latitudeE7=439614259
   * timelineObjects(4).activitySegment.startLocation.longitudeE7=124648596
   * </pre>
   *
   * @param p_sz
   */
  private void gestStartLoc(String p_sz, Object val) {
    double dbl = latLon(val);
    if (p_sz.contains("lati"))
      startLocation.setLatitude(dbl);
    else
      startLocation.setLongitude(dbl);
  }

  /**
   * gestisco:
   *
   * <pre>
   *  timelineObjects(2).activitySegment.endLocation.latitudeE7=439614859
   *  timelineObjects(2).activitySegment.endLocation.longitudeE7=124649917
   * </pre>
   *
   * @param p_sz
   */
  private void gestEndLoc(String p_sz, Object val) {
    double dbl = latLon(val);
    if (p_sz.contains(".latitude"))
      endLocation.setLatitude(dbl);
    else
      endLocation.setLongitude(dbl);
  }

  /**
   * <pre>
    * 175) timelineObjects(2).activitySegment.duration.startTimestamp=2023-07-01T08:32:53.866Z
    * 176) timelineObjects(2).activitySegment.duration.endTimestamp=2023-07-01T08:49:49.674Z
   * </pre>
   *
   * @param p_sz
   */
  private void gestDurationLoc(String p_sz, Object val) {
    if (p_sz.contains("startTime"))
      s_geofmt.parseTStamp(startLocation, val.toString());
    else
      s_geofmt.parseTStamp(endLocation, val.toString());
  }

  /**
   * <pre>
   * timelineObjects(2).activitySegment.parkingEvent.location.latitudeE7=439614256
   * timelineObjects(2).activitySegment.parkingEvent.location.longitudeE7=124648516
   * timelineObjects(2).activitySegment.parkingEvent.timestamp=2023-07-01T08:51:07.688Z
   * </pre>
   *
   * @param p_sz
   */
  private void gestParking(String p_sz, Object val) {
    double dbl;
    if (p_sz.contains(".latitude")) {
      dbl = latLon(val);
      parking.setLatitude(dbl);
    } else if (p_sz.contains(".longitude")) {
      dbl = latLon(val);
      parking.setLongitude(dbl);
    } else if (p_sz.contains(".timestamp")) {
      s_geofmt.parseTStamp(parking, val.toString());
    }
  }

  /**
   * <pre>
   * timelineObjects(2).activitySegment.waypointPath.waypoints(0).latE7=439183959
   * timelineObjects(2).activitySegment.waypointPath.waypoints(0).lngE7=124528722
   * timelineObjects(2).activitySegment.waypointPath.waypoints(1).latE7=439204368
   * timelineObjects(2).activitySegment.waypointPath.waypoints(1).lngE7=124524440
   * </pre>
   *
   * @param p_sz
   */
  private void gestWayPoint(String p_sz, Object val) {
    final String sz1 = ".waypoints(";
    final String sz2 = ").";
    int n1 = p_sz.indexOf(sz1);
    if (n1 < 0) {
      s_log.error("waypoint senza indice 1 : {}", p_sz);
      return;
    }
    n1 += sz1.length();
    int n2 = p_sz.indexOf(sz2, n1);
    if (n2 < 0) {
      s_log.error("waypoint senza indice 2 : {}", p_sz);
      return;
    }
    String szIndx = p_sz.substring(n1, n2);
    int currIndx = Integer.parseInt(szIndx);
    GeoCoord currGeo = null;
    if (waypoints.size() > currIndx)
      currGeo = waypoints.get(currIndx);
    if (currGeo == null) {
      currGeo = new GeoCoord(EGeoSrcCoord.google);
      waypoints.add(currIndx, currGeo);
    }
    double dbl = latLon(val);
    if (p_sz.contains(".latE7"))
      currGeo.setLatitude(dbl);
    else
      currGeo.setLongitude(dbl);
  }

  @Override
  public void close() {
    updateTimeStWayPoint();
    saveAll();
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

  private void updateTimeStWayPoint() {
    long dur = ChronoUnit.SECONDS.between(startLocation.getTstamp(), endLocation.getTstamp());
    long dlt = dur / (waypoints.size() - 1);
    var curr = startLocation.getTstamp();
    for (GeoCoord geo : waypoints) {
      geo.setTstamp(curr);
      curr = curr.plusSeconds(dlt);
    }
  }

  private void saveAll() {
    if (m_coll == null) {
      s_log.warn("No geocoord collector");
      return;
    }
    m_coll.add(startLocation);
    m_coll.add(endLocation);
    m_coll.add(parking);
    for (GeoCoord geo : waypoints) {
      m_coll.add(geo);
    }
  }

  @Override
  public void setCollector(IGeoCollector p_col) {
    m_coll = p_col;
  }

}

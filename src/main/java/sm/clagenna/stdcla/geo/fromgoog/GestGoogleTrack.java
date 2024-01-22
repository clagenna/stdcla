package sm.clagenna.stdcla.geo.fromgoog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.geo.GeoCoord;
import sm.clagenna.stdcla.geo.GeoList;

public class GestGoogleTrack implements IGoogleTrack, IGeoCollector {
  private static final Logger s_log = LogManager.getLogger(GestGoogleTrack.class);

  private static Pattern s_activitySeg = Pattern.compile(".*\\.activitySegment\\..*");
  private static Pattern s_placeVisit  = Pattern.compile(".*\\.placeVisit\\..*");

  @Getter @Setter
  private Path      fileJson;
  private IGestGoog m_goo;
  private int       lastTimLinIndex = -1;
  @Getter
  private GeoList   listGeo;

  public GestGoogleTrack() {
    //
  }

  /**
   * Gestire i "timelineObjects" "activitySegment"/placeVisit
   */
  @Override
  public void gestTrack(String p_pth, Object val) {
    int currIndx = getIndex(p_pth);
    if (lastTimLinIndex >= 0) {
      if (lastTimLinIndex != currIndx) {
        if (m_goo != null)
          m_goo.close();
        m_goo = null;
      }
    }
    lastTimLinIndex = currIndx;
    Matcher mtch = s_activitySeg.matcher(p_pth);
    if (mtch.find()) {
      activitySegment(p_pth, val);
      return;
    }
    mtch = s_placeVisit.matcher(p_pth);
    if (mtch.find()) {
      placeVisit(p_pth, val);
      return;
    }
    s_log.warn("Non conosco:{}", p_pth);
  }

  private int getIndex(String p_pth) {
    int nRet = -1;
    final String strt = "timelineObjects(";
    final String ends = ").";
    int n1 = p_pth.indexOf(strt);
    if (n1 < 0) {
      s_log.error("Non esiste index di {} su \"{}\"", strt, p_pth);
      return nRet;
    }
    n1 += strt.length();
    int n2 = p_pth.indexOf(ends, n1, n1 + 8);
    if (n2 < 0) {
      s_log.error("Non esiste index di {} su \"{}\"", ends, p_pth);
      return nRet;
    }
    String szIndx = p_pth.substring(n1, n2);
    nRet = Integer.parseInt(szIndx);
    return nRet;
  }

  private void activitySegment(String psz, Object p_val) {
    if (m_goo == null) {
      m_goo = new GoogActivity();
      m_goo.setCollector(this);
    }
    m_goo.gestRiga(psz, p_val);
  }

  private void placeVisit(String psz, Object p_val) {
    if (m_goo == null) {
      m_goo = new GoogPlaceVisit();
      m_goo.setCollector(this);
    }
    m_goo.gestRiga(psz, p_val);
  }

  @Override
  public void add(GeoCoord p_coo) {
    if (listGeo == null)
      listGeo = new GeoList();
    if ( !p_coo.isEmpty())
      listGeo.add(p_coo);
  }

  public void saveAll() {
    s_log.debug("GestGoogleTrack saveAll(size={}). sorting...", listGeo.size());
    listGeo.sortByTStamp();
    s_log.debug("GestGoogleTrack filter nearest points");
    GeoList li = listGeo.filterNearest();
    s_log.debug("no nearest (size={})", li.size());
    GeoConvGpx togpx = new GeoConvGpx();
    String destGpx = fileJson.toString().replace(".json", ".gpx");
    togpx.setDestGpxFile(Paths.get(destGpx));
    togpx.setListGeo(li);
    togpx.setOverwrite(true);
    togpx.saveToGpx();
    s_log.info("Saved GPX to {}", togpx.getDestGpxFile().toString());
  }
}

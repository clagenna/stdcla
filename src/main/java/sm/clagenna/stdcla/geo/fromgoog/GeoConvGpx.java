package sm.clagenna.stdcla.geo.fromgoog;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.geo.GeoCoord;
import sm.clagenna.stdcla.geo.GeoFormatter;
import sm.clagenna.stdcla.geo.GeoList;

@Data
public class GeoConvGpx {
  private static final Logger s_log = LogManager.getLogger(GeoConvGpx.class);

  private static final String GPX_HEAD = "" //
      + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" //
      + "<gpx creator=\"Garmin Desktop App\" \n" //
      + "     version=\"1.1\" \n" //
      + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/WaypointExtension/v1 http://www8.garmin.com/xmlschemas/WaypointExtensionv1.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/ActivityExtension/v1 http://www8.garmin.com/xmlschemas/ActivityExtensionv1.xsd http://www.garmin.com/xmlschemas/AdventuresExtensions/v1 http://www8.garmin.com/xmlschemas/AdventuresExtensionv1.xsd http://www.garmin.com/xmlschemas/PressureExtension/v1 http://www.garmin.com/xmlschemas/PressureExtensionv1.xsd http://www.garmin.com/xmlschemas/TripExtensions/v1 http://www.garmin.com/xmlschemas/TripExtensionsv1.xsd http://www.garmin.com/xmlschemas/TripMetaDataExtensions/v1 http://www.garmin.com/xmlschemas/TripMetaDataExtensionsv1.xsd http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensions/v1 http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensionsv1.xsd http://www.garmin.com/xmlschemas/CreationTimeExtension/v1 http://www.garmin.com/xmlschemas/CreationTimeExtensionsv1.xsd http://www.garmin.com/xmlschemas/AccelerationExtension/v1 http://www.garmin.com/xmlschemas/AccelerationExtensionv1.xsd http://www.garmin.com/xmlschemas/PowerExtension/v1 http://www.garmin.com/xmlschemas/PowerExtensionv1.xsd http://www.garmin.com/xmlschemas/VideoExtension/v1 http://www.garmin.com/xmlschemas/VideoExtensionv1.xsd\" \n" //
      + "     xmlns=\"http://www.topografix.com/GPX/1/1\" \n" //
      + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" //
      + "     xmlns:wptx1=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1\" \n" //
      + "     xmlns:gpxtrx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" \n" //
      + "     xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" \n" //
      + "     xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" \n" //
      + "     xmlns:trp=\"http://www.garmin.com/xmlschemas/TripExtensions/v1\" \n" //
      + "     xmlns:adv=\"http://www.garmin.com/xmlschemas/AdventuresExtensions/v1\" \n" //
      + "     xmlns:prs=\"http://www.garmin.com/xmlschemas/PressureExtension/v1\" \n" //
      + "     xmlns:tmd=\"http://www.garmin.com/xmlschemas/TripMetaDataExtensions/v1\" \n" //
      + "     xmlns:vptm=\"http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensions/v1\" \n" //
      + "     xmlns:ctx=\"http://www.garmin.com/xmlschemas/CreationTimeExtension/v1\" \n" //
      + "     xmlns:gpxacc=\"http://www.garmin.com/xmlschemas/AccelerationExtension/v1\" \n" //
      + "     xmlns:gpxpx=\"http://www.garmin.com/xmlschemas/PowerExtension/v1\" \n" //
      + "     xmlns:vidx1=\"http://www.garmin.com/xmlschemas/VideoExtension/v1\">\n" //
      + "\n" //
      + "\n" //
      + "<metadata>\n" //
      + "<link href=\"http://www.garmin.com\">\n" //
      + "  <text>Garmin International</text>\n" //
      + "</link>\n" //
      + "<time>{oggi}</time>\n" //
      + "<bounds maxlat=\"{maxlat}\" maxlon=\"{maxlon}\" minlat=\"{minlat}\" minlon=\"{minlon}\" />\n" //
      + "</metadata>\n" //
      + "\n" //
      + "\n" //
      + "<trk>\n" //
      + "<name>{trackname}</name>\n" //
      + "<extensions>\n" //
      + "  <gpxx:TrackExtension>\n" //
      + "    <gpxx:DisplayColor>Green</gpxx:DisplayColor>\n" //
      + "  </gpxx:TrackExtension>\n" //
      + "</extensions>\n" //
      + "<trkseg>\n" //
      + "\n"; //

  private static final String GPX_SEG = "" //
      + "<trkpt lat=\"{lat}\" lon=\"{lon}\">\n" //
      + "  <ele>{elev}</ele>\n" //
      + "  <time>{tstamp}</time>\n" //
      + "</trkpt>\n" //
      + "\n"; //

  private static final String GPX_END = "" //
      + "    </trkseg>\n" //
      + "  </trk>\n" //
      + "</gpx>\n"; //

  private GeoList listGeo;
  private Path    destGpxFile;
  private boolean overwrite;

  public GeoConvGpx() {
    overwrite = false;
  }

  public boolean saveToGpx(String p_sz) {
    boolean bRet = true;
    String lsz = p_sz;
    if ( !p_sz.toLowerCase().endsWith(".gpx"))
      lsz = p_sz + ".gpx";
    setDestGpxFile(Paths.get(lsz));
    if (Files.exists(destGpxFile)) {
      s_log.warn("Il file {} esiste gia'", lsz);
      bRet = false;
    } else
      bRet = saveToGpx();
    return bRet;
  }

  public boolean saveToGpx() {
    boolean bRet = true;
    if (destGpxFile == null) {
      s_log.warn("Non hai specificato il nome file GPX");
      return false;
    }
    if (Files.exists(destGpxFile) && !isOverwrite()) {
      s_log.warn("Il file {} esiste gia'", destGpxFile.toString());
      return false;
    }
    try (PrintWriter prw = new PrintWriter(destGpxFile.toFile(), Charset.forName("UTF-8"))) {
      String szOu = creaHead();
      prw.append(szOu);
      for (GeoCoord geo : listGeo) {
        szOu = creaSeg(geo);
        prw.append(szOu);
      }
      szOu = creaEnd();
      prw.append(szOu);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return bRet;
  }

  private String creaHead() {
    double minlat = 99999;
    double minlon = 99999;
    double maxlat = -99999;
    double maxlon = -99999;
    LocalDateTime oggi = LocalDateTime.now();
    String trackName = destGpxFile.getFileName().toString();
    for (GeoCoord geo : listGeo) {
      if (minlat > geo.getLatitude())
        minlat = geo.getLatitude();
      if (minlon > geo.getLongitude())
        minlon = geo.getLongitude();
      if (maxlat < geo.getLatitude())
        maxlat = geo.getLatitude();
      if (maxlon < geo.getLongitude())
        maxlon = geo.getLongitude();
    }
    String sz = replace(GPX_HEAD, "minlat", minlat);
    sz = replace(sz, "minlon", minlon);
    sz = replace(sz, "maxlat", maxlat);
    sz = replace(sz, "maxlon", maxlon);
    sz = replace(sz, "trackname", trackName);
    sz = replace(sz, "oggi", oggi);
    return sz;
  }

  private String creaSeg(GeoCoord p_geo) {
    double lat = p_geo.getLatitude();
    double lon = p_geo.getLongitude();
    double elev = p_geo.getAltitude();
    LocalDateTime tstam = p_geo.getTstamp();
    String sz = replace(GPX_SEG, "lat", lat);
    sz = replace(sz, "lon", lon);
    sz = replace(sz, "elev", elev);
    sz = replace(sz, "tstamp", tstam);
    return sz;
  }

  private String replace(String p_dove, String p_cosa, Object p_conche) {
    String szph = String.format("{%s}", p_cosa);
    String szche = null;
    String ret = p_dove;
    if (p_conche instanceof Double dbl) {
      szche = String.format("%.8f", dbl);
      szche = szche.replace(",", ".");
    } else if (p_conche instanceof String psz)
      szche = psz;
    else if (p_conche instanceof LocalDateTime ldt)
      szche = GeoFormatter.s_fmtTimeZ.format(ldt);
    if (szche != null)
      ret = ret.replace(szph, szche);
    return ret;
  }

  private String creaEnd() {
    return GPX_END;
  }

}

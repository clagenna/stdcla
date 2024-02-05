package sm.clagenna.stdcla.geo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoFormatter {

  private static Pattern      s_patNWGradiMinSec;
  private static Pattern      s_patGradiMinSecNW;
  private static Pattern      s_patDecimali;
  private static final String LNK_MAPS = "https://www.google.com/maps?z=15&t=h&q=%.8f,%.8f";
  private static boolean      showLink = false;

  public static final DateTimeFormatter s_fmtTimeOf;
  public static final DateTimeFormatter s_fmtTimeZ;
  public static final DateTimeFormatter s_fmt2Y4MD_hms;
  public static final DateTimeFormatter s_fmtmY4MD_hms;
  public static final DateTimeFormatter s_fmtmY4MD_hm;
  private static final ZoneId           s_zoneQui;
  private static final ZoneId           s_zoneUTC;
  public static final DateTimeFormatter s_dtfmt;

  public static final int LATITUDE    = 0;
  public static final int LONGITUDINE = 1;

  static {
    // s_patGradiMinSec = Pattern.compile("([+\\-]?[0-9]+). ([0-9]+). ([0-9,\\.]+).*");
    // N 17° 09' 58.3" W 179° 02' 46.8"
    s_patNWGradiMinSec = Pattern.compile("([nesw]) +([+\\-]?[0-9]+). ([0-9]+). ([0-9,\\.]+).*");
    // es: 17°09'58.3"S 179°02'46.8"W
    s_patGradiMinSecNW = Pattern.compile("([+\\-]?[0-9]+).([0-9]+).([0-9,\\.]+)\"([nNeEsSwW])+");
    s_patDecimali = Pattern.compile("[\\+\\-]?[0-9]+\\.[0-9]+");

    s_fmtTimeOf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    s_fmtTimeZ = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    s_fmt2Y4MD_hms = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    s_fmtmY4MD_hms = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    s_fmtmY4MD_hm = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    s_zoneQui = ZoneId.of("Europe/Rome");
    s_zoneUTC = ZoneId.of("UTC");
    s_dtfmt = DateTimeFormatter.ISO_DATE_TIME.withZone(s_zoneUTC);
  }

  public GeoFormatter() {
    //
  }

  public static String format(GeoCoord p_geo) {
    String szTim = "";
    String szRet = "";
    if (p_geo == null)
      return szRet;
    if (p_geo.getTstamp() != null)
      szTim = s_fmtmY4MD_hms.format(p_geo.getTstamp()) + "; ";
    if (showLink && p_geo.getLatitude() * p_geo.getLongitude() != 0) {
      String szLnk = String.format(Locale.US, LNK_MAPS, p_geo.getLatitude(), p_geo.getLongitude());
      szRet = String.format(Locale.US, "(%s)(%.10f,%.10f) (Alt:%.0f m) il %s" //
          , szLnk //
          , p_geo.getLatitude() //
          , p_geo.getLongitude(), p_geo.getAltitude(), szTim);

    } else if (p_geo.hasLonLat()) {
      GMS lonGMS = GeoFormatter.convertWGS84(p_geo.getLongitude(), LONGITUDINE);
      GMS latGMS = GeoFormatter.convertWGS84(p_geo.getLatitude(), LATITUDE);
      szRet = String.format(Locale.US, "(%s,%s)(%.10f,%.10f) (Alt:%.0f m) il %s" //
          , latGMS != null ? latGMS.toString() : "*null*" //
          , lonGMS != null ? lonGMS.toString() : "*null*" //
          , p_geo.getLatitude() //
          , p_geo.getLongitude(), p_geo.getAltitude(), szTim);
    } else {
      szRet = "(0°\"N,0°\"E)(0,0)";
    }
    if (null != p_geo.getFotoFile()) {
      szRet += String.format("\tfoto=\"%s\"", p_geo.getFotoFile().toString());
    }
    return szRet;
  }

  public static GMS convertWGS84(double vv, int pLatLon) {
    Double dd = Double.valueOf(Math.abs(vv));
    // String sgn = "";
    double gradi = dd.intValue();
    dd = (dd - gradi) * 60.;
    String orient = pLatLon == 0 ? "N" : "E";
    if (vv < 0)
      orient = pLatLon == 0 ? "S" : "W";
    double minu = Double.valueOf(dd).intValue();
    double seco = (dd - minu) * 60.;
    GMS temp = new GMS(orient, (int) gradi, (int) minu, seco);
    return temp;
  }

  public GeoCoord parse(String p_sz) {
    GeoCoord ret = null;
    return ret;
  }

  public LocalDateTime parseTStamp(String p_sz) {
    LocalDateTime tstamp = null;
    if (p_sz == null)
      return tstamp;
    // se date time Zulu -> UTC -> GMT(in disuso)
    try {
      if (p_sz.contains("+")) {
        // converto UTC in local datetime con ofset finale
        ZonedDateTime dtUTC = ZonedDateTime.parse(p_sz, s_fmtTimeOf);
        tstamp = dtUTC.toLocalDateTime();
        return tstamp;
      }
    } catch (Exception e) {
      //
    }

    try {
      if (p_sz.endsWith("Z")) {
        // converto UTC in local datetime considerando il fuso orario
        ZonedDateTime dtUTC = ZonedDateTime.parse(p_sz, s_dtfmt);
        // questa aggiungeva +2:00 ma l'orario aveva l'ora reale (anche se finiva con 'Z'...?)
        ZonedDateTime dtqui2 = dtUTC.withZoneSameInstant(s_zoneQui);
        // ZonedDateTime dtqui = dtUTC.withZoneSameInstant(s_zoneUTC);
        tstamp = dtqui2.toLocalDateTime();
        return tstamp;
      }
    } catch (Exception e) {
      //
    }
    try {
      if (tstamp == null)
        tstamp = LocalDateTime.parse(p_sz, s_fmt2Y4MD_hms);
    } catch (Exception e) {
      //
    }
    try {
      if (tstamp == null)
        tstamp = LocalDateTime.parse(p_sz, s_fmtTimeZ);
    } catch (Exception e) {
      //
    }
    try {
      if (tstamp == null)
        tstamp = LocalDateTime.parse(p_sz, s_fmtmY4MD_hms);
    } catch (Exception e) {
      //
    }
    try {
      if (tstamp == null)
        tstamp = LocalDateTime.parse(p_sz, s_fmtmY4MD_hm);
    } catch (Exception e) {
      //
    }
    if (tstamp == null)
      throw new UnsupportedOperationException("Errore timst:" + p_sz);
    return tstamp;
  }

  public GeoCoord parseTStamp(GeoCoord p_geo, String p_sz) {
    LocalDateTime tstamp = parseTStamp(p_sz);
    p_geo.setTstamp(tstamp);
    return p_geo;
  }

  public GeoCoord parseLongitude(GeoCoord p_geo, String p_sz) {
    p_geo.setLongitude(convert(p_sz, LONGITUDINE));
    return p_geo;
  }

  public GeoCoord parseLatitude(GeoCoord p_geo, String p_sz) {
    p_geo.setLongitude(convert(p_sz, LATITUDE));
    return p_geo;
  }

  private double convert(String p_sz, int pLatLon) {
    double ret = -1.;
    String orient = pLatLon == LATITUDE ? "N" : "E";
    int gradi;
    int minu;
    double seco;

    // Decimali es: -34.35245572676254
    Matcher res = s_patDecimali.matcher(p_sz);
    if (res.matches()) {
      ret = Double.parseDouble(p_sz);
      return ret;
    }
    // GMS  N 17° 09' 58.3" W 179° 02' 46.8"
    // GMS  N 34° 23' 34.73"
    res = s_patNWGradiMinSec.matcher(p_sz.toLowerCase());
    if (res.matches()) {
      orient = res.group(1).toLowerCase();
      gradi = Integer.parseInt(res.group(2));
      minu = Integer.parseInt(res.group(3));
      seco = Double.parseDouble(res.group(4).replace(",", "."));
      ret = gradi + minu / 60. + seco / 3600.;
      switch (orient) {
        case "W":
        case "w":
        case "S":
        case "s":
          ret *= -1.;
          break;
      }
      return ret;
    }
    // es: 17°09'58.3"S 179°02'46.8"W
    res = s_patGradiMinSecNW.matcher(p_sz.toLowerCase());
    if ( !res.matches())
      throw new UnsupportedOperationException("Non interpreto:" + p_sz);
    gradi = Integer.parseInt(res.group(1));
    minu = Integer.parseInt(res.group(2));
    seco = Double.parseDouble(res.group(3).replace(",", "."));
    orient = res.group(4);
    ret = gradi + minu / 60. + seco / 3600.;
    switch (orient) {
      case "W":
      case "w":
      case "S":
      case "s":
        ret *= -1.;
        break;
    }

    return ret;
  }

  public static void setShowLink(boolean bv) {
    showLink = bv;
  }

  public static boolean isShowLink() {
    return showLink;
  }
}

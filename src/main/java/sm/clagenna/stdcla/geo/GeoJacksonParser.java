package sm.clagenna.stdcla.geo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;

public class GeoJacksonParser {
  private static final Logger s_log = LogManager.getLogger(GeoJacksonParser.class);

  public static final String FLD_source            = "source";
  public static final String FLD_osLevel           = "osLevel";
  public static final String FLD_accuracy          = "accuracy";
  public static final String FLD_altitude          = "altitude";
  public static final String FLD_velocity          = "velocity";
  public static final String FLD_deviceTag         = "deviceTag";
  public static final String FLD_timestamp         = "timestamp";
  public static final String FLD_formFactor        = "formFactor";
  public static final String FLD_latitudeE7        = "latitudeE7";
  public static final String FLD_longitudeE7       = "longitudeE7";
  public static final String FLD_platformType      = "platformType";
  public static final String FLD_batteryCharging   = "batteryCharging";
  public static final String FLD_deviceTimestamp   = "deviceTimestamp";
  public static final String FLD_serverTimestamp   = "serverTimestamp";
  public static final String FLD_deviceDesignation = "deviceDesignation";
  public static final String FLD_verticalAccuracy  = "verticalAccuracy";

  private static ParseData s_tmParse = new ParseData();

  @Getter @Setter
  private Path          fileJson;
  @Getter @Setter
  private LocalDateTime minDateTime;
  @Getter @Setter
  private LocalDateTime maxDateTime;

  @Getter @Setter
  private GeoList liGeo;

  public GeoJacksonParser() {
    //
  }

  public GeoList parseJson(String p_fi) throws FileNotFoundException {
    fileJson = Paths.get(p_fi);
    if ( !Files.exists(fileJson, LinkOption.NOFOLLOW_LINKS))
      throw new FileNotFoundException(p_fi);
    GeoList li = parseJson();
    if (li != null)
      li.sortByTStamp();
    return li;
  }

  private GeoList parseJson() throws FileNotFoundException {
    liGeo = new GeoList();
    final int goodLiv = 2;
    @SuppressWarnings("unused")
    int arrLiv = 0;
    int nestLiv = 0;
    int riga = 0;
    String fldNam = null;
    Object fldVal = null;

    JsonFactory jsfact = new JsonFactory();
    GeoCoord geo = null;
    s_log.debug("Start parsing Json GPS {}", fileJson.toString());
    try (JsonParser jsp = jsfact.createParser(new FileInputStream(fileJson.toFile()))) {
      do {
        JsonToken tok = jsp.nextToken();
        riga = jsp.getCurrentLocation().getLineNr();
        switch (tok) {
          case START_ARRAY:
            arrLiv++;
            // s_log.trace("{})  liv={}:{} --({})--\n", riga, nestLiv, arrLiv, fldNam);
            break;
          case END_ARRAY:
            // s_log.trace("{})^^^ liv={}:{} ^^({})^^\n", riga, nestLiv, arrLiv, fldNam);
            arrLiv--;
            break;
          case START_OBJECT:
            nestLiv++;
            if (nestLiv == goodLiv) {
              geo = new GeoCoord();
              geo.setSrcGeo(EGeoSrcCoord.google);
              // s_log.trace("{})--- liv={}:{} --({})--\n", riga, nestLiv, arrLiv, fldNam);
            }
            break;
          case END_OBJECT:
            if (nestLiv == goodLiv) {
              liGeo.add(geo);
              geo = null;
              // s_log.trace("{})^^^ liv={}:{} ^^({})^^\n", riga, nestLiv, arrLiv, fldNam);
            }
            nestLiv--;
            break;

          case FIELD_NAME:
            fldNam = jsp.getText();
            fldVal = null;
            if (nestLiv == goodLiv) {
              JsonToken tok2 = jsp.nextToken();
              switch (tok2) {
                case VALUE_STRING:
                  fldVal = jsp.getText();
                  break;
                case VALUE_NUMBER_FLOAT:
                  fldVal = jsp.getDoubleValue();
                  break;
                case VALUE_NUMBER_INT:
                  fldVal = jsp.getIntValue();
                  break;
                case VALUE_FALSE:
                  fldVal = Boolean.FALSE;
                  break;
                case VALUE_TRUE:
                  fldVal = Boolean.TRUE;
                  break;
                default:
                  break;
              }
              if (fldVal != null) {
                // s_log.trace("{}) {} = {}", riga, fldNam, fldVal);
                geo = assignGeo(geo, fldNam, fldVal);
              }
            }
            break;
          default:
            break;
        }
      } while (nestLiv > 0);
      s_log.debug("End parsing Json GPS {}", fileJson.toString());
    } catch (IOException e) {
      s_log.error("Error parsing \"{}\", row={}, error={}", fileJson.toString(), riga, e.getMessage());
      liGeo = null;
    }
    return liGeo;
  }

  private GeoCoord assignGeo(GeoCoord p_geo, String p_fldNam, Object p_fldVal) {
    if (p_fldVal == null)
      return p_geo;
    double dbl;
    LocalDateTime dt;
    switch (p_fldNam) {
      case FLD_latitudeE7:
        dbl = Double.parseDouble(p_fldVal.toString()) / 10_000_000F;
        p_geo.setLatitude(dbl);
        break;
      case FLD_longitudeE7:
        dbl = Double.parseDouble(p_fldVal.toString()) / 10_000_000F;
        p_geo.setLongitude(dbl);
        break;
      case FLD_altitude:
        dbl = Double.parseDouble(p_fldVal.toString());
        p_geo.setAltitude(dbl);
        break;
      case FLD_timestamp:
        // privilegio il "deviceTimestamp"
        dt = s_tmParse.parseData(p_fldVal.toString());
        if (p_geo.getTstamp() == null)
          p_geo.setTstamp(dt);
        break;
      case FLD_deviceTimestamp:
        dt = s_tmParse.parseData(p_fldVal.toString());
        p_geo.setTstamp(dt);
        break;
      default:
        break;
    }
    return p_geo;
  }

}

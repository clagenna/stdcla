package sm.clagenna.stdcla.geo.fromgoog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import lombok.Getter;
import sm.clagenna.stdcla.geo.GeoList;

public class JacksonParseRecurse {
  private static final Logger s_log = LogManager.getLogger(JacksonParseRecurse.class);

  private int             m_riga;
  private PrintWriter     m_prw;
  @Getter
  private GestGoogleTrack gestTtack;

  public JacksonParseRecurse() {
    //
  }

  public GeoList parseGeo(String p_srcDir) {
    return parseGeo(Paths.get(p_srcDir));
  }

  public GeoList parseGeo(Path p_fileJson) {
    s_log.info("Parsing Google Takeout {}", p_fileJson);
    JsonFactory jsfact = new JsonFactory();
    gestTtack = new GestGoogleTrack();
    gestTtack.setFileJson(p_fileJson);
    String ou = p_fileJson.getFileName().toString();
    int n = ou.lastIndexOf(".");
    ou = ou.substring(0, n) + ".tmp";
    try ( /* FileWriter fw = new FileWriter(ou); */ PrintWriter prw = new PrintWriter(ou, Charset.forName("UTF-8"))) {
      m_prw = prw;
      try (JsonParser jsp = jsfact.createParser(new FileInputStream(p_fileJson.toFile()))) {
        JsonToken tok = nextToken(jsp);
        String padre = "";
        if (tok == JsonToken.START_ARRAY)
          parseArray(jsp, padre);
        else
          parseObject(jsp, padre);
      }
      // gestTtack.saveAll();
    } catch (IOException e) {
      s_log.error("Error parsing {}, err={}", p_fileJson, e.getMessage(), e);
      gestTtack = null;
    }
    return gestTtack.getListGeo();
  }

  private void parseObject(JsonParser p_jsp, String p_padre) throws IOException {
    JsonToken tok = nextToken(p_jsp);
    String fldName = null;
    Object fldVal = null;
    String locPadre = null;
    do {
      switch (tok) {
        case START_OBJECT:
          locPadre = p_padre + "." + fldName;
          parseObject(p_jsp, locPadre);
          break;
        case END_OBJECT:
          return;

        case START_ARRAY:
          if (p_padre != null && p_padre.length() > 1)
            locPadre = p_padre + "." + fldName;
          else
            locPadre = fldName;
          parseArray(p_jsp, locPadre);
          break;
        case END_ARRAY:
          break;

        case FIELD_NAME:
          fldName = p_jsp.getText();
          fldVal = null;
          break;
        case VALUE_FALSE:
          fldVal = Boolean.FALSE;
          break;
        case VALUE_TRUE:
          fldVal = Boolean.TRUE;
          break;
        case VALUE_NULL:
          fldVal = "*null*";
          break;
        case VALUE_NUMBER_FLOAT:
          fldVal = p_jsp.getDoubleValue();
          break;
        case VALUE_NUMBER_INT:
          fldVal = p_jsp.getIntValue();
          break;
        case VALUE_STRING:
          fldVal = p_jsp.getText();
          break;

        case VALUE_EMBEDDED_OBJECT:
        case NOT_AVAILABLE:
          s_log.error("Token sconosciuto: {}", tok.toString());
          // System.out.println("----> Sconosciuto:" + tok.toString());
          break;
        default:
          break;

      }
      if (fldVal != null) {
        String pth = String.format("%s.%s", p_padre, fldName);
        assign(pth, fldVal);
        fldVal = null;
      }
      tok = nextToken(p_jsp);
    } while (tok != JsonToken.END_OBJECT);
  }

  private void parseArray(JsonParser p_jsp, String p_padre) throws IOException {
    JsonToken tok = nextToken(p_jsp);
    String fldName = null;
    Object fldVal = null;
    String locPadre = null;
    int arrIndx = 0;
    do {
      locPadre = String.format("%s(%d)", p_padre, arrIndx);
      switch (tok) {
        case START_OBJECT:
          parseObject(p_jsp, locPadre);
          arrIndx++;
          break;
        case END_OBJECT:
          s_log.error("Improbabile END-OBJECT");
          arrIndx++;
          break;
        case START_ARRAY:
          parseArray(p_jsp, locPadre);
          break;
        case END_ARRAY:
          return;

        case FIELD_NAME:
          fldName = p_jsp.getText();
          fldVal = null;
          break;
        case VALUE_FALSE:
          fldVal = Boolean.FALSE;
          break;
        case VALUE_TRUE:
          fldVal = Boolean.TRUE;
          break;
        case VALUE_NULL:
          fldVal = "*null*";
          break;
        case VALUE_NUMBER_FLOAT:
          fldVal = p_jsp.getDoubleValue();
          break;
        case VALUE_NUMBER_INT:
          fldVal = p_jsp.getIntValue();
          break;
        case VALUE_STRING:
          fldVal = p_jsp.getText();
          break;

        case VALUE_EMBEDDED_OBJECT:
        case NOT_AVAILABLE:
          s_log.error("Token Sconosciuto:{}", tok.toString());
          break;
        default:
          break;
      }
      if (fldVal != null) {

        String pth = String.format("%s(%d).%s", p_padre, arrIndx, fldName);
        if (fldName == null)
          pth = String.format("%s(%d)", p_padre, arrIndx);
        assign(pth, fldVal);
        fldVal = null;
      }
      tok = nextToken(p_jsp);
    } while (tok != JsonToken.END_ARRAY);
  }

  private void assign(String p_pth, Object p_Val) {
    String val = p_Val.toString();
    if (p_pth.contains("E7") //
        || val.matches(".*[0-9]{4}-[0-9]{2}-[0-9]{2}.*")) {
      // System.out.printf("%d) %s=%s\n", m_riga, p_pth, val);
      m_prw.printf("%d) %s=%s\n", m_riga, p_pth, val);
      gestTtack.gestTrack(p_pth, p_Val);
    }
  }

  private JsonToken nextToken(JsonParser p_jsp) throws IOException {
    JsonToken tok = p_jsp.nextToken();
    m_riga = p_jsp.getCurrentLocation().getLineNr();
    return tok;

  }
}

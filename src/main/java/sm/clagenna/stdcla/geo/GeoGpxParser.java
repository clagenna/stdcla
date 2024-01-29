package sm.clagenna.stdcla.geo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Getter;
import sm.clagenna.stdcla.sys.TimerMeter;

public class GeoGpxParser extends DefaultHandler {
  private static final Logger s_log = LogManager.getLogger(GeoGpxParser.class);

  private GeoFormatter       m_fmt;
  private LinkedList<String> m_stack;
  private String             m_txChars;
  private GeoCoord           m_coo;
  private GeoList            m_liCoo;
  @Getter
  private Path               fileGPX;

  public GeoGpxParser() {
    //
  }

  public GeoList parseGpx(Path p_gpx) {
    s_log.debug("SAX parse di {}", p_gpx);
    TimerMeter tim = new TimerMeter("SAX parse");
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser;
    try {
      saxParser = factory.newSAXParser();
    } catch (ParserConfigurationException | SAXException e) {
      String szMsg = "Errore sul file GPX delle tracce: " + p_gpx + "; err=" + e.getMessage();
      s_log.error(szMsg, e);
      return m_liCoo;
    }
    s_log.debug("Open SAX, time={}", tim.stop());
    tim = new TimerMeter("SAX parsing");
    try {
      saxParser.parse(p_gpx.toString(), this);
      fileGPX = p_gpx;
    } catch (SAXException | IOException e) {
      String szMsg = "Errore parsing file GPX delle tracce: " + p_gpx + "; err=" + e.getMessage();
      s_log.error(szMsg, e);
      return m_liCoo;
    }
    s_log.debug("Parse SAX, time={}", tim.stop());
    return m_liCoo;
  }

  @Override
  public void startDocument() throws SAXException {
    m_stack = new LinkedList<>();
    m_liCoo = new GeoList();
    m_fmt = new GeoFormatter();
  }

  @Override
  public void endDocument() throws SAXException {
    // m_liCoo.stream().forEach(s -> System.out.println(s.toCsv()));
    // System.out.println("Elems = " + m_liCoo.size());
  }

  /**
   * Proviamo ad interpretare la seguente sequenza:
   *
   * <pre>
   * &lt;trk&gt;
    &lt;name&gt;2023-07-06 Courmayeur&lt;/name&gt;
    &lt;extensions&gt;
      &lt;gpxx:TrackExtension&gt;
        &lt;gpxx:DisplayColor&gt;DarkGray&lt;/gpxx:DisplayColor&gt;
      &lt;/gpxx:TrackExtension&gt;
    &lt;/extensions&gt;
    &lt;trkseg&gt;
      &lt;trkpt lat=&quot;45.129960989579558&quot; lon=&quot;9.658012976869941&quot;&gt;
        &lt;ele&gt;59.740000000000002&lt;/ele&gt;
        &lt;time&gt;2023-07-06T08:27:44Z&lt;/time&gt;
      &lt;/trkpt&gt;
      &lt;trkpt lat=&quot;45.132209016010165&quot; lon=&quot;9.653621027246118&quot;&gt;
        &lt;ele&gt;62.619999999999997&lt;/ele&gt;
        &lt;time&gt;2023-07-06T08:27:57Z&lt;/time&gt;
      &lt;/trkpt&gt;
   * </pre>
   *
   * trk - trkseg - trkpt : ele/time
   *
   */
  @Override
  public void startElement(String p_uri, String p_localName, String p_qName, Attributes p_attributes) throws SAXException {
    m_txChars = null;
    m_stack.push(p_qName);
    String szXpath = getXpath();
    switch (szXpath) {
      case "gpx/trk/trkseg/trkpt":
        m_coo = new GeoCoord(EGeoSrcCoord.track);
        Map<String, String> mp = parseAttributes(p_attributes);
        m_coo.setLatitude(Double.parseDouble(mp.get("lat")));
        m_coo.setLongitude(Double.parseDouble(mp.get("lon")));
        // System.out.println(mp);
        break;
    }
  }

  @Override
  public void characters(char[] p_ch, int p_start, int p_length) throws SAXException {
    m_txChars = new String(p_ch, p_start, p_length);
    m_txChars = m_txChars.replaceAll("\\n", "");
    m_txChars = m_txChars.replaceAll("\\r", "");
    m_txChars = m_txChars.trim();
    if (m_txChars.length() == 0)
      m_txChars = null;
  }

  @Override
  public void endElement(String p_uri, String p_localName, String p_qName) throws SAXException {
    String szXpath = getXpath();
    switch (szXpath) {
      case "gpx/trk/trkseg/trkpt/ele":
        m_coo.setAltitude(Double.parseDouble(m_txChars));
        break;
      case "gpx/trk/trkseg/trkpt/time":
        m_coo.setTstamp(m_fmt.parseTStamp(m_txChars));
        m_coo.setSrcGeo(EGeoSrcCoord.track);
        m_liCoo.add(m_coo);
        m_coo = null;
        break;
    }
    m_stack.pop();
  }

  private String getXpath() {
    Iterator<String> iter = m_stack.descendingIterator();
    String sz = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, 0), false) //
        .collect(Collectors.joining("/"));
    return sz;
  }

  private Map<String, String> parseAttributes(Attributes attributes) {
    int k = 0;
    if (attributes != null)
      k = attributes.getLength();
    Map<String, String> map = new HashMap<>();
    if (k == 0)
      return map;
    for (int i = 0; i < k; i++) {
      String ob = attributes.getLocalName(i);
      String vv = attributes.getValue(i);
      map.put(ob, vv);
    }
    return map;
  }

  public GeoList list() {
    return m_liCoo;
  }
}

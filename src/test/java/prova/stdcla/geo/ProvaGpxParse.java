package prova.stdcla.geo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import sm.clagenna.stdcla.geo.GeoFormatter;
import sm.clagenna.stdcla.geo.GeoGpxParser;
import sm.clagenna.stdcla.geo.GeoList;

public class ProvaGpxParse {
  private String CSZ_FI = "F:\\java\\photon2\\dbgps\\data\\CurrentTrackLog.gpx";

  public ProvaGpxParse() {
    //
  }

  @Test
  public void provalo() {
    String lsz = "2024-01-04T14:00:01+01:00";
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime odt = OffsetDateTime.parse(lsz, fmt);
    LocalDateTime ldt = odt.toLocalDateTime();
    System.out.println(GeoFormatter.s_fmtmY4MD_hms.format(ldt));

    GeoGpxParser parser = new GeoGpxParser();
    Path pth = Paths.get(CSZ_FI);
    GeoList li = parser.parseGpx(pth);
    System.out.println("Fine del parsing, elems=" + li.size());
  }

}

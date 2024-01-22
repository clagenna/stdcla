package sm.clagenna.stdcla.geo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeoList extends LinkedList<GeoCoord> {
  private static final long   serialVersionUID = 6350913396026124486L;
  private static final Logger s_log            = LogManager.getLogger(GeoList.class);

  private GeoCoord mints;
  private GeoCoord maxts;

  public GeoList() {
    mints = new GeoCoord();
    mints.setTstamp(LocalDateTime.MAX);
    maxts = new GeoCoord();
    maxts.setTstamp(LocalDateTime.MIN);
  }

  @Override
  public boolean add(GeoCoord p_e) {
    boolean b = super.add(p_e);
    if (p_e != null) {
      mints = p_e.compareTo(mints) < 0 ? p_e : mints;
      maxts = p_e.compareTo(maxts) > 0 ? p_e : maxts;
    }
    return b;
  }

  public List<GeoCoord> sortByTStamp() {
    Collections.sort(this, new Comparator<GeoCoord>() {
      @Override
      public int compare(GeoCoord p_o1, GeoCoord p_o2) {
        return p_o1.getTstamp().compareTo(p_o2.getTstamp());
      }
    });
    return this;
  }

  /**
   * Torna un List scartando i punti molto vicini (<2 metri e secondi < 10s)
   *
   * @return il list filtrato
   */
  public GeoList filterNearest() {
    GeoCoord prec = null;
    GeoList liNew = new GeoList();
    for (GeoCoord geo : this) {
      if (prec == null) {
        prec = geo;
        liNew.add(geo);
        continue;
      }
      double dist = prec.distance(geo);
      long secs = ChronoUnit.SECONDS.between(prec.getTstamp(), geo.getTstamp());
      if (dist > 2 || secs > 10) {
        liNew.add(geo);
        if (s_log.isTraceEnabled()) {
          s_log.trace("{}, {} diff={}, dist={}m", //
              geo.getLatitude(), //
              geo.getLongitude(), //
              secs, dist);
        }
      }
      prec = geo;
    }
    return liNew;
  }

  /**
   * Dato un GeoCoord (con timestamp) cerca nel List il GeoCoord con valore il
   * piu vicino possibile
   *
   * @param trova
   * @return Il GeoCoord piu vicino nel tempo a "trova"
   */
  public GeoCoord findNearest(LocalDateTime trova) {
    GeoCoord retGeo = null;
    if (mints.getTstamp().isAfter(maxts.getTstamp())) {
      s_log.error("Non ci sono dati");
      return retGeo;
    }
    if (mints.getTstamp().isAfter(trova) || maxts.getTstamp().isBefore(trova)) {
      s_log.error("Il valore e {} fuori range ({} - {} )", //
          GeoFormatter.s_fmt2Y4MD_hms.format(trova), //
          GeoFormatter.s_fmt2Y4MD_hms.format(mints.getTstamp()), //
          GeoFormatter.s_fmt2Y4MD_hms.format(maxts.getTstamp()));
      return retGeo;
    }
    // ausilio per min distanza in tempo
    long minDistSecs = 99_999_999L;
    GeoCoord geoTrova = new GeoCoord();
    geoTrova.setTstamp(trova);
    // allargo il campo (in secondi) per beccare i geoCoord piu vicini
    boolean looppa = true;
    // i geoCoord piu vicini
    List<GeoCoord> filtered = null;
    // ampiezza dello spazio temporale da filtrare
    int delta = 10;
    final int dltIncr = 10;
    int tries = 0;
    final int maxTries = 20;
    // data una finestra di tempo (-delta,+delta) cerco i "papabili"
    // faccio 10 tentativi (fino a -(dltIncr * 10) sec a +(dltIncr * 10))
    do {
      LocalDateTime loMom = trova.minusMinutes(delta);
      LocalDateTime hiMom = trova.plusMinutes(delta);
      filtered = this //
          .stream() //
          .filter( //
              s -> ( //
              s.getTstamp().isAfter(loMom) //
                  && s.getTstamp().isBefore(hiMom) //
              ) //
          ) //
          .toList();
      looppa = filtered == null || filtered.size() == 0;
      delta += dltIncr;
      tries++;
//      if (looppa && s_log.isDebugEnabled()) {
        s_log.debug("Allargo range a {} secs per il tentativo No {}", delta * 2, tries);
//      }
    } while (looppa && tries < maxTries);
    if (filtered == null || filtered.size() == 0) {
      s_log.error("Non trovo nearest a {}", GeoFormatter.s_fmtmY4MD_hms.format(trova));
      return retGeo;
    }
    //    System.out.println("------ List filtrati -------");
    //    filtered //
    //        .stream() //
    //        .forEach(s -> System.out.println(GeoFormatter.s_fmt2Y4MD_hms.format(s.getTstamp())));
    //    System.out.println("--------------------------------");
    // fra gli estratti cerco il più vicino
    for (GeoCoord geo : filtered) {
      if (retGeo == null) {
        retGeo = geo;
        minDistSecs = Math.abs(geo.distInSecs(geoTrova));
        // System.out.printf("ProvaSublist.findNearest(%s)\n", minDistSecs);
        continue;
      }
      long lDist = Math.abs(geo.distInSecs(geoTrova));
      if (lDist < minDistSecs) {
        retGeo = geo;
        minDistSecs = lDist;
        // System.out.printf("ProvaSublist.findNearest(%s)\n", minDistSecs);
      }
    }
    return retGeo;
  }

}

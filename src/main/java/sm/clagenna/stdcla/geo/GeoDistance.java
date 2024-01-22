package sm.clagenna.stdcla.geo;

/**
 * Calcola le distanze fra due punti geografici utilizzando le formule del
 * HaverSine e la formula(algoritmo) di Vincenty (vedi
 * <a href="https://www.baeldung.com/java-find-distance-between-points">sito
 * Baeldung</>)
 */
public class GeoDistance {

  private static final double EARTH_RADIUS       = 6_371_000;
  private static final double SEMI_MAJOR_AXIS_MT = 6_378_137;
  private static final double SEMI_MINOR_AXIS_MT = 6_356_752.314245;
  private static final double FLATTENING         = 1 / 298.257223563;
  private static final double ERROR_TOLERANCE    = 1e-12;

  public GeoDistance() {
    //
  }

  private double haversine(double val) {
    return Math.pow(Math.sin(val / 2), 2);
  }

  public double calcDistance(double startLat, double startLong, double endLat, double endLong) {

    double dLat = Math.toRadians(endLat - startLat);
    double dLong = Math.toRadians(endLong - startLong);

    startLat = Math.toRadians(startLat);
    endLat = Math.toRadians(endLat);

    double a = haversine(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversine(dLong);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;
  }

  /**
   * Calcola la distanza fra due punti A e B geografici su di un elissoide (la
   * Terra) con il metodo Vincenty ( <a href=
   * "https://community.esri.com/t5/coordinate-reference-systems-blog/distance-on-an-ellipsoid-vincenty-s-formulae/ba-p/902053">vedi
   * sito</a> )
   *
   * @param latitude1
   *          latitudine punto A
   * @param longitude1
   *          longitudine punto A
   * @param latitude2
   *          latitudine punto B
   * @param longitude2
   *          longitudine punto B
   * @return
   */
  public double calcDistanceVincenty(double latitude1, double longitude1, double latitude2, double longitude2) {
    double U1 = Math.atan( (1 - FLATTENING) * Math.tan(Math.toRadians(latitude1)));
    double U2 = Math.atan( (1 - FLATTENING) * Math.tan(Math.toRadians(latitude2)));

    double sinU1 = Math.sin(U1);
    double cosU1 = Math.cos(U1);
    double sinU2 = Math.sin(U2);
    double cosU2 = Math.cos(U2);

    double longitudeDifference = Math.toRadians(longitude2 - longitude1);
    double previousLongitudeDifference;

    double sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;

    do {
      sinSigma = Math.sqrt(Math.pow(cosU2 * Math.sin(longitudeDifference), 2)
          + Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * Math.cos(longitudeDifference), 2));
      cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * Math.cos(longitudeDifference);
      sigma = Math.atan2(sinSigma, cosSigma);
      sinAlpha = cosU1 * cosU2 * Math.sin(longitudeDifference) / sinSigma;
      cosSqAlpha = 1 - Math.pow(sinAlpha, 2);
      cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
      if (Double.isNaN(cos2SigmaM)) {
        cos2SigmaM = 0;
      }
      previousLongitudeDifference = longitudeDifference;
      double C = FLATTENING / 16 * cosSqAlpha * (4 + FLATTENING * (4 - 3 * cosSqAlpha));
      longitudeDifference = Math.toRadians(longitude2 - longitude1) + (1 - C) * FLATTENING * sinAlpha
          * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * ( -1 + 2 * Math.pow(cos2SigmaM, 2))));
    } while (Math.abs(longitudeDifference - previousLongitudeDifference) > ERROR_TOLERANCE);

    double uSq = cosSqAlpha * (Math.pow(SEMI_MAJOR_AXIS_MT, 2) - Math.pow(SEMI_MINOR_AXIS_MT, 2)) / Math.pow(SEMI_MINOR_AXIS_MT, 2);

    double A = 1 + uSq / 16384 * (4096 + uSq * ( -768 + uSq * (320 - 175 * uSq)));
    double B = uSq / 1024 * (256 + uSq * ( -128 + uSq * (74 - 47 * uSq)));

    double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * ( -1 + 2 * Math.pow(cos2SigmaM, 2))
        - B / 6 * cos2SigmaM * ( -3 + 4 * Math.pow(sinSigma, 2)) * ( -3 + 4 * Math.pow(cos2SigmaM, 2))));

    double distanceMt = SEMI_MINOR_AXIS_MT * A * (sigma - deltaSigma);
    return distanceMt;
  }
 
}

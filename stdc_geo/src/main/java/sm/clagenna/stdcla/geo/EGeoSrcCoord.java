package sm.clagenna.stdcla.geo;

/**
 * Oggetti che contengono sia
 * <ul>
 * <li>coordinate geografiche</li>
 * <li>sia Timestamp di rilevazione</li>
 * </ul>
 * nel nostro caso gestiamo:
 * <ul>
 * <li><b>Track</b> proveniente dal tracce del navigatore</li>
 * <li><b>Google Location History</b> fornita con
 * <a href="https://takeout.google.com">Google takeout</a></li>
 * <li><b>Foto</b> proveniente dalle info Exif</li>
 * </ul>
 */
public enum EGeoSrcCoord {
  /** Navigator Tracks */
  track, //
  /** Google Location History */
  google, //
  /** Exif info of position and Timestamp in fotos */
  foto;
}

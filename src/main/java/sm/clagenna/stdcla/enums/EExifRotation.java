package sm.clagenna.stdcla.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Vedi <a href="https://exiftool.org/TagNames/EXIF.html">Exif Tools</a>
 * 
 * <pre>
    1 = Horizontal (normal)
    2 = Mirror horizontal
    3 = Rotate 180
    4 = Mirror vertical
    5 = Mirror horizontal and rotate 270 CW
    6 = Rotate 90 CW
    7 = Mirror horizontal and rotate 90 CW
    8 = Rotate 270 CW
 * </pre>
 */
public enum EExifRotation {
  Horizontal(1), //
  Rotate_90(8), //
  Rotate_180(3), //
  Rotate_270(6), //

  Mirror_vert(2), //
  Mirror_hor(4), //
  Mirror_270(5), //
  Mirror_90(7);

  private int                                val;
  private static Map<Integer, EExifRotation> s_map;
  static {
    s_map = new HashMap<Integer, EExifRotation>();
    for (EExifRotation e : EExifRotation.values())
      s_map.put(e.val, e);
  }

  private EExifRotation(int v) {
    val = v;
  }

  public int getRotation() {
    return val;
  }

  public static EExifRotation parse(int v) {
    EExifRotation ret = null;
    if (s_map.containsKey(v))
      ret = s_map.get(v);
    return ret;
  }
}

package sm.clagenna.stdcla.utils;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.xml.sax.SAXParseException;

public class Utils {

  public static double DBL_XMAX    = 580.0f;
  public static double DBL_YMAX    = 800.0f;
  public static double F_A4wi      = 2480f;
  public static double F_A4he      = 3508f;
  public static double F_XCharMax  = 150f;
  public static double F_YRigheMax = 140f;

  private static Locale               S_LOCALE;
  private static DecimalFormatSymbols S_FMT_SYMS;
  private static String               S_Decimal_Sep;
  private static String               S_Group_Sep;

  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static final DecimalFormat s_fmtDbl;
  public static final DecimalFormat s_fmtInt;

  public static final SimpleDateFormat s_fmtY4MDHMSm   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  public static final SimpleDateFormat s_fmtY4MDHMS    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static final SimpleDateFormat s_fmtY4MDHMS_F  = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
  public static final SimpleDateFormat s_fmtY4MD       = new SimpleDateFormat("yyyy-MM-dd");
  public static final SimpleDateFormat s_fmtDMY4       = new SimpleDateFormat("dd/MM/yyyy");
  public static final SimpleDateFormat s_fmtdDMMY4HM   = new SimpleDateFormat("dd/MM/yyyy HH:mm");
  public static final SimpleDateFormat s_fmtDMY4_HMS   = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  public static final SimpleDateFormat s_fmtdDMMY4HMS  = new SimpleDateFormat("dd MMM yyyy HH:mm");
  public static final SimpleDateFormat s_fmt_ymd4b_hms = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  public static final SimpleDateFormat s_fmt_ymd4      = new SimpleDateFormat("yyyyMMdd");
  public static final SimpleDateFormat s_fmt_ymd4hms   = new SimpleDateFormat("yyyyMMdd_HHmmss");
  public static final SimpleDateFormat s_fmt_ymdThms   = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  public static final SimpleDateFormat s_sfmtTMZ       = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  public static final SimpleDateFormat s_sfmtTMZ2      = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

  static {
    s_fmtDbl = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault()); // ("#,##0.00", Locale.getDefault())
    s_fmtDbl.applyPattern("#,##0.00");
    s_fmtInt = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
    s_fmtInt.applyPattern("#,##0");
  }

  public Utils() {
    //
  }

  public static void setLocale(Locale ll) {
    S_LOCALE = ll;
    DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(S_LOCALE);
    S_FMT_SYMS = df.getDecimalFormatSymbols();
    Character decsep = S_FMT_SYMS.getDecimalSeparator();
    S_Decimal_Sep = decsep.toString();
    Character grpsep = S_FMT_SYMS.getGroupingSeparator();
    S_Group_Sep = grpsep.toString();
  }

  public static Locale getLocale() {
    return S_LOCALE;
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Genera un nome hash temporaneo (forse univoco) basandosi sulla funzione
   * UUID
   *
   * @return un nome hash random
   */
  public static String genTempNodeName() {
    UUID uuid = UUID.randomUUID();
    String sz = uuid + "@" + System.identityHashCode(uuid);
    String l_szTempNodeName = Utils.bytesToHex(Base64.getEncoder().encode(sz.getBytes()));
    return l_szTempNodeName;
  }

  public static boolean isChanged(File p_fi1, File p_fi2) {
    String sz1 = null;
    if (p_fi1 != null)
      sz1 = p_fi1.getAbsolutePath();
    String sz2 = null;
    if (p_fi2 != null)
      sz2 = p_fi2.getAbsolutePath();
    return Utils.isChanged(sz1, sz2);
  }

  public static boolean isChanged(Path p_fi1, Path p_fi2) {
    String sz1 = null;
    if (p_fi1 != null)
      sz1 = p_fi1.toAbsolutePath().toString();
    String sz2 = null;
    if (p_fi2 != null)
      sz2 = p_fi2.toAbsolutePath().toString();
    return Utils.isChanged(sz1, sz2);
  }

  public static boolean isChanged(String p_szPrimo, String p_szSecondo) {
    if (p_szPrimo == null && p_szSecondo == null)
      return false;
    if (p_szPrimo == null || p_szSecondo == null)
      return true;
    return !p_szPrimo.equals(p_szSecondo);
  }

  public static boolean isChanged(LocalDateTime p_dtVal, LocalDateTime p_dtOld) {
    if (p_dtOld == null && p_dtVal == null)
      return false;
    if (p_dtOld == null || p_dtVal == null)
      return true;
    return !p_dtOld.equals(p_dtVal);
  }

  public static boolean isChanged(Double p_dtVal, Double p_dtOld) {
    if (p_dtOld == null && p_dtVal == null)
      return false;
    if (p_dtOld == null || p_dtVal == null)
      return true;
    return !p_dtOld.equals(p_dtVal);
  }

  public static boolean isChanged(int p_primo, int p_secondo) {
    return Utils.isChanged(String.valueOf(p_primo), String.valueOf(p_secondo));
  }

  public static boolean isChanged(Date p_dt1, Date p_dt2) {
    String sz1 = null;
    if (p_dt1 != null)
      sz1 = s_fmtY4MDHMSm.format(p_dt1);
    String sz2 = null;
    if (p_dt2 != null)
      sz2 = s_fmtY4MDHMSm.format(p_dt2);
    return Utils.isChanged(sz1, sz2);
  }

  public static boolean isNumeric(String p_sz) {
    if ( !Utils.isValue(p_sz))
      return false;
    try {
      Long ii = Utils.parseLong(p_sz);
      if (null != ii)
        return true;
    } catch (NumberFormatException ex) {
      //
    }
    return false;
  }

  public static boolean isValue(Object p_v) {
    if (p_v == null)
      return false;
    if (p_v.getClass().getSimpleName().equals("Object"))
      return false;
    if (p_v.toString().trim().length() > 0)
      return true;
    return false;
  }

  public static boolean isValue(String p_v) {
    if (p_v == null)
      return false;
    if (p_v.trim().length() > 0)
      return true;
    return false;
  }

  public static boolean isValue(Double p_v) {
    if (p_v == null)
      return false;
    return p_v != 0;
  }

  public static boolean isValue(Date p_dt) {
    if (p_dt == null)
      return false;
    return p_dt.getTime() > 0;
  }

  public static boolean isValue(LocalDateTime p_dt) {
    if (null == p_dt)
      return false;
    Date dt = Date.from(p_dt.atZone(ZoneId.systemDefault()).toInstant());
    return dt.getTime() > 0;
  }

  public static boolean isValue(Integer p_i) {
    if (p_i == null)
      return false;
    return p_i.intValue() != 0;
  }

  public static boolean isValue(BigInteger p_i) {
    if (p_i == null)
      return false;
    return p_i.intValue() != 0;
  }

  public static boolean isValue(long p_i) {
    return p_i != 0;
  }

  public static boolean isValueEq(Integer p_v1, Integer p_v2) {
    if (p_v1 == null || p_v2 == null)
      return false;
    return p_v1.equals(p_v2);
  }

  public static boolean isValueEq(String p_v1, String p_v2) {
    if (p_v1 == null || p_v2 == null)
      return false;
    return p_v1.equals(p_v2);
  }

  public static boolean isValueEq(Date p_v1, Date p_v2) {
    if (p_v1 == null || p_v2 == null)
      return false;
    return p_v1.equals(p_v2);
  }

  public static boolean isValueEq(LocalDateTime dtExam, LocalDateTime dtExam2) {
    if (null == dtExam || null == dtExam2)
      return false;
    Date dt1 = Date.from(dtExam.atZone(ZoneId.systemDefault()).toInstant());
    Date dt2 = Date.from(dtExam2.atZone(ZoneId.systemDefault()).toInstant());
    return isValueEq(dt1, dt2);
  }

  public static double getDoubleProp(Properties p_prop, String p_propName) {
    return Utils.getDoubleProp(p_prop, p_propName, 0D);
  }

  public static double getDoubleProp(Properties prop, String propName, double defValue) {
    double retDbl = defValue;
    String p = prop.getProperty(propName);
    if (p == null)
      return defValue;
    try {
      retDbl = Double.parseDouble(p);
    } catch (NumberFormatException ex) {
      //
    }
    return retDbl;
  }

  public static Integer parseInt(Object vv) {
    if (null == vv)
      return null;
    return Utils.parseInt(vv.toString());
  }

  public static Integer parseInt(String psz) {
    if (null == S_LOCALE)
      Utils.setLocale(Locale.getDefault());
    Integer ii = null;
    if (null == psz)
      return ii;
    try {
      ii = Integer.parseInt(psz.trim().replace(S_Group_Sep, "").replace(S_Decimal_Sep, "."));
    } catch (NumberFormatException ex) {
      //
    }
    return ii;
  }

  public static Long parseLong(String psz) {
    if (null == S_LOCALE)
      Utils.setLocale(Locale.getDefault());
    Long ii = null;
    if (null == psz)
      return ii;
    try {
      ii = Long.parseLong(psz.trim().replace(S_Group_Sep, "").replace(S_Decimal_Sep, "."));
    } catch (NumberFormatException ex) {
      //
    }
    return ii;
  }

  public static double parseDouble(Object vv) {
    if (null == vv)
      return 0;
    return Utils.parseDouble(vv.toString());
  }

  public static Double parseDouble(String psz) {
    if (null == S_LOCALE)
      Utils.setLocale(Locale.getDefault());
    Double ii = null;
    if (null == psz)
      return ii;
    // ---------------------------------------------------
    // questo perche' dagli USA mi arrivano double della forma "-9,99" ?!?
    // devo sovrascrivere il tipo di formatter
    Locale locale = Utils.getLocale();
    int nv = psz.length() - psz.lastIndexOf(",");
    if (nv == 3)
      locale = Locale.ITALY;
    // ----------------------------------------------------
    NumberFormat fmt = NumberFormat.getInstance(locale);
    try {
      // cambiare i "." e "," a priori è un arbitrio, va chiamata la Utils.setLocale()
      // String sz = psz.trim().replace(S_Group_Sep, "").replace(S_Decimal_Sep, ".");
      String sz = psz.trim();
      ii = fmt.parse(sz).doubleValue();
    } catch (NumberFormatException | ParseException ex) {
      //
    }
    return ii;
  }

  public static String formatDouble(Double dbl) {
    String szRet = null;
    if (null == dbl)
      return szRet;
    if (null == S_LOCALE)
      Utils.setLocale(Locale.getDefault());
    NumberFormat fmt = NumberFormat.getInstance(S_LOCALE);
    szRet = fmt.format(dbl);
    return szRet;
  }

  /**
   * Toglie dalle date Gregoriane es:<br/>
   * <code>2017-09-22T07:31:05.231+02Z</code><br/>
   * la parte dei millisecondi e la timezone, ottenendo:<br/>
   * <code>2017-09-22T07:31:05Z</code>
   *
   * @param p_sz
   * @return
   */
  public static String togliMillesimiDaGregorian(String p_sz) {
    String sz = p_sz;
    String szMatch = "\\.000\\+[0-9][0-9]\\:[0-9][0-9]";
    sz = sz.replaceAll(szMatch, "");
    return sz;
  }

  public static String expandExc(Throwable p_ex) {
    if (p_ex == null)
      return "";
    Throwable l_ex = p_ex;
    StringBuilder szMsg = new StringBuilder();
    szMsg.append(Utils.getFormattedExceptionMessage(p_ex));
    if (szMsg == null || szMsg.length() < 3) {
      szMsg.append("");
    }
    //
    int nStackedCauseLevel = 0;
    final int nMAX_StackedCauseLevel = 5;//limite massimo
    while (l_ex != null && nStackedCauseLevel++ < nMAX_StackedCauseLevel) {
      String sz = Utils.getFormattedExceptionMessage(l_ex);
      if (sz.length() > 3 //
          && !szMsg.toString().contains(sz) //
          && (l_ex.getMessage() == null || !szMsg.toString().contains(l_ex.getMessage())) //
      ) {
        szMsg.append("\n").append(sz);
      }
      // prossimo!
      l_ex = l_ex.getCause();
    }
    return szMsg.toString();
  }

  /**
   * Torna l'estensione del file (comprensiva del punto '.') in lowercase
   *
   * @param p_pth
   * @return
   */
  public static String getFileExtention(Path p_pth) {
    if (null == p_pth)
      return null;
    String szExt = p_pth.getName(p_pth.getNameCount() - 1).toString();
    int n = szExt.lastIndexOf(".");
    if (n >= 0)
      szExt = szExt.substring(n);
    szExt = szExt.toLowerCase();
    return szExt;
  }

  /**
   *
   * @param p_thr
   * @return testo formattato
   */
  public static String getFormattedExceptionMessage(Throwable p_thr) {
    String szMsg = "";
    if (p_thr instanceof SAXParseException) {
      szMsg = String.format("%s: linea=%d colonna=%d %s" //
          , p_thr.getClass().getSimpleName() //
          , ((SAXParseException) p_thr).getLineNumber() //
          , ((SAXParseException) p_thr).getColumnNumber() //
          , p_thr.getMessage() //
      );
    } else {
      szMsg = String.format("%s: %s" //
          , p_thr.getClass().getSimpleName() //
          , p_thr.getMessage() //
      );
    }
    return szMsg;
  }

  public static String capitalize(String p_sz) {
    if (p_sz == null)
      return p_sz;
    return p_sz.substring(0, 1).toUpperCase() + p_sz.substring(1);
  }

  public static void changeLogLevel(Level level) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel(Level.DEBUG);
    ctx.updateLoggers(); // This causes all Loggers to refetch information from their LoggerConfig.
  }

  public static LocalDateTime min(LocalDateTime ldtMin, LocalDateTime dtTest) {
    if (null == ldtMin || null == dtTest || ldtMin.isAfter(dtTest))
      return dtTest;
    return ldtMin;
  }

  public static LocalDateTime max(LocalDateTime ldtMax, LocalDateTime dtTest) {
    if (null == ldtMax || null == dtTest || ldtMax.isBefore(dtTest))
      return dtTest;
    return ldtMax;
  }

}

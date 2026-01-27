package sm.clagenna.stdcla.pdf;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;
import sm.clagenna.stdcla.utils.sys.TimerMeter;

public class HtmlValue implements Comparable<HtmlValue>, Cloneable {
  private static final Logger s_log = LogManager.getLogger(HtmlValue.class);

  private static int           lastId          = 0;
  private static final String  CSZ_PAT_ALL, CSZ_PAT_TOP, CSZ_PAT_LEFT, CSZ_PAT_HE, CSZ_PAT_WI;
  public static final String   CSV_HEADER      = "sep=;\nid;page;py;px;ny;nx;typ;txt;time;html\n";
  private static double        F_Correttivo    = 1.;
  private static Locale        s_double_locale = Locale.ITALIAN;
  private static final Pattern patAll;
  private static final Pattern patTop;
  private static final Pattern patLeft;
  private static final Pattern patWi;
  private static final Pattern patHe;

  @Getter
  private int         id;
  @Getter @Setter
  private double      fx;
  @Getter @Setter
  private double      fy;
  /** dimensione del text fornito dal HTML tag "width:nn.nnpt" */
  @Getter @Setter
  private double      width;
  /** dimensione font fornito dal HTML tag "font-size:nn.nnpt" */
  @Getter @Setter
  private double      fontSize;
  @Getter @Setter
  private int         left;
  @Getter @Setter
  private int         top;
  @Getter
  private int         page;
  private String      txt;
  @Getter @Setter
  private String      rigaHtml;
  @Getter @Setter
  private boolean     noSeq;
  @Getter @Setter
  private ETipiDato   tipoDato;
  @Getter @Setter
  private Date        valData;
  @Getter @Setter
  private Double      valDouble;
  private Double      vMin;
  private Double      vMax;
  @Getter
  private Integer     intero;
  @Getter
  private BigDecimal  importo;
  @Getter
  private String      fattNo;    // TODO portare nella classe specializzata X AASS
  @Getter @Setter
  private String      contatore; // TODO portare nella classe specializzata X AASS
  @Getter @Setter
  private IPdfGestore pdfGest;

  private double                     timParse;
  public static final DateFormat     fmtData   = new SimpleDateFormat("dd/MM/yyyy");
  public static final DateFormat     fmtOraP   = new SimpleDateFormat("HH.mm.ss");
  public static final DateFormat     fmtOra    = new SimpleDateFormat("HH:mm:ss");
  @SuppressWarnings("unused")
  private static final DecimalFormat s_dblFmt2 = new DecimalFormat("#,###.00");
  private static final DecimalFormat s_dblFmt0 = new DecimalFormat("#,###");

  private static Pattern patInt15   = Pattern.compile(ETipiDato.IntN15.getRegex());
  private static Pattern patBarrato = Pattern.compile(ETipiDato.Barrato.getRegex());
  private static Pattern patData    = Pattern.compile(ETipiDato.Data.getRegex());
  private static Pattern patOra     = Pattern.compile(ETipiDato.Ora.getRegex());
  private static Pattern patReal    = Pattern.compile(ETipiDato.Float.getRegex());
  private static Pattern patRealUSA = Pattern.compile("(-{0,1}[\\d]*\\d+[\\.,]\\d+)");
  // private static Pattern patImpor   = Pattern.compile(ETipiDato.Importo.getRegex());
  private static Pattern patNum = Pattern.compile(ETipiDato.Intero.getRegex());
  //  private static Pattern patLess    = Pattern.compile(ETipiDato.Less.getRegex());
  //  private static Pattern patMinMax  = Pattern.compile(ETipiDato.MinMax.getRegex());

  // per suplire all'anno nel txt:  "Credito attuale anno 2022:"
  private static Pattern patNum2p = Pattern.compile("(\\d+):");

  static {
    /**
     * class="p" id="p63"<br/>
     * style="<br/>
     * top:138.7503pt;<br/>
     * left:54.95072pt; line-height:12.5pt;<br/>
     * font-family:Tahoma;<br/>
     * font-size:10.0pt;<br/>
     * width:58.200005pt;<br/>
     * ">accettazione:
     * </pre>
     *
     * <pre>
     *  div class="p" id="p64"<br/>
     *    style="<br/>
     *      top:152.25171pt;<br/>
     *      left:117.21266pt;<br/>
     *      line-height:12.5pt;<br/>
     *      font-family:Tahoma,Bold;<br/>
     *      font-size:10.0pt;<br/>
     *      font-weight:bold;<br/>
     *      width:62.499985pt;<br/>
     * ">07/10/2025
     * </pre>
     */
    CSZ_PAT_ALL = ".*<div .* style=\"" //
        + "top:(\\d+\\.\\d+)pt;" //
        + "left:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "font-size:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "width:(\\d+\\.\\d+)pt;" //
        + ".*>(.*)</div>";
    CSZ_PAT_TOP = ".*<div .* style=.*" //
        + "top:(\\d+\\.\\d+)pt;";
    CSZ_PAT_LEFT = ".*<div .* style=.*" //
        + "left:(\\d+\\.\\d+)pt;";
    CSZ_PAT_HE = ".*<div .* style=.*" //
        + "height:(\\d+\\.\\d+)pt;";
    CSZ_PAT_WI = ".*<div .* style=.*" //
        + "width:(\\d+\\.\\d+)pt;";
    patAll = Pattern.compile(CSZ_PAT_ALL);
    patTop = Pattern.compile(CSZ_PAT_TOP);
    patLeft = Pattern.compile(CSZ_PAT_LEFT);
    patWi = Pattern.compile(CSZ_PAT_WI);
    patHe = Pattern.compile(CSZ_PAT_HE);
  }

  public HtmlValue() {
    id = lastId++;
  }
  //
  //  public PHtmlValue(double p_x, double p_y, double p_fwidth, double p_FSiz, int page, String txt, String szRiHtml) {
  //    id = lastId++;
  //    setFx(p_x);
  //    setFy(p_y);
  //    setWidth(p_fwidth);
  //    setFontSize(p_FSiz);
  //    setPage(page);
  //    setTxt(txt);
  //    setRigaHtml(szRiHtml.trim());
  //    calcola();
  //  }

  public static void setDoubleLocale(Locale loc) {
    s_double_locale = loc;
  }

  private void calcola() {
    int px = (int) Math.round(getFx() / Utils.DBL_XMAX * Utils.F_XCharMax);
    int py = (int) Math.round(getFy() / Utils.DBL_YMAX * Utils.F_YRigheMax);
    // salto alla pagina
    if (py > 0)
      py += (int) ( (getPage() - 1) * Utils.F_YRigheMax);
    setLeft(px);
    setTop(py);
  }

  /**
   * Dato un testo tratto dal HTML del tipo:
   *
   * <pre>
   * &lt;div class=&quot;p&quot; id=&quot;p95&quot; style=&quot;top:272.92267pt;left:32.173244pt;line-height:11.25pt;font-family:Tahoma;font-size:9.0pt;width:35.937pt;&quot;&gt;Neutrofili&lt;/div&gt;
   * &lt;div class=&quot;p&quot; id=&quot;p96&quot; style=&quot;top:272.92267pt;left:408.0021pt;line-height:10.0pt;font-family:Tahoma;font-size:8.0pt;width:15.528046pt;&quot;&gt;2,00&lt;/div&gt;
   * &lt;div class=&quot;p&quot; id=&quot;p97&quot; style=&quot;top:272.92267pt;left:426.03415pt;line-height:10.0pt;font-family:Tahoma;font-size:8.0pt;width:2.9039917pt;&quot;&gt;-&lt;/div&gt;
   * &lt;div class=&quot;p&quot; id=&quot;p98&quot; style=&quot;top:272.92267pt;left:431.44214pt;line-height:10.0pt;font-family:Tahoma;font-size:8.0pt;width:15.528046pt;&quot;&gt;8,00&lt;/div&gt;
   * &lt;div class=&quot;p&quot; id=&quot;p99&quot; style=&quot;top:272.92267pt;left:312.3422pt;line-height:11.25pt;font-family:Tahoma;font-size:9.0pt;width:17.468994pt;&quot;&gt;2,35&lt;/div&gt;
   * </pre>
   *
   * Cerca di fare il parse della stringa <code>style</code> e popolare questo
   * oggetto con:
   * <ul>
   * <li>fx = left</li>
   * <li>fy = top</li>
   * <li>fontSize = font-size</li>
   * <li>width = width</li>
   * <li>txt = inner tag</li>
   * </ul>
   *
   * @return
   */
  public int parseHtmlText(String szRigaHtml, IPdfGestore pPdfGest) {
    TimerMeter tt = new TimerMeter("parseHtmlText");
    pdfGest = pPdfGest;
    String szLeft, szTop, szFontSiz, szWidth, szToken;
    szLeft = szTop = szFontSiz = szWidth = szToken = null;
    setRigaHtml(szRigaHtml);
    setTipoDato(ETipiDato.HTML);
    // 1) HTML Ausiliario
    if ( !szRigaHtml.contains("<div class=")) {
      fy = pdfGest.getMaxTop();
      pdfGest.setMaxTop(fy + 0.1);
      page = pdfGest.getNPage();
      tt.stop();
      timParse = tt.getMillis();
      return 1;
    }

    // 2) pattern più complesso, con token
    Matcher mtch = patAll.matcher(szRigaHtml);
    if (mtch.find()) {
      int k = 1;
      szTop = mtch.group(k++); // top
      szLeft = mtch.group(k++); // left
      szFontSiz = mtch.group(k++); // font-size
      szWidth = mtch.group(k++); // width
      szToken = mtch.group(k++); // inner tag
      trattaToken(szLeft, szTop, szFontSiz, szWidth, szToken, szRigaHtml);
      tt.stop();
      timParse = tt.getMillis();
      return 5;
    }

    // 3) div della pagina
    if (szRigaHtml.indexOf("class=\"page\"") >= 0) {
      pdfGest.setNPage(pdfGest.getNPage() + 1);
      pdfGest.setMaxTop(0.);
    }
    // 4) tutto quello che rimane di ausiliario
    mtch = patLeft.matcher(szRigaHtml);
    if (mtch.find()) {
      szLeft = mtch.group(1);
      fx = Double.parseDouble(szLeft);
    }

    mtch = patTop.matcher(szRigaHtml);
    if (mtch.find()) {
      szTop = mtch.group(1);
      fy = Double.parseDouble(szTop);
    } else {
      fy = pdfGest.getMaxTop();
      pdfGest.setMaxTop(fy + 0.1);
    }
    mtch = patHe.matcher(szRigaHtml);
    if (mtch.find()) {
      szFontSiz = mtch.group(1);
      fontSize = Double.parseDouble(szFontSiz);
    }
    mtch = patWi.matcher(szRigaHtml);
    if (mtch.find()) {
      szWidth = mtch.group(1);
      width = Double.parseDouble(szWidth);
    }
    page = pdfGest.getNPage();
    setTipoDato(ETipiDato.HTML);
    setRigaHtml(szRigaHtml);
    setTxt(null);

    analizzaHtmlValue();
    tt.stop();
    timParse = tt.getMillis();
    return 1;
  }

  //  private void trattaHtml(String szLeft, String szTop, String szHeigt, String szWidth, String szText, String szRigaHtml) {
  //    if (null == szTop)
  //      m_pdfGest.setMaxTop(m_pdfGest.getMaxTop() + 0.1);
  //    fx = null != szLeft ? Double.parseDouble(szLeft) : 0.;
  //    fy = null != szTop ? Double.parseDouble(szTop) : m_pdfGest.getMaxTop();
  //    fontSize = null != szHeigt ? Double.parseDouble(szHeigt) : 0.;
  //    width = null != szWidth ? Double.parseDouble(szWidth) : 0.;
  //    page = m_pdfGest.getNPage();
  //    setTipoDato(ETipiDato.HTML);
  //    setRigaHtml(szRigaHtml);
  //    setTxt(null);
  //
  //    analizzaHtmlValue();
  //  }

  public void analizzaHtmlValue() {
    // override per ulteriori analis
  }

  private void trattaToken(String szLeft, String szTop, String szFontSiz, String szWidth, String szText, String szRiHtml) {
    int pagNo = pdfGest.getNPage();
    if (pagNo <= 0)
      s_log.error("Pagina fuori range:{} su tag {}", pagNo, szText);
    if (null != szText && szText.contains("&"))
      szText = StringEscapeUtils.unescapeHtml4(szText);
    page = pagNo;
    fx = Double.parseDouble(szLeft);
    fy = Double.parseDouble(szTop);
    fontSize = Double.parseDouble(szFontSiz);
    width = Double.parseDouble(szWidth);
    txt = null == szText ? null : szText.trim();
    discerni();
    double mxTop = pdfGest.getMaxTop();
    if (fy > mxTop)
      pdfGest.setMaxTop(fy);
    calcola();
  }

  public boolean isNbsp() {
    return txt.indexOf("&nbsp;") >= 0;
  }

  public boolean isTextConParentesiQuadre() {
    if ( !isText())
      return false;
    if (getTxt().contains("[") || getTxt().contains("]"))
      return true;
    return false;
  }

  public String removeParentesiQuadre() {
    if ( !isTextConParentesiQuadre())
      return getTxt();
    txt = txt.replace("[", "");
    txt = txt.replace("]", "");
    discerni();
    return txt;
  }

  /**
   * I set <code>final</code> because of compile error:
   * <code>[this-escape] possible 'this' escape before subclass is fully initialized</code><br/>
   * I'm using this mehod on constructor and compiler complains about the fact
   * that some child class may override it.
   *
   * @param p_page
   */
  public final void setPage(int p_page) {
    page = p_page;
  }

  public String getTxt() {
    return txt;
  }

  public Object getMinMax() {
    double lMin = null != vMin ? vMin : 0f;
    double lMax = null != vMax ? vMax : 0f;
    String sz = String.format("[%s - %s]", Utils.formatDouble(lMin), Utils.formatDouble(lMax));
    return sz;
  }

  public void setTxt(String p_txt) {
    txt = p_txt;
    discerni();
  }

  protected synchronized void discerni() {
    tipoDato = ETipiDato.HTML;
    valData = null;
    valDouble = null;
    vMin = null;
    vMax = null;
    intero = null;
    importo = null;
    contatore = null;
    vMin = null;
    vMax = null;
    if (null == txt || txt.trim().length() == 0)
      return;

    switch (txt.trim()) {

      case "[":
        tipoDato = ETipiDato.QAper;
        return;
      case "]":
        tipoDato = ETipiDato.QChiu;
        return;

      case "+":
        tipoDato = ETipiDato.Plus;
        return;
      case "-":
        tipoDato = ETipiDato.Minus;
        return;

      case "(":
        tipoDato = ETipiDato.PAper;
        return;
      case ")":
        tipoDato = ETipiDato.PChiu;
        return;

      case ">":
        tipoDato = ETipiDato.Great;
        return;
      case "<":
        tipoDato = ETipiDato.Minor;
        return;

      case "%":
        tipoDato = ETipiDato.Perc;
        return;
      case "*":
        tipoDato = ETipiDato.Aster;
        return;

    }
    // ------------- EMAIL ------------------
    String lTxt = txt.trim();
    if (Utils.isEmail(lTxt)) {
      setTipoDato(ETipiDato.Email);
      return;
    }
    // ------------- DATA ------------------
    if (patData.matcher(lTxt).matches()) {
      //      try {
      //        valData = fmtData.parse(lTxt);
      //        tipoDato = ETipiDato.Data;
      //      } catch (Exception e) {
      //        s_log.error("Parse data:" + txt, e);
      //      }
      try {
        var dt = ParseData.parseData(lTxt);
        if (null != dt) {
          valData = ParseData.toDate(dt);
          tipoDato = ETipiDato.Data;
          return;
        }
      } catch (Exception e) {
        s_log.error("Parse data:" + lTxt, e);
      }
    }
    // ------------- ORA ------------------
    if (patOra.matcher(lTxt).matches()) {
      try {
        if (lTxt.indexOf(".") > 0)
          valData = fmtOraP.parse(lTxt);
        else
          valData = fmtOra.parse(lTxt);
        tipoDato = ETipiDato.Ora;
      } catch (Exception e) {
        s_log.error("Parse ora:" + txt, e);
      }
      return;
    }
    // -------------- IMPORTO ----------------
    //    if (patImpor.matcher(lTxt).matches()) {
    //      try {
    //        Double dbl = Utils.parseDouble(lTxt);
    //        if (null != dbl) {
    //          importo = BigDecimal.valueOf(dbl);
    //          importo = importo.setScale(2, RoundingMode.HALF_DOWN);
    //          tipoDato = ETipiDato.Importo;
    //          return;
    //        }
    //      } catch (NumberFormatException e) {
    //        s_log.error("Parse real:" + txt, e);
    //      }
    //    }
    // -------------- FLOAT ITA ----------------
    if (s_double_locale.equals(Locale.ITALIAN) || //
        s_double_locale.equals(Locale.ITALY)) {
      if (patReal.matcher(lTxt).matches()) {
        try {
          // vDbl = Double.parseDouble(lTxt.replace(',', '.'));
          valDouble = NumberFormat.getInstance(s_double_locale).parse(lTxt).doubleValue();
          tipoDato = ETipiDato.Float;
          return;
        } catch (NumberFormatException | ParseException e) {
          // e.printStackTrace();
          s_log.error("Parse real:" + txt, e);
        }
      }
    }
    if (s_double_locale.equals(Locale.US)) {
      if (patRealUSA.matcher(lTxt).matches()) {
        try {
          // vDbl = Double.parseDouble(lTxt.replace(',', '.'));
          valDouble = NumberFormat.getInstance(s_double_locale).parse(lTxt).doubleValue();
          tipoDato = ETipiDato.Float;
          return;
        } catch (NumberFormatException | ParseException e) {
          // e.printStackTrace();
          s_log.error("Parse real:" + txt, e);
        }
      }
    }
    // ------------- CONTATORE (int15) -------------------
    if (patInt15.matcher(txt).matches()) {
      try {
        contatore = txt;
        tipoDato = ETipiDato.IntN15;
        return;
      } catch (Exception e) {
        // e.printStackTrace();
        s_log.error("Parse Fatt. No:" + txt, e);
      }
    }
    // ------------- INTERO ---------------
    if (patNum.matcher(txt).matches()) {
      try {
        long ll = Long.MAX_VALUE;
        if (txt.length() <= 10)
          ll = Long.parseLong(txt.replace(".", ""));
        if (ll < Integer.MAX_VALUE) {
          intero = (int) ll;
          tipoDato = ETipiDato.Intero;
          return;
        }
      } catch (NumberFormatException e) {
        s_log.error("Parse number:" + txt, e);
      }
    }
    // ------------- INTERO con ':' ---------------
    if (patNum2p.matcher(txt).matches()) {
      try {
        long ll = Long.MAX_VALUE;
        String sza2p = txt.substring(0, txt.length() - 1);
        if (sza2p.length() <= 10)
          ll = Long.parseLong(sza2p);
        if (ll < Integer.MAX_VALUE) {
          intero = (int) ll;
          tipoDato = ETipiDato.Intero;
          return;
        }
      } catch (NumberFormatException e) {
        s_log.error("Parse number:" + txt, e);
      }
    }
    // ------------- Num Fattura (xxx/yyyyyy) -------------------
    if (patBarrato.matcher(txt).matches()) {
      try {
        fattNo = txt;
        tipoDato = ETipiDato.Barrato;
      } catch (Exception e) {
        s_log.error("Parse Fatt. No:" + txt, e);
      }
      return;
    }
    setTipoDato(ETipiDato.Stringa);
  }

  public boolean isNumero() {
    return tipoDato.isNumeric();
  }

  public boolean isIntero() {
    return tipoDato == ETipiDato.Intero;
  }

  public boolean isHiphen() {
    if (null != txt) {
      return txt.trim().equals("-");
    }
    return false;
  }

  public boolean isLessOrBig() {
    if (null != txt && (txt.contains("<") || txt.contains(">")))
      return true;
    return false;
  }

  public boolean isReale() {
    return tipoDato == ETipiDato.Float;
  }

  public boolean isData() {
    return tipoDato == ETipiDato.Data;
  }

  public boolean isFattNo() {
    return tipoDato == ETipiDato.Barrato;
  }

  public boolean isText() {
    return tipoDato == ETipiDato.Stringa;
  }

  /**
   * Cerco di tornare un valore double scelto tra i numerici. Questo è dovuto al
   * fatto che nelle fatture i valori numerici sono spesso <i>ballerini</i> tra
   * tipologie diverse. Vedi la "quantita" nei consumi.
   *
   * @return double fra i campi numerici valorizzati
   */
  public Double getvDbl() {
    if (null != valDouble)
      return valDouble;
    if (null != importo)
      return importo.doubleValue();
    if (null != intero)
      return Double.valueOf(intero);
    return valDouble;
  }

  public double getvMin() {
    if (null == vMin)
      return 0d;
    return vMin;
  }

  public double getvMax() {
    if (null == vMax)
      return 0d;
    return vMax;
  }

  @Override
  public int compareTo(HtmlValue p_o) {
    if (getPage() < p_o.getPage())
      return -1;
    if (getPage() > p_o.getPage())
      return 1;
    double diffY = Math.abs(getFy() - p_o.getFy());
    // solo se la diff top > 1. non e' la stessa riga
    if (diffY > 1.) {
      if (getFy() < p_o.getFy())
        return -1;
      if (getFy() > p_o.getFy())
        return 1;
    }
    if (getFx() < p_o.getFx())
      return -1;
    if (getFx() > p_o.getFx())
      return 1;
    return 0;
  }

  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    final String sep = ";";
    sb.append(id).append(sep);
    sb.append(page).append(sep);
    sb.append(Utils.formatDouble(fy)).append(sep);
    sb.append(Utils.formatDouble(fx)).append(sep);
    sb.append(top).append(sep);
    sb.append(left).append(sep);
    sb.append(tipoDato.getCod()).append(sep);
    if (null != txt)
      sb.append(txt.replace(sep, "|"));
    sb.append(sep);
    sb.append(Utils.formatDouble(timParse * 1000.));
    sb.append(sep);
    if (null != rigaHtml)
      sb.append(rigaHtml.trim().replace(sep, "|"));
    sb.append(sep);
    return sb.toString();
  }

  private String formatDbl(double dbl) {
    String szRet = s_dblFmt0.format(dbl);
    szRet = String.format("%5s", szRet);
    return szRet;
  }

  public boolean isConsecutivo(HtmlValue p_succ) {
    return isConsecutivo(p_succ, false);
  }

  /**
   * Verifico se e' un testo accodabile al precedente. Condizione "sin equa non"
   * che i pezzi siano ordinati in base al:
   * <ol>
   * <li>Page</li>
   * <li>top</li>
   * <li>left</li>
   * </ol>
   *
   * @param p_succ
   * @return
   */
  public boolean isConsecutivo_OLD(HtmlValue p_succ, boolean needText) {
    double diffY = Math.abs(top - p_succ.top);
    // double calcLenTx = fSiz * txt.length() / F_Correttivo;
    double calcLenTx = width / F_Correttivo;
    // double occupy = fx + txt.length() * calcLenTx;
    double rightMost = fx + calcLenTx;
    int nDiffX = (int) Math.abs(p_succ.fx - rightMost);
    // int fSize = (int) fontSize;
    //    if (diffX < 10.)
    // se "needtext" prec e succ *devono* essere stringhe
    if (needText)
      if ( ! (isText() && p_succ.isText()))
        return false;
    // prec e succ *devono* essere sulla stessa riga
    if (diffY >= 1)
      return false;
    // se !needText non interessa la consecutio asse X
    if ( !needText || nDiffX <= fontSize)
      return true;
    return false;
  }

  public boolean isSameRiga(HtmlValue p_succ) {
    if (null == p_succ)
      return true;
    double diffY = Math.abs(top - p_succ.top);
    // sono sulla stessa riga ?!?
    if (diffY >= 1)
      return false;
    return true;
  }

  public boolean isConsecutivo(HtmlValue p_succ, boolean forceText) {
    if ( (null == p_succ) || //
        !isSameRiga(p_succ) || //
        (forceText && ! (isText() && p_succ.isText())))
      return false;
    // calcolo il punto piu a destra del testo corrente
    double rightMost = fx + width;
    int nDiffX = (int) Math.abs(p_succ.fx - rightMost);
    return nDiffX <= fontSize;
  }

  public static void setCorrettivo(double p_v) {
    if (p_v >= 1. && p_v < 2.5)
      F_Correttivo = p_v;
  }

  public void append(HtmlValue p_next) {
    String otxt = txt;
    txt += " " + p_next.txt;
    String from = String.format(">%s</div", otxt);
    String totx = String.format(">%s</div", txt);
    // 6. e' il width del blank aggiunto
    width += p_next.width + fontSize; // eventuale blank
    // fSiz += p_next.fSiz + 6.;
    rigaHtml = rigaHtml.replace(from, totx);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    HtmlValue htRet = new HtmlValue();
    htRet.fx = fx;
    htRet.fy = fy;
    htRet.width = width;
    htRet.fontSize = fontSize;
    htRet.page = page;
    htRet.tipoDato = tipoDato;
    htRet.txt = txt;
    htRet.rigaHtml = rigaHtml;

    htRet.valData = valData;
    htRet.valDouble = valDouble;
    htRet.vMin = vMin;
    htRet.vMax = vMax;
    htRet.intero = intero;
    htRet.importo = importo;
    htRet.fattNo = fattNo;
    htRet.contatore = contatore;

    return htRet;
  }

  @Override
  public String toString() {
    String szIs = "txt";
    switch (tipoDato) {
      case Data:
        szIs = "Dta";
        break;
      case Barrato:
        szIs = "FatN";
        break;
      case Intero:
        szIs = "Int";
        break;
      case Float:
        szIs = "Rea";
        break;
      case Stringa:
        szIs = "txt";
        break;
      //      case Importo:
      //        szIs = "Imp";
      //        break;
      case IntN15:
        szIs = "n15";
        break;
      case Minus:
        szIs = "mns";
        break;
      case Aster:
        szIs = "ast";
        break;
      case Perc:
        szIs = "prc";
        break;
      case Less:
        szIs = "les";
        break;
      case MinMax:
        szIs = ETipiDato.MinMax.name();
        String sz = String.format("(%d,%s,%s)\t%s, %s\t%s=[%s - %s]", //
            getPage(), // ( %d
            formatDbl(getFy()), // ( %d, %s
            formatDbl(getFx()), // ( %d, %s, %s )
            formatDbl(getWidth()), // // ( %d, %s, %s )\t%d
            formatDbl(getLeft()), //
            szIs, //
            Utils.formatDouble(vMin), //
            Utils.formatDouble(vMax)); //
        return sz;

      default:
        szIs = null != tipoDato ? tipoDato.name() : ETipiDato.Stringa.name();
        break;
    }
    //    String sz = String.format("Top:%d(%d)\tleft:%d, %s=\"%s\"", //
    //        getTop(), getPage(), getLeft(), szIs, getTxt());
    String sz = String.format("(%d,%s,%s)\t %s\t%s=\"%s\"", //
        getPage(), //
        formatDbl(getFy()), //
        formatDbl(getFx()), //
        // getTop(), getLeft(), //
        formatDbl(width), //
        szIs, getTxt());
    return sz;
  }

  public void evidenzia() {
    if (tipoDato == ETipiDato.HTML)
      return;
    final String szDiv = "<div class=\"p\"";
    final String szEvid = "background-color: yellow; color: red;";
    int indx = rigaHtml.indexOf(szDiv);
    if (indx < 0)
      return;
    indx = rigaHtml.indexOf(szEvid);
    if (indx >= 0)
      return;
    indx = rigaHtml.indexOf("\">");
    if (indx < 0)
      return;
    StringBuilder sb = new StringBuilder();
    sb.append(rigaHtml.substring(0, indx)).append(szEvid).append(rigaHtml.substring(indx));
    rigaHtml = sb.toString();
  }
}

package sm.clagenna.stdcla.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.utils.Utils;

public class HtmlValue implements Comparable<HtmlValue>, Cloneable {

  private static final Logger s_log        = LogManager.getLogger(HtmlValue.class);
  private static int          lastId       = 0;
  public static final String  CSV_HEADER   = "sep=;\nid;page;py;px;ny;nx;typ;txt\n";
  private static double       F_Correttivo = 1.9;

  private int     id;
  private double  fx;
  private double  fy;
  private double  fSiz;
  private int     left;
  private int     top;
  private int     page;
  private String  txt;
  private String  rigaHtml;
  private boolean noSeq;

  private ETipiDato  m_ETipiDato;
  private Date       vData;
  private Double     vDbl;
  private Double     vMin;
  private Double     vMax;
  private Integer    vInt;
  private BigDecimal vImporto;
  private String     vFattNo;
  private String     vInt15;

  public static DateFormat           fmtData   = new SimpleDateFormat("dd/MM/yyyy");
  @SuppressWarnings("unused")
  private static final DecimalFormat s_dblFmt2 = new DecimalFormat("#,###.00");
  private static final DecimalFormat s_dblFmt0 = new DecimalFormat("#,###");

  private static Pattern patInt15   = Pattern.compile(ETipiDato.IntN15.getRegex());
  private static Pattern patBarrato = Pattern.compile(ETipiDato.Barrato.getRegex());
  private static Pattern patData    = Pattern.compile(ETipiDato.Data.getRegex());
  private static Pattern patReal    = Pattern.compile(ETipiDato.Float.getRegex());
  private static Pattern patRealUSA = Pattern.compile("(-{0,1}[\\d]*\\d+[\\.,]\\d+)");
  private static Pattern patImpor   = Pattern.compile(ETipiDato.Importo.getRegex());
  private static Pattern patNum     = Pattern.compile(ETipiDato.Intero.getRegex());
  private static Pattern patLess    = Pattern.compile(ETipiDato.Less.getRegex());
  private static Pattern patMinMax  = Pattern.compile(ETipiDato.MinMax.getRegex());

  // per suplire all'anno nel txt:  "Credito attuale anno 2022:"
  private static Pattern patNum2p = Pattern.compile("(\\d+):");

  public HtmlValue(double p_x, double p_y, double p_fsiz, int page, String txt, String szRiHtml) {
    setFx(p_x);
    setFy(p_y);
    setFSiz(p_fsiz);
    setPage(page);
    setTxt(txt);
    rigaHtml = szRiHtml.trim();
    calcola();
  }

  private void calcola() {
    int px = (int) Math.round(getFx() / Utils.DBL_XMAX * Utils.F_XCharMax);
    int py = (int) Math.round(getFy() / Utils.DBL_YMAX * Utils.F_YRigheMax);
    // salto alla pagina
    py += (int) ( (getPage() - 1) * Utils.F_YRigheMax);
    setLeft(px);
    setTop(py);
    id = HtmlValue.lastId++;
  }

  public int getId() {
    return id;
  }

  public boolean isNbsp() {
    return txt.indexOf("&nbsp;") >= 0;
  }

  public double getFx() {
    return fx;
  }

  public final void setFx(double p_fx) {
    fx = p_fx;
  }

  public double getFy() {
    return fy;
  }

  public final void setFy(double p_fy) {
    fy = p_fy;
  }

  public final void setFSiz(double p_v) {
    fSiz = p_v;
  }

  public int getLeft() {
    return left;
  }

  public void setLeft(int p_left) {
    left = p_left;
  }

  public int getTop() {
    return top;
  }

  public void setTop(int p_top) {
    top = p_top;
  }

  public int getPage() {
    return page;
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

  public String getRigaHtml() {
    return rigaHtml;
  }

  public void setNoSeq(boolean bv) {
    noSeq = bv;
  }

  public boolean isNoSeq() {
    return noSeq;
  }

  public ETipiDato getTipo() {
    return m_ETipiDato;
  }

  public void setTipo(ETipiDato et) {
    m_ETipiDato = et;
  }

  public String getFattNo() {
    return vFattNo;
  }

  public BigDecimal getImporto() {
    return vImporto;
  }

  public String getContatore() {
    return vInt15;
  }

  public void setTxt(String p_txt) {
    txt = p_txt;
    discerni();
  }

  protected void discerni() {
    m_ETipiDato = ETipiDato.Stringa;
    vData = null;
    vDbl = null;
    vMin = null;
    vMax = null;
    vInt = null;
    vImporto = null;
    vInt15 = null;
    vMin = null;
    vMax = null;
    String lTxt = txt;
    if (lTxt.matches("\\[.+\\]")) {
      lTxt = lTxt.replace("[", "");
      lTxt = lTxt.replace("]", "");
    }
    //    Pattern patMMx1 = Pattern.compile("([0-9]+[,\\.]*[0-9]*)[ \t]*([\\-<>]+)[ \\t]*([0-9]+[,\\\\.]*[0-9]*)");
    //    Pattern patMMx2 = Pattern.compile("([\\-<>]+)[ \\t]*([0-9]+[,\\\\.]*[0-9]*)");
    // --------- MinMax ----------------------
    Matcher mtch = patMinMax.matcher(lTxt);
    if (mtch.matches()) {
      m_ETipiDato = ETipiDato.MinMax;
      vMin = Utils.parseDouble(mtch.group(1));
      vMax = Utils.parseDouble(mtch.group(3));
      return;
    }
    // --------- Less ----------------------
    Matcher mtch2 = patLess.matcher(lTxt);
    if (mtch2.matches()) {
      m_ETipiDato = ETipiDato.MinMax;
      vMin = 0d;
      vMax = Utils.parseDouble(mtch2.group(2));
      return;
    }
    if (lTxt.length() == 1 && lTxt.equals("<")) {
      m_ETipiDato = ETipiDato.Less;
      vMin = 0.;
      vMax = 0.;
      return;
    }
    // ------------- DATA ------------------
    if (lTxt == null || lTxt.length() == 0)
      return;
    if (patData.matcher(lTxt).matches()) {
      try {
        vData = fmtData.parse(lTxt);
        m_ETipiDato = ETipiDato.Data;
      } catch (Exception e) {
        s_log.error("Parse data:" + txt, e);
      }
      return;
    }
    // -------------- IMPORTO ----------------
    if (patImpor.matcher(lTxt).matches()) {
      try {
        String szV = lTxt.replace(".", "");
        szV = szV.replace(',', '.');
        vImporto = new BigDecimal(Double.parseDouble(szV));
        vImporto = vImporto.setScale(2, RoundingMode.HALF_DOWN);
        m_ETipiDato = ETipiDato.Importo;
      } catch (NumberFormatException e) {
        // e.printStackTrace();
        s_log.error("Parse real:" + txt, e);
      }
      return;
    }
    // -------------- FLOAT ----------------
    if (patReal.matcher(lTxt).matches() || patRealUSA.matcher(lTxt).matches()) {
      try {
        vDbl = Double.parseDouble(lTxt.replace(',', '.'));
        m_ETipiDato = ETipiDato.Float;
      } catch (NumberFormatException e) {
        // e.printStackTrace();
        s_log.error("Parse real:" + txt, e);
      }
      return;
    }
    // ------------- CONTATORE (int15) -------------------
    if (patInt15.matcher(txt).matches()) {
      try {
        vInt15 = txt;
        m_ETipiDato = ETipiDato.IntN15;
      } catch (Exception e) {
        // e.printStackTrace();
        s_log.error("Parse Fatt. No:" + txt, e);
      }
      return;
    }
    // ------------- INTERO ---------------
    if (patNum.matcher(txt).matches()) {
      try {
        long ll = Long.MAX_VALUE;
        if (txt.length() <= 10)
          ll = Long.parseLong(txt.replace(".", ""));
        if (ll < Integer.MAX_VALUE) {
          vInt = (int) ll;
          m_ETipiDato = ETipiDato.Intero;
        }
      } catch (NumberFormatException e) {
        s_log.error("Parse number:" + txt, e);
      }
      return;
    }
    // ------------- INTERO con ':' ---------------
    if (patNum2p.matcher(txt).matches()) {
      try {
        long ll = Long.MAX_VALUE;
        String sza2p = txt.substring(0, txt.length() - 1);
        if (sza2p.length() <= 10)
          ll = Long.parseLong(sza2p);
        if (ll < Integer.MAX_VALUE) {
          vInt = (int) ll;
          m_ETipiDato = ETipiDato.Intero;
        }
      } catch (NumberFormatException e) {
        s_log.error("Parse number:" + txt, e);
      }
      return;
    }
    // ------------- Num Fattura (xxx/yyyyyy) -------------------
    if (patBarrato.matcher(txt).matches()) {
      try {
        vFattNo = txt;
        m_ETipiDato = ETipiDato.Barrato;
      } catch (Exception e) {
        s_log.error("Parse Fatt. No:" + txt, e);
      }
      return;
    }
  }

  public boolean isNumero() {
    return m_ETipiDato.isNumeric();
  }

  public boolean isIntero() {
    return m_ETipiDato == ETipiDato.Intero;
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
    return m_ETipiDato == ETipiDato.Float;
  }

  public boolean isData() {
    return m_ETipiDato == ETipiDato.Data;
  }

  public boolean isFattNo() {
    return m_ETipiDato == ETipiDato.Barrato;
  }

  public boolean isText() {
    return m_ETipiDato == ETipiDato.Stringa;
  }

  public Date getvData() {
    return vData;
  }

  /**
   * Cerco di tornare un valore double scelto tra i numerici. Questo è dovuto al
   * fatto che nelle fatture i valori numerici sono spesso <i>ballerini</i> tra
   * tipoligie diverse. Vedi la "quantita" nei consumi.
   *
   * @return double fra i campi numerici valorizzati
   */
  public Double getvDbl() {
    if (null != vDbl)
      return vDbl;
    if (null != vImporto)
      return vImporto.doubleValue();
    if (null != vInt)
      return Double.valueOf(vInt);
    return vDbl;
  }

  public Integer getvInt() {
    return vInt;
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

  @Override
  public String toString() {
    String szIs = "txt";
    switch (m_ETipiDato) {
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
      case Importo:
        szIs = "Imp";
        break;
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
        String sz = String.format("(%d,%s,%s)\t%d, %d\t%s=[%s - %s]", //
            getPage(), //
            formatDbl(getFy()), formatDbl(getFx()), //
            getTop(), getLeft(), szIs, //
            Utils.formatDouble(vMin), //
            Utils.formatDouble(vMax)); //
        return sz;

      default:
        break;
    }
    //    String sz = String.format("Top:%d(%d)\tleft:%d, %s=\"%s\"", //
    //        getTop(), getPage(), getLeft(), szIs, getTxt());
    String sz = String.format("(%d,%s,%s)\t%d, %d\t%s=\"%s\"", //
        getPage(), //
        formatDbl(getFy()), formatDbl(getFx()), //
        getTop(), getLeft(), szIs, getTxt());
    return sz;
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
    sb.append(m_ETipiDato.getCod()).append(sep);
    sb.append(txt.replace(sep, "|")).append(sep);
    return sb.toString();
  }

  private String formatDbl(double dbl) {
    String szRet = s_dblFmt0.format(dbl);
    szRet = String.format("%5s", szRet);
    return szRet;
  }

  /**
   * Verifico se e' un testo accodabile al precedente
   *
   * @param p_succ
   * @return
   */
  public boolean isConsecutivo_OLD(HtmlValue p_succ) {
    double diffX = Math.abs(p_succ.fx - fx);
    double diffY = Math.abs(top - p_succ.top);
    // double dimChMax = 10.; // c'era 7.5
    double dimCh = 999.9F;
    if (txt != null)
      dimCh = diffX / txt.length();
    if ( !isText() || !p_succ.isText() || diffY >= 1 || left > p_succ.left)
      return false;
    // if (dimCh > dimChMax)
    if (dimCh > fSiz)
      return false;
    // return true;
    return false;
  }

  /**
   * Verifico se e' un testo accodabile al precedente
   *
   * @param p_succ
   * @return
   */
  public boolean isConsecutivo(HtmlValue p_succ) {
    double diffY = Math.abs(top - p_succ.top);
    double calcLenTx = fSiz * txt.length() / F_Correttivo;
    double occupy = calcLenTx + fx;
    double diffX = Math.abs(p_succ.fx - occupy);
    //    if (diffX < 10.)
    if (isText() && p_succ.isText() && (diffX <= 10.))
      return true;
    if ( !isText() || !p_succ.isText() || (diffY >= 1))
      return false;
    return true;
  }

  public static void setCorrettivo(double p_v) {
    if (p_v >= 1. && p_v < 2.5)
      F_Correttivo = p_v;
  }

  public void append(HtmlValue p_rec) {
    String otxt = txt;
    txt += " " + p_rec.txt;
    String from = String.format(">%s</div", otxt);
    String totx = String.format(">%s</div", txt);
    rigaHtml = rigaHtml.replace(from, totx);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    HtmlValue htRet = new HtmlValue(fx, fy, fSiz, page, txt, getRigaHtml());
    return htRet;
  }
}

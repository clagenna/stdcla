package sm.clagenna.stdcla.pdf;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.sys.ex.ReadPDFValoreException;


public abstract class Valore {
  private static final Logger s_log = LogManager.getLogger(Valore.class);

  public static final String            NULL_VAL = "*---*";
  private static final SimpleDateFormat s_fmtDt  = new SimpleDateFormat("dd/MM/yyyy");

  protected String      m_fldNam;
  protected ETipiDato   m_tipoc;
  protected boolean     m_AllNumerics;
  protected boolean     m_isArray;
  private List<Object>  valore;
  private List<Boolean> stimati;

  public Valore() {
    //
  }

  public Valore(String p_fldNam, ETipiDato p_tipoc, boolean p_isArray) {
    m_fldNam = p_fldNam;
    m_tipoc = p_tipoc;
    m_isArray = p_isArray;
  }

  /**
   *
   * @param p_liCmp
   * @param p_k
   * @return
   */
  public abstract int estraiValori(List<HtmlValue> p_liCmp, int p_k) throws ReadPDFValoreException;

  public void assegna(ETipiDato p_tipoc, boolean p_bArray) {
    m_tipoc = p_tipoc;
    m_isArray = p_bArray;
  }

  public void assegnaValDaCampo(HtmlValue p_cmp) throws ReadPDFValoreException {
    assegnaValDaCampo(p_cmp, 0);
  }

  public void assegnaValDaCampo(HtmlValue p_cmp, int p_riga) throws ReadPDFValoreException {
    initValore(p_riga);
    if ( !m_isArray && !valore.get(0).getClass().getSimpleName().equals("Object")) {
      String sz = String.format("Doppia assegnazione campo %s", m_fldNam);
      throw new ReadPDFValoreException(sz);
    }
    Object vv = null;
    switch (m_tipoc) {
      case Data:
        vv = p_cmp.getvData();
        break;
      case Barrato:
        vv = p_cmp.getFattNo();
        break;
      case Intero:
        vv = p_cmp.getvInt();
        break;
      case Float:
        vv = p_cmp.getvDbl();
        break;
      case Stringa:
        vv = p_cmp.getTxt();
        break;
      case Importo:
        String sz = p_cmp.getTxt().replace(".", "").replace(",", ".");
        vv = new BigDecimal(sz);
        break;
      case IntN15:
        vv = p_cmp.getTxt();
        break;
      case Aster:
      case Perc:
      case Minus:
      case Less:
        vv = p_cmp.getTxt();
        break;
      case MinMax:
        vv = p_cmp.getMinMax();
        break;
      default:
        break;
    }
    if (vv == null) {
      String sz = String.format("Il valore del campo %s e' *NULL*", m_fldNam);
      throw new ReadPDFValoreException(sz);
    }
    valore.set(p_riga, vv);
    stimati.set(p_riga, Boolean.FALSE);
  }

  public void setStimato(int rig, boolean p_b) {
    stimati.set(rig, p_b);
  }

  private void initValore(int riga) {
    if (valore == null) {
      valore = new ArrayList<>();
      valore.add(new Object());
      stimati = new ArrayList<>();
      stimati.add(Boolean.FALSE);
    }
    if (m_isArray && riga >= valore.size()) {
      // se è un array inserisco Object come place holder
      for (int i = valore.size(); i <= riga; i++) {
        valore.add(new Object());
        stimati.add(Boolean.FALSE);
      }
    }
  }

  public void assegna(Valore p_val) {
    initValore(0);
    Object objVal = null;
    try {
      objVal = p_val.getValore();
    } catch (ReadPDFValoreException e) {
      // e.printStackTrace();
      s_log.error("Errore assegna valore \"{}\" in \"{}\"", p_val.toString(), this.toString());
      return;
    }
    if (isAssegnabile()) {
      if (m_isArray && objVal instanceof List<?>) {
        List<?> lio = (List<?>) objVal;
        int n = 0;
        for (Object ob : lio)
          valore.set(n++, ob);
      } else
        valore.set(0, objVal);
    } else {
      s_log.error("Non posso assegnare \"{}\" a \"{}\"", p_val.toString(), this.toString());
    }
  }

  public void setStimato(boolean bv) {

  }

  public boolean isArray() {
    return m_isArray;
  }

  /**
   * Verifica l'assegnabilita in base a
   * <ol>
   * <li>se valore contiene null</li>
   * <li>se array e list.size() == 0</li>
   * </ol>
   *
   * @return se posso assegnare un valore
   */
  public boolean isAssegnabile() {
    return valore == null || valore.size() == 0 || m_isArray;
  }

  public boolean isAssegnato() {
    return valore != null && valore.get(0) != null;
  }

  public Object getValore() throws ReadPDFValoreException {
    if (valore == null || valore.size() == 0) {
      String sz = String.format("campo %s non e' assegnato", m_fldNam);
      throw new ReadPDFValoreException(sz);
    }
    if (m_isArray)
      return valore;
    return valore.get(0);
  }

  public int size() {
    if ( !isArray() || null == valore)
      return 0;
    return valore.size();
  }

  public Object getValore(int rig) throws ReadPDFValoreException {
    if (valore == null || valore.size() == 0) {
      String sz = String.format("campo %s non e' assegnato", m_fldNam);
      throw new ReadPDFValoreException(sz);
    }
    if ( !m_isArray)
      throw new ReadPDFValoreException("Non e' un array");
    if (rig < 0 || rig >= valore.size())
      throw new ReadPDFValoreException("Index out of bounds");
    return valore.get(rig);
  }

  public boolean isStimato(int rig) {
    if ( !m_isArray || stimati == null)
      return false;
    if (rig < 0 || rig >= valore.size()) {
      s_log.error("isStimato index out of bounds : {}", rig);
      return false;
    }
    return stimati.get(rig);
  }

  public Object getValoreNoEx() {
    if (valore == null || valore.size() == 0)
      return null;
    return valore.get(0);
  }

  public Object getValoreNoEx(int rig) {
    if (valore == null || //
        valore.size() == 0 || //
        !m_isArray && rig > 0)
      return null;
    if (rig < 0 || rig >= valore.size())
      return null;
    return valore.get(rig);
  }

  public String getFieldName() {
    return m_fldNam;
  }

  public void setFieldName(String p_nam) throws ReadPDFValoreException {
    if (m_fldNam != null && !m_fldNam.equals(p_nam))
      throw new ReadPDFValoreException(String.format("Trying change name from \"%s\" with \"%s\"", m_fldNam, p_nam));
    m_fldNam = p_nam;
  }

  public ETipiDato getTipoDato() {
    return m_tipoc;
  }

  public void setAllNumerics(boolean bv) {
    m_AllNumerics = bv;
  }

  public boolean isAllNumerics() {
    return m_AllNumerics;
  }

  @Override
  public String toString() {
    String sz = String.format("%s%s[%s]\t=", m_fldNam, m_isArray ? "[]" : " ", m_tipoc);
    if (valore == null || valore.size() == 0) {
      sz += "*NULL*";
      return sz;
    }
    if (m_isArray) {
      sz += "\n\t\t[";
      String vir = "";
      int rig = 1;
      for (Object ob : valore) {
        String tosz = formattaObj(ob);
        sz += String.format("%s(%d)%s", vir, rig++, tosz);
        vir = "\n\t\t";
      }
      sz += "]";
      return sz;
    }
    try {
      Object ob = getValore();
      String tosz = formattaObj(ob);
      sz += tosz;
    } catch (ReadPDFValoreException e) {
      e.printStackTrace();
      s_log.error("toString err valore \"{}\" in \"{}\"", valore, e);
    }
    return sz;
  }

  public String toStringLess() {
    String sz = String.format("%s[%s]", m_fldNam, m_tipoc);
    return sz;
  }

  public String formattaObj(Object p_ob) {
    String ret = NULL_VAL;
    if (p_ob == null || p_ob.getClass().getSimpleName().equals("Object"))
      return ret;
    if (p_ob instanceof String sz) {
      ret = String.format("\"%s\"", sz);
    } else if (p_ob instanceof Date dt) {
      ret = s_fmtDt.format(dt);
    } else if (p_ob instanceof Double dbl) {
      ret = String.format("%.6f", dbl);
    } else if (p_ob instanceof BigDecimal cy) {
      ret = String.format("%.2f", cy.doubleValue());
    } else
      ret = p_ob.toString();
    return ret;
  }

  public boolean hasRiga(int p_r) {
    if (valore == null)
      return false;
    return p_r < valore.size();
  }

  // solo JDK 17+ con preview
  //  private String formattaObj2(Object p_ob) {
  //    String ret = "*null*";
  //    if (p_ob == null)
  //      return ret;
  //    switch (p_ob) {
  //      case String sz -> {
  //        ret = String.format("\"%s\"", sz);
  //      }
  //      case Date dt -> {
  //        ret = s_fmtDt.format(dt);
  //      }
  //      case Double dbl -> {
  //        ret = String.format("%.6f", dbl);
  //      }
  //      case BigDecimal cy) {
  //        ret = String.format("%.2f", cy.doubleValue());
  //      }
  //      default -> {
  //        ret = p_ob.toString();
  //      }
  //    }
  //    return ret;
  //  }
}

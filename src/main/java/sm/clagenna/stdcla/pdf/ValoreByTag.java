package sm.clagenna.stdcla.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.sys.ex.ReadPDFValoreException;

public class ValoreByTag extends Valore {

  private static final Logger s_log = LogManager.getLogger(ValoreByTag.class);
  private List<Civetta>       m_liCivetta;
  private int                 m_nExcRiga;
  private int                 m_nExcCol;

  public ValoreByTag() {
    super();
  }

  public ValoreByTag(String p_fldNam, String p_civetta, ETipiDato p_tipoc, boolean p_isArray) {
    super(p_fldNam, p_tipoc, p_isArray);
    setCivetta(p_civetta);
  }

  public void assegna(String p_szCivetta, ETipiDato p_tipoc, boolean p_bArray) {
    super.assegna(p_tipoc, p_bArray);
    setCivetta(p_szCivetta);
  }

  public boolean verificaCivetta(HtmlValue p_cmp) {
    boolean bRet = true;
    if (m_liCivetta == null)
      return bRet;
    for (Civetta civ : m_liCivetta) {
      bRet = civ.verificaCivetta(p_cmp);
      if (bRet)
        break;
    }
    return bRet;
  }

  /**
   * Cerco il valore in base al contenuto del campo <code>p_cmp</code>
   * {@link ETipiDato#Stringa} che
   * <ol>
   * <li><b>deve</b> corrispondere al valore di {@link #m_civetta}<br/>
   * </li>
   * <li>Se si, allora verifico che il successivo sia di tipo
   * {@link Valore#getTipoDato()}</li>
   * <li>Se sì allora assegno a valore il campo immediatamente successivo.</li>
   * </ol>
   * in pseudocode<br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;<code>if cmp[i] == civetta then valore=cmp[i+1]</code><br/>
   * Esempio: Nel caso che civetta sia "Data Scadenza", allora la seguente
   * sequenza è valida per l'assegnazione:
   *
   * <pre>
   * 29(1), 88   txt="Data Scadenza"
   * 29(1), 126  Dta="31/03/2022"
   * </pre>
   *
   * In {@link #getValore()} ritrovo il tipo {@link ETipiDato#Data} con
   * "<code>31/03/2022</code>"
   *
   * @param p_liCmp
   *          la lista di campi Tagged
   * @param p_k
   *          l'indice del elemento valore corrente in p_liCmp
   * @return 0 se non è trattato altrimenti (p_k + 2) per indicare che il campo
   *         è stato preso in carico come valore
   * @throws ReadPDFValoreException
   */
  @Override
  public int estraiValori(List<HtmlValue> p_liCmp, int p_k) throws ReadPDFValoreException {
    HtmlValue cmpVal = p_liCmp.get(p_k);
    if (cmpVal.getTipo() != m_tipoc) {
      s_log.debug("Non assegno a {} il tag {}", this.toString(), cmpVal.toString());
      return 0;
    }
    assegnaValDaCampo(cmpVal);
    return 1;
  }

  public boolean parseProp(String p_szTagVal) {
    String[] arr = p_szTagVal.split(":");
    if (arr == null || arr.length < 5) {
      s_log.error("pochi campi tag {}", p_szTagVal);
      return false;
    }
    m_fldNam = arr[0];
    setCivetta(arr[1]);
    m_tipoc = ETipiDato.decode(arr[2]);
    m_nExcCol = -1;
    m_nExcRiga = -1;
    if ( !arr[3].equals("-") && !arr[4].equals("-")) {
      m_nExcCol = arr[3].toLowerCase().charAt(0) - 'a';
      m_nExcRiga = Integer.parseInt(arr[4]) - 1;
    }
    if (arr.length >= 6) {
      String sz = arr[5].toLowerCase();
      switch (sz) {
        case "t":
        case "1":
          m_isArray = true;
          break;
      }
    }
    return true;
  }

  /**
   * il campo m_civetta è una o piu stringhe (separate da "pipe='|'") che
   * <b>deve</b> essere presente per riconoscere il tag.<br/>
   * Se la singola stringa e' racchiusa in apici singoli "'" allora diventa un
   * match esatto nella {@link #verificaCivetta(HtmlValue)}
   *
   * @param p_sz
   */
  private void setCivetta(String p_sz) {
    if (p_sz == null || p_sz.equals("*"))
      return;
    if (m_liCivetta == null)
      m_liCivetta = new ArrayList<>();
    String arr[] = p_sz.split("\\|");
    for (String sz : arr) {
      Civetta civ = new Civetta(sz);
      m_liCivetta.add(civ);
    }
  }

  public int getExcRiga() {
    return m_nExcRiga;
  }

  public int getExcCol() {
    return m_nExcCol;
  }

  public void setExcelCoord(int p_nExcCol, int p_nExcRiga) {
    m_nExcCol = p_nExcCol;
    m_nExcRiga = p_nExcRiga;
  }

  @Override
  public String toString() {
    var sz2 = super.toString();
    if (hasCivetta()) {
      String sz = m_liCivetta //
          .stream() //
          .map(s -> s.getCivetta()) //
          .collect(Collectors.joining(";"));
      sz2 = String.format("%s\t<--(\"%s\")", sz2, sz);
    }
    return sz2;
  }

  public boolean hasCivetta() {
    return m_liCivetta != null;
  }
}

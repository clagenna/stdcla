package sm.clagenna.stdcla.pdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sys.ex.ReadPDFValoreException;

public class ValoreBySeq {
  private static final Logger s_log          = LogManager.getLogger(ValoreBySeq.class);
  private static boolean      s_debEstraiVal = false;

  private List<ValoreByTag> m_liSeq;
  @Getter
  private RigaHolder        rigaHolder;
  @Getter @Setter
  private int               numSeq;
  private TagValFactory     m_tagFact;

  public ValoreBySeq() {
    //
  }

  public ValoreBySeq(String p_fldNam, ETipiDato p_tipoc, boolean p_isArray) {
    addSeq(p_fldNam, p_tipoc, p_isArray);
  }

  /**
   * Verifica che il tipo del campo p_cmp è uguale al tipo del primo elemento
   * della sequenza
   *
   * @param p_cmp
   *          campo tagged fornito come partenza della sequenza
   * @return true se combacia la tipologia
   */
  public boolean goodStart(HtmlValue p_cmp) {
    ValoreByTag primo = m_liSeq.get(0);
    boolean bRet = p_cmp.getTipo().isCompatible(primo.getTipoDato());
    if (bRet) {
      // il tipo combacia, vediamo se anche il campo civetta
      if (primo.hasCivetta())
        bRet = primo.verificaCivetta(p_cmp);
    }
    return bRet;
  }

  /**
   * Aggiunge elemento di sequenza<br/>
   * Abbiamo almeno 5 elementi per ogni membro di sequenza<br/>
   * <ol>
   * <li>Il nome campo definito con {@link ValoreByTag}</li>
   * <li>eventuale campo civetta (opzionale)</li>
   * <li>Tipo di dato aspettato (vedi {@link ETipiDato})</li>
   * <li>riga posizione in Excel</li>
   * <li>Colonna in Excel</li>
   * </ol>
   *
   * @param p_szProp
   * @param p_nSeq
   * @return
   */
  public boolean addSeq(String p_szProp, int p_nSeq) {
    String[] arr = p_szProp.split(":");
    if (arr == null || arr.length < 4) {
      s_log.error("pochi campi tag {}", p_szProp);
      return false;
    }
    String szNam = arr[0];
    String szCivetta = arr[1];
    boolean bAllNumerics = "N".equalsIgnoreCase(arr[2]);
    ETipiDato tipoc = ETipiDato.decode(arr[2]);
    if (null == tipoc && bAllNumerics)
      tipoc = ETipiDato.Float;
    int nExcCol = -1;
    int nExcRiga = -1;
    if ( !arr[3].equals("-") && !arr[4].equals("-")) {
      nExcCol = arr[3].toLowerCase().charAt(0) - 'a';
      nExcRiga = Integer.parseInt(arr[4]) - 1;
    }
    boolean bArray = false;
    if (arr.length >= 6) {
      String sz = arr[5].toLowerCase();
      switch (sz) {
        case "t":
        case "1":
          bArray = true;
          break;
      }
    }
    if (m_liSeq == null)
      m_liSeq = new ArrayList<>();
    ValoreByTag cmp = m_tagFact.creaValTag(szNam); // new ValoreByTag(szNam, szCivetta, tipoc, bArray);
    cmp.assegna(szCivetta, tipoc, bArray);
    cmp.setExcelCoord(nExcCol, nExcRiga);
    cmp.setAllNumerics(bAllNumerics);
    setNumSeq(p_nSeq);
    m_liSeq.add(cmp);
    return true;
  }

  public void addSeq(String p_fldNam, ETipiDato p_tipoc, boolean b_arr) {
    if (m_liSeq == null)
      m_liSeq = new ArrayList<>();
    ValoreByTag cmp = m_tagFact.creaValTag(p_fldNam); // new ValoreByTag(p_fldNam, "*", p_tipoc, b_arr);
    cmp.assegna("*", p_tipoc, b_arr);
    m_liSeq.add(cmp);
  }

  public int estraiValori(List<HtmlValue> p_liCmp, int p_k) {
    int j = 0;
    HtmlValue htmlV;
    // verifico che la la tipologia sequenza combaci con tipolog tags
    HtmlValue htmlFirst = p_liCmp.get(p_k);
    boolean bObbWord = m_tagFact.isObbWord(htmlFirst.getTxt());
    for (ValoreByTag tg : m_liSeq) {
      int indx = p_k + j++;
      if (indx >= p_liCmp.size())
        return 0;
      htmlV = p_liCmp.get(indx);
      ETipiDato seqTip = tg.getTipoDato();
      ETipiDato tgvTip = htmlV.getTipo();
      if (s_debEstraiVal)
        debugEstrVal("?", p_liCmp, p_k, j - 1);
      // if ( ! (seqTip.isCompatible(tgvTip) && tg.verificaCivetta(tgv))) {
      if (j < 4 || !bObbWord) {
        if ( !seqTip.isCompatible(tgvTip)) {
          s_log.trace("Seq({})[{}]:\"{}\"({}) <> Tgv:\"{}\"({})", //
              getNumSeq(), //
              j - 1, //
              tg.getFieldName(), //
              tg.getTipoDato(), //
              htmlV.getTxt(), //
              htmlV.getTipo()); //
          return 0;
        }
      }
      String tx = htmlV.getTxt();
      ETipiDato etp = tg.getTipoDato();
      switch (etp) {
        case Minus:
        case Perc:
        case Aster:
        case Less:
          if ( !etp.isGoodChar(tx))
            return 0;
          break;
        default:
          break;
      }
    }
    if (s_debEstraiVal)
      debugEstrVal("!", p_liCmp, p_k, j - 1);
    // tutta la sequenza combacia per tipologia
    j = 0;
    for (ValoreByTag tg : m_liSeq) {
      htmlV = p_liCmp.get(p_k + j++);
      try {
        tg.assegnaValDaCampo(htmlV, rigaHolder.getRiga());
        // System.out.println("ValoreBySeq.estraiValori()=" + this.toString());
      } catch (ReadPDFValoreException e) {
        s_log.error("Errore assegna seq:{} = {}", tg.getFieldName(), htmlV.toString());
      }
    }
    return j;
  }

  private void debugEstrVal(String boh, List<HtmlValue> p_liCmp, int p_k, int i) {
    StringBuilder sb = new StringBuilder();
    int prel = 1;
    int inik = p_k - prel;
    if (inik < 0) {
      prel = 0;
      inik = p_k;
    }
    int qtaTok = m_liSeq.size();
    String sz = null;
    // emetto TGV : da -1 ... a qtaSeq.size + 1
    for (int j = 0; j < qtaTok + 2; j++) {
      if (inik >= p_liCmp.size())
        break;
      HtmlValue tgv = p_liCmp.get(inik++);
      sz = tgv.getTxt();
      if (sz.length() > 17)
        sz = sz.substring(0, 17) + "...";
      sz = String.format("%-20s|", sz);
      sb.append(sz);
    }
    sb.append("\n");
    inik = 0;
    // emetto SEQ
    if (prel > 0)
      sb.append(" ".repeat(21));
    for (ValoreByTag tg : m_liSeq) {
      sz = String.format("%-20s|", tg.toStringLess());
      sb.append(sz);
    }
    sb.append("\n");
    int qta = 21 * (i + prel) + 5;
    sb.append(String.format("%sseq:%02d", boh, numSeq)).append(" ".repeat(qta)).append("--^\n");
    System.out.println(sb.toString());
  }

  /**
   * Imposta il flag di <code>stimato</code> su tutti i <b>valori</b> della
   * sequenza alla riga specificata dal {@link RigaHolder}
   *
   * @param p_b
   *          il valore di "Stimato"
   */
  public void setStimato(boolean p_b) {
    int nRig = rigaHolder.getRiga();
    for (ValoreByTag tg : m_liSeq) {
      tg.setStimato(nRig, p_b);
    }
  }

  @Override
  public String toString() {
    String sz = String.format("\nSeq(%d) {%s}\n\t", getNumSeq(), rigaHolder != null ? rigaHolder.toString() : "*noRigH*");
    String vir = "";
    if (m_liSeq == null || m_liSeq.size() == 0) {
      sz += "**NULL**";
      return sz;
    }
    int k = 0;
    for (ValoreByTag p : m_liSeq) {
      sz += vir; // + p.getTipoDato();
      //      Object val = p.getValoreNoEx();
      //      String szV = (val != null && !val.getClass().getSimpleName().equals("Object")) ? p.toString() : "*null*";
      //      sz += String.format("(%d){%s}", k++, szV);
      sz += String.format("(%d){%s\n\t}", k++, p.toString());
      vir = "\n\t";
    }
    return sz;
  }

  public Collection<? extends ValoreByTag> allVals() {
    return m_liSeq;
  }

  public int size() {
    if (m_liSeq == null)
      return 0;
    return m_liSeq.size();
  }

  public void addRiga() {
    if (rigaHolder != null)
      rigaHolder.addRiga();
    else
      s_log.error("La seq {} non ha riga holder!", numSeq);
    // System.out.println("ValoreBySeq.addRiga()" + this.toString());
  }

  public void setRigaHolder(RigaHolder p_rh) {
    rigaHolder = p_rh;
    p_rh.addSeq(this);
  }

  public void setTagValFactory(TagValFactory p_Fact) {
    m_tagFact = p_Fact;
  }

  public Object getValoreTag(int p_i) {
    ValoreByTag ret = null;
    if (m_liSeq == null || m_liSeq.size() <= p_i)
      return ret;
    return m_liSeq.get(p_i);
  }
}

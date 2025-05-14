package sm.clagenna.stdcla.pdf;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.sys.ex.ReadPDFValoreException;
import sm.clagenna.stdcla.utils.AppProperties;

public class TagValFactory {
  private static final Logger      s_log = LogManager.getLogger(TagValFactory.class);
  private Map<String, ValoreByTag> m_map;
  private List<String>             obblWords;

  public TagValFactory() {
    init();
  }

  private void init() {
    //    obblWords = new ArrayList<String>();
    //    obblWords.add("emoglobina");
    //    obblWords.add("colesterolo");
    //    obblWords.add("psa");
    //    obblWords.add("vitamina");
  }

  public void setProperties(AppProperties p_p) {
    String obbw = p_p.getProperty("ObblWords");
    if (null == obbw)
      return;
    obblWords = Arrays.asList(obbw.split(","));
  }

  public ValoreByTag creaValTag(String p_nam) {
    ValoreByTag ret = null;
    if (p_nam == null) {
      s_log.error("Unnamed Tag ?!?");
      return ret;
    }
    if (m_map == null) {
      // @see https://www.baeldung.com/java-map-with-case-insensitive-keys
      m_map = new TreeMap<String, ValoreByTag>(String.CASE_INSENSITIVE_ORDER);
    }
    ret = m_map.get(p_nam);
    if (ret == null) {
      try {
        ret = new ValoreByTag();
        ret.setFieldName(p_nam);
        m_map.put(p_nam, ret);
      } catch (ReadPDFValoreException e) {
        s_log.error("Errore creaValTag(\"{}\"", p_nam, e);
      }
    } else
      s_log.debug("Il tag \"{}\" esiste gia' vedi:{} !", p_nam, ret.toString());
    return ret;
  }

  public ValoreByTag get(String p_nam) {
    ValoreByTag ret = m_map.get(p_nam);
    if (ret == null)
      s_log.error("Il tag \"{}\" non e' stato creato !", p_nam);
    return ret;
  }

  public Date getDate(String p_nam) {
    Date ret = null;
    ValoreByTag tgv = m_map.get(p_nam);
    try {
      if (null != tgv)
        ret = (Date) tgv.getValore();
    } catch (ReadPDFValoreException e) {
      s_log.error("Il tag \"{}\" e' errato ! err={}", p_nam, e.getMessage());
    }
    return ret;
  }

  public Date getDate(String p_nam, int riga) {
    Date ret = null;
    ValoreByTag tgv = m_map.get(p_nam);
    try {
      if (null != tgv)
        ret = (Date) tgv.getValore(riga);
    } catch (ReadPDFValoreException e) {
      s_log.error("Il tag \"{}\" e' errato ! err={}", p_nam, e.getMessage());
    }
    return ret;
  }

  public List<String> getAllTagsNames() {
    List<String> li = null;
    if (m_map == null)
      return li;
    li = m_map.keySet().stream().collect(Collectors.toList());
    return li;
  }

  public int getSize() {
    if (null == m_map)
      return 0;
    int qta = 1;
    for (ValoreByTag vv : m_map.values()) {
      if (vv.m_isArray) {
        int ii = vv.size();
        if (ii > qta)
          qta = ii;
      }
    }
    return qta;
  }

  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append("sep=;\n");
    if (null == m_map || m_map.size() == 0) {
      sb.append("null\n");
      return sb.toString();
    }
    List<String> lina = getAllTagsNames();
    // creo l'header del CSV
    int k = 0;
    for (String key : lina) {
      sb.append(key);
      if (++k < lina.size())
        sb.append(";");
    }
    sb.append("\n");
    // calcolo quante righe devo emettere in out: iMaxRow
    int iMaxRow = -1;
    int qtaf = 0;
    for (String key : lina) {
      qtaf++;
      ValoreByTag vt = m_map.get(key);
      int n = vt.size();
      iMaxRow = iMaxRow >= n ? iMaxRow : n;
    }
    // genero le righe
    for (int rig = 0; rig < iMaxRow; rig++) {
      k = 0;
      for (String key : lina) {
        ValoreByTag vt = m_map.get(key);
        String szOut = "";
        if (vt.hasRiga(rig))
          szOut = vt.formattaObj(vt.getValoreNoEx(rig));
        if ( !Valore.NULL_VAL.equals(szOut))
          sb.append(szOut);
        if (++k < qtaf)
          sb.append(";");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public boolean isObbWord(String psz) {
    if (null == psz)
      return false;
    String lsz = psz.toLowerCase();
    boolean bret = false;
    for (String ob : obblWords) {
      if (lsz.contains(ob)) {
        bret = true;
        break;
      }
    }
    return bret;
  }

  @Override
  public String toString() {
    return toCsv();
  }
}

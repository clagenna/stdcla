package sm.clagenna.stdcla.pdf;

/**
 * Classe ausiliaria per verificare la presenza di parole/frasi all'interno di
 * un {@link HtmlValue}. Se la parola o frase e' contorniata da apice singolo
 * ( &apos; ) allora il match sara' solo exact (vedi
 * {@link #verificaCivetta(HtmlValue)}
 */
public class Civetta {
  private String  m_civetta;
  private boolean m_exact;

  public Civetta(String p_sz) {
    setCivetta(p_sz);
  }

  /**
   * Controlla che il {@link HtmlValue} passato contenga il valore della civetta 
   * @param p_cmp TaggedValue
   * @return true se la civetta e' inclusa nel {@link HtmlValue}
   */
  public boolean verificaCivetta(HtmlValue p_cmp) {
    if (m_civetta == null || m_civetta.equals("*"))
      return false;
    if (m_exact)
      return p_cmp.getTxt().equals(m_civetta);
    return p_cmp.getTxt().toLowerCase().indexOf(m_civetta) >= 0;
  }

  public void setCivetta(String p_sz) {
    m_exact = false;
    m_civetta = p_sz.toLowerCase();
    if (p_sz.trim().startsWith("'")) {
      m_exact = true;
      m_civetta = p_sz.replace("'", "");
    }
  }

  public String getCivetta() {
    return m_civetta;
  }
  
  public String toString() {
    String szEx = m_exact ? "'" : "";
    return String.format("%s%s%s", szEx,m_civetta,szEx); 
  }

}

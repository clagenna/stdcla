package sm.clagenna.stdcla.enums;

public enum ETipoPDF {

  GAS("GAS", "Servizio Gas Naturale"), //
  EnergiaElettrica("EE", "Servizio Energia Elettrica"), //
  Acqua("H2O", "Servizio Idrico Integrato"), //
  Analisi("SANG", "Medicina Trasfusionale");

  private String titolo;
  private String identificativo;

  private ETipoPDF(String tit, String ident) {
    titolo = tit;
    identificativo = ident;
  }

  public String getTitolo() {
    return titolo;
  }

  public String getIdentificativo() {
    return identificativo;
  }

  public static ETipoPDF parse(String p_sz) {
    ETipoPDF ret = null;
    if (p_sz == null || p_sz.length() < 3)
      return ret;
    for (ETipoPDF t : ETipoPDF.values()) {
      if (t.titolo.equals(p_sz)) {
        ret = t;
        break;
      }
    }
    return ret;
  }
}

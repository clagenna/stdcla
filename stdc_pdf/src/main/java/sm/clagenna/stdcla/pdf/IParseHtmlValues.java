package sm.clagenna.stdcla.pdf;

public interface IParseHtmlValues {

  void setDebug(boolean bv);

  int parse(FromPdf2Html pdf2html);

  boolean isMyToken(ETipiDato... tp);

  boolean isMyToken(String sz);

  boolean isThatText(String str);

  HtmlValue nextToken();

}

package sm.clagenna.stdcla.pdf;

import java.util.List;

public interface IParseHtmlValues {

  void setDebug(boolean bv);

  int parse(List<HtmlValue> p_vals);
}

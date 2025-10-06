package sm.clagenna.stdcla.pdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.utils.Utils;


public class TextPrint {

  private static final Logger s_log = LogManager.getLogger(TextPrint.class);
  private int                 m_nQtaPages;

  private List<String>        m_a4;

  public TextPrint() {
    init(true, 1);

  }

  public TextPrint(boolean p_bPat, int p_pages) {
    init(p_bPat, p_pages);
  }

  private void init(boolean bMark, int p_pages) {
    m_a4 = new ArrayList<>();
    m_nQtaPages = p_pages;
    StringBuilder sb = new StringBuilder();
    if (bMark)
      for (int i = 1; i < (Utils.F_XCharMax / 10); i++)
        sb.append("----+----").append(String.valueOf(i % 10));
    else {
      char[] arr = new char[10];
      Arrays.fill(arr, ' ');
      sb.append(arr);
    }
    for (int pg = 0; pg < m_nQtaPages; pg++)
      for (int i = 0; i < Utils.F_YRigheMax; i++)
        m_a4.add(sb.toString());
  }

  public void scrivi(HtmlValue p_cm) {
    //    int px = (int) (p_cm.left() / DBL_XMAX * F_XCharMax);
    //    int py = (int) (p_cm.top() / DBL_YMAX * F_YRigheMax);
    //    // salto alla pagina
    //    py += (p_cm.page() - 1) * F_YRigheMax;
    //    if (m_a4.size() <= py) {
    //      System.err.println("Poche Pagine");
    //    }
    if (p_cm.getTop() >= m_a4.size()) {
      s_log.error("La tabella A4 ha solo {} righe, indice fuori dal range: indx={}", m_a4.size(), p_cm.getTop() + 1);
      throw new IndexOutOfBoundsException(p_cm.getTop());
    }
    String riga = m_a4.get(p_cm.getTop());
    int lMax = (p_cm.getLeft() + p_cm.getTxt().length());
    if (riga.length() < lMax) {
      int n = lMax - riga.length();
      char[] arr = new char[n];
      Arrays.fill(arr, ' ');
      riga = riga + String.valueOf(arr);
    }
    String sz2 = riga.substring(0, p_cm.getLeft()) + p_cm.getTxt() + riga.substring(p_cm.getLeft() + p_cm.getTxt().length());
    m_a4.set(p_cm.getTop(), sz2);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int k = 1;
    for (String sz : m_a4)
      sb.append(String.format("%2s)", k++)).append(sz).append('\n');
    return sb.toString();
  }
}

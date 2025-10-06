package sm.clagenna.stdcla.pdf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;

import lombok.Getter;

import sm.clagenna.stdcla.utils.Utils;

public class FromPdf2Html {
  private static final Logger s_log = LogManager.getLogger(FromPdf2Html.class);

  // <div class="p" id="p0" style="top:60.508736pt;left:292.5pt;line-height:10.02375pt;font-family:Verdana;font-size:8.25pt;font-weight:bold;width:33.88269pt;">Fattura</div>
  private static String       CSZ_PAT2       = ".*<div .* style=\"top:(\\d+\\.\\d+)pt;left:(\\d+\\.\\d+)pt;.*;width:(\\d+\\.\\d+)pt;\">(.*)</div>";
  private static final String CSZ_EVID_NOSEQ = ";background-color: #f02020;color: yellow;\">";

  private List<String>            m_outHtml;
  private int                     m_nPage;
  private Map<Integer, HtmlValue> m_map;
  private HtmlValue               m_lastCp;
  @Getter
  private boolean                 joinText;

  public FromPdf2Html() {
    //
  }

  public boolean convertiPDF(Path p_fiPdf) {
    s_log.info("Parse del file \"{}\"", p_fiPdf.toString());
    return (convToHtml(p_fiPdf) && scanRigheHTML());
  }

  public Map<Integer, HtmlValue> getMap() {
    return m_map;
  }

  /**
   * Prende l'elenco dei HtmlValue e li sorta in base alla loro pag, py, px
   *
   * @return
   */
  public List<HtmlValue> getListCampi(boolean joinT) {
    if (null == m_map)
      return null;
    List<HtmlValue> li = new ArrayList<HtmlValue>(m_map.values());
    Collections.sort(li);
    if (joinT)
      li = unifyAdiacentText(li);
    return li;
  }

  private List<HtmlValue> unifyAdiacentText(List<HtmlValue> li) {
    // List<HtmlValue> li = getListCampi();
    List<HtmlValue> liRet = new ArrayList<HtmlValue>();
    int iMax = li.size() - 3;
    HtmlValue precHtml = null;
    boolean bPrecToAdd = false;
    for (int i = 0; i < iMax; i++) {
      HtmlValue html = li.get(i);
      //  prec null
      if (null == precHtml) {
        if (html.isText())
          precHtml = html;
        liRet.add(html);
        bPrecToAdd = false;
        continue;
      }
      // prec.text  curr.Num
      if ( !html.isText()) {
        if (bPrecToAdd)
          liRet.add(precHtml);
        liRet.add(html);
        precHtml = null;
        bPrecToAdd = false;
        continue;
      }
      // prec.text curr.text
      if (html.isConsecutivo(precHtml)) {
        precHtml.append(html);
        bPrecToAdd = true;
      } else {
        if (bPrecToAdd)
          liRet.add(precHtml);
        precHtml = html;
        bPrecToAdd = true;
      }
    }
    if (null != precHtml && bPrecToAdd)
      liRet.add(precHtml);
    return liRet;
  }

  /**
   * Con questa cerco di riunire il range che hanno forma <code>"10 - 40"</code>
   * oppure <code>"[ 10 -40 ]"</code>
   *
   * @return
   */
  public List<HtmlValue> unifyByRanges() {
    List<HtmlValue> li = getListCampi(false);
    List<HtmlValue> liRet = new ArrayList<HtmlValue>();
    int iMax = li.size() - 3;
    for (int i = 0; i < iMax; i++) {
      HtmlValue html0 = li.get(i);
      HtmlValue html1 = li.get(i + 1);
      HtmlValue html2 = li.get(i + 2);
      try {
        if (html0.isNumero() && html1.isHiphen() && html2.isNumero()) {
          String szTxt = String.format("%s - %s", Utils.formatDouble(html0.getvDbl()), Utils.formatDouble(html2.getvDbl()));
          HtmlValue loc = (HtmlValue) html0.clone();
          loc.setTxt(szTxt);
          liRet.add(loc);
          i += 2;
          continue;
        }
        if (html0.isLessOrBig() && html1.isNumero()) {
          String szTxt = String.format(" < %s", Utils.formatDouble(html0.getvDbl()), Utils.formatDouble(html2.getvDbl()));
          HtmlValue loc = (HtmlValue) html0.clone();
          loc.setTxt(szTxt);
          loc.setTipo(ETipiDato.MinMax);
          liRet.add(loc);
          i += 1;
          continue;
        }
        if (html0.isLessOrBig()) {
          String lsz = html0.getTxt().trim().replace("<", "").replace(">", "");
          Double dbl = Utils.parseDouble(lsz);
          String szTxt = String.format(" < %s", Utils.formatDouble(dbl));
          HtmlValue loc = (HtmlValue) html0.clone();
          loc.setTxt(szTxt);
          loc.setTipo(ETipiDato.MinMax);
          liRet.add(loc);
          continue;
        }
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
      liRet.add(html0);
    }
    liRet.add(li.get(iMax));
    liRet.add(li.get(iMax + 1));
    liRet.add(li.get(iMax + 2));
    m_map.clear();
    m_map = null;
    m_map = new TreeMap<>();
    for (HtmlValue h : liRet) {
      m_map.put(h.getId(), h);
    }
    return liRet;
  }

  /**
   * Converte il file PDF in formato HTML dove sono presenti i tags da
   * interpretare
   *
   * <pre>
   *   <div class="page" id="page_0" style=
  "width:595.0pt;height:841.0pt;overflow:hidden;">
   *     <div class="r" style=
  "left:292.25pt;top:299.75pt;width:276.15002pt;height:0.0pt;border-bottom:1.5pt solid #0057af;">&nbsp;</div>
   * </pre>
   *
   * @param p_fiPdf
   * @return
   */
  private boolean convToHtml(Path p_fiPdf) {
    PDDocument pdf;
    try {
      pdf = PDDocument.load(p_fiPdf.toFile());
    } catch (IOException e) {
      s_log.error("Errore \"{}\" in lettura file PDF: {}", e.getMessage(), p_fiPdf.toString());
      return false;
    }
    StringWriter swr = new StringWriter();
    //    Writer output = new PrintWriter(szFiHtml, "utf-8");
    try {
      new PDFDomTree().writeText(pdf, swr);
      swr.close();
      pdf.close();
    } catch (IOException e) {
      s_log.error("Errore \"{}\" Scrittura HTML", e.getMessage());
      return false;
    }
    m_outHtml = new ArrayList<>();
    String[] arr = swr.toString().split("\n");
    m_outHtml.addAll(Arrays.asList(arr));
    return true;
  }

  private boolean scanRigheHTML() {
    m_nPage = 0;
    Pattern pat2 = Pattern.compile(CSZ_PAT2);
    String szLeft = null, szTop = null, szWidth = null, szText = null;
    // m_liCampi = new ArrayList<>();
    m_map = new TreeMap<>();
    for (String szRigaHtml : m_outHtml) {
      if (szRigaHtml.indexOf(">&nbsp;</div>") >= 0)
        continue;
      if (szRigaHtml.indexOf("class=\"page\"") >= 0) {
        m_nPage++;
        continue;
      }
      if (szRigaHtml.indexOf("div class=\"p\"") < 0)
        continue;
      Matcher mtch = pat2.matcher(szRigaHtml);
      // se non ho top e left la scarto
      if ( !mtch.find())
        continue;
      int k = 1;
      szTop = mtch.group(k++);
      szLeft = mtch.group(k++);
      szWidth = mtch.group(k++);
      szText = mtch.group(k++);
      if (null != szText && szText.contains("&"))
        szText = org.apache.commons.text.StringEscapeUtils.unescapeHtml4(szText);
      if (null != szText && szText.contains("["))
        szText = szText.replace("[", "");
      if (null != szText && szText.contains("]"))
        szText = szText.replace("]", "");
      trattaRiga(szLeft, szTop, szWidth, szText, szRigaHtml);
    }
    if (m_map.size() < 5) {
      s_log.error("Non sembra essere una fattura");
      return false;
    }
    // Collections.sort(m_liCampi);
    return true;
  }

  @SuppressWarnings("unused")
  public void trattaRiga(String szLeft, String szTop, String szWidth, String szTxt, String szRigaHtml) {
    String szDebug = null;
    if (szDebug != null && szTxt.toLowerCase().contains(szDebug))
      System.out.printf("FromHtml.trattaRiga(\"%s\")=%s\n", szDebug, szTxt);

    if (szTxt != null && szTxt.indexOf("nbsp;") >= 0)
      return;
    //    if (m_liCampi == null)
    //      m_liCampi = new ArrayList<>();
    if (null == m_map)
      m_map = new TreeMap<>();
    double nLeft = Double.parseDouble(szLeft);
    double nTop = Double.parseDouble(szTop);
    double nWidth = Double.parseDouble(szWidth);
    // double nCharSize = 6.0;
    if (m_nPage <= 0)
      s_log.error("Pagina fuori range:{} su tag {}", m_nPage, szTxt);
    HtmlValue rec = new HtmlValue(nLeft, nTop, nWidth, m_nPage, szTxt, szRigaHtml);
    if (m_lastCp != null && m_lastCp.isConsecutivo(rec))
      m_lastCp.append(rec);
    else {
      // m_liCampi.add(rec);
      m_map.put(rec.getId(), rec);
      m_lastCp = rec;
    }
  }

  public String getTextTAGs() {
    String sz = "**NULL**";
    //    if (m_liCampi == null || m_liCampi.size() == 0)
    //      return sz;
    if (null == m_map || m_map.size() == 0)
      return sz;
    //    sz = m_liCampi //
    //        .stream() //
    //        .map(t -> t.toString()) //
    //        .collect(Collectors.joining("\n"));
    sz = getListCampi(isJoinText()).stream() //
        .map(t -> t.toString()) //
        .collect(Collectors.joining("\n"));
    return sz;
  }

  public String getCSVTAGs() {
    String sz = "sep=;";
    //    if (m_liCampi == null || m_liCampi.size() == 0)
    //      return sz;
    if (null == m_map || m_map.size() == 0)
      return sz;
    //    sz = m_liCampi //
    //        .stream() //
    //        .map(t -> t.toString()) //
    //        .collect(Collectors.joining("\n"));
    sz = getListCampi(isJoinText()) //
        .stream() //
        .sorted() //
        .map(t -> t.toCsv()) //
        .collect(Collectors.joining("\n"));
    return HtmlValue.CSV_HEADER + sz;
  }

  public void saveTagFile(String p_tagFile) {
    String sz = getTextTAGs();
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_tagFile))) {
      bw.write(sz);
      s_log.info("Scritto TAGs file {}", p_tagFile);
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  public void saveCSVFile(String p_csvFile) {
    String sz = getCSVTAGs();
    try {
      Files.deleteIfExists(Paths.get(p_csvFile));
    } catch (IOException e) {
      s_log.error("Error {} on delete {}", e.getMessage(), p_csvFile);
      return;
    }
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_csvFile))) {
      bw.write(sz);
      s_log.info("Scritto CSV file {}", p_csvFile);
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  public void saveTxtFile(String p_txtFile) {
    TextPrint txp = new TextPrint(false, 5);
    for (HtmlValue cm : getListCampi(isJoinText()))
      txp.scrivi(cm);
    // System.out.println(txp.toString());
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_txtFile))) {
      bw.write(txp.toString());
      s_log.info("Scritto pdf text file {}", p_txtFile);
    } catch (IOException e) {
      s_log.error("Errore scrittura pdf text", e);
    }
  }

  /**
   * Questa salva il file come da intrepretazione
   *
   * @param p_htmlFile
   */
  public void saveHtmlFile(String p_htmlFile) {
    evidenziaNoSeqs(p_htmlFile);
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_htmlFile))) {
      String szHtml = m_outHtml //
          .stream() //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", p_htmlFile);
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  /**
   * Questa salva il file dopo l'interpretazione dei singoli campi, dopo i vari
   * join per parole vicine, dopo eliminazione dei &amp;NBSP, dopo il sort (in
   * base top,left), etc ...
   *
   * @param p_htmlFile
   */
  public void saveHtmlFile2(String p_htmlFile) {
    evidenziaNoSeqs(p_htmlFile);
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_htmlFile))) {
      //      String szHtml = m_outHtml //
      //          .stream() //
      //          .collect(Collectors.joining(System.lineSeparator()));
      String szHtml = m_map.values() //
          .stream() //
          .sorted() //
          .map(s -> s.getRigaHtml()) //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", p_htmlFile);
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  private void evidenziaNoSeqs(String p_htmlFile) {
    boolean bEvid = false;
    int qta = 0;
    // style="background-color: #f02020;color: yellow;"
    for (HtmlValue tgv : getListCampi(isJoinText())) {
      if ( !tgv.isNoSeq())
        continue;
      String szHTML = tgv.getRigaHtml();
      int ii = m_outHtml.indexOf(szHTML);
      if (ii < 0) {
        s_log.warn("Non trovo HTML:{}", szHTML);
        continue;
      }
      qta++;
      String sz = szHTML.replace(";\">", CSZ_EVID_NOSEQ);
      m_outHtml.set(ii, sz);
      bEvid = true;
    }
    if (bEvid)
      s_log.warn("Evidenziati nel file HTML \"{}\", {} campi numerici trascurati dalle sequenze", p_htmlFile, qta);
  }

  public Object parseRiga(String p_l) {
    return null;
  }

  public void setPage(int p_i) {
    m_nPage = p_i;
  }

}

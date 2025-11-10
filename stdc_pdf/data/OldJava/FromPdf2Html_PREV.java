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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;

import lombok.Getter;
import lombok.Setter;

public class FromPdf2Html_PREV {
  private static final Logger s_log          = LogManager.getLogger(FromPdf2Html_PREV.class);
  private static final String CSZ_PAT3;
  private static final String CSZ_EVID_NOSEQ = ";background-color: #f02020;color: yellow;\">";

  private List<String>            m_outHtml;
  private int                     m_nPage;
  private Map<Integer, HtmlValue> mapHtmV;
  // private HtmlValue               m_lastCp;
  @Getter @Setter
  private boolean salvaHTML;
  //  @Getter @Setter
  //  private boolean             salvaHTML2;
  @Getter @Setter
  private boolean         joinText;
  private List<HtmlValue> liHtmlValues;

  static {
    /**
     * class="p" id="p63"<br/>
     * style="<br/>
     * top:138.7503pt;<br/>
     * left:54.95072pt; line-height:12.5pt;<br/>
     * font-family:Tahoma;<br/>
     * font-size:10.0pt;<br/>
     * width:58.200005pt;<br/>
     * ">accettazione:
     * </pre>
     *
     * <pre>
     *  div class="p" id="p64"<br/>
     *    style="<br/>
     *      top:152.25171pt;<br/>
     *      left:117.21266pt;<br/>
     *      line-height:12.5pt;<br/>
     *      font-family:Tahoma,Bold;<br/>
     *      font-size:10.0pt;<br/>
     *      font-weight:bold;<br/>
     *      width:62.499985pt;<br/>
     * ">07/10/2025
     * </pre>
     */
    CSZ_PAT3 = ".*<div .* style=\"" //
        + "top:(\\d+\\.\\d+)pt;" //
        + "left:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "font-size:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "width:(\\d+\\.\\d+)pt;" //
        + ".*>(.*)</div>";
  }

  public FromPdf2Html_PREV() {
    //
  }

  public boolean convertiPDF(Path p_fiPdf) {
    s_log.info("Parse del file \"{}\"", p_fiPdf.toString());
    if ( !convToHtml(p_fiPdf))
      return false;
    if (salvaHTML) {
      String szHtm = p_fiPdf.toString().toLowerCase().replace(".pdf", "_0.html");
      saveHtmlFile(szHtm);
    }
    return scanRigheHTML();
  }

  public Map<Integer, HtmlValue> getMap() {
    return mapHtmV;
  }

  public void setMap(Map<Integer, HtmlValue> mp) {
    mapHtmV = mp;
  }

  public void clearMap() {
    if (null != mapHtmV)
      mapHtmV.clear();
    mapHtmV = null;
  }

  /**
   * Prende l'elenco dei HtmlValue e li sorta in base alla loro pag, py, px
   *
   * @return
   */
  public List<HtmlValue> getListCampi() {
    if (null == mapHtmV)
      return null;
    if (null != liHtmlValues)
      return liHtmlValues;
    liHtmlValues = new ArrayList<HtmlValue>(mapHtmV.values());
    Collections.sort(liHtmlValues);
    if (joinText)
      liHtmlValues = unifyAdiacentText(liHtmlValues);
    return liHtmlValues;
  }

  /**
   * Join di tutti quei TAG Html di tipo TEXT adiacenti in uno solo
   *
   * @param li
   *          List di HtmlValue sortato per page,top,left
   * @return il nuovo List di HtmlValue con text accodati
   */
  private List<HtmlValue> unifyAdiacentText(List<HtmlValue> li) {
    // List<HtmlValue> li = getListCampi();
    List<HtmlValue> liRet = new ArrayList<HtmlValue>();
    int iMax = li.size() - 3;
    HtmlValue precHtml = null;
    for (int i = 0; i < iMax; i++) {
      HtmlValue html = li.get(i);
      //  1 - prec null
      if (null == precHtml) {
        if (html.isText())
          precHtml = html;
        else
          liRet.add(html);
        continue;
      }
      // 2 - prec.text  curr.Num
      if ( !html.isText()) {
        liRet.add(precHtml);
        liRet.add(html);
        precHtml = null;
        continue;
      }
      // prec.text curr.text
      if (precHtml.isConsecutivo(html)) {
        precHtml.append(html);
      } else {
        liRet.add(precHtml);
        precHtml = html;
      }
    }
    if (null != precHtml)
      liRet.add(precHtml);
    return liRet;
  }

  /**
   * Converte il file PDF utilizzando la libreria "pdfbox (2.0.4)" in formato
   * HTML dove sono presenti i tags da interpretare
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
    try (PDDocument pdf = PDDocument.load(p_fiPdf.toFile()); StringWriter swr = new StringWriter();) {
      new PDFDomTree().writeText(pdf, swr);
      m_outHtml = new ArrayList<>();
      String[] arr = swr.toString().split("\n");
      m_outHtml.addAll(Arrays.asList(arr));
    } catch (IOException e) {
      System.err.printf("Errore \"%s\" in lettura file PDF: %s\n", e.getMessage(), p_fiPdf.toString());
      return false;
    }
    return true;
  }

  private boolean scanRigheHTML() {
    m_nPage = 0;
    Pattern pat2 = Pattern.compile(CSZ_PAT3);
    String szLeft = null, szTop = null, szFSiz = null, szWidth = null, szText = null;
    // m_liCampi = new ArrayList<>();
    boolean breakSpace = false;
    mapHtmV = new TreeMap<>();
    for (String szRigaHtml : m_outHtml) {
      if (szRigaHtml.indexOf(">&nbsp;</div>") >= 0) {
        breakSpace = true;
        continue;
      }
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
      szFSiz = mtch.group(k++);
      szWidth = mtch.group(k++);
      szText = mtch.group(k++);
      if (null != szText && szText.contains("&"))
        szText = StringEscapeUtils.unescapeHtml4(szText);
      if (null != szText && szText.contains("["))
        szText = szText.replace("[", "");
      if (null != szText && szText.contains("]"))
        szText = szText.replace("]", "");
      trattaRiga(szLeft, szTop, szFSiz, szWidth, szText, szRigaHtml, breakSpace);
      breakSpace = false;
    }
    if (mapHtmV.size() < 5) {
      s_log.error("Non sembra essere una fattura");
      return false;
    }
    return true;
  }

  public void trattaRiga(String szLeft, String szTop, String szFSiz, String szWidth, String szTxt, String szRiHtml,
      boolean breakSpace) {
    //    String szDebug = null;
    //    if (null != szDebug && szTxt.toLowerCase().contains(szDebug))
    //      System.out.printf("FromHtml.trattaRiga(\"%s\")=%s\n", szDebug, szTxt);
    if (szTxt != null && szTxt.indexOf("nbsp;") >= 0)
      return;
    if (null == mapHtmV)
      mapHtmV = new TreeMap<>();
    //    double nLeft = Double.parseDouble(szLeft);
    //    double nTop = Double.parseDouble(szTop);
    //    double nFSiz = Double.parseDouble(szFSiz);
    //    double nWidth = Double.parseDouble(szWidth);
    if (m_nPage <= 0)
      s_log.error("Pagina fuori range:{} su tag {}", m_nPage, szTxt);
    //    HtmlValue rec = new HtmlValue(nLeft, nTop, nWidth, nFSiz, m_nPage, szTxt, szRiHtml);
    //    rec = analizzaHtmlValue(rec);
    //    if ( !breakSpace && m_lastCp != null && m_lastCp.isConsecutivo(rec))
    //      m_lastCp.append(rec);
    //    else {
    //      mapHtmV.put(rec.getId(), rec);
    //      m_lastCp = rec;
    //    }
  }

  /**
   * Permette di analizzare meglio l'elemento HTML nel caso sia un costrutto più
   * specifico
   * 
   * @param rec
   * @return
   */
  public HtmlValue analizzaHtmlValue(HtmlValue rec) {
    return rec;
  }

  public String getTextTAGs() {
    String sz = "**NULL**";
    //    if (m_liCampi == null || m_liCampi.size() == 0)
    //      return sz;
    if (null == mapHtmV || mapHtmV.size() == 0)
      return sz;
    //    sz = m_liCampi //
    //        .stream() //
    //        .map(t -> t.toString()) //
    //        .collect(Collectors.joining("\n"));
    sz = getListCampi() //
        .stream() //
        .map(t -> t.toString()) //
        .collect(Collectors.joining("\n"));
    return sz;
  }

  public String getCSVTAGs() {
    String sz = "sep=;";
    //    if (m_liCampi == null || m_liCampi.size() == 0)
    //      return sz;
    if (null == mapHtmV || mapHtmV.size() == 0)
      return sz;
    //    sz = m_liCampi //
    //        .stream() //
    //        .map(t -> t.toString()) //
    //        .collect(Collectors.joining("\n"));
    sz = getListCampi() //
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
    for (HtmlValue cm : getListCampi())
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
    if (null == mapHtmV || mapHtmV.size() == 0)
      return;
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_htmlFile))) {
      //      String szHtml = m_outHtml //
      //          .stream() //
      //          .collect(Collectors.joining(System.lineSeparator()));
      String szHtml = mapHtmV.values() //
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
    if (null == getListCampi())
      return;
    boolean bEvid = false;
    int qta = 0;
    // style="background-color: #f02020;color: yellow;"
    for (HtmlValue tgv : getListCampi()) {
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

  /**
   * Ritorna l'elenco delle stringhe convertite in HTML
   *
   * @return
   */
  public List<String> getLiHtml() {
    return m_outHtml;
  }

  public void setPage(int p_i) {
    m_nPage = p_i;
  }

}

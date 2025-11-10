package prove.stdc_pdf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.junit.Test;

import sm.clagenna.stdcla.pdf.ETipiDato;
import sm.clagenna.stdcla.pdf.IPdfGestore;
import sm.clagenna.stdcla.pdf.HtmlValue;

public class ProvaPdf2Html implements IPdfGestore {
  private static final Logger  s_log = LogManager.getLogger(ProvaPdf2Html.class);
  private static final String  CSZ_PAT_ALL, CSZ_PAT_TOP, CSZ_PAT_LEFT, CSZ_PAT_HE, CSZ_PAT_WI;
  private static final Pattern patAll;
  private static final Pattern patTop;
  private static final Pattern patLeft;
  private static final Pattern patWi;
  private static final Pattern patHe;
  private int                  m_nPage;
  private List<String>         m_outHtml;
  private List<HtmlValue>     liHtmlValues;
  private List<HtmlValue>     liHtmlValues2;
  private double               m_nMaxTop;
  private Path                 pthPdf;

  public ProvaPdf2Html() {
    //
  }

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
    CSZ_PAT_ALL = ".*<div .* style=\"" //
        + "top:(\\d+\\.\\d+)pt;" //
        + "left:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "font-size:(\\d+\\.\\d+)pt;" //
        + ".*" //
        + "width:(\\d+\\.\\d+)pt;" //
        + ".*>(.*)</div>";
    CSZ_PAT_TOP = ".*<div .* style=.*" //
        + "top:(\\d+\\.\\d+)pt;";
    CSZ_PAT_LEFT = ".*<div .* style=.*" //
        + "left:(\\d+\\.\\d+)pt;";
    CSZ_PAT_HE = ".*<div .* style=.*" //
        + "height:(\\d+\\.\\d+)pt;";
    CSZ_PAT_WI = ".*<div .* style=.*" //
        + "width:(\\d+\\.\\d+)pt;";
    patAll = Pattern.compile(CSZ_PAT_ALL);
    patTop = Pattern.compile(CSZ_PAT_TOP);
    patLeft = Pattern.compile(CSZ_PAT_LEFT);
    patWi = Pattern.compile(CSZ_PAT_WI);
    patHe = Pattern.compile(CSZ_PAT_HE);
  }

  @Test
  public void doIt() {
    pthPdf = Paths.get("data/Analisi_2025-10-07.pdf");
    if ( !convToHtml(pthPdf))
      return;
    saveHtml0();
    System.out.printf("Conv 2 HTML, %d righe\n---------------\n", m_outHtml.size());
    m_outHtml.stream().limit(24).forEach(System.out::println);
    System.out.println("...");
    scanRigheHTML();
    saveHtml(liHtmlValues, "1");
    saveCSVFile(liHtmlValues, "1");
    liHtmlValues2 = liHtmlValues.stream().sorted().toList();
    saveHtml(liHtmlValues2, "2");
    saveCSVFile(liHtmlValues2, "2");
  }

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
    m_nMaxTop = 0.;
    String szLeft, szTop, szHeigt, szWidth, szToken;
    Matcher mtch = null;

    liHtmlValues = new ArrayList<>();
    for (String szRigaHtml : m_outHtml) {
      szLeft = szTop = szHeigt = szWidth = szToken = null;

      // 1) pattern più complesso, con token
      mtch = patAll.matcher(szRigaHtml);
      if (mtch.find()) {
        int k = 1;
        szTop = mtch.group(k++);
        szLeft = mtch.group(k++);
        szHeigt = mtch.group(k++);
        szWidth = mtch.group(k++);
        szToken = mtch.group(k++);
        trattaToken(szLeft, szTop, szHeigt, szWidth, szToken, szRigaHtml);
        continue;
      }
      // riga html ausiliaria
      //      if (szRigaHtml.indexOf("class=\"p") < 0) {
      //        trattaHtml(szLeft, szTop, szHeigt, szWidth, szToken, szRigaHtml);
      //        continue;
      //      }

      // 2) div della pagina
      if (szRigaHtml.indexOf("class=\"page\"") >= 0) {
        m_nPage++;
        m_nMaxTop = 0.;
        trattaHtml(szLeft, szTop, szHeigt, szWidth, szToken, szRigaHtml);
        continue;
      }
      // 3) tutto quello che rimane di ausiliario
      mtch = patLeft.matcher(szRigaHtml);
      if (mtch.find())
        szLeft = mtch.group(1);
      mtch = patTop.matcher(szRigaHtml);
      if (mtch.find())
        szTop = mtch.group(1);
      mtch = patHe.matcher(szRigaHtml);
      if (mtch.find())
        szHeigt = mtch.group(1);
      mtch = patWi.matcher(szRigaHtml);
      if (mtch.find())
        szWidth = mtch.group(1);
      trattaHtml(szLeft, szTop, szHeigt, szWidth, szToken, szRigaHtml);
      continue;
    }
    if (liHtmlValues.size() < 5) {
      s_log.error("Non sembra essere una fattura");
      return false;
    }
    return true;
  }

  private void trattaHtml(String szLeft, String szTop, String szHeigt, String szWidth, String szText, String szRigaHtml) {
    if (null == szTop)
      m_nMaxTop += 0.1;

    //    double nLeft = null != szLeft ? Double.parseDouble(szLeft) : 0.;
    //    double nTop = null != szTop ? Double.parseDouble(szTop) : m_nMaxTop;
    //    double nHei = null != szHeigt ? Double.parseDouble(szHeigt) : 0.;
    //    double nWidth = null != szWidth ? Double.parseDouble(szWidth) : 0.;
    //    HtmlValue rec = new HtmlValue(nLeft, nTop, nWidth, nHei, m_nPage, null, szRigaHtml);
    HtmlValue rec = new HtmlValue();
    rec.parseHtmlText(szRigaHtml, this);
    rec.setTipoDato(ETipiDato.HTML);
    rec = analizzaHtmlValue(rec);
    liHtmlValues.add(rec);
  }

  public void trattaToken(String szLeft, String szTop, String szHeig, String szWidth, String szText, String szRiHtml) {
    //    String szDebug = null;
    //    if (null != szDebug && szTxt.toLowerCase().contains(szDebug))
    //      System.out.printf("FromHtml.trattaRiga(\"%s\")=%s\n", szDebug, szTxt);
    if (m_nPage <= 0)
      s_log.error("Pagina fuori range:{} su tag {}", m_nPage, szText);
    //    if (szText != null && szText.indexOf("nbsp;") >= 0)
    //      return;
    //    if (null != szText && szText.contains("&"))
    //      szText = StringEscapeUtils.unescapeHtml4(szText);
    //    if (null != szText && szText.contains("["))
    //      szText = szText.replace("[", "");
    //    if (null != szText && szText.contains("]"))
    //      szText = szText.replace("]", "");

    //    double nLeft = Double.parseDouble(szLeft);
    //    double nTop = Double.parseDouble(szTop);
    //    double nFSiz = Double.parseDouble(szHeig);
    //    double nWidth = Double.parseDouble(szWidth);
    //    m_nMaxTop = m_nMaxTop < nTop ? nTop : m_nMaxTop;
    // HtmlValue rec = new HtmlValue(nLeft, nTop, nWidth, nFSiz, m_nPage, szText, szRiHtml);
    HtmlValue rec = new HtmlValue();
    rec.parseHtmlText(szRiHtml, this);
    rec = analizzaHtmlValue(rec);
    liHtmlValues.add(rec);
  }

  public HtmlValue analizzaHtmlValue(HtmlValue rec) {
    return rec;
  }

  private void saveHtml0() {
    String szFi = pthPdf.toAbsolutePath().toString();
    int n = szFi.toLowerCase().indexOf(".pdf");
    if (n > 0)
      szFi = szFi.substring(0, n) + "_0.htm";
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(szFi))) {
      String szHtml = m_outHtml //
          .stream() //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", szFi);
    } catch (IOException e) {
      s_log.error("Errore scrittura HTML 0", e);
    }
  }

  private void saveHtml(List<HtmlValue> li, String sufx) {
    String szFi = pthPdf.toAbsolutePath().toString();
    int n = szFi.toLowerCase().indexOf(".pdf");
    if (n > 0)
      szFi = String.format("%s_%s.htm", szFi.substring(0, n), sufx);
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(szFi))) {
      String szHtml = li //
          .stream() //
          .map(s -> s.getRigaHtml()) //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", szFi);
    } catch (IOException e) {
      s_log.error("Errore scrittura HTML 0", e);
    }
  }

  private void saveCSVFile(List<HtmlValue> li, String sufx) {
    String szFi = pthPdf.toAbsolutePath().toString();
    int n = szFi.toLowerCase().indexOf(".pdf");
    if (n > 0)
      szFi = String.format("%s_%s.csv", szFi.substring(0, n), sufx);
    try {
      Files.deleteIfExists(Paths.get(szFi));
    } catch (IOException e) {
      s_log.error("Error {} on delete {}", e.getMessage(), szFi);
      return;
    }
    String sz1 = HtmlValue.CSV_HEADER;
    String sz2 = li //
        .stream() //
        .map(t -> t.toCsv()) //
        .collect(Collectors.joining("\n"));
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(szFi))) {
      bw.write(sz1);
      bw.write(sz2);
      s_log.info("Scritto CSV file {}", szFi);
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  @Override
  public int getNPage() {
    return m_nPage;
  }

  @Override
  public void setNPage(int vPage) {
    m_nPage = vPage;

  }

  @Override
  public double getMaxTop() {
    return m_nMaxTop;
  }

  @Override
  public void setMaxTop(double vVal) {
    m_nMaxTop = vVal;

  }

}

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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.sys.TimerMeter;

public class FromPdf2Html implements IPdfGestore {
  private static final Logger s_log           = LogManager.getLogger(FromPdf2Html.class);
  private static final String CSZ_SAVE_SUBDIR = "AcqInfo";

  @Getter @Setter
  private Path            filePDF;
  private List<String>    m_outHtml;
  /** list of HtmlValue UN-ordered */
  private List<HtmlValue> liHtmlValues;
  /** list of HtmlValue Ordered */
  private List<HtmlValue> liHtmlValues2;

  @Getter @Setter
  private boolean debug;
  @Getter @Setter
  private boolean saveHTML;
  @Getter @Setter
  private boolean saveCSV;
  @Getter @Setter
  private boolean saveTXT;
  //  @Getter @Setter
  //  private IParseHtmlValues parserHtml;
  @Getter @Setter
  private int    nPage;
  @Getter @Setter
  private double maxTop;

  public FromPdf2Html() {
    debug = false;
    saveHTML = false;
    saveCSV = false;
    saveTXT = false;
  }

  public boolean parsePDF(Path p_fiPdf) {
    setFilePDF(p_fiPdf);
    if ( !convToHtml(p_fiPdf))
      return false;
    if (saveHTML)
      saveHtml0();
    if (debug) {
      System.out.printf("Conv 2 HTML, %d righe\n---------------\n", m_outHtml.size());
      m_outHtml.stream().limit(24).forEach(System.out::println);
      System.out.println("...");
    }
    scanRigheHTML();
    saveHtml(liHtmlValues, "_1");
    saveCSVFile(liHtmlValues, "_1");
    liHtmlValues2 = liHtmlValues.stream().sorted().toList();
    saveHtml(liHtmlValues2, "_2");
    saveCSVFile(liHtmlValues2, "_2");
    saveTxtFile(liHtmlValues2, "_2");
    return true;
  }

  public List<HtmlValue> getLiHtml() {
    return liHtmlValues2;
  }

  private boolean scanRigheHTML() {
    nPage = 0;
    maxTop = 0.;
    liHtmlValues = new ArrayList<>();

    for (String szRigaHtml : m_outHtml) {
      HtmlValue rec = new HtmlValue();
      if (rec.parseHtmlText(szRigaHtml, this) > 0) {
        if ( !seSpezzabile(rec))
          liHtmlValues.add(rec);
      }
    }
    if (liHtmlValues.size() < 5) {
      s_log.error("Non sembra essere una file PDF");
      return false;
    }
    return true;
  }

  private boolean seSpezzabile(HtmlValue re) {
    String tok = re.getTxt();
    if (re.getTipoDato() != ETipiDato.Stringa || //
        null == tok || //
        tok.trim().length() == 0)
      return false;
    String first = tok.substring(0, 1);
    ETipiDato tp = ETipiDato.parseChar(first);
    if (null == tp || tp != ETipiDato.Minor)
      return false;
    String second = tok.substring(1);
    try {
      String tokEscap = StringEscapeUtils.escapeHtml4(tok);
      String unoEscap = StringEscapeUtils.escapeHtml4(first);
      // ------------- UNO -------------------
      HtmlValue uno = (HtmlValue) re.clone();
      uno.setTxt(first);
      String szHtm = uno.getRigaHtml().replace(tokEscap, unoEscap);
      uno.setRigaHtml(szHtm);
      uno.setTipoDato(tp);
      uno.setWidth(uno.getFontSize());
      uno.parseHtmlText(szHtm, this);
      liHtmlValues.add(uno);

      // ------------- DUE -------------------
      HtmlValue due = (HtmlValue) re.clone();
      due.setTxt(second);
      szHtm = due.getRigaHtml().replace(tokEscap, second);
      due.setRigaHtml(szHtm);
      // due.parseHtmlText(due.getRigaHtml(), this);
      // due.setTxt(second);
      due.setFx(due.getFx() + 1.);
      due.setLeft(due.getLeft() + 1);
      due.setWidth(due.getWidth() - due.getFontSize() / 2.);
      due.parseHtmlText(szHtm, this);
      liHtmlValues.add(due);
      // -------------------------------------
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return true;
  }

  private boolean convToHtml(Path p_fiPdf) {
    String threadName = Thread.currentThread().getName();
    TimerMeter tt = new TimerMeter("convToHtml:" + p_fiPdf.toString());
    try (PDDocument pdf = PDDocument.load(p_fiPdf.toFile()); StringWriter swr = new StringWriter();) {
      new PDFDomTree().writeText(pdf, swr);
      m_outHtml = new ArrayList<>();
      String[] arr = swr.toString().split("\n");
      m_outHtml.addAll(Arrays.asList(arr));
      s_log.debug("{},{}", threadName, tt.stop());
    } catch (IOException e) {
      s_log.error("Errore \"{}\" in lettura file PDF: {}", e.getMessage(), p_fiPdf.toString());
      return false;
    }
    return true;
  }

  private void saveHtml0() {
    //    Path pthPadre = getFilePDF().getParent();
    //    String szFi = getFilePDF().getFileName().toString();
    //    int n = szFi.toLowerCase().indexOf(".pdf");
    //    if (n > 0)
    //      szFi = szFi.substring(0, n);
    //    //    Path pthOut = Paths.get(pthPadre.toString(), CSZ_SAVE_SUBDIR, szFi + "_0.html");
    //    Path pthOut = Paths.get(pthPadre.toString(), CSZ_SAVE_SUBDIR);
    //    try {
    //      if ( !Files.exists(pthOut))
    //        Files.createDirectory(pthOut);
    //    } catch (IOException e) {
    //      s_log.error("SaveHTML0:Non sono riuscito a creare il dir {}, err={}", pthOut.toString(), e.getMessage());
    //      return;
    //    }
    //    Path fiOut = Paths.get(pthOut.toString(), szFi + "_0.html");
    Path pthOut = creaOutFile(getFilePDF(), "_0", "html");
    if (null == pthOut)
      return;
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(pthOut.toFile()))) {
      String szHtml = m_outHtml //
          .stream() //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", pthOut.toString());
    } catch (IOException e) {
      s_log.error("Errore scrittura HTML 0", e);
    }
  }

  private Path creaOutFile(Path pthIn, String sufx, String ext) {
    Path pthPadre = getFilePDF().getParent();
    String szFi = pthIn.getFileName().toString();
    int n = szFi.toLowerCase().lastIndexOf(".");
    if (n > 0)
      szFi = szFi.substring(0, n);
    Path pthOut = null;
    Path pthDir = Paths.get(pthPadre.toString(), CSZ_SAVE_SUBDIR);
    try {
      if ( !Files.exists(pthDir))
        Files.createDirectory(pthDir);
    } catch (IOException e) {
      s_log.error("SaveHTML0:Non sono riuscito a creare il dir {}, err={}", pthDir.toString(), e.getMessage());
      return pthOut;
    }
    String szOut = String.format("%s%s.%s", szFi, sufx, ext);
    pthOut = Paths.get(pthDir.toString(), szOut);
    return pthOut;
  }

  public void saveHtml(String sufx) {
    saveHtml(liHtmlValues2, sufx);
  }

  public void saveHtml(List<HtmlValue> li, String sufx) {
    if ( !saveHTML)
      return;
    //    String szFi = getFilePDF().toAbsolutePath().toString();
    //    int n = szFi.toLowerCase().indexOf(".pdf");
    //    if (n > 0)
    //      szFi = String.format("%s_%s.htm", szFi.substring(0, n), sufx);
    Path pthOut = creaOutFile(getFilePDF(), sufx, "html");
    if (null == pthOut)
      return;
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(pthOut.toFile()))) {
      String szHtml = li //
          .stream() //
          .map(s -> s.getRigaHtml()) //
          .collect(Collectors.joining(System.lineSeparator()));
      bw.write(szHtml);
      s_log.info("Scritto HTML file {}", pthOut.toString());
    } catch (IOException e) {
      s_log.error("Errore scrittura HTML 0", e);
    }
  }

  private void saveCSVFile(List<HtmlValue> li, String sufx) {
    if ( !saveCSV)
      return;
    //    String szFi = getFilePDF().toAbsolutePath().toString();
    //    int n = szFi.toLowerCase().indexOf(".pdf");
    //    if (n > 0)
    //      szFi = String.format("%s_%s.csv", szFi.substring(0, n), sufx);
    Path pthOut = creaOutFile(getFilePDF(), sufx, "csv");
    if (null == pthOut)
      return;
    try {
      Files.deleteIfExists(pthOut);
    } catch (IOException e) {
      s_log.error("Error {} on delete {}", e.getMessage(), pthOut.toString());
      return;
    }
    String sz1 = HtmlValue.CSV_HEADER;
    String sz2 = li //
        .stream() //
        .map(t -> t.toCsv()) //
        .collect(Collectors.joining("\n"));
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(pthOut.toFile()))) {
      bw.write(sz1);
      bw.write(sz2);
      s_log.info("Scritto CSV file {}", pthOut.toString());
    } catch (IOException e) {
      s_log.error("Errore scrittura TAGs", e);
    }
  }

  public void saveTxtFile(List<HtmlValue> li, String sufx) {
    if ( !saveTXT)
      return;
    //    String szFi = getFilePDF().toAbsolutePath().toString();
    //    int n = szFi.toLowerCase().indexOf(".pdf");
    //    if (n > 0)
    //      szFi = String.format("%s_%s.txt", szFi.substring(0, n), sufx);
    Path pthOut = creaOutFile(getFilePDF(), sufx, "txt");
    if (null == pthOut)
      return;
    try {
      Files.deleteIfExists(pthOut);
    } catch (IOException e) {
      s_log.error("Error {} on delete {}", e.getMessage(), pthOut.toString());
      return;
    }
    TextPrint txp = new TextPrint(false, 5);
    for (HtmlValue cm : li)
      txp.scrivi(cm);
    // System.out.println(txp.toString());
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(pthOut.toFile()))) {
      bw.write(txp.toString());
      s_log.info("Scritto pdf text file {}", pthOut.toString());
    } catch (IOException e) {
      s_log.error("Errore scrittura pdf text", e);
    }
  }

  //  private void parseHtmlValues(List<HtmlValue> liHtmlValues22) {
  //    parserHtml.setDebug(debug);
  //    int qta = parserHtml.parse(liHtmlValues22);
  //    s_log.debug("Generato {} records\n", qta);
  //  }

}

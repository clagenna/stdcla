package prove.stdc_pdf.ansan;

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
import org.junit.Test;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.pdf.ETipiDato;
import sm.clagenna.stdcla.pdf.IPdfGestore;
import sm.clagenna.stdcla.pdf.HtmlValue;
import sm.clagenna.stdcla.pdf.TextPrint;

public class ProvaNuovoHtmlValue implements IPdfGestore {
  private static final Logger s_log = LogManager.getLogger(ProvaNuovoHtmlValue.class);

  @Getter @Setter
  private int    nPage;
  @Getter @Setter
  private double maxTop;

  private List<String>     m_outHtml;
  private List<HtmlValue> liHtmlValues;
  private List<HtmlValue> liHtmlValues2;
  private Path             pthPdf;

  public ProvaNuovoHtmlValue() {
    //
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
    saveTxtFile(liHtmlValues2, "2");
    parseHtmlValues(liHtmlValues2);
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

  public void saveTxtFile(List<HtmlValue> li, String sufx) {
    String szFi = pthPdf.toAbsolutePath().toString();
    int n = szFi.toLowerCase().indexOf(".pdf");
    if (n > 0)
      szFi = String.format("%s_%s.txt", szFi.substring(0, n), sufx);
    try {
      Files.deleteIfExists(Paths.get(szFi));
    } catch (IOException e) {
      s_log.error("Error {} on delete {}", e.getMessage(), szFi);
      return;
    }
    TextPrint txp = new TextPrint(false, 5);
    for (HtmlValue cm : li)
      txp.scrivi(cm);
    // System.out.println(txp.toString());
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(szFi))) {
      bw.write(txp.toString());
      s_log.info("Scritto pdf text file {}", szFi);
    } catch (IOException e) {
      s_log.error("Errore scrittura pdf text", e);
    }
  }

  private void parseHtmlValues(List<HtmlValue> liHtmlValues22) {
    //    PParserHtmlValues prs = new PParserHtmlValues();
    //    PParserHtmlValues.setDebug(true);
    //    int qta = prs.parse(liHtmlValues22);
    //    System.out.printf("Generato %d record Anal. Sangue\n", qta);
  }

}

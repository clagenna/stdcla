module stdc_pdf {
  exports sm.clagenna.stdcla.pdf;

  requires javafx.base;
  requires transitive stdc_utils;
  requires transitive stdc_sql;

  requires lombok;
  requires org.apache.logging.log4j;
  requires org.apache.pdfbox;
  requires net.sf.cssbox.pdf2dom;
  requires org.apache.commons.text;
  
}

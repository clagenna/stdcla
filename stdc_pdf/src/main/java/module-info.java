// @ SuppressWarnings("requires-automatic")
/**
 * <code>SuppressWarnings("requires-automatic")</code> 
 * sostituito con
 * <pre>
 *   &lt;compilerArgs&gt;
 *      &lt;arg&gt;-Xlint:all&lt;/arg&gt;
 *      &lt;arg&gt;-Xlint:-requires-automatic&lt;/arg&gt;
 *      &lt;arg&gt;-Xlint:-requires-transitive-automatic&lt;/arg&gt;
 * </pre> 
 */
module stdc_pdf {
  exports sm.clagenna.stdcla.pdf;

  requires javafx.base;
  requires transitive stdc_utils;
  requires transitive stdc_sql;

  requires lombok;
  requires org.apache.logging.log4j;

  requires org.apache.pdfbox;
  
  // requires transitive org.apache.pdfbox.io;
  requires net.sf.cssbox.pdf2dom;
  requires org.apache.commons.text;
}

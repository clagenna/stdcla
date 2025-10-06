module stdc_javafx {
  exports sm.clagenna.stdcla.javafx;

  requires javafx.base;
  requires java.desktop;
  requires transitive stdc_sql;
  requires transitive stdc_utils;
  requires lombok;
  requires org.apache.logging.log4j;
  requires transitive javafx.controls;
  requires transitive javafx.graphics;
}

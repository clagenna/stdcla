module stdc_sql {
  exports sm.clagenna.stdcla.sql;

  requires transitive stdc_utils;
  
  requires transitive java.sql;
  requires lombok;
  requires transitive org.apache.logging.log4j;
  
  requires org.apache.poi.poi;
  requires org.apache.poi.ooxml;
  requires transitive commons.math3;
  
  requires com.opencsv;
  requires org.xerial.sqlitejdbc;
}

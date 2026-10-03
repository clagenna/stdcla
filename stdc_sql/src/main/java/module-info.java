// senza quel SuppressWarnings dava warning durante la "mvn clean install"
@SuppressWarnings("requires-automatic")
module stdc_sql {
  // c'era: @ SuppressWarnings("requires-transitive-automatic")
  exports sm.clagenna.stdcla.sql;

  requires transitive stdc_utils;
  
  requires transitive java.sql;
  requires lombok;
  requires transitive org.apache.logging.log4j;
  
  requires org.apache.poi.poi;
  requires org.apache.poi.ooxml;
  requires transitive commons.math3;
  
  // requires transitive com.opencsv;
  // manda il warning:
  // [exports] class CsvException in module com.opencsv is not indirectly exported using 'requires transitive'
  requires transitive com.opencsv;
  requires transitive org.xerial.sqlitejdbc;
}

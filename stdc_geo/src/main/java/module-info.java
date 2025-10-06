module stdc_geo {
  exports sm.clagenna.stdcla.geo;
  exports sm.clagenna.stdcla.geo.fromgoog;

  requires transitive stdc_utils;

  requires com.fasterxml.jackson.core;
  requires transitive java.xml;
  requires transitive java.desktop;
  requires lombok;
  requires transitive org.apache.commons.imaging;
  requires org.apache.logging.log4j;
}

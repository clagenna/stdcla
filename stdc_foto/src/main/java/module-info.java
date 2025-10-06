module stdc_foto {
  exports sm.clagenna.stdcla.fotoscan;
  
  requires transitive stdc_utils;
  requires transitive stdc_geo;
  requires lombok;
  requires transitive java.desktop;
  requires transitive javafx.base;
  requires transitive javafx.controls;
  requires transitive javafx.graphics;

}
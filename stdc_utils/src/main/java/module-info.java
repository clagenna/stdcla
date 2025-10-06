module stdc_utils {
  exports sm.clagenna.stdcla.utils;
  exports sm.clagenna.stdcla.utils.concurrency;
  exports sm.clagenna.stdcla.utils.sys;
  exports sm.clagenna.stdcla.utils.sys.ex;
  
  requires transitive org.apache.logging.log4j;
  requires transitive org.apache.logging.log4j.core;
  requires java.sql;
  requires lombok;
}

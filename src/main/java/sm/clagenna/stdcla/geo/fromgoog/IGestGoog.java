package sm.clagenna.stdcla.geo.fromgoog;

public interface IGestGoog {
  void setCollector(IGeoCollector col);

  void open();

  void gestRiga(String path, Object val);

  void close();
}

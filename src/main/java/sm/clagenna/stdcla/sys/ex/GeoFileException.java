package sm.clagenna.stdcla.sys.ex;

public class GeoFileException extends StdclaException {
  private static final long serialVersionUID = 7452098301936737416L;

  public GeoFileException() {
    //
  }

  public GeoFileException(String p_msg) {
    super(p_msg);
  }

  public GeoFileException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

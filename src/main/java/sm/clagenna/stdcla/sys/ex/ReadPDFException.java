package sm.clagenna.stdcla.sys.ex;

public class ReadPDFException extends StdclaException {

  /** long */
  private static final long serialVersionUID = 2271943661219302651L;

  public ReadPDFException() {
    //
  }

  public ReadPDFException(String p_msg) {
    super(p_msg);
  }

  public ReadPDFException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

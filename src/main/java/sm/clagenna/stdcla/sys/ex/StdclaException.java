package sm.clagenna.stdcla.sys.ex;

public class StdclaException extends Exception {

  /** serialVersionUID */
  private static final long serialVersionUID = 7288868203289764643L;

  public StdclaException() {
    //
  }

  public StdclaException(String p_msg) {
    super(p_msg);
  }

  public StdclaException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

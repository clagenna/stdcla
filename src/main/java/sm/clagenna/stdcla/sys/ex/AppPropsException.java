package sm.clagenna.stdcla.sys.ex;

public class AppPropsException extends StdclaException {

  /** serialVersionUID */
  private static final long serialVersionUID = 7288868203289764643L;

  public AppPropsException() {
    //
  }

  public AppPropsException(String p_msg) {
    super(p_msg);
  }

  public AppPropsException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

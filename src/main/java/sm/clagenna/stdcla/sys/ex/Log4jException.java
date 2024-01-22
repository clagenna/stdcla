package sm.clagenna.stdcla.sys.ex;

public class Log4jException extends StdclaException {

  /** serialVersionUID */
  private static final long serialVersionUID = 7288868203289764643L;

  public Log4jException() {
    //
  }

  public Log4jException(String p_msg) {
    super(p_msg);
  }

  public Log4jException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

package sm.clagenna.stdcla.sys.ex;

public class DatasetException extends StdclaException {

  /** serialVersionUID */
  private static final long serialVersionUID = 7288868203289764643L;

  public DatasetException() {
    //
  }

  public DatasetException(String p_msg) {
    super(p_msg);
  }

  public DatasetException(String p_msg, Throwable p_ex) {
    super(p_msg, p_ex);
  }

}

package sm.clagenna.stdcla.utils;

import org.apache.logging.log4j.Level;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sys.ex.Log4jException;

public class Log4jRow {

  @Getter @Setter
  private String logClass;
  @Getter @Setter
  private String szTime;
  @Getter @Setter
  private Level  level;
  @Getter @Setter
  private String message;

  public Log4jRow() {
    //
  }

  public Log4jRow(String[] arr) throws Log4jException {
    if (arr == null)
      throw new Log4jException("Null array");
    if (arr.length < 4)
      throw new Log4jException("Log4jRow.Log4jRow(), pochi campi:" + arr.length);
    setLogClass(arr[0]);
    setSzTime(arr[1]);
    setLevel(Level.valueOf(arr[2]));
    setMessage(arr[3]);
  }

  @Override
  public String toString() {
    String sz = String.format("%s %6s %s", szTime, level.toString(), message);
    return sz;
  }

  public String getTime() {
    String sz = szTime.substring(10).trim();
    int n = sz.indexOf(",");
    if (n >= 0)
      sz = sz.substring(0, n);
    return sz;
  }
}

package sm.clagenna.stdcla.utils.sys;

public class StackViewer {

  public static String viewStackTrace(String szId) {
    StackTraceElement[] stck = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();
    String[] scarta = { "java.", "javafx." };
    int coda = 0;
    for (StackTraceElement ste : stck) {
      if (coda++ < 2)
        continue;
      String sz = ste.toString();
      boolean bGood = true;
      for (String sc : scarta) {
        if (sz.startsWith(sc)) {
          bGood = false;
          break;
        }
      }
      if (bGood)
        sb.append("\t").append(sz).append("\n");
    }
    return String.format("Stack id:%s\n%s", szId, sb.toString());
  }

}

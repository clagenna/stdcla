package sm.clagenna.stdcla.sys;

import java.time.Duration;
import java.time.Instant;

public class TimerMeter {

  private String  cosa;
  private Instant t1;
  private double  f;

  public TimerMeter(String p_cosa) {
    cosa = p_cosa;
    start();
  }

  public TimerMeter start() {
    t1 = Instant.now();
    return this;
  }

  public String stop() {
    Instant t2 = Instant.now();
    f = Duration.between(t1, t2).toMillis() / 1000F;
    return toString();
  }

  @Override
  public String toString() {
    String sz = String.format("Passato %.4f,\t\"%s\"", f, cosa);
    return sz;
  }
}

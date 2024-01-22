package sm.clagenna.stdcla.enums;

public enum EServerId {
  SqlServer, //
  HSqlDB, //
  SQLite(true), //
  SQLite3(true);

  private boolean dateAsString;

  private EServerId() {
    dateAsString = false;
  }

  private EServerId(boolean p_v) {
    dateAsString = p_v;
  }

  public static EServerId parse(String p_id) {
    EServerId ret = null;
    if (p_id == null)
      return ret;
    String remid = p_id.toLowerCase();
    for (EServerId id : EServerId.values()) {
      String lid = id.name().toLowerCase();
      if (lid.equals(remid)) {
        ret = id;
        break;
      }
    }
    return ret;
  }

  public boolean isDateAsString() {
    return dateAsString;
  }
}

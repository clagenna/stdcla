package sm.clagenna.stdcla.sql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;

public class DBConnFactory {
  private static final Logger s_log     = LogManager.getLogger(DBConnFactory.class);
  @Getter @Setter
  private static boolean      singleton = true;
  private DBConnFactory       s_inst;
  @Getter @Setter
  private DBConn              conn;

  public DBConnFactory() {
    if (DBConnFactory.isSingleton()) {
      if (s_inst != null) {
        s_log.error("DBConnFactory() gia' istanziato");
        throw new UnsupportedOperationException("DBConnFactory() gia' istanziato");
      }
      s_inst = this;
    }
  }

  public DBConnFactory getInst() {
    if ( !DBConnFactory.isSingleton()) {
      s_log.error("getInst() ma DBConnFactory() non e' singleton");
      throw new UnsupportedOperationException("DBConnFactory() non e' singleton");
    }
    return s_inst;
  }

  public DBConn get(String p_id) {
    EServerId tip = EServerId.parse(p_id);
    return get(tip);
  }

  public DBConn get(EServerId tip) {
    if (tip == null) {
      s_log.error("Non capisco il tipo di DB: {}", tip);
      throw new UnsupportedOperationException("Non capisco il tipo di DB:" + tip);
    }
    conn = null;
    switch (tip) {
      case HSqlDB:
        break;
      case SQLite:
      case SQLite3:
        conn = new DBConnSQLite();
        break;
      case SqlServer:
        conn = new DBConnSQL();
        break;
    }
    s_log.info("Connessione al DB di tipo {}", tip);
    return conn;
  }
}

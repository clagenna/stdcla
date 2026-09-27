package sm.clagenna.stdcla.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StmtShowQueryContainer {

  private static final Logger s_log = LogManager.getLogger(StmtShowQueryContainer.class);

  private DBConn                                dbconn;
  private Map<PreparedStatement, StmtShowQuery> mapQueries;

  public StmtShowQueryContainer(DBConn dbc) {
    dbconn = dbc;
  }

  /**
   * Assegna la query (leggibile) del PreparedStatement al map delle query.
   * L'hascode del PreparedStatement viene usato come chiave per la mappa. La
   * query viene utilizzata per mostrare la query con i suoi relativi parametri
   * posizionali.
   *
   * @param p_stmt
   *          PreparedStatement da cui estrarre la query
   * @param p_szQry
   *          Query da assegnare (opzionale)
   * @return ID (hash code)
   */
  public void assign(PreparedStatement p_stmt, String p_szQry) {
    if ( !dbconn.isShowStatement())
      return;
    if (null == p_stmt) {
      s_log.error("assign() - PreparedStatement is null");
      return;
    }
    if (null == mapQueries)
      mapQueries = new HashMap<>();
    if (mapQueries.containsKey(p_stmt))
      return;
    StmtShowQuery ssq = new StmtShowQuery(p_szQry);
    mapQueries.put(p_stmt, ssq);
  }

  public void remove(PreparedStatement stmt) {
    if (stmt == null)
      return;
    if (mapQueries != null)
      mapQueries.remove(stmt);
  }

  public void setStmtParam(PreparedStatement p_stmt, int p_index, Object p_obj) throws SQLException {
    StmtShowQuery ssq = mapQueries.get(p_stmt);
    if (ssq == null) {
      s_log.warn("setStmtParam() - PreparedStatement not found in mapQueries");
      return;
    }
    ssq.assignParameter(p_index, p_obj);
  }

  public String toString(PreparedStatement p_stmt) {
    StmtShowQuery ssq = mapQueries.get(p_stmt);
    if (ssq == null) {
      String szMsg = "toString() - PreparedStatement not found in mapQueries";
      s_log.warn(szMsg);
      return szMsg;
    }
    return ssq.toString();
  }

}

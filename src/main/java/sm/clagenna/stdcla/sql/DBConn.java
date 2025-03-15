package sm.clagenna.stdcla.sql;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.utils.AppProperties;
import sm.clagenna.stdcla.utils.Utils;

public abstract class DBConn implements Closeable {

  private static final String QRY_PATT_VIEW = "SELECT * FROM %s WHERE 1=1";

  @Getter @Setter
  private String            host;
  @Getter @Setter
  private int               service;
  @Getter @Setter
  private String            dbname;
  @Getter @Setter
  private String            user;
  @Getter @Setter
  private String            passwd;
  @Getter
  private Connection        conn;
  private Savepoint         m_savePoint;
  private PreparedStatement stmtLastRowId;

  public DBConn() {
    //
  }

  public abstract Logger getLog();

  public abstract String getURL();

  public abstract EServerId getServerId();

  // public abstract int getLastIdentity() throws SQLException;

  public abstract void changePragma();

  public abstract String getQueryLastRowID();

  public abstract String getQueryListViews();

  /**
   * La funzione serve per suplire alla (pessima) caratteristica di SQLite3 che
   * <b>NON</b> ha il tipo dato "DATE"!
   *
   * @see <a href="https://sqlite.org/datatype3.html">SQLite data Types</a>
   *
   * @param p_stmt
   * @param p_index
   * @param p_dt
   * @throws SQLException
   */
  public abstract void setStmtInt(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtDate(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtDatetime(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtDouble(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtString(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract String addTopRecs(String qry, int qta);

  public Connection doConn() {
    String szUrl = getURL();
    try {
      changePragma();
      conn = DriverManager.getConnection(szUrl, user, passwd);
      EServerId id = getServerId();
      getLog().info("Connected DBType={}, DB name={}", id, getDbname());
    } catch (SQLException e) {
      getLog().error("Error in open connection:{}", e.getMessage(), e);
    }
    return conn;
  }

  /**
   * Trova il Last Row ID dell'ultimo record inserito
   */
  public int getLastIdentity() throws SQLException {
    if (getConn() == null)
      throw new SQLException("No connection yet");
    if (null == stmtLastRowId) {
      try {
        String szQry = getQueryLastRowID();
        stmtLastRowId = conn.prepareStatement(szQry);
      } catch (SQLException e) {
        getLog().error("Errore prep statement Last RowID with err={}", e.getMessage());
        return -1;
      }
    }
    int lastRowid = 0;
    try {
      ResultSet res = stmtLastRowId.executeQuery();
      while (res.next()) {
        lastRowid = res.getInt(1);
      }
    } catch (SQLException e) {
      getLog().error("Errore Last Row ID with err={}", e.getMessage());
    }
    return lastRowid;
  }

  /**
   * Ritorna una Map con tutte le views presenti nel DB
   *
   * @return
   */
  public Map<String, String> getListDBViews() {
    Connection conn = getConn();
    Map<String, String> liViews = new HashMap<>();

    String szQry = getQueryListViews();
    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(szQry)) {
      while (rs.next()) {
        String view = rs.getString(1);
        String qry = String.format(QRY_PATT_VIEW, view);
        liViews.put(view, qry);
      }
    } catch (SQLException e) {
      getLog().error("Query {}; err={}", szQry, e.getMessage(), e);
    }
    return liViews;
  }

  public void beginTrans() {
    try {
      conn.setAutoCommit(false);
      m_savePoint = conn.setSavepoint();
    } catch (SQLException e) {
      getLog().error("BEGIN TRAN Error {}", e.getMessage());
    }
  }

  public void commitTrans() {
    try {
      conn.setAutoCommit(true);
      m_savePoint = null;
    } catch (SQLException e) {
      getLog().error("COMMIT TRAN Error {}", e.getMessage());
    }
  }

  public void rollBackTrans() {
    try {
      conn.rollback(m_savePoint);
      m_savePoint = null;
    } catch (SQLException e) {
      getLog().error("BEGIN TRAN Error {}", e.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    try {
      if (null != stmtLastRowId)
        stmtLastRowId.close();
      stmtLastRowId = null;
      if (conn != null)
        conn.close();
      conn = null;
    } catch (SQLException e) {
      getLog().error("Error in close connection:{}", e.getMessage(), e);
    }
    conn = null;
  }

  public void readProperties(AppProperties p_props) {
    String szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_name);
    setDbname(szv);
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_Host);
    setHost(szv);
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_service);
    if (Utils.isValue(szv))
      setService(Integer.parseInt(szv));
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_user);
    setUser(szv);
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_passwd);
    setPasswd(szv);
  }

  public boolean testQuery(String szQry) {
    boolean bRet = false;
    if (null == szQry || szQry.length() < 3)
      return bRet;
    int n = szQry.toLowerCase().indexOf("order by");
    String szQry2 = n > 0 ? szQry.substring(0, n) : szQry;

    if (null == conn) {
      getLog().error("No connection to test: {}", szQry2);
      return bRet;
    }
    szQry2 = addTopRecs(szQry2, 1);
    try (PreparedStatement stmt = conn.prepareStatement(szQry2)) {
      try (ResultSet res = stmt.executeQuery()) {
        bRet = true;
      }
    } catch (Exception e) {
      getLog().error("Errore Query: {}", e.getMessage());
    }
    return bRet;
  }

}

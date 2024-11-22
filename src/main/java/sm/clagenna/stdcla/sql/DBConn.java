package sm.clagenna.stdcla.sql;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.utils.AppProperties;

public abstract class DBConn implements Closeable {

  @Getter @Setter
  private String     host;
  @Getter @Setter
  private int        service;
  @Getter @Setter
  private String     dbname;
  @Getter @Setter
  private String     user;
  @Getter @Setter
  private String     passwd;
  @Getter
  private Connection conn;

  public DBConn() {
    //
  }

  public abstract Logger getLog();

  public abstract String getURL();

  public abstract EServerId getServerId();

  public abstract int getLastIdentity() throws SQLException;

  public abstract void changePragma();

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
  public abstract void setStmtDate(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract void setStmtString(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException;

  public abstract Map<String, String> getListDBViews();

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

  @Override
  public void close() throws IOException {
    try {
      if (conn != null)
        conn.close();
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
    setService(Integer.parseInt(szv));
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_user);
    setUser(szv);
    szv = p_props.getProperty(AppProperties.CSZ_PROP_DB_passwd);
    setPasswd(szv);
  }

}

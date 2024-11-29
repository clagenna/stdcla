package sm.clagenna.stdcla.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Pragma;

import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.utils.Utils;

public class DBConnSQLite extends DBConn {
  private static final Logger s_log = LogManager.getLogger(DBConnSQLite.class);

  private static final String CSZ_URL = "jdbc:sqlite:%s";

  private static final String QRY_LASTID     = "select last_insert_rowid()";
  private static final String QRY_LIST_VIEWS = "SELECT name FROM sqlite_master WHERE type = 'view'";
  private static final String QRY_PATT_VIEW  = "SELECT * FROM %s WHERE 1=1";

  private PreparedStatement m_stmt_lastid;

  static {
    try {
      DriverManager.registerDriver(new org.sqlite.JDBC());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public DBConnSQLite() {
    //    if (s_inst != null)
    //      throw new UnsupportedOperationException("DBConn gia istanziata");
    //    s_inst = this;
  }

  public DBConnSQLite(String p_dbNam) {
    setDbname(p_dbNam);
  }

  @Override
  public String getURL() {
    String szUrl = String.format(CSZ_URL, getDbname());
    return szUrl;
  }

  @Override
  public EServerId getServerId() {
    return EServerId.SQLite;
  }

  @Override
  public int getLastIdentity() throws SQLException {
    if (getConn() == null)
      throw new SQLException("No connection yet");
    if (m_stmt_lastid == null)
      m_stmt_lastid = getConn().prepareStatement(QRY_LASTID);
    int retId = -1;
    try (ResultSet res = m_stmt_lastid.executeQuery()) {
      while (res.next()) {
        retId = res.getInt(1);
      }
    }
    return retId;
  }

  @Override
  public Map<String, String> getListDBViews() {
    Connection conn = getConn();
    Map<String, String> liViews = new HashMap<>();
    // liViews.put((String)null, null);
    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(QRY_LIST_VIEWS)) {
      while (rs.next()) {
        String view = rs.getString(1);
        String qry = String.format(QRY_PATT_VIEW, view);
        liViews.put(view, qry);
      }
    } catch (SQLException e) {
      s_log.error("Query {}; err={}", QRY_LIST_VIEWS, e.getMessage(), e);
    }
    return liViews;
  }

  @Override
  public void close() throws IOException {
    try {
      if (m_stmt_lastid != null)
        m_stmt_lastid.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    m_stmt_lastid = null;
    super.close();
  }

  @Override
  public void changePragma() {
    SQLiteConfig conf = new SQLiteConfig();
    Properties prop = conf.toProperties();
    prop.setProperty(Pragma.DATE_STRING_FORMAT.pragmaName, "yyyy-MM-dd");
  }

  /**
   * SQLite preferisce le date in
   * <a href="https://en.wikipedia.org/wiki/ISO_8601}">ISO 8601 Date Format</a>
   * e Data Type <code>String</code>. Per la discussione sul formato delle date
   * in SQLite3 <a href="https://github.com/xerial/sqlite-jdbc/issues/88">vedi
   * il sito GitHub</a> <br/>
   * per un elenco delle funzioni in SQLite
   * <a href="https://sqlite.org/lang_datefunc.html">vedere sito SQLite</a><br/>
   *
   * @param p_stmt
   *          lo statement SQl su cui applicare il valore
   * @param p_index
   *          index della colonna nello statement
   * @param p_dt
   *          il valore da settare
   *
   * @see <a href="https://en.wikipedia.org/wiki/ISO_8601}">ISO 8601 Date
   *      Format</a>
   * @see <a href="https://sqlite.org/datatype3.html">SQLite data Types</a>
   */
  @Override
  public void setStmtDate(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    java.sql.Date dt = null;
    if (p_dt instanceof java.sql.Date) {
      dt = (java.sql.Date) p_dt;
    } else if (p_dt instanceof java.util.Date) {
      java.util.Date udt = (java.util.Date) p_dt;
      dt = new java.sql.Date(udt.getTime());
    } else if (p_dt instanceof LocalDate ldt) {
      java.util.Date udt = java.util.Date.from(ldt.atStartOfDay(ZoneId.systemDefault()).toInstant());
      dt = new java.sql.Date(udt.getTime());
    } else if (p_dt instanceof LocalDateTime ldt) {
      ZonedDateTime zo = ldt.atZone(ZoneId.systemDefault());
      java.util.Date udt = java.util.Date.from(zo.toInstant());
      dt = new java.sql.Date(udt.getTime());
    }
    try {
      if (dt != null) {
        String sz = Utils.s_fmtY4MD.format(dt);
        p_stmt.setString(p_index, sz);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setStmtInt(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    Integer iv = null;
    if (p_dt instanceof Integer ii) {
      iv = ii;
    } else if (p_dt instanceof Short ii) {
      iv = ii.intValue();

    } else if (p_dt instanceof Long ii) {
      iv = ii.intValue();
    }
    try {
      if (iv != null) {
        p_stmt.setInt(p_index, iv);
      } else
        p_stmt.setNull(p_index, Types.INTEGER);
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setStmtDatetime(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    // String sz = Utils.s_fmtY4MD.format(p_dt);
    // String sz = Utils.s_fmtY4MDHMS.format(p_dt);
    // p_stmt.setString(p_index, sz);
    java.sql.Timestamp dt = null;
    if (p_dt instanceof java.sql.Date pdt) {
      dt = new Timestamp(pdt.getTime());
    } else if (p_dt instanceof java.util.Date pdt) {
      dt = new java.sql.Timestamp(pdt.getTime());
    } else if (p_dt instanceof LocalDate ldt) {
      java.util.Date udt = java.util.Date.from(ldt.atStartOfDay(ZoneId.systemDefault()).toInstant());
      dt = new java.sql.Timestamp(udt.getTime());
    } else if (p_dt instanceof LocalDateTime ldt) {
      ZonedDateTime zo = ldt.atZone(ZoneId.systemDefault());
      java.util.Date udt = java.util.Date.from(zo.toInstant());
      dt = new java.sql.Timestamp(udt.getTime());
    }
    try {
      if (dt != null) {
        String sz = Utils.s_fmtY4MDHMS.format(dt);
        p_stmt.setString(p_index, sz);
      } else
        p_stmt.setNull(p_index, Types.VARCHAR);
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    p_stmt.setDouble(p_index, (Double) p_dt);
  }

  @Override
  public void setStmtString(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    p_stmt.setString(p_index, (String) p_dt);
  }

  @Override
  public Logger getLog() {
    return s_log;
  }

}

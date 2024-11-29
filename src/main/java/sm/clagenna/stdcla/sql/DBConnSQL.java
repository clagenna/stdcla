package sm.clagenna.stdcla.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.enums.EServerId;

public class DBConnSQL extends DBConn {
  private static final Logger s_log = LogManager.getLogger(DBConnSQL.class);

  @SuppressWarnings("unused")
  private static final String CSZ_DRIVER     = "com.mysql.cj.jdbc.Driver";
  private static final String CSZ_URL        = "jdbc:sqlserver://%s:%d;"                  //
      + "database=%s;"                                                                    //
      + "user=%s;"                                                                        //
      + "password=%s;"                                                                    //
      + "encrypt=false;"                                                                  //
      + "trustServerCertificate=false;"                                                   //
      + "loginTimeout=10;";
  private static final String QRY_LASTID     = "select @@identity";
  private static final String QRY_LIST_VIEWS = "SELECT name FROM sys.views ORDER BY name";
  private static final String QRY_PATT_VIEW  = "SELECT * FROM %s WHERE 1=1";

  private PreparedStatement m_stmt_lastid;

  public DBConnSQL() {
    //
  }

  @Override
  public String getURL() {
    String szUrl = String.format(CSZ_URL, "localhost", //
        getService(), //
        getDbname(), //
        getUser(), //
        getPasswd());
    return szUrl;
  }

  @Override
  public EServerId getServerId() {
    return EServerId.SqlServer;
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
    // per compensare al SQLite pragma date 'yyyy-MM-dd'
  }

   /**
   * SQL Server gestisce le date come java.sql.Date
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
    if (dt != null)
      p_stmt.setDate(p_index, dt);
    else
      p_stmt.setNull(p_index, Types.DATE);
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
    java.sql.Timestamp dt = null;
    if (p_dt instanceof java.sql.Timestamp pdt) {
      dt = pdt;
    } else if (p_dt instanceof java.util.Date udt) {
      dt = new java.sql.Timestamp(udt.getTime());
    } else if (p_dt instanceof LocalDate ldt) {
      java.util.Date udt = java.util.Date.from(ldt.atStartOfDay(ZoneId.systemDefault()).toInstant());
      dt = new java.sql.Timestamp(udt.getTime());
    } else if (p_dt instanceof LocalDateTime ldt) {
      ZonedDateTime zo = ldt.atZone(ZoneId.systemDefault());
      java.util.Date udt = java.util.Date.from(zo.toInstant());
      dt = new java.sql.Timestamp(udt.getTime());
    }
    if (dt != null)
      p_stmt.setTimestamp(p_index, dt);
    else
      p_stmt.setNull(p_index, Types.TIMESTAMP);
  }

  @Override
  public void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    BigDecimal bd = null;
    if (p_dt instanceof Double dbl) {
      bd = BigDecimal.valueOf(dbl);
    } else if (p_dt instanceof Integer ii) {
      bd = BigDecimal.valueOf(ii);
    }
    if (bd != null)
      p_stmt.setBigDecimal(p_index, bd);
    else
      p_stmt.setNull(p_index, Types.DECIMAL);
  }

  @Override
  public void setStmtString(PreparedStatement p_stmt, int p_index, Object p_sz) throws SQLException {
    String sz = null;
    if (null != p_sz)
      sz = (String) p_sz;
    if (null != sz)
      p_stmt.setString(p_index, sz);
    else
      p_stmt.setNull(p_index, Types.VARCHAR);
  }

  @Override
  public Logger getLog() {
    return s_log;
  }

}

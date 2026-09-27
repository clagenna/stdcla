package sm.clagenna.stdcla.sql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  public DBConnSQL() {
    super();
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
  public void setServerId(EServerId id) {
    // nothing
  }

  @Override
  public EServerId getServerId() {
    return EServerId.SqlServer;
  }

  @Override
  public String getQueryLastRowID() {
    return QRY_LASTID;
  }

  @Override
  public String getQueryListViews() {
    return QRY_LIST_VIEWS;
  }

  @Override
  public void changePragma() {
    // per compensare al SQLite pragma date 'yyyy-MM-dd'
  }

  @Override
  public void setStmtInt(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    Integer iv = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_dt);
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
    java.sql.Timestamp dt = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_dt);
    try {
      if (p_dt instanceof java.sql.Date dt1) {
        dt = new java.sql.Timestamp(dt1.getTime());
      } else if (p_dt instanceof java.util.Date dt1) {
        dt = new java.sql.Timestamp(dt1.getTime());
      } else if (p_dt instanceof LocalDate ldt) {
        java.util.Date udt = java.util.Date.from(ldt.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dt = new java.sql.Timestamp(udt.getTime());
      } else if (p_dt instanceof LocalDateTime ldt) {
        //        ZonedDateTime zo = ldt.atZone(ZoneId.systemDefault());
        //        java.util.Date udt = java.util.Date.from(zo.toInstant());
        //        dt = new java.sql.Timestamp(udt.getTime());
        dt = Timestamp.valueOf(ldt);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (dt != null)
      p_stmt.setTimestamp(p_index, dt);
    else
      p_stmt.setNull(p_index, Types.DATE);
  }

  @Override
  public void setStmtDatetime(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    java.sql.Timestamp dt = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_dt);

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
  public LocalDateTime getStmtDatetime(ResultSet p_res, int p_index) throws SQLException {
    Timestamp ts = p_res.getTimestamp(p_index);
    if (ts != null)
      return ts.toLocalDateTime();
    return null;
  }

  @Override
  public LocalDateTime getStmtDatetime(ResultSet p_res, String szColNam) throws SQLException {
    Timestamp ts = p_res.getTimestamp(szColNam);
    if (ts != null)
      return ts.toLocalDateTime();
    return null;
  }

  @Override
  public void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    BigDecimal bd = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_dt);

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
  public BigDecimal getStmtImporto(ResultSet p_res, int p_index) throws SQLException {
    BigDecimal bd = p_res.getBigDecimal(p_index);
    return bd;
  }

  @Override
  public BigDecimal getStmtImporto(ResultSet p_res, String p_colNam) throws SQLException {
    BigDecimal bd = p_res.getBigDecimal(p_colNam);
    return bd;
  }

  @Override
  public Double getStmtDouble(ResultSet p_res, int p_index) throws SQLException {
    Double bd = p_res.getDouble(p_index);
    return bd;
  }

  @Override
  public Double getStmtDouble(ResultSet p_res, String pColNam) throws SQLException {
    Double bd = p_res.getDouble(pColNam);
    return bd;
  }

  @Override
  public void setStmtDouble(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    Double bd = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_dt);

    if (p_dt instanceof Double dbl) {
      bd = dbl;
    } else if (p_dt instanceof Integer ii) {
      bd = Double.valueOf(ii);
    }
    if (bd != null)
      p_stmt.setDouble(p_index, bd);
    else
      p_stmt.setNull(p_index, Types.DOUBLE);
  }

  @Override
  public void setStmtString(PreparedStatement p_stmt, int p_index, Object p_sz) throws SQLException {
    String sz = null;
    if ( isShowStatement())
      setStmtParam(p_stmt, p_index, p_sz);
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

  @Override
  public String addTopRecs(String qry, int qta) {
    final String str = "select";
    int n = qry.toLowerCase().indexOf(str);
    if (n < 0)
      return qry;
    n += str.length();
    StringBuilder sb = new StringBuilder();
    sb.append(qry.substring(0, n));
    sb.append(" top ").append(qta);
    sb.append(qry.substring(n));
    return sb.toString();
  }

  //  public String toString(PreparedStatement stmt) {
  //    if ( !isShowStatement())
  //      return "no show statement!";
  //    StringBuilder sb = new StringBuilder(stmt.toString());
  //    if ( sb.indexOf(": null") > 0 )
  //      sb = new StringBuilder(sho);
  //    int nPos = 1;
  //    int k = 0;
  //    while (nPos > 0) {
  //      String szPh = String.format("@P%d", k++);
  //      nPos = sb.indexOf(szPh);
  //      if (nPos > 0) {
  //        String szVal = getStmtShowParameter(k);
  //        int nPos2 = nPos + szPh.length();
  //        sb.replace(nPos, nPos2, szVal);
  //      }
  //    }
  //    return sb.toString();
  //  }

}

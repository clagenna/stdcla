package sm.clagenna.stdcla.sql;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Pragma;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class DBConnSQLite extends DBConn {
  private static final Logger s_log = LogManager.getLogger(DBConnSQLite.class);

  private static final String CSZ_URL = "jdbc:sqlite:%s";

  private static final String QRY_LASTID     = "select last_insert_rowid()";
  private static final String QRY_LIST_VIEWS = "SELECT name FROM sqlite_master WHERE type = 'view'";
  @Getter @Setter
  private static boolean      testExistsDB;

  static {
    try {
      DriverManager.registerDriver(new org.sqlite.JDBC());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    testExistsDB = true;
  }

  public DBConnSQLite() {
    super();
  }

  @Override
  public String getQueryListViews() {
    return QRY_LIST_VIEWS;
  }

  public DBConnSQLite(String p_dbNam) {
    setDbname(p_dbNam);
  }

  @Override
  public String getURL() {
    if (DBConnSQLite.isTestExistsDB()) {
      if ( !Files.exists(Paths.get(getDbname()), LinkOption.NOFOLLOW_LINKS)) {
        getLog().error("Il DB SQLite \"{}\" *NON* esiste !", getDbname());
        throw new UnsupportedOperationException("Non esiste il DB SQLite " + getDbname());
      }
    }
    String szUrl = String.format(CSZ_URL, getDbname());
    return szUrl;
  }

  @Override
  public EServerId getServerId() {
    return EServerId.SQLite;
  }

  @Override
  public void setServerId(EServerId id) {
    // nothing
  }

  @Override
  public String getQueryLastRowID() {
    return QRY_LASTID;
  }

  @Override
  public void changePragma() {
    SQLiteConfig conf = new SQLiteConfig();
    Properties prop = conf.toProperties();
    prop.setProperty(Pragma.DATE_STRING_FORMAT.pragmaName, "yyyy-MM-dd");
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
    try {
      if (p_dt instanceof java.sql.Date) {
        dt = (java.sql.Date) p_dt;
      } else if (p_dt instanceof java.util.Date) {
        java.util.Date udt = (java.util.Date) p_dt;
        dt = new java.sql.Date(udt.getTime());
      } else if (p_dt instanceof LocalDate ldt) {
        java.util.Date udt = java.util.Date.from(ldt.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dt = new java.sql.Date(udt.getTime());
      } else if (p_dt instanceof LocalDateTime ldt) {
        if (ldt.equals(LocalDateTime.MIN) || ldt.equals(LocalDateTime.MAX)) {
          s_log.warn("Date Time near MIN/MAX, ignored!");
        } else {
          ZonedDateTime zo = ldt.atZone(ZoneId.systemDefault());
          java.util.Date udt = java.util.Date.from(zo.toInstant());
          dt = new java.sql.Date(udt.getTime());
        }
      }
    } catch (Exception e) {
      s_log.error("SQLite.setStmtDate error:", e.getMessage());
    }
    try {
      if (dt != null) {
        String sz = Utils.s_fmtY4MDHMS.format(dt);
        p_stmt.setString(p_index, sz);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setStmtDatetime(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
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
  public LocalDateTime getStmtDatetime(ResultSet p_res, int p_index) throws SQLException {
    String sz = p_res.getString(p_index);
    if (sz == null || sz.isEmpty())
      return null;
    LocalDateTime dt = ParseData.parseData(sz);
    return dt;
  }

  @Override
  public LocalDateTime getStmtDatetime(ResultSet p_res, String szColNam) throws SQLException {
    String sz = p_res.getString(szColNam);
    if (sz == null || sz.isEmpty())
      return null;
    try {
      Long ll = Long.parseLong(sz);
      Date dt = new Date(ll);
      return dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } catch (Exception e) {
      // nothing
    }
    if (sz == null || sz.isEmpty())
      return null;
    LocalDateTime dt = ParseData.parseData(sz);
    return dt;
  }

  @Override
  public BigDecimal getStmtImporto(ResultSet p_res, int p_index) throws SQLException {
    Double d = p_res.getDouble(p_index);
    if (p_res.wasNull())
      return null;
    BigDecimal bd = BigDecimal.valueOf(d);
    bd.setScale(2, RoundingMode.HALF_UP);
    return bd;
  }

  @Override
  public BigDecimal getStmtImporto(ResultSet p_res, String pColNam) throws SQLException {
    Double d = p_res.getDouble(pColNam);
    if (p_res.wasNull())
      return null;
    BigDecimal bd = BigDecimal.valueOf(d);
    bd.setScale(2, RoundingMode.HALF_UP);
    return bd;
  }

  @Override
  public void setStmtImporto(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    p_stmt.setDouble(p_index, (Double) p_dt);
  }

  @Override
  public void setStmtDouble(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    p_stmt.setDouble(p_index, (Double) p_dt);
  }

  @Override
  public Double getStmtDouble(ResultSet p_res, int p_index) throws SQLException {
    Double d = p_res.getDouble(p_index);
    return d;
  }

  @Override
  public Double getStmtDouble(ResultSet p_res, String pColNam) throws SQLException {
    Double d = p_res.getDouble(pColNam);
    return d;
  }

  @Override
  public void setStmtString(PreparedStatement p_stmt, int p_index, Object p_dt) throws SQLException {
    p_stmt.setString(p_index, (String) p_dt);
  }

  @Override
  public Logger getLog() {
    return s_log;
  }

  @Override
  public String addTopRecs(String qry, int qta) {
    return qry + " limit " + qta;
  }

}

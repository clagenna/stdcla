package sm.clagenna.stdcla.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class DtsCols implements Cloneable {
  private static final Logger s_log    = LogManager.getLogger(DtsCols.class);
  @Getter
  private static int          colWidth = 16;
  @Getter
  private static String       colFmtL;
  @Getter
  private static String       colFmtR;
  @Getter
  private static String       colFmtDbl;
  @Getter
  private static String       colFmt;

  //   @SuppressWarnings("unused") private Dataset dtset;

  @Getter
  private List<DtsCol>        columns;
  private Map<String, DtsCol> nomecol;
  private StringBuilder       sbIntesta;

  static {
    DtsCols.setWidthCh(colWidth);
  }

  private DtsCols() {
    init();
  }

  public DtsCols(Dataset p_dt) {
    //    dtset = p_dt;
    init();
  }

  private void init() {
    columns = new ArrayList<>();
    nomecol = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  public static void setWidthCh(int p_coWth) {
    colWidth = p_coWth;
    colFmtL = String.format("%%-%ds ", p_coWth);
    colFmtR = String.format("%%%ds ", p_coWth);
    colFmtDbl = "%.2f";
    colFmt = colFmtL;
  }

  public int parseColsStatement(PreparedStatement p_stmt) throws DatasetException {
    String szVirg = "";
    // int decplace = 6;
    sbIntesta = new StringBuilder();
    ResultSetMetaData rsmd = null;
    try {
      rsmd = p_stmt.getMetaData();
    } catch (SQLException e) {
      s_log.error("Errore in getMetaData(), msg={}", e.getMessage());
      throw new DatasetException("Errore in getMetaData()", e);
    }

    int colCount = -1;
    try {
      colCount = rsmd.getColumnCount();
    } catch (SQLException e) {
      s_log.error("Errore in getColumnCount(), msg={}", e.getMessage());
      throw new DatasetException("Errore in getColumnCount()", e);
    }

    try {
      for (int i = 1; i <= colCount; i++) {
        String szNam = rsmd.getColumnName(i).trim();
        int nJavaSqlTyp = rsmd.getColumnType(i);
        SqlTypes mySqlType = DtsCol.decoType(nJavaSqlTyp);
        String szFmt = DtsCol.getSzFmt();
        DtsCol col = new DtsCol();
        col.setName(szNam);
        col.setType(mySqlType);
        col.setIndex(i - 1);
        col.setFormat(szFmt);
        addCol(col);
        // System.out.printf("%16s{%s}=%s\tcol=%s\n", szNam, szTyp, szFmt, col.toString());
        sbIntesta.append(szVirg).append(szNam);
        // sbFmt.append(szVirg).append(szFmt);
        szVirg = szVirg.length() == 0 ? "\t" : szVirg;
      }
    } catch (SQLException e) {
      s_log.error("Errore in getColumnType(), msg={}", e.getMessage());
      throw new DatasetException("Errore in getColumnType()", e);
    }
    return columns.size();
  }

  public void creaCols(Map<String, SqlTypes> p_map) {
    for (String k : p_map.keySet()) {
      addCol(k, p_map.get(k));
    }
  }

  public void addCol(String szNam, SqlTypes p_ty) {
    DtsCol col = new DtsCol();
    col.setName(szNam);
    col.setType(p_ty);
    addCol(col);
  }

  public void addCol(DtsCol p_col) {
    if (p_col == null)
      return;
    // zero-based index
    p_col.setIndex(columns.size());
    columns.add(p_col);
    nomecol.put(p_col.getName(), p_col);
  }

  public DtsCol getCol(String p_nam) {
    return nomecol.get(p_nam);
  }

  public DtsCol getCol(int p_i) {
    if (null == columns || columns.size() <= p_i)
      throw new UnsupportedOperationException("Col non esiste:" + p_i);
    return columns.get(p_i);
  }

  public int size() {
    if (columns == null || columns.size() == 0)
      throw new UnsupportedOperationException("No colums present");
    return columns.size();
  }

  public int getColIndex(String p_nam) {
    DtsCol rr = nomecol.get(p_nam);
    if (null == rr)
      return -1;
    return rr.getIndex();
  }

  public String csvIntestazione(String p_sep) {
    String szRet = columns.stream().map(s -> s.getName()).collect(Collectors.joining(p_sep));
    return szRet;
  }

  public String getIntestazione() {
    StringBuilder sb = new StringBuilder();
    for (DtsCol col : columns) {
      String sz = String.format(colFmt, col.getName());
      switch (col.getType()) {
        case BIGINT:
        case NUMERIC:
        case DECIMAL:
        case INTEGER:
        case SMALLINT:
        case TINYINT:
        case DOUBLE:
        case FLOAT:
        case REAL:
          sz = String.format(colFmtR, col.getName());
          break;
        default:
          sz = String.format(colFmt, col.getName());
          break;
      }
      sb.append(sz);
    }
    return sb.toString();
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    DtsCols ret = new DtsCols();
    for (DtsCol col : columns) {
      ret.addCol(col);
    }
    return ret;
  }

  @Override
  public String toString() {
    if (null == columns)
      return "**NO COLS**";
    StringBuilder sb = new StringBuilder();
    for (DtsCol col : columns)
      sb.append(col.toString()).append("\n");
    return sb.toString();
    // return getIntestazione();
  }
}

package sm.clagenna.stdcla.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sys.ex.DatasetException;


public class DtsCols {
  private static final Logger s_log    = LogManager.getLogger(DtsCols.class);
  @Getter
  private static int          colWidth = 16;
  @Getter
  private static String       colFmtL;
  @Getter
  private static String       colFmtR;

  class DtsCol {
    /** Il nome <b>univoco</b> e <b>case insensitive</b> della colonna */
    @Getter @Setter
    private String   name;
    /** la posizione 0-based nel dataset */
    @Getter @Setter
    private int      index;
    @Getter @Setter
    private SqlTypes type;
    @Getter @Setter
    private String   format;
    @Getter
    private boolean  inferredDate;

    public DtsCol() {
      index = -1;
    }

    public void setInferredDate(boolean bv) {
      // la prima data valida inferisce
      inferredDate |= bv;
    }

    @Override
    public String toString() {
      String szRet = "col:";
      szRet += name != null ? name : "??name??";
      szRet += index >= 0 ? String.format("(%d)", index) : "(?)";
      szRet += type != null ? "[" + type + "]" : "[??type??]";
      return szRet;
    }
  }

  @SuppressWarnings("unused")
  private Dataset             dtset;

  @Getter
  private List<DtsCol>        columns;
  private Map<String, DtsCol> nomecol;
  private StringBuilder       sbIntesta;

  static {
    DtsCols.setWidthCh(colWidth);
  }

  public DtsCols(Dataset p_dt) {
    dtset = p_dt;
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
  }

  public int parseColsStatement(PreparedStatement p_stmt) throws DatasetException {
    String szVirg = "";
    int decplace = 6;
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
        int nTyp = rsmd.getColumnType(i);
        String szTyp = "";
        String szFmt = "";
        DtsCol col = new DtsCol();
        switch (nTyp) {
          case Types.SMALLINT:
            szFmt = String.format("%%%dd ", colWidth);
            szTyp = "INTEGER";
            break;
          case Types.INTEGER:
            szFmt = String.format("%%%dd ", colWidth);
            szTyp = "INTEGER";
            break;
          case Types.VARCHAR:
            szFmt = String.format("%%-%ds ", colWidth);
            szTyp = "VARCHAR";
            break;
          case Types.NUMERIC:
            szFmt = String.format("%%-%ds ", colWidth);
            szTyp = "NUMERIC";
            break;
          case Types.DECIMAL:
            szFmt = String.format("%%%d.%df ", colWidth, decplace);
            szTyp = "DECIMAL";
            break;
          case Types.FLOAT:
            szFmt = String.format("%%%d.%df ", colWidth, decplace);
            szTyp = "FLOAT";
            break;
          case Types.DOUBLE:
            szFmt = String.format("%%%d.%df ", colWidth, decplace);
            szTyp = "DOUBLE";
            break;
          case Types.REAL:
            szFmt = String.format("%%%d.%df ", colWidth, decplace);
            szTyp = "REAL";
            break;
          case Types.DATE:
            szFmt = String.format("%%%ds ", colWidth);
            szTyp = "DATE";
            break;
          default:
            System.err.printf("Non interpreto tipo %d per col %s\n", nTyp, szNam);
            break;
        }
        col.setName(szNam);
        col.setType(SqlTypes.decode(nTyp));
        col.setIndex(i - 1);
        col.setFormat(szFmt);
        addCol(col);
        System.out.printf("%16s{%s}=%s\tcol=%s\n", szNam, szTyp, szFmt, col.toString());
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

  public void addCol(String szNam, SqlTypes p_ty) {
    DtsCol col = new DtsCol();
    col.setName(szNam);
    col.setType(p_ty);
    addCol(col);
  }

  public void addCol(DtsCol p_col) {
    if (p_col == null)
      return;
    p_col.setIndex(columns.size());
    columns.add(p_col);
    nomecol.put(p_col.getName(), p_col);
  }

  public DtsCol getCol(String p_nam) {
    return nomecol.get(p_nam);
  }

  public int size() throws DatasetException {
    if (columns == null || columns.size() == 0)
      throw new DatasetException("No colums present");
    return columns.size();
  }

  public int getColIndex(String p_nam) {
    return nomecol.get(p_nam).getIndex();
  }

  public String getIntestazione() {
    StringBuilder sb = new StringBuilder();
    for (DtsCol col : columns) {
      sb.append(String.format(colFmtR, col.getName()));
    }
    return sb.toString();
  }
}

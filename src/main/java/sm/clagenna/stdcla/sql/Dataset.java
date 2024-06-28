package sm.clagenna.stdcla.sql;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class Dataset implements Closeable {
  private static final Logger       s_log = LogManager.getLogger(Dataset.class);
  @Getter @Setter private EServerId tipoServer;
  private DtsCols                   columns;
  @Getter @Setter private DBConn    db;
  @Getter private List<DtsRow>      righe;

  public Dataset() {
    setTipoServer(EServerId.SqlServer);
  }

  public Dataset(EServerId p_serverId) {
    setTipoServer(p_serverId);
  }

  public Dataset(DBConn p_con) {
    setDb(p_con);
    setTipoServer(p_con.getServerId());
  }

  public boolean executeQuery(String p_qry) {
    boolean bRet = false;
    Connection conn = db.getConn();
    try (PreparedStatement stmt = conn.prepareStatement(p_qry); //
        ResultSet res = stmt.executeQuery()) {
      creaCols(stmt);
      addRows(res);
      bRet = true;
    } catch (SQLException | DatasetException e) {
      s_log.error("Error execute Query, err={}", e.getMessage());
    }
    return bRet;
  }

  public void creaCols(PreparedStatement p_stmt) throws DatasetException {
    columns = new DtsCols(this);
    columns.parseColsStatement(p_stmt);
  }

  public void creaCols(Map<String, SqlTypes> p_map) {
    columns = new DtsCols(this);
    columns.creaCols(p_map);
  }

  private void creaCols(DtsCols p_columns) {
    try {
      columns = (DtsCols) p_columns.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

  public int addRows(ResultSet p_stmt) throws DatasetException {
    int nRet = -1;
    try {
      while (p_stmt.next()) {
        DtsRow row = new DtsRow(this);
        row.addRow(p_stmt);
        if (s_log.isTraceEnabled()) {
          System.out.printf("%s\n%s\n", columns.getIntestazione(), row.toString());
        }
        addRow(row);
      }
      if (null != righe)
        nRet = righe.size();
    } catch (SQLException e) {
      s_log.error("Errore in addRow(resultset), msg={}", e.getMessage());
      throw new DatasetException("Errore in addRow(resultset)", e);
    }
    return nRet;
  }

  public int addRow(DtsRow p_r) {
    if (righe == null)
      righe = new ArrayList<>();
    righe.add(p_r);
    return righe.size();
  }

  public int addRow(List<Object> p_lio) throws DatasetException {
    DtsRow row = new DtsRow(this);
    row.addRow(p_lio);
    addRow(row);
    return righe.size();
  }

  public int getQtaCols() throws DatasetException {
    return columns.size();
  }

  public DtsCols getColumns() {
    return columns;
  }

  public int getColumNo(String p_nam) {
    return columns.getColIndex(p_nam);
  }

  public List<Object> colArray(String p_colNam) {
    List<Object> li = new ArrayList<>();
    int ii = columns.getColIndex(p_colNam);
    for (DtsRow row : righe) {
      li.add(row.get(ii));
    }
    return li;
  }

  public Dataset convert(String p_col, IDtsConvCol<Object> p_conv) {
    Dataset ret = new Dataset();
    ret.creaCols(columns);
    SqlTypes newty = null;
    try {
      for (DtsRow row : righe) {
        DtsRow r = (DtsRow) row.clone();
        Object vv = r.get(p_col);
        Object cnv = p_conv.converti(vv);
        if (null == newty) {
          newty = SqlTypes.translate(cnv);
          DtsCol co = ret.columns.getCol(p_col);
          co.setType(newty);
        }
        r.set(p_col, cnv);
        ret.addRow(r);
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return ret;
  }

  public double mean(String p_colNam) throws DatasetException {
    DtsCol col = columns.getCol(p_colNam);
    if (null == col)
      throw new DatasetException("no col:" + p_colNam);
    if (0 == size())
      throw new DatasetException("mean on no data");
    SqlTypes typ = col.getType();
    switch (typ) {
      case TINYINT:
      case SMALLINT:
      case INTEGER:
      case NUMERIC:
      case DECIMAL:
      case FLOAT:
      case DOUBLE:
        break;
      default:
        throw new DatasetException("Mean on not numeric:"+p_colNam);
    }
    List<Object> colv = colArray(p_colNam);
    double somma = colv //
        .stream() //
        .mapToDouble(s -> Double.parseDouble(s.toString())) //
        .sum();
    return somma / (double) size();
  }

  public int size() {
    if (null == righe)
      return 0;
    return righe.size();
  }

  @Override
  public void close() throws IOException {
    //
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(columns.toString()).append("\n");
    for (DtsRow row : righe)
      sb.append(row.toString()).append("\n");
    return sb.toString();
  }

  public List<Map<String, Object>> toList() {
    List<Map<String, Object>> lim = new ArrayList<Map<String, Object>>();
    for (DtsRow row : righe) {
      Map<String, Object> mp = row.toMap();
      lim.add(mp);
    }
    return lim;
  }

}

package sm.clagenna.stdcla.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.sql.DtsCols.DtsCol;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class Dataset {
  private static final Logger s_log = LogManager.getLogger(Dataset.class);
  @Getter @Setter
  private EServerId           tipoServer;
  private DtsCols             columns;
  @Getter @Setter
  private DBConn              db;

  @Getter
  private List<DtsRow>        righe;

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

  public int getQtaCols() throws DatasetException {
    return columns.size();
  }

  public List<DtsCol> getColumns() {
    return columns.getColumns();
  }

  public int getColumNo(String p_nam) {
    return columns.getColIndex(p_nam);
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

  public void creaCols(PreparedStatement p_stmt) throws DatasetException {
    columns = new DtsCols(this);
    columns.parseColsStatement(p_stmt);
  }

}

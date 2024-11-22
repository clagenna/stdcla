package sm.clagenna.stdcla.javafx;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sql.DBConn;
import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sql.DtsCol;
import sm.clagenna.stdcla.sql.DtsCols;
import sm.clagenna.stdcla.sql.DtsRow;

public class TableViewFiller extends Task<String> {
  private static final Logger s_log       = LogManager.getLogger(TableViewFiller.class);
  private static String       CSZ_NULLVAL = "**null**";

  @Getter @Setter
  private String                  szQry;
  @Getter @Setter
  private TableView<List<Object>> tableview;
  private DBConn                  m_db;
  private Dataset                 m_dts;

  public TableViewFiller(TableView<List<Object>> tblview, DBConn p_conn) {
    setTableview(tblview);
    m_db = p_conn;
  }

  public static void setNullRetValue(String vv) {
    CSZ_NULLVAL = vv;
  }

  @Override
  protected String call() throws Exception {
    s_log.debug("Start creazione Table View con i dati...");
    openDataSet();
    if (null == m_dts) {
      s_log.warn("Nulla da mostrare sulla tabella");
      return ".. nulla da mostrare";
    }
    // creato semaforo altrimenti la "runlater" parte dopo la fillTableView
    Semaphore semaf = new Semaphore(0);
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        creaTableView(m_dts);
        semaf.release();
      }
    });
    semaf.acquire();
    fillTableView();
    return "..Finito!";
  }
  //
  //  private TableView<List<Object>> openQuery(String szQryFltr) {
  //    szQry = szQryFltr;
  //    openDataSet();
  //    if (null == m_dts) {
  //      s_log.warn("Nulla da mostrare sulla tabella");
  //      return tableview;
  //    }
  //    creaTableView(m_dts);
  //    fillTableView();
  //    return tableview;
  //  }

  public Dataset getDataset() {
    return m_dts;
  }

  private Dataset openDataSet() {
    m_dts = null;
    s_log.debug("Lancio query:{}", szQry);
    try (Dataset dtset = new Dataset(m_db)) {
      if ( !dtset.executeQuery(szQry)) {
        s_log.error("Lettura andata male !");
      } else
        m_dts = dtset;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return m_dts;
  }

  private void creaTableView(Dataset p_dts) {
    DtsCols cols = p_dts.getColumns();
    tableview.getItems().clear();
    tableview.getColumns().clear();
    int k = 0;
    for (DtsCol col : cols.getColumns()) {
      final int j = k++;
      String szColNam = col.getName();
      String cssAlign = "-fx-alignment: center-left;";
      switch (col.getType()) {
        case BIGINT:
        case DATE:
        case DOUBLE:
        case DECIMAL:
        case FLOAT:
        case INTEGER:
        case NUMERIC:
        case REAL:
          cssAlign = "-fx-alignment: center-right;";
          break;
        default:
          break;
      }
      TableColumn<List<Object>, Object> tbcol = new TableColumn<>(szColNam);
      tbcol.setCellValueFactory(param -> {
        SimpleObjectProperty<Object> cel = new SimpleObjectProperty<Object>(formattaCella(param.getValue().get(j)));
        return cel;
      });

      tbcol.setStyle(cssAlign);
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          tableview.getColumns().add(tbcol);
        }
      });
    }
  }

  private void fillTableView() {
    // System.out.println("TableViewFiller.fillTableView()");
    ObservableList<List<Object>> dati = FXCollections.observableArrayList();
    List<DtsRow> righe = m_dts.getRighe();
    if (righe == null) {
      s_log.info("Nessuna informazione da mostrare");
      return;
    }
    for (DtsRow riga : m_dts.getRighe()) {
      ObservableList<Object> tbRiga = FXCollections.observableArrayList();
      tbRiga.addAll(riga.getValues(true));
      dati.add(tbRiga);
    }
    tableview.setItems(dati);
  }

  private Object formattaCella(Object p_o) {
    if (p_o == null)
      return CSZ_NULLVAL;
    String szCls = p_o.getClass().getSimpleName();
    switch (szCls) {
      case "String":
        return p_o;
      case "Integer":
        if ( ((Integer) p_o) == 0)
          return "";
        return p_o;
      case "Float":
        if ( ((Float) p_o) == 0)
          return "";
        return p_o;
      case "Double":
        if ( ((Double) p_o) == 0)
          return "";
        return p_o;
    }
    return p_o;
  }

}

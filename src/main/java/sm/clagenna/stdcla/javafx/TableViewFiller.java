package sm.clagenna.stdcla.javafx;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
  private static final Logger s_log           = LogManager.getLogger(TableViewFiller.class);
  private static String       CSZ_NULLVAL     = "**null**";
  private static String       QRY_WHE_NOTRASF = "AND abicaus not in ('45','S3','S4') AND descr NOT LIKE '%wise%'";

  private static DecimalFormat fmtDbl;

  //  @Getter @Setter
  //  private ResultView resView;
  @Getter @Setter
  private String szQry;
  //  @Getter @Setter
  //  private boolean                 fltrParolaRegEx;
  @Getter @Setter
  private String                  fltrParola;
  @Getter @Setter
  private TableView<List<Object>> tableview;
  @Getter @Setter
  private DBConn                  dbconn;
  private Dataset                 m_dts;
  private boolean                 m_bScartaImpTrasf;
  @Getter @Setter
  private boolean                 conRecTotali;

  // private List<IRigaBanca>        excludeCols;
  private List<String> excludeCols;

  static {
    fmtDbl = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault()); // ("#,##0.00", Locale.getDefault())
    fmtDbl.applyPattern("#,##0.00");
  }

  public TableViewFiller(TableView<List<Object>> tblview, DBConn p_dbc) {
    setTableview(tblview);
    setDbconn(p_dbc);
    conRecTotali = false;
    // m_db = LoadBancaMainApp.getInst().getConnSQL();
  }

  public static void setNullRetValue(String vv) {
    CSZ_NULLVAL = vv;
  }

  @Override
  public String call() throws Exception {
    s_log.debug("Start creazione Table View con i dati(B)...");

    try {
      openDataSet();
      if (null == m_dts) {
        s_log.warn("Nulla da mostrare sulla tabella");
        return ".. nulla da mostrare";
      }
      clearColumsTableView();
      //      creaTableView(m_dts);
      //      fillTableView();
    } catch (Exception e) {
      s_log.error("Errore Task riempi table view: {}", e.getMessage(), e);
    }
    return "..Finito!";
  }

  private void clearColumsTableView() {
    Semaphore semaf = new Semaphore(0);
    Platform.runLater(() -> {
      tableview.getItems().clear();
      tableview.getColumns().clear();
      creaTableView(m_dts);
      fillTableView();
      semaf.release();
    });
    try {
      semaf.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public Dataset getDataset() {
    return m_dts;
  }

  public Dataset openDataSet() {
    m_dts = null;
    if (m_bScartaImpTrasf) {
      int ndx = szQry.toLowerCase().indexOf("order");
      if (ndx > 0) {
        StringBuilder szQry2 = new StringBuilder();
        szQry2.append(szQry.substring(0, ndx)) //
            .append(QRY_WHE_NOTRASF) //
            .append(" ") //
            .append(szQry.substring(ndx));
        szQry = szQry2.toString();
      }
    }
    s_log.debug("Lancio query:{}", szQry);

    try (Dataset dtset = new Dataset(dbconn)) {
      if ( !dtset.executeQuery(szQry)) {
        s_log.error("errore lettura query {}", szQry);
      } else {
        m_dts = dtset;
        datasetReady();
      }
    } catch (IOException e) {
      s_log.error("errore creazione DataSet con query {}, err= {}", szQry, e.getMessage());
    }
    return m_dts;
  }

  public void datasetReady() {
    //
  }

  public boolean isExcludedCol(String p_colNam) {
    if (null == excludeCols || null == p_colNam)
      return false;
    //    IRigaBanca rb = IRigaBanca.parse(p_colNam);
    //    if (null == rb)
    //      return false;
    //    return excludeCols.contains(rb);
    return excludeCols.contains(p_colNam.toLowerCase());
  }

  public void addExcludedCols(String[] cols) {
    if (null == cols || cols.length == 0)
      return;
    if (null == excludeCols)
      excludeCols = new ArrayList<String>();
    excludeCols.addAll(Arrays.asList(cols));
  }

  private void creaTableView(Dataset p_dts) {
    DtsCols cols = p_dts.getColumns();
    // System.out.printf("creaTableView.nCols=%d\n", cols.size());
    int k = 0;
    for (DtsCol col : cols.getColumns()) {
      final int j = k++;
      String szColNam = col.getName();
      if (isExcludedCol(szColNam))
        continue;
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
      // System.out.printf("\tcreaTableView(%s(%s),\"%s\")\n", col.getName(), col.getType().toString(), cssAlign);
      TableColumn<List<Object>, Object> tbcol = new TableColumn<>(szColNam);
      //      tbcol.setCellFactory(tc -> {
      //        TableCell<List<Object>, Object> cel = new TableCell<List<Object>, Object>();
      //        cel.setOnMouseClicked(e -> {
      //          if ( !cel.isEmpty()) {
      //            Object ob = cel.getItem();
      //            System.out.printf("TableViewFiller.cellMouseClick(%s)\n", ob.toString());
      //          }
      //        });
      //        return cel;
      //      });
      tbcol.setCellValueFactory(param -> {
        SimpleObjectProperty<Object> cel = new SimpleObjectProperty<Object>(formattaCella(param.getValue().get(j)));
        return cel;
      });
      tbcol.setStyle(cssAlign);
      Platform.runLater(() -> tableview.getColumns().add(tbcol));
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
    //    Pattern patt = null;
    //    if (fltrParolaRegEx)
    //      patt = Pattern.compile(fltrParola.toLowerCase());
    for (DtsRow riga : m_dts.getRighe()) {
      if (scartaRiga(riga))
        continue;
      addRiga(riga);
      //      if (fltrParolaRegEx) {
      //        String desc = (String) riga.get(IRigaBanca.DESCR.getColNam());
      //        if ( !Utils.isValue(desc))
      //          continue;
      //        var lo = desc.toLowerCase();
      //        if ( !patt.matcher(lo).find())
      //          continue;
      //      }
      ObservableList<Object> tbRiga = FXCollections.observableArrayList();
      tbRiga.addAll(riga.getValues(true));
      dati.add(tbRiga);
    }
    tableview.setItems(dati);
    tableViewFilled();
  }

  public boolean scartaRiga(DtsRow riga) {
    return false;
  }

  public void addRiga(DtsRow riga) {
    //
  }

  public void tableViewFilled() {
    //
  }

  private Object formattaCella(Object p_o) {
    if (p_o == null)
      return CSZ_NULLVAL;
    String szCls = p_o.getClass().getSimpleName();
    switch (szCls) {
      case "String":
        return p_o;
      case "Integer":
        if ((Integer) p_o == 0)
          return "";
        return p_o;
      case "Float":
        if ((Float) p_o == 0)
          return "";
        return p_o;
      case "Double":
        if ((Double) p_o == 0)
          return "";
        // return Utils.formatDouble((Double) p_o);
        return fmtDbl.format(p_o);
    }
    return p_o;
  }

  public void setScartaImpTrasf(boolean selected) {
    m_bScartaImpTrasf = selected;
  }

}

package sm.clagenna.stdcla.javafx;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sql.DBConn;
import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sql.DtsCol;
import sm.clagenna.stdcla.sql.DtsCols;
import sm.clagenna.stdcla.sql.DtsRow;
import sm.clagenna.stdcla.utils.AppProperties;
import sm.clagenna.stdcla.utils.Utils;

// @ SuppressWarnings("this-escape")
public class TableViewFiller extends Task<String> implements ITableColBuilder {
  private static final Logger s_log                     = LogManager.getLogger(TableViewFiller.class);
  public static final String  FX_ALIGNMENT_CENTER_LEFT  = "-fx-alignment: center-left;";
  public static final String  FX_ALIGNMENT_CENTER_RIGHT = "-fx-alignment: center-right;";
  // property per salvare le colonne della TableView come ${prefix}.tbcol_${colname}=${index};${width};${sortorder}
  // esempio
  // private static final String  TBCOL_MASK                = "tbcol_%s";
  // tolto il "tbcol_" perche' non serve a nulla, basta il nome della colonna
  @Setter
  private static String        CSZ_NULLVAL = "**null**";
  private static DecimalFormat fmtDbl;

  //  @Getter @Setter
  //  private ResultView resView;
  @Getter @Setter
  private String                                               szQry;
  @Getter @Setter
  private PropertyChangeSupport                                propChanger;
  @Getter @Setter
  private String                                               fltrParola;
  @Getter @Setter
  private TableView<List<Object>>                              tableview;
  @Getter @Setter
  private String                                               propPrefix;
  @Getter @Setter
  private AppProperties                                        props;
  /** Lista delle proprietà delle colonne della TableView salvate in precedenza */
  private List<JFXUtils.TableColProp>                          liColsProps;
  @Getter @Setter
  private DBConn                                               dbconn;
  private Dataset                                              m_dts;
  @Getter @Setter
  private boolean                                              conRecTotali;
  private List<ITableColBuilderListener<List<Object>, Object>> liBuildCol;

  // private List<IRigaBanca>        excludeCols;
  private List<String>                 excludeCols;
  private TableColumn<List<Object>, ?> sortCol;
  // private TableColumn.SortType         sortType;

  static {
    fmtDbl = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault()); // ("#,##0.00", Locale.getDefault())
    fmtDbl.applyPattern("#,##0.00");
  }

  public TableViewFiller(TableView<List<Object>> tblview, DBConn p_dbc) {
    setTableview(tblview);
    setDbconn(p_dbc);
    conRecTotali = false;
  }

  public static void setNullRetValue(String vv) {
    CSZ_NULLVAL = vv;
  }

  @Override
  public String call() throws Exception {
    // s_log.debug("Start creazione Table View con i dati(B)...");
    // System.out.println(StackViewer.viewStackTrace("Start creazione Table View con i dati(B)..."));
    try {
      openDataSet();
      if (null == m_dts) {
        s_log.warn("Nulla da mostrare sulla tabella");
        return ".. nulla da mostrare";
      }
      populateTableView();
    } catch (Exception e) {
      s_log.error("Errore Task riempi table view: {}", e.getMessage(), e);
    }
    return "..Finito!";
  }

  /**
   * FIXME cos'e' questo bislacco semaforo ?!?
   */
  //  public void clearColumsTableViewBackground() {
  //    Semaphore semaf = new Semaphore(0);
  //    populateTableView();
  //    semaf.release();
  //    try {
  //      semaf.acquire();
  //    } catch (InterruptedException e) {
  //      e.printStackTrace();
  //    }
  //  }

  public void populateTableView() {
    Platform.runLater(() -> {
      populateLaterTableView();
    });
  }

  private void populateLaterTableView() {
    saveColsProps();
    tableview.getItems().clear();
    tableview.getColumns().clear();
    creaTableView(m_dts);
    fillTableView();
  }

  /**
   * Salva le proprietà delle colonne della TableView che comprende:
   * <ol>
   * <li>Index</li>
   * <li>Col name</li>
   * <li>width</li>
   * <li>sort order oppure "none"</li>
   * </ol>
   * in modo da poterle ripristinare successivamente
   */
  private void saveColsProps() {
    liColsProps = JFXUtils.saveTableviewColWidth(tableview, props, propPrefix);
  }

  public void setDataset(Dataset p_dt) {
    m_dts = p_dt;
  }

  public Dataset getDataset() {
    return m_dts;
  }

  public Dataset openDataSet() {
    m_dts = null;
    szQry = modifyQuery(szQry);
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

  public String modifyQuery(String szQry) {
    return szQry;
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
    if (null == liColsProps || liColsProps.isEmpty())
      liColsProps = JFXUtils.restoreTableviewColWidth(tableview, props, propPrefix);
    List<TableColumn<List<Object>, ?>> liTbCols = new ArrayList<>();
    sortCol = null;
    // sortType = null;
    DtsCols cols = p_dts.getColumns();
    int k = 0;
    for (DtsCol col : cols.getColumns()) {
      final int j = k++;
      String szColNam = col.getName();
      if (isExcludedCol(szColNam))
        continue;
      String cssAlign = FX_ALIGNMENT_CENTER_LEFT;
      switch (col.getType()) {
        case BIGINT:
        case DATE:
        case DOUBLE:
        case DECIMAL:
        case FLOAT:
        case INTEGER:
        case NUMERIC:
        case REAL: {
          // ---------------  Colonne Numeriche -----------------
          TableColumn<List<Object>, Number> tbcol = new TableColumn<>(szColNam);
          cssAlign = FX_ALIGNMENT_CENTER_RIGHT;
          // setCellValueFactory definisce cosa far vedere come valore della cella, in questo caso viene convertito in Number
          tbcol.setCellValueFactory(obj -> {
            SimpleObjectProperty<Number> cel = new SimpleObjectProperty<>(Utils.parseDouble(obj.getValue().get(j)));
            return cel;
          });
          // setCellFactory definisce come viene visualizzato il contenuto della cella, in questo caso allineato a destra e formattato come importo
          tbcol.setCellFactory(_ -> new TableCell<List<Object>, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
              super.updateItem(item, empty);
              setStyle(FX_ALIGNMENT_CENTER_RIGHT);
              if (empty || null == item) {
                setText("");
                return;
              }
              setText(Utils.formatImporto(item, true));
            }
          });

          // tbcol.setId(String.format(TBCOL_MASK, szColNam.toLowerCase()));
          tbcol.setId(szColNam.toLowerCase());
          // tbcol.setStyle(cssAlign);
          // colBuilded(tbcol);
          // Platform.runLater(() -> tableview.getColumns().add(tbcol));
          liTbCols.add(tbcol);
        }
          break;
        default: {
          // ---------------  Colonne Testuali -----------------
          TableColumn<List<Object>, Object> tbcol = new TableColumn<>(szColNam);
          cssAlign = FX_ALIGNMENT_CENTER_LEFT;
          tbcol.setCellValueFactory(param -> {
            SimpleObjectProperty<Object> cel = new SimpleObjectProperty<>(formattaCella(param.getValue().get(j)));
            return cel;
          });
          // tbcol.setId(String.format(TBCOL_MASK, szColNam.toLowerCase()));
          tbcol.setId(szColNam.toLowerCase());
          tbcol.setStyle(cssAlign);
          // colBuilded(tbcol);
          // Platform.runLater(() -> tableview.getColumns().add(tbcol));
          liTbCols.add(tbcol);
        }
          break;
      }
    }
    // ho l'elenco di tutte le TableColumn, ora le aggiungo alla TableView, se ho salvato le proprietà delle colonne le ripristino
    List<TableColumn<List<Object>, ?>> liTbCols2 = new ArrayList<>(liTbCols.size());
    if (null != liColsProps) {
      for (TableColumn<List<Object>, ?> tbcol : liTbCols) {
        String szColName = tbcol.getId();
        for (JFXUtils.TableColProp colProp : liColsProps) {
          if (colProp.colName().equals(szColName)) {
            tbcol.setPrefWidth(colProp.width());
            while (liTbCols2.size() <= colProp.index())
              liTbCols2.add(null);
            liTbCols2.add(colProp.index(), tbcol);
            if ( !colProp.sortType().equals("none")) {
              sortCol = tbcol;
              // sortType = TableColumn.SortType.valueOf(colProp.sortType());
            }
            break;
          }
        }
      }
    } else {
      liTbCols2.addAll(liTbCols);
    }
    // tolgo tutte le entry == null
    // vedi: https://www.baeldung.com/java-remove-nulls-from-list
    while (liTbCols2.remove(null))
      ;
    // le aggiungo alla TableView nell'ordine corretto, se ho salvato le proprietà delle colonne le ripristino
    Platform.runLater(() -> {
      tableview.getColumns().addAll(liTbCols2);
    });
    if (null != sortCol)
      Platform.runLater(() -> {
        // sortCol.setSortType(sortType);
        tableview.getSortOrder().add(sortCol);
      });

  }

  public void colBuilded(TableColumn<List<Object>, Object> pcol) {
    if (null == liBuildCol)
      return;
    for (ITableColBuilderListener<List<Object>, Object> ll : liBuildCol)
      ll.tableColBuilded(pcol);
  }

  private void fillTableView() {
    ObservableList<List<Object>> dati = FXCollections.observableArrayList();
    List<DtsRow> righe = m_dts.getRighe();
    if (righe == null) {
      s_log.info("Nessuna informazione da mostrare");
      return;
    }
    for (DtsRow riga : m_dts.getRighe()) {
      if (scartaRiga(riga))
        continue;
      addRiga(riga);
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

  /**
   * Torna una stringa formattata in base al tipo di dato, oppure null
   *
   * @param p_o
   * @return
   */
  private Object formattaCella(Object p_o) {
    if (null == p_o)
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
        // return p_o;
        return fmtDbl.format( ((Float) p_o).doubleValue());
      case "Double":
        if ((Double) p_o == 0)
          return "";
        // return Utils.formatDouble((Double) p_o);
        return fmtDbl.format(p_o);
    }
    return p_o;
  }

  /*
   * Torna una stringa formattata in base al tipo di dato, oppure null
   * @param p_o
   * @return
   */
  //  private Number formattaCellaNum(Object p_o) {
  //    if (null == p_o)
  //      return null;
  //
  //    Number retVal = switch (p_o.getClass().getSimpleName()) {
  //      case "String" -> Utils.parseDouble((String) p_o);
  //      case "Integer" -> (Integer) p_o == 0 ? null : (Integer) p_o;
  //      case "Float" -> (Float) p_o == 0 ? null : (Float) p_o;
  //      case "Double" -> (Double) p_o == 0 ? null : (Double) p_o;
  //      default -> null;
  //    };
  //    return retVal;
  //  }

  @Override
  public void addTableColBuilderListener(ITableColBuilderListener<List<Object>, Object> liste) {
    if (null == liBuildCol)
      liBuildCol = new ArrayList<>();
    liBuildCol.add(liste);
  }

}

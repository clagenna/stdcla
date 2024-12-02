package prova.stdcla.javafx.tableview;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * For columns of Integer we use Number see <a href=
 * "https://stackoverflow.com/questions/14413040/converting-integer-to-observablevalueinteger-in-javafx"
 * target="_blank"> StackOverflow</a> {@link }
 */
public class TableViewController extends Application implements Initializable {

  private static final String CSZ_RELPATH = "F:\\Google Drive\\gennari\\Banche";

  private TableView<FileCSV> tblvw;

  private TableColumn<FileCSV, String> colName;
  private TableColumn<FileCSV, String> colRelPath;
  private TableColumn<FileCSV, Number> colSize;
  private TableColumn<FileCSV, Number> colQtaRecs;
  private TableColumn<FileCSV, String> colDtmin;
  private TableColumn<FileCSV, String> colDtmax;
  private TableColumn<FileCSV, String> colUltagg;

  private ObservableList<FileCSV> elenco;

  public TableViewController() {
    //
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    initialize(null, null);
    creaDati(CSZ_RELPATH);

    creaForm(primaryStage);

  }

  private void creaForm(Stage stage) {
    Scene scene = new Scene(new Group());
    stage.setTitle("Table View Sample");
    stage.setWidth(800);
    stage.setHeight(500);

    final Label label = new Label("Files di Import CSV");
    // label.setFont(new Font("Arial", 20));
    final AnchorPane ancp = new AnchorPane();
    // ancp.setSpacing(5);
    ancp.setPadding(new Insets(10, 0, 0, 10));
    ancp.getChildren().addAll(label, tblvw);
    AnchorPane.setBottomAnchor(tblvw, 0.);
    AnchorPane.setLeftAnchor(tblvw, 0.);
    AnchorPane.setTopAnchor(tblvw, 0.);
    AnchorPane.setRightAnchor(tblvw, 0.);

    ((Group) scene.getRoot()).getChildren().addAll(ancp);

    stage.setScene(scene);
    stage.show();
  }

  private void creaDati(String p_Relpath) {
    elenco = FXCollections.observableArrayList();
    String szGlob = String.format("glob:*:/**/{%s}*.{csv,xls,xlsx}", "estratto");
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher(szGlob);
    List<Path> result = null;
    try (Stream<Path> walk = Files.walk(Paths.get(p_Relpath))) {
      result = walk.filter(p -> !Files.isDirectory(p)) //
          // not a directory
          // .map(p -> p.toString().toLowerCase()) // convert path to string
          .filter(f -> matcher.matches(f)) // check end with
          .collect(Collectors.toList()); // collect all matched to a List
    } catch (IOException e) {
      e.printStackTrace();
    }
    for (Path pth : result) {
      elenco.add(new FileCSV().assignPath(pth));
    }
    tblvw.getItems().addAll(elenco);
    tblvw.setEditable(true);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    colName = new TableColumn<>("Nome");
    colName.setCellValueFactory(cell -> cell.getValue().getName());

    colRelPath = new TableColumn<>("Path Rel.");
    colRelPath.setCellValueFactory(cell -> cell.getValue().getRelPath());

    colSize = new TableColumn<>("Dimensione");
    colSize.setCellValueFactory(cell -> cell.getValue().getSize());

    colQtaRecs = new TableColumn<>("Qta .rows");
    colQtaRecs.setCellValueFactory(cell -> cell.getValue().getQtaRecs());

    colDtmin = new TableColumn<>("Data min.");
    colDtmin.setCellValueFactory(cell -> cell.getValue().getDtmin());

    colDtmax = new TableColumn<>("Data Reg.");
    colDtmax.setCellValueFactory(cell -> cell.getValue().getDtmax());

    colUltagg = new TableColumn<>("Data Reg.");
    colUltagg.setCellValueFactory(cell -> cell.getValue().getUltagg());

    tblvw = new TableView<FileCSV>();
    tblvw.getColumns().addAll(colName, colRelPath, colSize, colQtaRecs, colDtmin, colDtmax, colUltagg);
  }

  public static void main(String[] args) {
    Application.launch(args);
  }
}

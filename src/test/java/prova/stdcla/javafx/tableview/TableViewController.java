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
import javafx.scene.layout.VBox;
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
  private TableColumn<FileCSV, Number> colQtaRows;
  private TableColumn<FileCSV, String> colDateReg;

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
    stage.setWidth(300);
    stage.setHeight(500);

    final Label label = new Label("Address Book");
    // label.setFont(new Font("Arial", 20));
    final VBox vbox = new VBox();
    vbox.setSpacing(5);
    vbox.setPadding(new Insets(10, 0, 0, 10));
    vbox.getChildren().addAll(label, tblvw);

    ((Group) scene.getRoot()).getChildren().addAll(vbox);

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

    colQtaRows = new TableColumn<>("Qta .rows");
    colQtaRows.setCellValueFactory(cell -> cell.getValue().getQtaRows());

    colDateReg = new TableColumn<>("Data Reg.");
    colDateReg.setCellValueFactory(cell -> cell.getValue().getDatereg());

    tblvw = new TableView<FileCSV>();
    tblvw.getColumns().addAll(colName, colRelPath, colSize, colQtaRows, colDateReg);
  }

  public static void main(String[] args) {
    Application.launch(args);
  }
}

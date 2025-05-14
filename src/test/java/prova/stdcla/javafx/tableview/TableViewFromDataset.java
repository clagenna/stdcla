package prova.stdcla.javafx.tableview;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

import com.opencsv.exceptions.CsvException;

import javafx.application.Application;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import sm.clagenna.stdcla.javafx.TableViewFiller;
import sm.clagenna.stdcla.sql.Dataset;

public class TableViewFromDataset extends Application implements Initializable {

  private Stage                             primStage;
  private TableView<List<Object>>           tblview;
  private Dataset                           dataset;
  private TableViewFiller                   tblvf;
  @SuppressWarnings("unused")
  private TableColumn<List<Object>, Number> colId;
  @SuppressWarnings("unused")
  private TableColumn<List<Object>, String> colName;
  @SuppressWarnings("unused")
  private TableColumn<List<Object>, String> colQualif;

  public TableViewFromDataset() {
    //
  }

  public static void main(String[] args) {
    Application.launch(args);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    //

  }

  /**
   * Dato un {@linkplain Dataset} popolo un {@linkplain TableView} con tale
   * Dataset
   */
  @Override
  public void start(Stage primaryStage) throws Exception {
    primStage = primaryStage;
    creaDati();
    creaTableView();
    creaForm();
    primStage.show();
  }

  private void creaDati() {
    Path pthFi = Paths.get("src/test/resources/prova/stdcla/dts", "abitanti2.csv");
    try (Dataset dts = new Dataset()) {
      // lettura del file CSV
      int qta = dts.readcsv(pthFi).size();
      System.out.printf("\nLetti %d records\n", qta);
      dataset = dts;
    } catch (IOException | CsvException e) {
      e.printStackTrace();
    }
  }

  private void creaTableView() {
    tblview = new TableView<>();
    tblvf = new TableViewFiller(tblview, null);
    tblvf.setDataset(dataset);
    tblvf.populateTableView();
    // dts.creaTableViewCols(tblview);
  }

  private void creaForm() {
    primStage.setTitle("Table View Sample");
    primStage.setWidth(800);
    primStage.setHeight(500);

    AnchorPane ancp = new AnchorPane();
    final Label label = new Label("Una label Qualunque");
    label.setFont(new Font("Arial", 20));
    ancp.getChildren().addAll(label, tblview);

    Scene scene = new Scene(ancp);
    AnchorPane.setTopAnchor(tblview, 26.);
    AnchorPane.setBottomAnchor(tblview, 0.);
    AnchorPane.setLeftAnchor(tblview, 0.);
    AnchorPane.setRightAnchor(tblview, 0.);

    primStage.setScene(scene);

  }

}

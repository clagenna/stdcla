package sm.clagenna.stdcla.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sm.clagenna.stdcla.utils.AppProperties;
import sm.clagenna.stdcla.utils.Utils;

public class JFXUtils {

  public JFXUtils() {
    //
  }

  public record ScreenDim(int poxX, int posY, int width, int height) {
  }

  public static ScreenDim getScreenMinMax(int posX, int posY, int wi, int he) {
    @SuppressWarnings("unused") int minx = 0, maxx = 0, miny = 0, maxy = 0, maxWi = 0, maxHe = 0;
    for (Screen scr : Screen.getScreens()) {
      Rectangle2D schermo = scr.getBounds();
      // System.out.println(schermo);
      minx = (int) (schermo.getMinX() < minx ? schermo.getMinX() : minx);
      maxx = (int) (schermo.getMaxX() >= maxx ? schermo.getMaxX() : maxx);
      miny = (int) (schermo.getMinY() < miny ? schermo.getMinY() : miny);
      maxy = (int) (schermo.getMaxY() >= maxy ? schermo.getMaxY() : maxy);
    }
    maxWi = maxx - minx;
    maxHe = maxy - miny;
    int lposX = posX < minx ? minx : posX;
    int lposY = posY < miny ? miny : posY;
    lposX = posX > maxx - wi ? maxx - wi : lposX;
    lposY = posY > maxy - he ? maxy - he : lposY;
    lposX = lposX < minx ? minx : lposX;
    lposY = lposY < miny ? miny : lposY;

    return new ScreenDim(lposX, lposY, wi, he);
  }

  public static void savePosStage(Stage stag, AppProperties props, String prefix) {
    Scene sce = stag.getScene();
    double px = sce.getWindow().getX();
    double py = sce.getWindow().getY();
    double dx = sce.getWindow().getWidth();
    double dy = sce.getWindow().getHeight();

    String szPosX = String.format("%s.posX", prefix);
    String szPosY = String.format("%s.posY", prefix);
    String szwidt = String.format("%s.width", prefix);
    String szHeig = String.format("%s.heigt", prefix);

    props.setProperty(szPosX, (int) px);
    props.setProperty(szPosY, (int) py);
    props.setProperty(szwidt, (int) dx);
    props.setProperty(szHeig, (int) dy);
  }

  public static void readPosStage(Stage stag, AppProperties props, String prefix) {
    String szPosX = String.format("%s.posX", prefix);
    String szPosY = String.format("%s.posY", prefix);
    String szwidt = String.format("%s.width", prefix);
    String szHeig = String.format("%s.heigt", prefix);

    int px = props.getIntProperty(szPosX);
    int py = props.getIntProperty(szPosY);
    int dx = props.getIntProperty(szwidt);
    int dy = props.getIntProperty(szHeig);

    var mm = JFXUtils.getScreenMinMax(px, py, dx, dy);
    if (mm.poxX() != -1 && mm.posY() != -1 && mm.poxX() != -1 && mm.posY() != -1) {
      stag.setX(mm.poxX());
      stag.setY(mm.posY());
      stag.setWidth(mm.width());
      stag.setHeight(mm.height());
    }
  }

  public static void setStageCenter(Stage stag) {
    Scene sce = stag.getScene();
    double dx = sce.getWindow().getWidth();
    double dy = sce.getWindow().getHeight();
    Rectangle2D schermo = Screen.getPrimary().getBounds();
    double px = schermo.getMinX() + (schermo.getWidth() - dx) / 2;
    double py = schermo.getMinY() + (schermo.getHeight() - dy) / 2;
    stag.setX(px);
    stag.setY(py);
  }

  public static void setStageCenter(Stage stag, Stage parent) {
    Scene sce = stag.getScene();
    double dx = sce.getWindow().getWidth();
    double dy = sce.getWindow().getHeight();
    double px = parent.getX() + (parent.getWidth() - dx) / 2;
    double py = parent.getY() + (parent.getHeight() - dy) / 2;
    stag.setX(px);
    stag.setY(py);
  }

  public static void saveTableviewColWidth(TableView<?> tvf, AppProperties props, String prefix) {
    if (null == tvf || null == props || null == prefix)
      return;
    for (TableColumn<?, ?> col : tvf.getColumns()) {
      String szColNam = col.getId();
      if (null == szColNam)
        continue;
      int widt = (int) col.getWidth();
      String szKeyProp = String.format("%s.%s", prefix, szColNam);
      props.setProperty(szKeyProp, widt);
    }
  }

  public static void restoreTableviewColWidth(TableView<?> tvf, AppProperties props, String prefix) {
    if (null == tvf || null == props || null == prefix)
      return;

    for (TableColumn<?, ?> col : tvf.getColumns()) {
      String szColNam = col.getId();
      if (null == szColNam)
        continue;
      String szKeyProp = String.format("%s.%s", prefix, szColNam);
      int widt = props.getIntProperty(szKeyProp, 60);
      if (widt > 0)
        col.setPrefWidth(widt);
    }
  }

  /**
   * Mostra un popup con la guida alle scorciatoie da tastiera
   *
   * @param owner
   *          stage proprietario del popup
   */
  public static void showHelpPopup(Stage owner, String prologo, String[][] shortcuts, String title) {
    Stage dialog = new Stage();
    dialog.initOwner(owner);
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.initStyle(StageStyle.UTILITY);
    dialog.setTitle("Guida ai tasti");
    dialog.setResizable(false);

    // Titolo
    Label lbTitle = new Label(title);
    lbTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

    VBox boxPrologo = null;

    if ( !Utils.isValue(prologo)) {
      prologo = "";
      boxPrologo = new VBox(4, new Label(prologo));
    } else if (prologo.contains("\n")) {
      String[] lines = prologo.split("\n");
      boxPrologo = new VBox(4);
      for (String line : lines) {
        boxPrologo.getChildren().add(new Label(line));
      }
    } else if (prologo.contains("<br/>")) {
      String[] lines = prologo.split("<br/>");
      boxPrologo = new VBox(4);
      for (String line : lines) {
        boxPrologo.getChildren().add(new Label(line));
      }
    }

    // Griglia tasto → descrizione
    GridPane grid = new GridPane();
    grid.setHgap(16);
    grid.setVgap(8);
    grid.setPadding(new Insets(12, 0, 4, 0));

    //     String[][] shortcuts = {
    //         {"F5",      "Ripeti la ricerca"},
    //         {"Enter",   "Esegui la ricerca o conferma la selezione"},
    //         {"Shift",   "Attiva la selezione multipla su piu righe consecutive"},
    //         {"Ctrl",    "Attiva la selezione multipla non consecutive"},
    //         {"Doppio click", "Modifica il codice Stat. selezionato"},
    //         {"Ctrl + Doppio click", "Aggiunge un figlio al codice Stat. selezionato"},
    //         {"?",       "Mostra questo aiuto"},
    //         {"Esc",     "Chiudi Help"},
    //     };

    for (int i = 0; i < shortcuts.length; i++) {
      String szkey = shortcuts[i][0];
      String szdesc = shortcuts[i][1];
      Label key = new Label(szkey);
      Label desc = new Label(szdesc);
      key.setFont(Font.font("Monospaced", 13));
      String szKeyStyle = """
          "-fx-background-color: #e8e8e8;" + //
              " -fx-border-color: #aaa;" + //
              " -fx-border-radius: 4;" + //
              " -fx-background-radius: 4;" + //
              " -fx-padding: 2 8 2 8;")""";
      if ( !Utils.isValue(szdesc)) {
        szKeyStyle += " colspan=2; -fx-alignment: center;";
        key.setStyle(szKeyStyle);
        key.setFont(Font.font("System", FontWeight.BOLD, 12));
        grid.add(key, 0, i);
      } else {
        key.setStyle(szKeyStyle);
        grid.add(key, 0, i);
        grid.add(desc, 1, i);
      }
    }

    VBox root = new VBox(8, lbTitle, boxPrologo, grid);
    root.setPadding(new Insets(16, 20, 16, 20));

    Scene scene = new Scene(root);

    // Chiudi con Escape o cliccando fuori
    scene.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ESCAPE)
        dialog.close();
    });

    dialog.setScene(scene);
    dialog.showAndWait();
  }

}

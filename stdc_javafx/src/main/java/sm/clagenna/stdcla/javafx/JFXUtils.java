package sm.clagenna.stdcla.javafx;

import java.util.ArrayList;
import java.util.List;

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
  private static final String S_POS_X = "%s.posX";
  private static final String S_POS_Y = "%s.posY";
  private static final String S_WIDTH = "%s.width";
  private static final String S_HEIGT = "%s.heigt";

  public JFXUtils() {
    //
  }

  public record ScreenDim(int poxX, int posY, int width, int height) {
  }

  public record TableColProp(Integer index, String colName, int width, String sortType) {
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
    wi = wi < 200 ? 200 : wi;
    he = he < 150 ? 150 : he;
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

    String szPosX = String.format(S_POS_X, prefix);
    String szPosY = String.format(S_POS_Y, prefix);
    String szwidt = String.format(S_WIDTH, prefix);
    String szHeig = String.format(S_HEIGT, prefix);

    props.setProperty(szPosX, (int) px);
    props.setProperty(szPosY, (int) py);
    props.setProperty(szwidt, (int) dx);
    props.setProperty(szHeig, (int) dy);
  }

  public static void readPosStage(Stage stag, AppProperties props, String prefix) {
    String szPosX = String.format(S_POS_X, prefix);
    String szPosY = String.format(S_POS_Y, prefix);
    String szwidt = String.format(S_WIDTH, prefix);
    String szHeig = String.format(S_HEIGT, prefix);

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

  /**
   * Salva le proprietà delle colonne della TableView che comprende:
   * <ol>
   * <li>Col name</li>
   * <li>Index</li>
   * <li>width</li>
   * <li>sort order oppure "none"</li>
   * </ol>
   * se prefix = "resview.col.tbcol_" allora un'esempio:
   * 
   * <pre>
   * ...
   * resview.col.tbcol_avere = 4;75;none
   * resview.col.tbcol_cardid = 5;59;none
   * resview.col.tbcol_descr = 6;743;ASCENDING
   * resview.col.tbcol_codstat = 7;65;none
   * ...
   * </pre>
   * 
   * in modo da poterle ripristinare successivamente
   */
  public static List<TableColProp> saveTableviewColWidth(TableView<?> tvf, AppProperties props, String prefix) {
    if (null == tvf || null == props || null == prefix)
      return null;
    int k = 0;
    // Map<String, TableColProp> mapCols = new TreeMap<>();
    List<TableColProp> liCols = new ArrayList<>();
    for (TableColumn<?, ?> col : tvf.getColumns()) {
      String szColNam = col.getId();
      if (null == szColNam)
        continue;
      int width = (int) col.getWidth();
      String sortType = tvf.getSortOrder().contains(col) ? col.getSortType().name() : "none";
      TableColProp tcp = new TableColProp(k++, szColNam, width, sortType);
      // mapCols.put(tcp.colName(), tcp);
      liCols.add(tcp);
    }

    // mapCols.forEach((_, rec) -> {
    liCols.forEach(rec -> {
      String szKeyProp = String.format("%s.%s", prefix, rec.colName());
      String szVal = String.format("%d;%s;%s", rec.index(), rec.width(), rec.sortType());
      props.setProperty(szKeyProp, szVal);
      System.out.printf(" %s = %s\n", szKeyProp, szVal);
    });
    return liCols;
    //
    //    if (tvf.getSortOrder().size() > 0) {
    //      // prefx.sortcol.indx=name,type
    //      // prefx.sortype.indx=name,type
    //      for (TableColumn<?, ?> col : tvf.getSortOrder()) {
    //        String szColNam = col.getId();
    //        if (null == szColNam)
    //          continue;
    //        k++;
    //        String szKeyProp = String.format("%s.sortcol.%d", prefix, k);
    //        String szKeyType = String.format("%s.sortype.%d", prefix, k);
    //        props.setProperty(szKeyProp, szColNam);
    //        props.setProperty(szKeyType, col.getSortType().name());
    //      }
    //    }
    //    for (TableColumn<?, ?> col : tvf.getColumns()) {
    //      String szColNam = col.getId();
    //      if (null == szColNam)
    //        continue;
    //      int widt = (int) col.getWidth();
    //      String szKeyProp = String.format("%s.%s", prefix, szColNam);
    //      props.setProperty(szKeyProp, widt);
    //      System.out.printf("saveTableviewColWidth: %d) %s = %d%n", k++, szKeyProp, widt);
    //    }
  }

  public static List<TableColProp> restoreTableviewColWidth(TableView<?> tvf, AppProperties props, String prefix) {
    List<TableColProp> liCols = new ArrayList<>();
    if (null == tvf || null == props || null == prefix)
      return liCols;

    props.getProperties().forEach((key, value) -> {
      String szKey = key.toString();

      if (szKey.startsWith(prefix)) {
        String szColNam = szKey.substring(prefix.length() + 1);
        String szVal = value.toString();
        String[] parts = szVal.split(";");
        Integer index = null;
        Integer width = null;
        String sortType = null;
        // se index, width, sortType non sono presenti, ignora la proprietà
        switch (parts.length) {
          case 0:
            System.err.printf("Error: missing column Index for %s\n", szColNam);
            break;
          case 1:
            System.err.printf("Error: missing width for %s\n", szColNam);
            break;
          case 2:
            index = Integer.parseInt(parts[0]);
            width = Integer.parseInt(parts[1]);
            sortType = "none"; // default sort type
            break;
          case 3:
            index = Integer.parseInt(parts[0]);
            width = Integer.parseInt(parts[1]);
            sortType = parts[2];
            break;
          default:
            break;
        }
        if (index != null && width != null) {
          TableColProp tcp = new TableColProp(index, szColNam, width, sortType);
          liCols.add(tcp);
        }
      }
    });
    for (TableColumn<?, ?> col : tvf.getColumns()) {
      String szColNam = col.getId();
      if (null == szColNam)
        continue;
      for (TableColProp tcp : liCols) {
        if (tcp.colName().equals(szColNam)) {
          col.setPrefWidth(tcp.width());
          if (!"none".equals(tcp.sortType())) {
            col.setSortType(TableColumn.SortType.valueOf(tcp.sortType()));
            // tvf.getSortOrder().add(col);
          }
          break;
        }
      }
    }

    return liCols;
  }

  public static void restoreTableviewColWidth_OLD(TableView<?> tvf, AppProperties props, String prefix) {
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
      else
        col.setPrefWidth(50);
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

package sm.clagenna.stdcla.javafx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import sm.clagenna.stdcla.utils.AppProperties;

public class JFXUtils {

  public JFXUtils() {
    //
  }

  public record ScreenDim(int poxX, int posY, int width, int height) {
  }

  public static ScreenDim getScreenMinMax(int posX, int posY, int wi, int he) {
    @SuppressWarnings("unused")
    int minx = 0, maxx = 0, miny = 0, maxy = 0, maxWi = 0, maxHe = 0;
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

  public static void savePosStage(Stage prim, AppProperties props, String prefix) {
    Scene sce = prim.getScene();
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
  
  public static void readPosStage(Stage prim, AppProperties props, String prefix) {
    String szPosX = String.format("%s.posX", prefix);
    String szPosY = String.format("%s.posY", prefix);
    String szwidt = String.format("%s.width", prefix);
    String szHeig = String.format("%s.heigt", prefix);

    int px = props.getIntProperty(szPosX);
    int py = props.getIntProperty(szPosY);
    int dx = props.getIntProperty(szwidt);
    int dy = props.getIntProperty(szHeig);

    var mm = JFXUtils.getScreenMinMax(px, py, dx, dy);
    if (mm.poxX() != -1 && mm.posY() != -1 && mm.poxX() * mm.posY() != 0) {
      prim.setX(mm.poxX());
      prim.setY(mm.posY());
      prim.setWidth(mm.width());
      prim.setHeight(mm.height());
    }
  }
  
}

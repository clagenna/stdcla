package sm.clagenna.stdcla.javafx;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class JFXUtils {

  public JFXUtils() {
    //
  }

  public record ScreenDim(int poxX, int posY, int width, int height) {
  }

  @SuppressWarnings("unused")
  public static ScreenDim getScreenMinMax(int posX, int posY, int wi, int he) {
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

}

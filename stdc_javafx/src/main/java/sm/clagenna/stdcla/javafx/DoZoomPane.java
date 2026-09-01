package sm.clagenna.stdcla.javafx;

import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Per zoom-are il contenuto di uno stage si utilizza la DoZoomPane in questa
 * maniera:
 * 
 * <pre>
 * @Override
 * public void start(Stage stage) {
 *   // Contenuto "disegnato"
 *   Pane drawPane = disegnaQualcosa();
 * 
 *   // Wrapper che gestisce trasformazioni e resize
 *   DoZoomPane zoomPane = new DoZoomPane(drawPane);
 * 
 *   Scene scene = new Scene(zoomPane, 900, 600);
 *   stage.setTitle("Zoom & Pan su Pane");
 *   stage.setScene(scene);
 *   stage.show();
 * 
 *   // All'avvio forza layout corretto
 *   zoomPane.requestLayout();
 * }
 * </pre>
 */
public class DoZoomPane extends StackPane {
  private static final double MIN_SCALE  = 0.2;
  private static final double MAX_SCALE  = 10.0;
  private static final double ZOOM_DELTA = 1.1;

  // private ZoomPane04Data data = new ZoomPane04Data(1.0, 0, 0);
  private Pane   drawPane;
  private double scale;
  private double translateX;
  private double translateY;
  private double dragStartSceneX;
  private double dragStartSceneY;
  private double dragStartTranslateX;
  private double dragStartTranslateY;

  public DoZoomPane(Pane pcontent) {
    drawPane = pcontent;
    scale = 1.0;
    translateX = 0;
    translateY = 0;

    getChildren().add(drawPane);

    // Il contenuto mantiene il suo layout naturale
    drawPane.setManaged(false);

    // Trasformazioni iniziali
    drawPane.setScaleX(scale);
    drawPane.setScaleY(scale);
    drawPane.setTranslateX(translateX);
    drawPane.setTranslateY(translateY);

    setupZoom();
    setupPan();
    setupAutosize();

  }

  private void setupZoom() {
    addEventFilter(ScrollEvent.SCROLL, e -> {
      double oldScale = scale;
      double zoomFactor = e.getDeltaY() > 0 ? ZOOM_DELTA : 1.0 / ZOOM_DELTA;
      double newScale = clamp(oldScale * zoomFactor, MIN_SCALE, MAX_SCALE);

      // Punto del mouse nel sistema di coordinate del contenuto
      double mouseX = e.getX();
      double mouseY = e.getY();

      // Mantieni fermo il punto sotto il mouse
      translateX = mouseX - (mouseX - translateX) * (newScale / oldScale);
      translateY = mouseY - (mouseY - translateY) * (newScale / oldScale);

      scale = newScale;
      applyTransform();

      e.consume();
    });
  }

  private void setupPan() {
    addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        dragStartSceneX = e.getSceneX();
        dragStartSceneY = e.getSceneY();
        dragStartTranslateX = translateX;
        dragStartTranslateY = translateY;
      }
    });

    addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        translateX = dragStartTranslateX + (e.getSceneX() - dragStartSceneX);
        translateY = dragStartTranslateY + (e.getSceneY() - dragStartSceneY);
        applyTransform();
      }
    });
  }

  private void setupAutosize() {
    // Centra/scala inizialmente quando cambia la dimensione del contenitore
    widthProperty().addListener((obj, ov, nv) -> fitToViewport());
    heightProperty().addListener((obj, ov, nv) -> fitToViewport());

    // Se vuoi che il contenuto si ridisponga al resize del pane interno
    drawPane.layoutBoundsProperty().addListener((obj, ov, nv) -> fitToViewport());

    // Applica una volta dopo il layout iniziale
    sceneProperty().addListener((obj, ov, newScene) -> {
      if (newScene != null) {
        newScene.windowProperty().addListener((obj2, ov2, nw) -> {
          if (nw != null) {
            nw.setOnShown(ob -> fitToViewport());
          }
        });
      }
    });
  }

  private void fitToViewport() {
    if (getWidth() <= 0 || getHeight() <= 0)
      return;

    Bounds bounds = drawPane.getLayoutBounds();
    if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0)
      return;

    // Se vuoi SOLO autosize iniziale, puoi mettere una guardia qui.
    // In questo esempio, al resize della finestra si ricalcola una scala di fit.
    double sx = getWidth() / bounds.getWidth();
    double sy = getHeight() / bounds.getHeight();
    double fittedScale = Math.min(sx, sy);

    // Se preferisci non forzare sempre il fit durante zoom/pan, puoi commentare queste 3 righe
    scale = clamp(fittedScale, MIN_SCALE, MAX_SCALE);

    translateX = (getWidth() - bounds.getWidth() * scale) / 2 - bounds.getMinX() * scale;
    translateY = (getHeight() - bounds.getHeight() * scale) / 2 - bounds.getMinY() * scale;

    applyTransform();
  }

  private void applyTransform() {
    drawPane.setScaleX(scale);
    drawPane.setScaleY(scale);
    drawPane.setTranslateX(translateX);
    drawPane.setTranslateY(translateY);
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

}

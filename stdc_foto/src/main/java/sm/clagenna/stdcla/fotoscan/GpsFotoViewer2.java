package sm.clagenna.stdcla.fotoscan;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import sm.clagenna.stdcla.geo.EExifRotation;
import sm.clagenna.stdcla.geo.EGeoSrcCoord;
import sm.clagenna.stdcla.geo.GeoCoord;

/**
 * copiato a piene mani da
 * <a href="https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a">James-d
 * PlutoExplorer</a>
 *
 */
public class GpsFotoViewer2 extends Stage //
    implements PropertyChangeListener {

  private static final Logger       s_log                = LogManager.getLogger(GpsFotoViewer2.class);
  private static final String       IMAGE_FOTO_ICO       = "fotografia2.png";
  private static final int          MIN_PIXELS           = 10;
  private static final double       STAGE_WIDTH          = 1000.;
  private static final TagInfoShort EXIF_TAG_ORIENTATION = new TagInfoShort("Orientation", 0x0112,
      TiffDirectoryType.EXIF_DIRECTORY_EXIF_IFD);

  private FotoViewerProducer      prod;
  private Stage                   primaryStage;
  private Scene                   m_scene;
  private TableView<GeoCoord>     tblvRecDB;
  private File                    m_imageFile;
  private Image                   m_image;
  private double                  m_wi;
  private double                  m_he;
  private double                  m_prefSizeHe;
  private double                  m_prefSizeWi;
  private ImageView               m_imageView;
  private ObjectProperty<Point2D> m_mouseDown;
  // l'ultima foto mostrata nel viewer
  private GeoCoord m_lastShow;

  public GpsFotoViewer2() {
    prod = null;
    primaryStage = null;
    m_scene = null;
    s_log.debug("Istanzio un GpsFotoViewer()");
    m_prefSizeHe = 600;
    m_prefSizeWi = 800;
  }
  
  public GpsFotoViewer2(FotoViewerProducer p_prod) {
    prod = p_prod;
    primaryStage = p_prod.getPrimaryStage();
    m_scene = null;
    s_log.debug("Istanzio un GpsFotoViewer()");
    m_prefSizeHe = 600;
    m_prefSizeWi = 800;
  }

  public void showImage(TableRow<GeoCoord> row) {
    GeoCoord p_geo = row.getItem();
    tblvRecDB = row.getTableView();

    mostraImmagine(p_geo);

    impostaIco();
    setTitle(p_geo.getFotoFile().toString());
    initModality(Modality.NONE);
    initOwner(primaryStage);
    setScene(m_scene);
    ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
      // System.out.println("Height: " + getHeight() + " Width: " + getWidth());
      m_prefSizeHe = getHeight();
      m_prefSizeWi = getWidth();
    };
    widthProperty().addListener(stageSizeListener);
    heightProperty().addListener(stageSizeListener);

    show();
  }

  private void mostraImmagine(GeoCoord p_geo) {
    if ( !p_geo.hasFotoFile() || null != m_lastShow && m_lastShow.equals(p_geo)) {
      // s_log.debug("Gia mostrato {}", p_geo.getFotoFile().toString());
      return;
    }
    m_lastShow = p_geo;

    ImageView imageView = caricaImgDaFile(p_geo);
    double prop = m_wi / m_he;
    int stageWith = (int) STAGE_WIDTH - 45;
    int stageHeight = (int) (STAGE_WIDTH / prop);
    if (m_prefSizeHe != 0 && m_prefSizeWi != 0) {
      stageWith = (int) m_prefSizeWi;
      stageHeight = (int) m_prefSizeHe;
    }
    setWidth(stageWith);
    setHeight(stageHeight);

    // reset(imageView, stageWith / 2, stageHeight / 2);

    m_mouseDown = new SimpleObjectProperty<>();

    imageView.setOnMousePressed(e -> {
      Point2D mousePress = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
      m_mouseDown.set(mousePress);
    });

    imageView.setOnMouseDragged(e -> onMouseDragged(e));
    imageView.setOnScroll(e -> onMouseScroll(e));
    imageView.setOnMouseClicked(e -> {
      if (e.getClickCount() == 2) {
        reset(imageView, m_wi, m_he);
      }
    });

    Pane container = new Pane(imageView);
    container.setPrefSize(m_prefSizeWi, m_prefSizeHe);

    imageView.fitWidthProperty().bind(container.widthProperty());
    imageView.fitHeightProperty().bind(container.heightProperty());
    VBox root = new VBox(container);
    root.setFillWidth(true);
    VBox.setVgrow(container, Priority.ALWAYS);

    if (null == m_scene)
      m_scene = new Scene(root);
    else
      m_scene.setRoot(root);

    m_scene.setOnKeyReleased(new EventHandler<KeyEvent>() {

      @Override
      public void handle(KeyEvent event) {
        // System.out.printf("DBGpsInfoController.imagePopupWindowShow(%s)\n", event.toString());
        switch (event.getCode()) {
          case RIGHT:
          case LEFT:
          case UP:
          case DOWN:
            doRightLeft(event.getCode());
            break;
          case ESCAPE:
            windowClosing(null);
            close();
            break;
          default:
            // System.out.println("GpsFotoViewer.mostraImmagine:" + event.toString());
            String cc = event.getText();
            if ( !event.isShiftDown() || cc == null || cc.length() == 0)
              break;
            switch (cc) {
              case "+":
              case "-":
              case "*":
                doZoom(cc, event);
                break;
              default:
                break;
            }
            break;
        }

      }
    });
    this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::windowClosing);
    reset(imageView, m_wi, m_he);
    setTitle("Foto " + p_geo.getFotoFile().toString());
  }

  protected void onMouseDragged(MouseEvent e) {
    Point2D dragPoint = imageViewToImage(m_imageView, new Point2D(e.getX(), e.getY()));
    shift(m_imageView, dragPoint.subtract(m_mouseDown.get()));
    m_mouseDown.set(imageViewToImage(m_imageView, new Point2D(e.getX(), e.getY())));
  }

  protected void onMouseScroll(ScrollEvent e) {
    double delta = e.getDeltaY();
    // System.out.println("GpsFotoViewer2.onMouseScroll() delta="+delta);
    Rectangle2D viewport = m_imageView.getViewport();
    // System.out.println("onMouseScroll() viewport="+viewport);

    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
    double minPix = Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight());
    // don't scale so that we're bigger than image dimensions:
    double maxPix = Math.max(m_wi / viewport.getWidth(), m_he / viewport.getHeight());
    double scale = clamp(Math.pow(1.01, delta), minPix, maxPix);

    Point2D mouse = imageViewToImage(m_imageView, new Point2D(e.getX(), e.getY()));

    double newWidth = viewport.getWidth() * scale;
    double newHeight = viewport.getHeight() * scale;
    /**
     * To keep the visual point under the mouse from moving,
     *
     * we need (x - newViewportMinX) / (x - currentViewportMinX) = scale where x
     * is the mouse X coordinate in the image
     *
     * solving this for newViewportMinX gives
     *
     * newViewportMinX = x - (x - currentViewportMinX) * scale
     *
     * we then clamp this value so the image never scrolls out of the imageview:
     */
    double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale, 0, m_wi - newWidth);
    double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale, 0, m_he - newHeight);
    m_imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
  }

  protected void doZoom(String p_cc, KeyEvent p_event) {
    // System.out.printf("GpsFotoViewer2.doZoom(%s)\n", p_cc);
    double delta = 0;
    switch (p_cc) {
      case "+":
        delta = -10;
        break;
      case "-":
        delta = +10;
        break;
      case "*":
        reset(m_imageView, m_wi, m_he);
        delta = 0;
        break;
      default:
        delta = 0;
    }
    if (delta == 0)
      return;
    Rectangle2D viewport = m_imageView.getViewport();
    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
    double minPix = Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight());
    // don't scale so that we're bigger than image dimensions:
    double maxPix = Math.max(m_wi / viewport.getWidth(), m_he / viewport.getHeight());
    double scale = clamp(Math.pow(1.01, delta), minPix, maxPix);
    double wi = viewport.getWidth();
    double he = viewport.getHeight();
    Point2D mouse = imageViewToImage(m_imageView, new Point2D(wi / 2, he / 2));

    double newWidth = wi * scale;
    double newHeight = he * scale;
    double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale, 0, m_wi - newWidth);
    double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale, 0, m_he - newHeight);
    m_imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
  }

  private void impostaIco() {
    InputStream stre = getClass().getResourceAsStream(IMAGE_FOTO_ICO);
    if (stre == null)
      stre = getClass().getClassLoader().getResourceAsStream(IMAGE_FOTO_ICO);
    if (stre != null) {
      Image ico = new Image(stre);
      getIcons().add(ico);
    } else
      s_log.debug("Non trovo l'ICO {}", IMAGE_FOTO_ICO);
  }

  private ImageView caricaImgDaFile(GeoCoord p_fi) {
    if ( !p_fi.hasFotoFile())
      return null;
    m_imageFile = p_fi.getFotoFile().toFile();
    m_image = new Image(m_imageFile.toURI().toString());
    m_wi = m_image.getWidth();
    m_he = m_image.getHeight();
    m_imageView = new ImageView(m_image);
    EExifRotation rot = getRotation(p_fi.getFotoFile());
    if (null != rot)
      switch (rot) {
        case Horizontal:
          break;
        case Mirror_270:
          s_log.info("Immagine ruotata e Mirror 270 Gradi");
          break;
        case Mirror_90:
          s_log.info("Immagine ruotata e Mirror 90 Gradi");
          break;
        case Mirror_hor:
          s_log.info("Immagine Mirror horizontale");
          break;
        case Mirror_vert:
          s_log.info("Immagine Mirror verticale");
          break;
        case Rotate_180:
          m_imageView.setRotate(180);
          s_log.info("Immagine ruotata di 180 Gradi");
          break;
        case Rotate_270:
          m_imageView.setRotate(90);
          s_log.info("Immagine ruotata di 270 Gradi");
          break;
        case Rotate_90:
          m_imageView.setRotate( -90);
          s_log.info("Immagine ruotata di 90 Gradi");
          break;
        default:
          break;
      }
    m_imageView.setPreserveRatio(true);
    m_imageView.setSmooth(true);
    m_imageView.setCache(true);
    reset(m_imageView, m_wi / 2, m_he / 2);
    return m_imageView;
  }

  private EExifRotation getRotation(Path p_fotoFile) {
    EExifRotation ret = EExifRotation.Horizontal;
    ImageMetadata metadata = null;
    try {
      metadata = Imaging.getMetadata(p_fotoFile.toFile());
    } catch (IOException e) {
      s_log.error("Errore Lettura metadata! file={} ", p_fotoFile.toString(), e);
      return ret;
    }

    TiffImageMetadata exif = null;
    if (metadata instanceof JpegImageMetadata) {
      exif = ((JpegImageMetadata) metadata).getExif();
    } else if (metadata instanceof TiffImageMetadata) {
      exif = (TiffImageMetadata) metadata;
    } else {
      s_log.info("Sul file {} mancano completamente le info EXIF!", p_fotoFile.toString());
      // return;
    }
    if (exif == null)
      return ret;
    try {
      Short rot = (Short) exif.getFieldValue(EXIF_TAG_ORIENTATION);
      // System.out.printf("FSFoto.leggiExifDtOriginal(%s)\n", obj.getClass().getSimpleName());
      if (null != rot)
        ret = EExifRotation.parse(rot);
    } catch (ImagingException e) {
      s_log.error("Errore su Exif Rotation! file={} ", p_fotoFile.toString(), e);
    }
    return ret;
  }

  /**
   * convert mouse coordinates in the imageView to coordinates in the actual
   * image:
   *
   * @param imageView
   * @param imageViewCoordinates
   * @return
   */
  private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
    double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
    double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

    Rectangle2D viewport = imageView.getViewport();
    return new Point2D(viewport.getMinX() + xProportion * viewport.getWidth(),
        viewport.getMinY() + yProportion * viewport.getHeight());
  }

  private void reset(ImageView imageView, double width, double height) {
    imageView.setViewport(new Rectangle2D(0, 0, width, height));
  }

  // shift the viewport of the imageView by the specified delta, clamping so
  // the viewport does not move off the actual image:
  private void shift(ImageView imageView, Point2D delta) {
    Rectangle2D viewport = imageView.getViewport();

    double width = imageView.getImage().getWidth();
    double height = imageView.getImage().getHeight();

    double maxX = width - viewport.getWidth();
    double maxY = height - viewport.getHeight();

    double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
    double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

    imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
  }

  private double clamp(double value, double min, double max) {
    if (value < min)
      return min;
    if (value > max)
      return max;
    return value;
  }

  protected void doRightLeft(KeyCode code) {
    boolean bOk = false;
    int qta = tblvRecDB.getItems().size();
    int indx = tblvRecDB.getSelectionModel().getSelectedIndex();
    GeoCoord geo = tblvRecDB.getSelectionModel().getSelectedItem();
    boolean bLoop = true;
    switch (code) {
      case UP:
      case LEFT:
        while (bLoop) {
          tblvRecDB.getFocusModel().focusPrevious();
          indx--;
          if (indx < 0) {
            bLoop = false;
            break;
          }
          tblvRecDB.getSelectionModel().select(indx);
          geo = tblvRecDB.getSelectionModel().getSelectedItem();
          if (geo != null && //
              geo.getSrcGeo() == EGeoSrcCoord.foto && //
              geo.getFotoFile() != null) {
            bLoop = false;
            bOk = true;
          }

        }
        break;
      case DOWN:
      case RIGHT:
        while (bLoop) {
          tblvRecDB.getFocusModel().focusNext();
          indx++;
          if (indx >= qta) {
            bLoop = false;
            break;
          }
          tblvRecDB.getSelectionModel().select(indx);
          geo = tblvRecDB.getSelectionModel().getSelectedItem();
          if (geo != null && //
              geo.getSrcGeo() == EGeoSrcCoord.foto && //
              geo.getFotoFile() != null) {
            bLoop = false;
            bOk = true;
          }
        }
        break;
      default:
        break;
    }
    if ( !bOk)
      return;

    tblvRecDB.getSelectionModel().select(indx);
    Platform.runLater(() -> {
      tblvRecDB.requestFocus();
      int i = tblvRecDB.getSelectionModel().getSelectedIndex();
      tblvRecDB.getSelectionModel().select(i);
      i = i > 1 ? i - 2 : i;
      tblvRecDB.scrollTo(i);
    });
    if (null != geo)
      mostraImmagine(geo);
  }

  @Override
  public void propertyChange(PropertyChangeEvent p_evt) {
    // System.out.println("GpsFotoViewer.propertyChange():" + p_evt.toString());
    @SuppressWarnings("unchecked") TableRow<GeoCoord> row = (TableRow<GeoCoord>) p_evt.getNewValue();
    GeoCoord geo = row.getItem();
    if ( !geo.hasFotoFile())
      return;
    mostraImmagine(geo); // -- 1
  }

  private WindowEvent windowClosing(WindowEvent t1) {
    // System.out.println("GpsFotoViewer.windowClosing()");
    prod.closeWindow(this);
    return t1;
  }

}

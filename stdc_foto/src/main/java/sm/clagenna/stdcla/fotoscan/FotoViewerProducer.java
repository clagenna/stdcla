package sm.clagenna.stdcla.fotoscan;

import java.beans.PropertyChangeSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.control.TableRow;
import javafx.stage.Stage;
import sm.clagenna.stdcla.geo.GeoCoord;

public class FotoViewerProducer {

  private static final Logger   s_log = LogManager.getLogger(FotoViewerProducer.class);
  private PropertyChangeSupport m_listeners;
  private int                   qtaListeners;
  private Stage                 primaryStage;

  public FotoViewerProducer(Stage primStage) {
    m_listeners = new PropertyChangeSupport(this);
    primaryStage = primStage;
    qtaListeners = 0;
  }

  public GpsFotoViewer2 creaFotoViewer(TableRow<GeoCoord> p_row) {
    if (qtaListeners != 0)
      return null;
    GpsFotoViewer2 retVwr = new GpsFotoViewer2(this);
    retVwr.showImage(p_row);
    m_listeners.addPropertyChangeListener(retVwr);
    qtaListeners++;
    GeoCoord geo = p_row.getItem();
    s_log.info("Creo foto viewer per {}", geo.toStringSimple());
    return retVwr;
  }

  public void closeWindow(GpsFotoViewer2 p_gpsFotoViewer) {
    m_listeners.removePropertyChangeListener(p_gpsFotoViewer);
    qtaListeners = 0;
  }

  public void newEvent(String p_string, TableRow<GeoCoord> p_row) {
    // System.out.println("FotoViewerProducer.newEvent():" + p_row.getItem().toStringSimple());
    TableRow<GeoCoord> p_old = new TableRow<>();
    m_listeners.firePropertyChange(p_string, p_old, p_row);
  }

  public Stage getPrimaryStage() {
    return primaryStage;
  }

}

package sm.clagenna.stdcla.fotoscan;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.geo.GeoCoordFoto;

public class FotoReader {
  private static final Logger s_log          = LogManager.getLogger(FotoReader.class);
  private static final int    MIN_WIDTH_FOTO = 64;

  /** flag per transform img in grigi */
  @Getter @Setter
  private boolean grayed;
  /** la dimensione della larghezza (resized) della foto */
  @Getter @Setter
  private double  desiredSize;
  @Getter @Setter
  private double  width;
  @Getter @Setter
  private double  height;
  private FSJpeg  fsjpg;
  @Getter @Setter
  private double  currWidth;
  @Getter @Setter
  private double  currHeight;
  @Getter @Setter
  private int     bitXpixel;
  @Getter @Setter
  private Path    fotoFile;

  @Getter @Setter
  private BufferedImage               imgOrig;
  @Getter @Setter
  private BufferedImage               currImg;
  @Getter @Setter
  private transient JpegImageMetadata jpegMetadata;
  @Getter @Setter
  private transient TiffImageMetadata exif;

  public FotoReader() {
    init();
  }

  public FotoReader(Path p_foto) {
    init();
    fotoFile = p_foto;
  }

  public FotoReader(GeoCoordFoto p_foto) {
    init();
    fotoFile = p_foto.getFotoFile();
  }

  public FotoReader(String p_fo) {
    fotoFile = Paths.get(p_fo);
  }

  private void init() {
    grayed = false;
    desiredSize = 0.;
    width = 0.;
    height = 0.;
    currWidth = 0.;
    currHeight = 0.;
    bitXpixel = 0;
  }

  public BufferedImage readFotoFile(Path p_f) throws ImagingException, IOException {
    setFotoFile(p_f);
    return readFotoFile();
  }

  public BufferedImage readFotoFile() throws ImagingException, IOException {
    return readFotoFile((int) desiredSize, grayed);
  }

  public BufferedImage readFotoFile(int newWidth, boolean bGrayes) throws ImagingException, IOException {
    fsjpg = new FSJpeg();
    fsjpg.setExifParseable(true);
    fsjpg.setPath(fotoFile);
    setDesiredSize(newWidth);
    setGrayed(bGrayes);
    readFileImage();
    // no redim && no graying
    if (newWidth < MIN_WIDTH_FOTO && !bGrayes)
      return imgOrig;
    currImg = redimFoto(currImg, newWidth, bGrayes);
    return currImg;
  }

  //
  //  private ImageMetadata readMetadataJpg() throws ImagingException, IOException {
  //    File jpegImageFile = getFotoFile().toFile();
  //    exif = null;
  //    jpegMetadata = null;
  //
  //    try {
  //      jpegMetadata = (JpegImageMetadata) Imaging.getMetadata(jpegImageFile);
  //      ImageInfo ii = Imaging.getImageInfo(jpegImageFile);
  //      width = ii.getWidth();
  //      height = ii.getHeight();
  //      bitXpixel = ii.getBitsPerPixel();
  //    } catch (IllegalArgumentException | IOException e) {
  //      s_log.error("Err read Metadata: \"{}\"", e.getMessage());
  //      return jpegMetadata;
  //    }
  //    return jpegMetadata;
  //  }
  //  private void readMetadataJpg() throws FileNotFoundException {
  //    fsjpg = new FSJpeg(getFotoFile());
  //
  //  }

  public Path getTempFileName() {
    Path pth = getFotoFile();
    Path parent = pth.getParent();
    //    String szNam = String.format("%s%stmp_%s", //
    //        parent.toAbsolutePath().toString(),//
    //        FileSystems.getDefault().getSeparator(), //
    //        pth.getFileName().toString());
    Path ret = Paths.get(//
        parent.toAbsolutePath().toString() //
        , "tmp_" //
        , pth.getFileName().toString());
    return ret;
  }

  public void writeFotoFile(String foOut, String pType) throws IOException {
    File fiOu = new File(foOut);
    try {
      ImageIO.write(currImg, pType, fiOu);
    } catch (IOException e) {
      s_log.error("Error writing foto {}", foOut);
      throw e;
    }
    addExifData(fiOu);
  }

  /**
   * Questa legge lil file di foto alle sue dimensioni reali
   *
   * @return
   */
  private BufferedImage readFileImage() {
    try {
      if (null == imgOrig) {
        imgOrig = ImageIO.read(getFotoFile().toFile());
        currImg = imgOrig;
      }
    } catch (IOException e) {
      s_log.error("Image not found ! file = {}", getFotoFile().toString(), e);
    }
    return currImg;
  }

  /**
   * Ridimensiona l'immagine utilizzando il nuovo newWidth per calcolare
   * proporzionalmente l'altezza della nuova foto
   *
   * @param lImg
   *          l'immagine da ridimensionare
   * @param newWidth
   *          la nuova larghezza della foto
   * @return l'immagine ridimensionata
   */
  private BufferedImage redimFoto(BufferedImage pImg, int newWidth, boolean bGrayed) {
    double rapporto = (double) newWidth / (double) pImg.getWidth();
    currWidth = newWidth;
    currHeight = pImg.getHeight() * rapporto;
    int tip = bGrayed ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB;
    BufferedImage lRedim = new BufferedImage((int) currWidth, (int) currHeight, tip);
    Graphics2D grph = null;
    try {
      grph = lRedim.createGraphics();
      grph.drawImage(pImg, 0, 0, (int) currWidth, (int) currHeight, null);
    } catch (Exception e) {
      s_log.error("Error redimFoto: {}", e.getMessage());
      lRedim = null;
    } finally {
      if (null != grph)
        grph.dispose();
      grph = null;
    }
    return lRedim;
  }

  private void addExifData(File fiOu) {
    FSFoto fsfo = null;
    try {
      fsfo = new FSJpeg(fiOu.toPath());
    } catch (FileNotFoundException e) {
      s_log.error("Cannot change EXIF info on {}, err={}", fiOu.getName(), e.getMessage());
      return;
    }

    fsfo.setDtAcquisizione(fsjpg.getAcquisizione());
    // fsfo.setDtCreazione(fsjpg.getDtCreazione());
    fsfo.setDtUltModif(fsjpg.getDtUltModif());
    fsfo.setLatitude(fsjpg.getLatitude());
    fsfo.setLongitude(fsjpg.getLongitude());
    fsfo.setInterpolato(true);
    fsfo.setRotation(fsjpg.getRotation());
    fsfo.cambiaExifInfoOnFile();
  }

}

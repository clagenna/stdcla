package sm.clagenna.stdcla.geo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GpsInfo;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

/**
 * Gestione specializzata delle Coordinate Geografiche specifca per i file di
 * foto (JPG, HEIC) per cui oltre alla gestione dei dati di {@link GeoCoord} qui
 * abbiamo aggiunto:
 * <ul>
 * <li>il dir della foto</li>
 * <li>il nome file della foto</li>
 * <li>la {@link LocalDateTime} {@link #getDtAssunta()} da assumere e
 * privileggiare in base a {@link EExifPriority}</li>
 * <li>la {@link LocalDateTime} {@link #getDtCreazione()} di creazione file</li>
 * <li>la {@link LocalDateTime} {@link #getDtUltModif()} di ult. modifica
 * file</li>
 * <li>la {@link LocalDateTime} {@link #getDtAcquisizione()} di acquisizione
 * della foto</li>
 * </ul>
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GeoCoordFoto extends GeoCoord implements Serializable {
  private static final long   serialVersionUID = 5812233941539440255L;
  private static final Logger s_log            = LogManager.getLogger(GeoCoordFoto.class);

  @SuppressWarnings("unused")
  private static final int          MIN_WIDTH_FOTO       = 64;
  private static final int          TAG_OFFSET_TIME      = 0x9010;
  private static final TagInfoAscii EXIF_TAG_OFFSET_TIME = new TagInfoAscii("OffsetTime", TAG_OFFSET_TIME, 20,
      TiffDirectoryType.TIFF_DIRECTORY_ROOT);

  public static ZoneOffset s_zoneOffSet;

  private EExifPriority exifPriority;
  private LocalDateTime dtAssunta;
  private LocalDateTime dtNomeDir;
  private LocalDateTime dtNomeFile;
  private LocalDateTime dtCreazione;
  private LocalDateTime dtUltModif;
  private LocalDateTime dtAcquisizione;
  private BufferedImage image;
  private double        widthCurr;
  private BufferedImage imageRedim;
  private boolean       grayedFoto;
  private BufferedImage imgGrayed;

  @Getter @Setter
  private double width;
  @Getter @Setter
  private double height;
  @Getter @Setter
  private int    bitXpixel;

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient ImageMetadata       m_metadata;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient JpegImageMetadata   m_jpegMetadata;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient TiffImageMetadata   m_exif;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient TiffOutputSet       m_outputSet;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient TiffOutputDirectory m_rootDir;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  private transient TiffOutputDirectory m_exifDir;

  static {
    s_zoneOffSet = OffsetDateTime.now().getOffset();
  }

  public GeoCoordFoto() {
    super();
    widthCurr = -1;
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(Path p_jpg) {
    super(EGeoSrcCoord.foto);
    init(p_jpg);
  }

  public GeoCoordFoto(double p_lat, double p_lon) {
    super(p_lat, p_lon);
    init(null);
  }

  public GeoCoordFoto(LocalDateTime pdt, double p_lat, double p_lon) {
    super(pdt, p_lat, p_lon);
    init(null);
  }

  public GeoCoordFoto(LocalDateTime pdt, double p_lat, double p_lon, double p_alt) {
    super(pdt, p_lat, p_lon, p_alt);
    init(null);
  }

  public GeoCoordFoto(EGeoSrcCoord p_v) {
    super(p_v);
    init(null);
  }

  private void init(Path p_jpg) {
    widthCurr = -1;

    setFotoFile(p_jpg);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoord esaminaFotoFile(Path p_jpg) {
    setFotoFile(p_jpg);
    setSrcGeo(EGeoSrcCoord.foto);
    return esaminaFotoFile();
  }

  public GeoCoord esaminaFotoFile() {
    leggiExifInfos2();
    leggiDtParentDir();
    interpretaDateTimeDaNomefile();
    leggiFilesAttributes();
    switch (exifPriority) {
      case ExifFileDir:
        dtAssunta = daExifADir();
        break;
      case FileDirExif:
        dtAssunta = daFileADir();
        break;
      case DirFileExif:
        dtAssunta = daDirAFile();
        break;
      default:
        break;
    }
    setTstamp(dtAssunta);
    return this;
  }

  @SuppressWarnings("unused")
  private void leggiExifInfos() {
    GeoList geolist = new GeoList();
    GeoScanJpg scj = new GeoScanJpg(geolist);
    GeoCoord geo = scj.gestFileJpg(getFotoFile());
    if (geo != null)
      this.assign(geo);
  }

  private void leggiExifInfos2() {
    try {
      readMetadataJpg();
    } catch (IOException e) {
      s_log.error("Errore lettura EXIF \"{}\", err={}", getFotoFile().toString(), e.getMessage());
      return;
    }
    if (null == m_exif)
      return;
    String szDt = null;
    String szZoneOfset = null;
    try {
      String[] arr = null;
      // provo a leggere la data di acquisizione
      if (null != m_exif)
        arr = m_exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      if (arr != null && arr.length > 0) {
        szDt = arr[0];
      }
      if (szDt != null)
        dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
      // provo a leggere Time Offset
      if (null != m_exif)
        arr = m_exif.getFieldValue(EXIF_TAG_OFFSET_TIME);
      if (arr != null && arr.length > 0) {
        szZoneOfset = arr[0];
        if ( !Utils.isValue(szZoneOfset) && arr.length > 1)
          szZoneOfset = arr[1];
      }
      if (Utils.isValue(szZoneOfset) && !szZoneOfset.matches("[+\\-][0-9:]+"))
        szZoneOfset = "+" + szZoneOfset;
      try {
        // provo ad interpretare il ZoneOffset
        @SuppressWarnings("unused") ZoneOffset zof = null;
        if (null != szZoneOfset)
          zof = ZoneOffset.of(szZoneOfset);
        // arrivo qui se è interpretabile
      } catch (Exception e) {
        // non è parse-able, lo annullo
        s_log.error("Errore Zone Offset \"{}\" su  \"{}\", err={}", szZoneOfset, getFotoFile().toString(), e.getMessage());
        szZoneOfset = null;
      }
      if ( !Utils.isValue(szZoneOfset))
        szZoneOfset = "+01:00";
    } catch (ImagingException | DateTimeParseException e) {
      s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }
    // provo a leggere le info GPS
    GpsInfo gpsi = null;
    try {
      if (null != m_exif)
        gpsi = m_exif.getGpsInfo();
      if (gpsi != null) {
        // setLongitude(gpsi.getLongitudeAsDegreesEast());
        // setLatitude(gpsi.getLatitudeAsDegreesNorth());
        setLongitude(getLongitudeAsDegreesEast(gpsi));
        setLatitude(getLatitudeAsDegreesNorth(gpsi));
      }
    } catch (ImagingException | DateTimeParseException e) {
      s_log.error("Errore leggi GPS \"{}\", err={}", getFotoFile().getFileName().toString(), e.getMessage());
    }
    if (null != dtAcquisizione)
      setTstamp(dtAcquisizione);
    parseZoneOffset(szZoneOfset);
    setSrcGeo(EGeoSrcCoord.foto);
  }

  /**
   * Questa legge lil file di foto alle sue dimensioni reali
   * 
   * @return
   */
//  public BufferedImage getImage() { // !!
//    try {
//      if (null == image) {
//        image = ImageIO.read(getFotoFile().toFile());
//        imgGrayed = null;
//      }
//    } catch (IOException e) {
//      s_log.error("Image not found ! file = {}", getFotoFile().toString(), e);
//      e.printStackTrace();
//    }
//    return image;
//  }
//
//  public BufferedImage getImage(int newWidth, boolean bGrayed) {
//    BufferedImage lImg = getImage();
//    if (newWidth < MIN_WIDTH_FOTO && !bGrayed)
//      return lImg;
//    if (newWidth >= MIN_WIDTH_FOTO) {
//      if (newWidth != widthCurr || bGrayed != grayedFoto) {
//        lImg = redimFoto(lImg, newWidth, bGrayed);
//        imageRedim = lImg;
//        widthCurr = newWidth;
//        grayedFoto = bGrayed;
//        if (grayedFoto)
//          imgGrayed = imageRedim;
//      } else
//        lImg = imageRedim;
//    }
//    return lImg;
//  }

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
//  private BufferedImage redimFoto(BufferedImage lImg, int newWidth, boolean bGrayed) {
//    double rapporto = (double) newWidth / (double) lImg.getWidth();
//    int lwi = newWidth;
//    int lhe = (int) ((double) lImg.getHeight() / rapporto);
//    int tip = bGrayed ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_BYTE_GRAY;
//    BufferedImage lRedim = new BufferedImage(lwi, lhe, tip);
//    Graphics2D grph = null;
//    try {
//      grph = lRedim.createGraphics();
//      grph.drawImage(lImg, 0, 0, lwi, lhe, null);
//    } catch (Exception e) {
//      s_log.error("Error redimFoto: {}", e.getMessage());
//      lRedim = null;
//    } finally {
//      if (null != grph)
//        grph.dispose();
//      grph = null;
//    }
//    return lRedim;
//  }

//  public BufferedImage getGrayed() {
//    try {
//      if (null == image)
//        image = ImageIO.read(getFotoFile().toFile());
//      if (null == imgGrayed)
//        imgGrayed = convertToGrayscale(image);
//    } catch (IOException e) {
//      s_log.error("Image not found ! file = {}", getFotoFile().toString(), e);
//      e.printStackTrace();
//    }
//    return imgGrayed;
//  }

//  public BufferedImage getGrayed(double pNewWidth) {
//    try {
//      if (null == image) {
//        image = ImageIO.read(getFotoFile().toFile());
//      }
//      if (null == imgGrayed || pNewWidth != widthCurr)
//        convertToGrayscale(image, pNewWidth);
//    } catch (IOException e) {
//      s_log.error("Image not found ! file = {}", getFotoFile().toString(), e);
//      e.printStackTrace();
//    }
//    return image;
//  }

//  private BufferedImage convertToGrayscale(BufferedImage img) {
//    widthCurr = img.getWidth();
//    imgGrayed = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//    imgGrayed.getGraphics().drawImage(img, 0, 0, null);
//    return imgGrayed;
//  }

//  private BufferedImage convertToGrayscale(BufferedImage img, double newWidth) {
//    widthCurr = (int) newWidth;
//    double width = img.getWidth();
//    double height = img.getHeight();
//    double propz = width / height;
//    int nwi = (int) newWidth;
//    int nhe = (int) (newWidth / propz);
//
//    imgGrayed = new BufferedImage(nwi, nhe, BufferedImage.TYPE_BYTE_GRAY);
//    Graphics2D grph2d = null;
//    try {
//      grph2d = imgGrayed.createGraphics();
//      grph2d.drawImage(imgGrayed, 0, 0, nwi, nhe, null);
//      imgGrayed.getGraphics().drawImage(img, 0, 0, null);
//    } finally {
//      grph2d.dispose();
//    }
//    return imgGrayed;
//  }

  /**
   * Visto un bug nella classe {@link TiffImageMetadata.GpsInfo} nella funzione
   * <code>getLongitudeAsDegreesEast</code> dove cerca di interpretare
   * <code>longitudeRef</code> contenente &quot;S?&quot;<br/>
   * Es: per north - "N" ho <code>hex:4e ef bf bd</code><br/>
   * che e' la conversione in UTF-8 del carattere per il replace per UNICODE che
   * non e' convertibile in un certo Code Page
   *
   * @param gpsi
   * @return
   * @throws ImagingException
   */
  private double getLongitudeAsDegreesEast(GpsInfo gpsi) throws ImagingException {
    final double result = gpsi.longitudeDegrees.doubleValue() //
        + gpsi.longitudeMinutes.doubleValue() / 60.0 //
        + gpsi.longitudeSeconds.doubleValue() / 3600.0;
    //    byte[] bb = gpsi.longitudeRef.getBytes();
    //    byte[] bb1 = gpsi.longitudeRef.getBytes(StandardCharsets.UTF_8);
    //    byte[] b2 = gpsi.longitudeRef.substring(0, 1).getBytes();
    //    HexFormat hex = HexFormat.of();
    //    String sz = hex.formatHex(bb);
    //    String sz1 = hex.formatHex(bb1);
    //    String sz2 = hex.formatHex(b2);
    if (gpsi.longitudeRef.trim().substring(0, 1).equalsIgnoreCase("e")) {
      return result;
    }
    if (gpsi.longitudeRef.trim().substring(0, 1).equalsIgnoreCase("w")) {
      return -result;
    }
    throw new ImagingException("Unknown longitude ref: \"" + gpsi.longitudeRef + "\"");
  }

  private double getLatitudeAsDegreesNorth(GpsInfo gpsi) throws ImagingException {
    final double result = gpsi.latitudeDegrees.doubleValue() //
        + gpsi.latitudeMinutes.doubleValue() / 60.0 //
        + gpsi.latitudeSeconds.doubleValue() / 3600.0;

    if (gpsi.latitudeRef.trim().substring(0, 1).equalsIgnoreCase("n")) {
      return result;
    }
    if (gpsi.latitudeRef.trim().substring(0, 1).equalsIgnoreCase("s")) {
      return -result;
    }
    throw new ImagingException("Unknown longitude ref: \"" + gpsi.longitudeRef + "\"");
  }

  private ImageMetadata readMetadataJpg() throws ImagingException, IOException {
    File jpegImageFile = getFotoFile().toFile();
    m_exif = null;
    m_outputSet = null;
    m_metadata = null;
    m_jpegMetadata = null;
    m_rootDir = null;
    m_exifDir = null;

    try {
      m_metadata = Imaging.getMetadata(jpegImageFile);
      ImageInfo ii = Imaging.getImageInfo(jpegImageFile);
      width = ii.getWidth();
      height = ii.getHeight();
      bitXpixel = ii.getBitsPerPixel();
    } catch (IllegalArgumentException | IOException e) {
      s_log.error("Err read Metadata: \"{}\"", e.getMessage());
      return m_jpegMetadata;
    }
    m_jpegMetadata = (JpegImageMetadata) m_metadata;
    if (null != m_jpegMetadata) {
      m_exif = m_jpegMetadata.getExif();
      if (null != m_exif) {
        m_outputSet = m_exif.getOutputSet();
      }
    }
    if (null == m_outputSet)
      m_outputSet = new TiffOutputSet();
    m_rootDir = m_outputSet.getOrCreateRootDirectory();
    m_exifDir = m_outputSet.getOrCreateExifDirectory();
    return m_jpegMetadata;
  }

  private LocalDateTime daExifADir() {
    LocalDateTime dt = goodTime(dtAcquisizione);
    if (null == dt /* || dtNomeFile.isBefore(dt) */)
      dt = goodTime(dtNomeFile);
    if (null == dt /* || dtNomeDir.isBefore(dt) */)
      dt = goodTime(dtNomeDir);
    return dt;
  }

  private LocalDateTime daFileADir() {
    // FIXME decidere se privileggiare la data piu antecedente
    LocalDateTime dt = goodTime(dtNomeFile);
    if (null == dt /* || dtNomeDir.isBefore(dt) */)
      dt = goodTime(dtNomeDir);
    if (null == dt)
      dt = goodTime(dtAcquisizione);
    if (null == dt /* || dtCreazione.isBefore(dt) */)
      dt = dtCreazione.isBefore(dtUltModif) ? dtCreazione : dtUltModif;
    dt = goodTime(dt);
    return dt;
  }

  private LocalDateTime goodTime(LocalDateTime dt) {
    return dt != null && dt.isBefore(LocalDateTime.MAX) ? dt : null;
  }

  private LocalDateTime daDirAFile() {
    LocalDateTime dt = goodTime(dtNomeDir);
    if (null == dt)
      dt = goodTime(dtNomeFile);
    if (null == dt)
      dt = goodTime(dtAcquisizione);
    if (null == dt)
      dt = dtCreazione.isBefore(dtUltModif) ? dtCreazione : dtUltModif;
    dt = goodTime(dt);
    return dt;
  }

  private void leggiDtParentDir() {
    Path parent = getFotoFile().getParent();
    String sz = parent.getName(parent.getNameCount() - 1).toString();
    dtNomeDir = ParseData.guessData(sz);
    if (dtNomeDir == null) {
      s_log.trace("Parent Dir name No e' DateTime :" + sz);
      dtNomeDir = LocalDateTime.MAX;
    } else if (getTstamp() == null)
      setTstamp(dtNomeDir);

  }

  private void interpretaDateTimeDaNomefile() {
    dtNomeFile = null;
    Path ft = getFotoFile();
    String sz = ft.getName(ft.getNameCount() - 1).toString();
    int n = sz.lastIndexOf(".");
    if (n > 0)
      sz = sz.substring(0, n);
    //    if (sz.startsWith("23-"))
    //      System.out.println("Trovato");
    setDtNomeFile(ParseData.guessData(sz));
    if (null == dtNomeFile) {
      if (EExifPriority.DirFileExif == exifPriority || //
          EExifPriority.FileDirExif == exifPriority)
        s_log.warn("File name {} no e' DateTime", sz);
      else
        s_log.trace("File name no e' DateTime :" + sz);
      dtNomeFile = LocalDateTime.MAX;
    } else if (getTstamp() == null)
      setTstamp(dtNomeFile);
  }

  private void leggiFilesAttributes() {
    BasicFileAttributes attr = null;
    dtUltModif = LocalDateTime.MAX;
    dtCreazione = LocalDateTime.MAX;
    try {
      attr = Files.readAttributes(getFotoFile(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      s_log.error("Errore {} lettura attr file {}", e.getMessage(), getFotoFile().toString());
      return;
    }
    OffsetDateTime ux2 = toOfsetDatetime(attr.lastModifiedTime());
    setDtUltModif(LocalDateTime.ofInstant(ux2.toInstant(), s_zoneOffSet).withNano(0));
    ux2 = toOfsetDatetime(attr.creationTime());
    setDtCreazione(LocalDateTime.ofInstant(ux2.toInstant(), s_zoneOffSet).withNano(0));
  }

  private OffsetDateTime toOfsetDatetime(FileTime ux) {
    LocalDateTime ux1 = LocalDateTime.ofInstant(ux.toInstant(), s_zoneOffSet);
    OffsetDateTime ux2 = ux1.atOffset(s_zoneOffSet);
    return ux2;
  }

  @Override
  public String toString() {
    String sz = super.toString();
    String szAss = null != dtAssunta ? GeoFormatter.s_fmtmY4MD_hms.format(dtAssunta) : "-";
    String szDir = null != dtNomeDir ? GeoFormatter.s_fmtmY4MD_hms.format(dtNomeDir) : "-";
    String szFil = null != dtNomeFile ? GeoFormatter.s_fmtmY4MD_hms.format(dtNomeFile) : "-";
    String szCrea = null != dtCreazione ? GeoFormatter.s_fmtmY4MD_hms.format(dtCreazione) : "-";
    String szAcq = null != dtAcquisizione ? GeoFormatter.s_fmtmY4MD_hms.format(dtAcquisizione) : "-";
    sz += String.format("\n\tass:%s\n\tacq:%s\n\tdir:%s\n\tfil:%s\n\tcrea:%s", szAss, szAcq, szDir, szFil, szCrea);
    return sz;
  }
}

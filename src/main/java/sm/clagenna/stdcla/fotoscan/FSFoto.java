package sm.clagenna.stdcla.fotoscan;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GpsInfo;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.fieldtypes.AbstractFieldType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import sm.clagenna.stdcla.enums.EExifRotation;
import sm.clagenna.stdcla.enums.ETipoCambioNome;
import sm.clagenna.stdcla.utils.AppProperties;
import sm.clagenna.stdcla.utils.ParseData;

/**
 * per le costatnti Exif vedi
 * <a href="https://exiftool.org/TagNames/EXIF.html">Exif tools</a>
 */
public abstract class FSFoto extends FSFile {
  private static final String       CSZ_FOTOFILEPATTERN  = "'f'yyyyMMdd_HHmmss";
  private static final TagInfoShort EXIF_TAG_ORIENTATION = new TagInfoShort("Orientation", 0x0112,
      TiffDirectoryType.EXIF_DIRECTORY_EXIF_IFD);

  private static final List<DateTimeFormatter> s_arrFmt;
  /** questa funziona col fuso estivo, verificare in inverno se va */
  //  private static final ZoneOffset              s_offset            = ZoneOffset.of("+01:00");
  private static ZoneOffset                    s_zoneOffSet;
  private static ZoneId                        s_zone;

  static {
    s_arrFmt = new ArrayList<DateTimeFormatter>();
    FSFoto.s_arrFmt.add(DateTimeFormatter.ofPattern("'f'yyyyMMdd'_'HHmmss"));
    FSFoto.s_arrFmt.add(DateTimeFormatter.ofPattern("'WhatsApp Image 'yyyy-MM-dd' at 'HH.mm.ss"));
    FSFoto.s_arrFmt.add(DateTimeFormatter.ofPattern("yyyy-MM-dd' at 'HH.mm.ss"));

    s_zone = ZoneId.systemDefault();
    LocalDateTime now = LocalDateTime.now();
    s_zoneOffSet = s_zone.getRules().getOffset(now);
  }

  /** da EXIF_TAG_DATE_TIME_ORIGINAL */
  private boolean       m_bFileInError;
  private LocalDateTime dtNomeFile;
  private LocalDateTime dtCreazione;
  private LocalDateTime dtUltModif;
  private LocalDateTime dtAcquisizione;
  private LocalDateTime dtParentDir;
  private LocalDateTime dtAssunta = null;
  private EExifRotation rotation;

  private boolean m_bExifParseable = true;

  enum CosaFare {
    setNomeFile, //
    setDtCreazione, //
    setUltModif, //
    setDtAcquisizione
  }

  private Set<CosaFare> m_daFare;

  public FSFoto() {
    super();
  }

  public FSFoto(Path p_fi) throws FileNotFoundException {
    super(p_fi);
  }

  @Override
  public void setPath(Path p_fi) throws FileNotFoundException {
    super.setPath(p_fi);

    m_bFileInError = false;
    leggiFilesAttributes();
    interpretaDateTimeDaNomefile();
    leggiExifDtOriginal();
    leggiDtParentDir();
    //    System.out.println(this.toString());
  }

  private void leggiExifDtOriginal() {
    if ( !isExifParseable())
      return;
    ImageMetadata metadata = null;
    File fi = getPath().toFile();
    try {
      metadata = Imaging.getMetadata(fi);
    } catch (IOException e) {
      // manda in crisi la pipe in FileSystemVisitatore:34
      // setFileInError(true);
      getLogger().error("Errore Lettura metadata:" + fi.getAbsolutePath(), e);
      return;
    }
    // getLogger().info("-------->" + getPath().getFileName());
    TiffImageMetadata exif = null;
    if (metadata instanceof JpegImageMetadata) {
      exif = ((JpegImageMetadata) metadata).getExif();
    } else if (metadata instanceof TiffImageMetadata) {
      exif = (TiffImageMetadata) metadata;
    } else {
      getLogger().info("Sul file {} mancano completamente le info EXIF!", fi.getName());
      // return;
    }
    if (exif == null)
      return;
    String szDt = null;
    try {
      // String[] arr = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      //      if (arr != null && arr.length > 0) {
      //        szDt = arr[0];
      //      }
      TiffField tfld = exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      if (tfld != null) {
        szDt = tfld.getStringValue();
      }
      if (szDt != null)
        dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
    } catch (DateTimeParseException | ImagingException e) {
      // setFileInError(true);
      getLogger().error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }

    // 2024-09-11 Cerco il campo orientation
    try {
      setRotation(EExifRotation.Horizontal);
      Short rot = (Short) exif.getFieldValue(EXIF_TAG_ORIENTATION);
      // System.out.printf("FSFoto.leggiExifDtOriginal(%s)\n", obj.getClass().getSimpleName());
      if (null != rot)
        rotation = EExifRotation.parse(rot);
    } catch (ImagingException e) {
      // e.printStackTrace();
      getLogger().error("Errore leggi Rotation, err={}", e.getMessage());
    }
    // -------------------------------------

    try {
      // GPSInfo gpsi = exif.getGPS();
      GpsInfo gpsi = exif.getGpsInfo();
      if (gpsi != null) {
        setLongitude(gpsi.getLongitudeAsDegreesEast());
        setLatitude(gpsi.getLatitudeAsDegreesNorth());
      }
    } catch (ImagingException | DateTimeParseException e) {
      // setFileInError(true);
      getLogger().error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }

  }

  public String toStringExif() {
    StringBuilder sb = new StringBuilder();
    ImageMetadata metadata = null;
    try {
      metadata = Imaging.getMetadata(getPath().toFile());
    } catch (IOException e) {
      getLogger().error("Lettura metadata", e);
      return null;
    }
    // getLogger().info("-------->" + getPath().getFileName());
    TiffImageMetadata exif = null;
    if (metadata instanceof JpegImageMetadata) {
      exif = ((JpegImageMetadata) metadata).getExif();
    } else if (metadata instanceof TiffImageMetadata) {
      exif = (TiffImageMetadata) metadata;
    } else {
      getLogger().info("Sul file {} mancano completamente le info EXIF!", getPath().toString());
      // return;
    }

    @SuppressWarnings("unchecked") List<TiffImageMetadata.TiffMetadataItem> items = (List<TiffImageMetadata.TiffMetadataItem>) exif
        .getItems();
    /**
     * <pre>
     *
     * -------->f20031030_174032.jpg
    271   (0x10f:   Make):  'PENTAX Corporation' (19 ASCII)
    272   (0x110:   Model):   'PENTAX Optio 550' (17 ASCII)
    282   (0x11a:   XResolution):   72 (1 Rational)
    283   (0x11b:   YResolution):   72 (1 Rational)
    296   (0x128:   ResolutionUnit):  2 (1 Short)
    305   (0x131:   Software):  '1.00' (5 ASCII)
    306   (0x132:   DateTime):  '2003:10:30 17:40:31' (20 ASCII)
    531   (0x213:   YCbCrPositioning):  2 (1 Short)
    34665   (0x8769:  ExifOffset):  502 (1 Long)
    50341   (0xc4a5:  PrintIM):   80, 114, 105, 110, 116, 73, 77, 0, 48, 50, 53, 48, 0, 0, 12, 0, 1, 0, 22, 0, 22, 0, 2, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0... (128) (128 Undefined)
    33434   (0x829a:  ExposureTime):  10/2050 (0,005) (1 Rational)
    33437   (0x829d:  FNumber):   3 (1 Rational)
    36864   (0x9000:  ExifVersion):   48, 50, 50, 48 (4 Undefined)
    36867   (0x9003:  DateTimeOriginal):  '2003:10:30 17:40:31' (20 ASCII)
    36868   (0x9004:  DateTimeDigitized):   '2003:10:30 17:40:31' (20 ASCII)
    37121   (0x9101:  ComponentsConfiguration):   1, 2, 3, 0 (4 Undefined)
    37122   (0x9102:  CompressedBitsPerPixel):  5 (1 Rational)
    37380   (0x9204:  ExposureCompensation):  0 (1 SRational)
    37381   (0x9205:  MaxApertureValue):  3 (1 Rational)
    37383   (0x9207:  MeteringMode):  2 (1 Short)
    37385   (0x9209:  Flash):   9 (1 Short)
    37386   (0x920a:  FocalLength):   78/10 (7,8) (1 Rational)
    37500   (0x927c:  MakerNote):   65, 79, 67, 0, 0, 0, 41, 0, 1, 0, 3, 0, 1, 0, 0, 0, 2, 0, 0, 0, 2, 0, 3, 0, 1, 0, 0, 0, -128, 2, -32, 1, 3, 0, 4, 0, 1, 0, 0, 0, 122, -97, 0, 0, 4, 0, 4, 0, 1, 0, 0... (1238) (1238 Undefined)
    40960   (0xa000:  FlashpixVersion):   48, 49, 48, 48 (4 Undefined)
    40961   (0xa001:  ColorSpace):  1 (1 Short)
    40962   (0xa002:  ExifImageWidth):  2592 (1 Long)
    40963   (0xa003:  ExifImageLength):   1944 (1 Long)
    40965   (0xa005:  InteropOffset):   922 (1 Long)
    41985   (0xa401:  CustomRendered):  0 (1 Short)
    41986   (0xa402:  ExposureMode):  0 (1 Short)
    41987   (0xa403:  WhiteBalance):  0 (1 Short)
    41988   (0xa404:  DigitalZoomRatio):  0 (1 Rational)
    41989   (0xa405:  FocalLengthIn35mmFormat):   37 (1 Short)
    41990   (0xa406:  SceneCaptureType):  0 (1 Short)
    41992   (0xa408:  Contrast):  0 (1 Short)
    41993   (0xa409:  Saturation):  0 (1 Short)
    41994   (0xa40a:  Sharpness):   0 (1 Short)
    41996   (0xa40c:  SubjectDistanceRange):  3 (1 Short)
    1   (0x1:   InteroperabilityIndex):   'R98' (4 ASCII)
    2   (0x2:   InteroperabilityVersion):   48, 49, 48, 48 (4 Undefined)
    259   (0x103:   Compression):   6 (1 Short)
    282   (0x11a:   XResolution):   72 (1 Rational)
    283   (0x11b:   YResolution):   72 (1 Rational)
    296   (0x128:   ResolutionUnit):  2 (1 Short)
    513   (0x201:   JpgFromRawStart):   4084 (1 Long)
    514   (0x202:   JpgFromRawLength):  7811 (1 Long)
     *
     * </pre>
     */
    sb.append("File:").append(getPath()).append("\n");
    boolean bGPSDone = false;
    double altitude = 0;
    for (TiffImageMetadata.TiffMetadataItem item : items) {
      TiffField fld;
      String sz = "";
      try {
        fld = item.getTiffField();
        String szNam = fld.getTagInfo().name;
        if (szNam.startsWith("GPSAlt")) {
          altitude = fld.getDoubleValue();
          continue;
        }
        if (szNam.startsWith("GPS")) {
          if (bGPSDone)
            continue;
          bGPSDone = true;
          GpsInfo gpsi = exif.getGpsInfo();
          if (null != gpsi) {
            // lonRef = gpsi.longitudeRef;
            setLongitude(gpsi.getLongitudeAsDegreesEast());
            setLatitude(gpsi.getLatitudeAsDegreesNorth());
            sz = String.format("0x0001\t(Degree)\t%-16s Lon %s %.10f Lat %s %.10f Alt %f", //
                "GPSInfo", //
                gpsi.longitudeRef, //
                gpsi.getLongitudeAsDegreesEast(), //
                gpsi.latitudeRef, //
                gpsi.getLatitudeAsDegreesNorth(), altitude);
          }
        } else {
          sz = String.format("0x%04X\t(%s[%d])\t%-16s %s", //
              fld.getTagInfo().tag, //
              fld.getFieldType().getName(), //
              fld.getCount(), //
              fld.getTagInfo().name, //
              fld.getValue().toString());
          // sb.append(fld.toString()).append("\n");
        }
        System.out.println(sz);
        sb.append(sz).append("\n");
      } catch (ImagingException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }

  private void leggiFilesAttributes() {
    BasicFileAttributes attr = null;
    try {
      attr = Files.readAttributes(getPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      getLogger().error("Errore lettura attr files", e);
      return;
    }
    OffsetDateTime ux2 = toOfsetDatetime(attr.lastModifiedTime());
    setDtUltModif(LocalDateTime.ofInstant(ux2.toInstant(), s_zoneOffSet).withNano(0));
    ux2 = toOfsetDatetime(attr.creationTime());
    setDtCreazione(LocalDateTime.ofInstant(ux2.toInstant(), s_zoneOffSet).withNano(0));
  }

  private void interpretaDateTimeDaNomefile() {
    dtNomeFile = null;
    Path pth = getPath();
    String sz = pth.getName(pth.getNameCount() - 1).toString();
    int n = sz.lastIndexOf(".");
    if (n > 0)
      sz = sz.substring(0, n);
    //    if (sz.startsWith("23-"))
    //      System.out.println("Trovato");
    setDtNomeFile(ParseData.guessData(sz));
    if (dtNomeFile == null)
      getLogger().debug("File name no e' DateTime :" + sz);

  }

  private void leggiDtParentDir() {
    Path pth = getParent();
    if (pth == null) {
      getLogger().debug("Il dir padre non ha data interpretabile:*NULL*");
      return;
    }
    String sz = pth.getName(pth.getNameCount() - 1).toString();
    dtParentDir = ParseData.parseData(sz);
    if (dtParentDir == null)
      getLogger().trace("No e' DateTime Parent Dir name :" + sz);
  }

  public LocalDateTime getDtAcquisizione() {
    return dtAcquisizione;
  }

  public void setDtAcquisizione(LocalDateTime p_dtAcquisizione) {
    dtAcquisizione = p_dtAcquisizione;
  }

  public LocalDateTime getDtCreazione() {
    return dtCreazione;
  }

  public void setDtCreazione(LocalDateTime p_dtCreazione) {
    dtCreazione = p_dtCreazione;
  }

  public LocalDateTime getDtUltModif() {
    return dtUltModif;
  }

  public void setDtUltModif(LocalDateTime p_dtUltModif) {
    dtUltModif = p_dtUltModif;
  }

  public LocalDateTime getDtNomeFile() {
    return dtNomeFile;
  }

  public void setDtNomeFile(LocalDateTime p_dtNomeFile) {
    dtNomeFile = p_dtNomeFile;
  }

  public LocalDateTime getDtAssunta() {
    return dtAssunta;
  }

  public void setDtAssunta(LocalDateTime p_dtAssunta) {
    dtAssunta = p_dtAssunta;
  }

  public LocalDateTime getDtParentDir() {
    return dtParentDir;
  }

  public void setDtParentDir(LocalDateTime p_dt) {
    dtParentDir = p_dt;
  }

  private void studiaIlDaFarsi() {
    String szPath = getPath().toString();
    getLogger().trace("Analizzo {}", szPath);
    //    if (szPath.contains("2023-12-04"))
    //      System.out.println("trovato");

    m_daFare = new HashSet<>();
    // 2024-09-11 tolta dipendenza da AppPropreties
    //    ImgModel mod = AppProperties.getInst().getModel();
    //    EExifPriority prio = mod.getPriority();

    switch (getModel().getPriority()) {
      case ExifFileDir:
        studiaConExifFileDir();
        break;
      case DirFileExif:
        sudiaConDirFiExif();
        break;
      case FileDirExif:
        sudiaConFiDirExif();
        break;
      default:
        break;
    }
  }

  /**
   * segue la priorità in tabella per l'assegnazione
   * <table>
   * <tr>
   * <th></th>
   * <th>ExFiDi</th>
   * <th>DiFiEx</th>
   * <th>FiDiEx</th>
   * <tr>
   * <tr>
   * <td>attuale</td>
   * <tr>
   * <tr>
   * <td>creazione</td>
   * <td>4</td>
   * <td>3</td>
   * <td>4</td>
   * <tr>
   * <tr>
   * <td>ultmodif</td>
   * <td>5</td>
   * <td>5</td>
   * <td>5</td>
   * <tr>
   * <tr>
   * <td>acquisiz</td>
   * <td>1</td>
   * <td>4</td>
   * <td>3</td>
   * <tr>
   * <tr>
   * <td>parent</td>
   * <td>3</td>
   * <td>1</td>
   * <td>2</td>
   * <tr>
   * <tr>
   * <td>nomefile</td>
   * <td>2</td>
   * <td>2</td>
   * <td>1</td>
   * <tr>
   * </table>
   */
  private void studiaConExifFileDir() {
    // String szPath = getPath().toString();
    //    if (szPath.contains("heic"))
    //      System.out.println("trovato");

    LocalDateTime dt = null;
    if (dtAcquisizione != null)
      dt = dtAcquisizione;
    if (dt == null && dtNomeFile != null)
      dt = dtNomeFile;
    if (dt == null && dtParentDir != null)
      dt = dtParentDir;
    if (dt == null && dtCreazione != null)
      dt = dtCreazione;
    if (dt == null && dtUltModif != null)
      dt = dtUltModif;

    /*
     * se la dtAcquisizione *non* c'è allora prediligo la dtCreazione purchè non
     * sia troppo distante dalla calcolata dt (<15 gg) 2023-12-15: Tolto perchè
     * cozza contro la dtNomeFile
     */
    //    if (dtAcquisizione == null && dtCreazione != null) {
    //      long mins = Duration.between(dtCreazione, dt).toMinutes();
    //      final long MAXMIN = 24 * 60 * 15;
    //      if (Math.abs(mins) < MAXMIN)
    //        dt = dtCreazione;
    //    }

    if (dtAcquisizione == null) {
      dtAcquisizione = dt;
      m_daFare.add(CosaFare.setDtAcquisizione);
      m_daFare.add(CosaFare.setNomeFile);
      m_daFare.add(CosaFare.setDtCreazione);
      m_daFare.add(CosaFare.setUltModif);
    }
    if (dtNomeFile == null) {
      dtNomeFile = dt;
      m_daFare.add(CosaFare.setNomeFile);
      m_daFare.add(CosaFare.setUltModif);
    }

    if (dtCreazione == null)
      m_daFare.add(CosaFare.setDtCreazione);

    if (dtUltModif == null)
      m_daFare.add(CosaFare.setUltModif);

    if ( !dtAcquisizione.isEqual(dt)) {
      m_daFare.add(CosaFare.setDtAcquisizione);
      m_daFare.add(CosaFare.setNomeFile);
      m_daFare.add(CosaFare.setDtCreazione);
      m_daFare.add(CosaFare.setUltModif);
    }
    if ( !dtNomeFile.isEqual(dt)) {
      m_daFare.add(CosaFare.setNomeFile);
      m_daFare.add(CosaFare.setDtCreazione);
      m_daFare.add(CosaFare.setUltModif);
    }
    if ( !dtCreazione.isEqual(dt)) {
      m_daFare.add(CosaFare.setDtCreazione);
      m_daFare.add(CosaFare.setUltModif);
    }

    if ( !dtUltModif.isEqual(dt)) {
      m_daFare.add(CosaFare.setUltModif);
    }

    if ( !m_daFare.contains(CosaFare.setNomeFile)) {
      String szNam = creaNomeFile();
      if ( !getPath().endsWith(szNam))
        m_daFare.add(CosaFare.setNomeFile);
    }
    setDtAssunta(dt);
  }

  private void sudiaConDirFiExif() {
    LocalDateTime dt = null;
    if (dtParentDir != null) {
      dt = dtParentDir;
    }
    if (dt == null)
      if (dtNomeFile != null)
        dt = dtNomeFile;
    if (dt == null)
      if (dtAcquisizione != null)
        dt = dtAcquisizione;
    if ( !m_daFare.contains(CosaFare.setNomeFile)) {
      String szNam = creaNomeFile();
      if ( !getPath().endsWith(szNam))
        m_daFare.add(CosaFare.setNomeFile);
    }
    m_daFare.add(CosaFare.setDtAcquisizione);
    setDtNomeFile(dt);
    setDtAssunta(dt);
  }

  private void sudiaConFiDirExif() {
    LocalDateTime dt = null;
    if (dtNomeFile != null)
      dt = dtNomeFile;
    if (dt == null)
      if (dtParentDir != null)
        dt = dtParentDir;
    if (dt == null)
      if (dtAcquisizione != null)
        dt = dtAcquisizione;
    setDtNomeFile(dt);
    setDtAssunta(dt);
    if ( !m_daFare.contains(CosaFare.setNomeFile)) {
      String szNam = creaNomeFile();
      if ( !getPath().endsWith(szNam))
        m_daFare.add(CosaFare.setNomeFile);
    }
  }

  public boolean isDaAggiornare() {
    return m_daFare != null && m_daFare.size() != 0;
  }

  public Set<CosaFare> getCosaFare() {
    return m_daFare;
  }

  public LocalDateTime getPiuVecchiaData() {
    LocalDateTime dt = LocalDateTime.MAX;
    if (dtAssunta != null)
      return dtAssunta;
    // verifico le altre ...
    //    if ( dtNomeFile != null && dtNomeFile.equals(LocalDateTime.MAX))
    //      if ( dtParentDir != null)
    //        dt = dtParentDir;
    if (dtNomeFile != null && dt.isAfter(dtNomeFile))
      dt = dtNomeFile;
    if (dtCreazione != null && dt.isAfter(dtCreazione))
      dt = dtCreazione;
    if (dtUltModif != null && dt.isAfter(dtUltModif))
      dt = dtUltModif;

    if (dtAcquisizione != null) {
      if (dt.isAfter(dtAcquisizione))
        dt = dtAcquisizione;
    } else {
      if (dtNomeFile != null)
        if (dt.isAfter(dtNomeFile))
          dt = dtNomeFile;
      // se non ha dtAcq allora prendo il parent Dt
      if (dtParentDir != null)
        if (dt.isAfter(dtParentDir))
          dt = dtParentDir;
    }
    return dt;
  }

  public void cambiaNomeFile() {
    //    String szPath = getPath().toString().toLowerCase();
    //    if (szPath.contains("f20230621_150426_01."))
    //      System.out.println("trovato");

    LocalDateTime dt = getPiuVecchiaData();
    String fnam = creaNomeFile(dt);
    String fnamExt = fnam;
    Path pthFrom = getPath();
    Path pthTo = Paths.get(getParent().toString(), fnam);
    if (pthFrom.compareTo(pthTo) == 0)
      return;
    int k = 1; // loop
    AppProperties app = AppProperties.getInstance();
    ETipoCambioNome tipoambio = app.getTipoCambioNome();
    while (Files.exists(pthTo, LinkOption.NOFOLLOW_LINKS)) {
      getLogger().debug("change fil.nam: {} esiste!", pthTo.getFileName());
      String szExt = String.format("_%02d.", k);
      //      String sz = fnam.replace(".", String.format("_%d.", k++));
      //      pthTo = Paths.get(getParent().toString(), sz);
      if (k++ > 1000)
        throw new UnsupportedOperationException("Troppi loop sul nome file:" + pthFrom.toString());
      switch (tipoambio) {
        case conSuffisso:
          fnamExt = fnam.replace(".", szExt);
          break;
        case piu1Minuto:
          dt = dt.plusMinutes(1);
          fnamExt = creaNomeFile(dt);
          break;
        case piu1Secondo:
          dt = dt.plusSeconds(1);
          fnamExt = creaNomeFile(dt);
          break;
      }
      pthTo = Paths.get(getParent().toString(), fnamExt);
      if (pthTo.compareTo(pthFrom) == 0) {
        getLogger().info("No rename per {}", pthFrom.toString());
        return;
      }
    }
    setDtAssunta(dt);
    try {
      // CopyOption[] opt = { StandardCopyOption.COPY_ATTRIBUTES };
      // Files.move(pthFrom, pthTo, opt);
      getLogger().info("Cambio nome da \"{}\" a \"{}\"", //
          pthFrom.getName(pthFrom.getNameCount() - 1), //
          pthTo.getName(pthTo.getNameCount() - 1) //
      );
      Files.move(pthFrom, pthTo, new CopyOption[0]);
      setPath(pthTo);
    } catch (IOException e) {
      getLogger().error("Errore rename per {}", getPath().toString(), e);
    }
    //    ISwingLogger isw = AppProperties.getInst().getSwingLogger();
    //    Date dt2 = Date.from(dt.atZone(ZoneId.of("GMT")).toInstant());
    //    isw.addRow(pthFrom.getFileName().toString(), fnam, pthFrom.getParent().toAbsolutePath(), dt2);
  }

  public String creaNomeFile() {
    return creaNomeFile(null);
  }

  private String creaNomeFile(LocalDateTime p_dt) {
    if (p_dt == null)
      p_dt = getPiuVecchiaData();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(FSFoto.CSZ_FOTOFILEPATTERN);
    String fnam = String.format("%s.%s", fmt.format(p_dt), getFileExtention());
    return fnam;
  }

  public void cambiaDtCreazione() {
    LocalDateTime dt = getPiuVecchiaData();
    // Instant inst = dt.toInstant(OffsetDateTime.now().getOffset());
    Instant inst = dt.toInstant(s_zoneOffSet);
    FileTime tm = FileTime.fromMillis(inst.toEpochMilli());

    try {
      // Files.setAttribute(getPath(), "creationTime", tm, LinkOption.NOFOLLOW_LINKS);
      Files.setAttribute(getPath(), "creationTime", tm);
    } catch (IOException e) {
      getLogger().error("Errore di set CreatedTime per {}", getPath().toString(), e);
    }
  }

  public void cambiaDtUltModif() {
    LocalDateTime dt = getPiuVecchiaData();
    Instant inst = dt.toInstant(s_zoneOffSet);
    FileTime tm = FileTime.from(inst);

    try {
      Files.setAttribute(getPath(), "lastModifiedTime", tm, LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      getLogger().error("Errore di set CreatedTime per {}", getPath().toString(), e);
    }
  }

  private Path backupFotoFile() {
    Path pthCopy = Paths.get(getParent().toString(), UUID.randomUUID().toString() + "." + getFileExtention());
    try {
      Files.copy(getPath(), pthCopy, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      pthCopy = null;
      getLogger().error("Errore {} backup file per {}", e.getMessage(), getPath().toString(), e);
    }
    return pthCopy;
  }

  public void cambiaExifInfoOnFile() {
    if (this instanceof FSTiff) {
      getLogger().error("Non cambio EXIF per {}", getPath().toString());
      return;
    }
    boolean bOk = true;
    Path pthCopy = backupFotoFile();
    if (pthCopy == null)
      return;
    File jpegImageFile = pthCopy.toFile();
    File dst = getPath().toFile();
    try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos)) {
      TiffOutputSet outputSet = null;
      JpegImageMetadata jpegMetadata = (JpegImageMetadata) Imaging.getMetadata(jpegImageFile);
      TiffImageMetadata exif = null;
      if (jpegMetadata != null)
        exif = jpegMetadata.getExif();
      // TiffOutputSet class sono i dati EXIF che devo andare a scrivere
      if (exif != null)
        outputSet = exif.getOutputSet();

      // Se il file non contiene metadata EXIF, ne creiamo uno vuoto
      if (null == outputSet)
        outputSet = new TiffOutputSet();

      TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
      // se devo cambio le coordinate GPS alla foto
      var fLatLon = getLongitude() * getLatitude();
      if (isInterpolato() && fLatLon != 0) {
        outputSet.setGpsInDegrees(getLongitude(), getLatitude());
      }
      // sovrascrivo la dtAcq con la data ottenuta perche adesso deve diventare quella!
      dtAcquisizione = getPiuVecchiaData();
      String szDt = ParseData.s_fmtDtExif.format(dtAcquisizione);

      TiffOutputField dd = new TiffOutputField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, AbstractFieldType.ASCII, szDt.length(),
          szDt.getBytes());
      exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      exifDirectory.add(dd);
      // printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);

      new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
    } catch (FileNotFoundException e) {
      bOk = false;
      getLogger().error("Errore sul file {}", pthCopy.toString(), e);
    } catch (IOException e) {
      bOk = false;
      getLogger().error("Errore I/O sul file {}", pthCopy.toString(), e);
    } 
    try {
      if (bOk)
        Files.delete(pthCopy);
      else
        Files.move(pthCopy, getPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      getLogger().error("Errore di cancellazione di {}", pthCopy.toString(), e);
    }
  }

  public void cambiaGpsCoordinate() {
    if (this instanceof FSTiff) {
      getLogger().error("Non cambio EXIF per {}", getPath().toString());
      return;
    }
    if ( !isInterpolato()) {
      getLogger().debug("Non cambio GPS per {}, non e' interpolato", getPath().toString());
      return;
    }
    if (getLongitude() * getLatitude() == 0) {
      getLogger().debug("Non cambio GPS per {}, non ha coordinate!", getPath().toString());
      return;
    }
    getLogger().debug("Aggiungo coord. GPS per {}", getPath().toString());
    boolean bOk = true;
    Path pthCopy = backupFotoFile();
    if (pthCopy == null)
      return;
    File jpegImageFile = pthCopy.toFile();
    File dst = getPath().toFile();
    try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos);) {
      TiffOutputSet outputSet = null;
      // note that metadata might be null if no metadata is found.
      final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
      final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
      if (null != jpegMetadata) {
        // note that exif might be null if no Exif metadata is found.
        final TiffImageMetadata exif = jpegMetadata.getExif();

        if (null != exif) {
          // TiffImageMetadata class is immutable (read-only).
          // TiffOutputSet class represents the Exif data to write.
          //
          // Usually, we want to update existing Exif metadata by
          // changing
          // the values of a few fields, or adding a field.
          // In these cases, it is easiest to use getOutputSet() to
          // start with a "copy" of the fields read from the image.
          outputSet = exif.getOutputSet();
        }
      }

      // if file does not contain any exif metadata, we create an empty
      // set of exif metadata. Otherwise, we keep all of the other
      // existing tags.
      if (null == outputSet)
        outputSet = new TiffOutputSet();
      // final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
      outputSet.setGpsInDegrees(getLongitude(), getLatitude());
      new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
    } catch (FileNotFoundException e) {
      bOk = false;
      getLogger().error("Errore sul file {}", pthCopy.toString(), e);
    } catch (IOException e) {
      bOk = false;
      getLogger().error("Errore I/O sul file {}", pthCopy.toString(), e);
    } 
    try {
      if (bOk)
        Files.delete(pthCopy);
      else
        Files.move(pthCopy, getPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      getLogger().error("Errore di cancellazione di {}", pthCopy.toString(), e);
    }

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getPath().toString()) //
        .append("\n\t");
    sb.append("   Oldest Data:\t" + getPiuVecchiaData().toString()) //
        .append("\n\t");
    /*
     * dtNomeFile; dtCreazione; dtUltModif; dtAcquisizione;
     */
    sb.append("    dtNomeFile:\t" + (dtNomeFile != null ? dtNomeFile.toString() : "*NULL*")) //
        .append("\n\t");
    sb.append("   dtCreazione:\t" + (dtCreazione != null ? dtCreazione.toString() : "*NULL*")) //
        .append("\n\t");
    sb.append("    dtUltModif:\t" + (dtUltModif != null ? dtUltModif.toString() : "*NULL*")) //
        .append("\n\t");
    sb.append("dtAcquisizione:\t" + (dtAcquisizione != null ? dtAcquisizione.toString() : "*NULL*")) //
        .append("\n\t");
    sb.append("dtParentDir:\t" + (dtParentDir != null ? dtParentDir.toString() : "*NULL*")) //
        .append("\n\t");

    return sb.toString();
  }

  protected void setFileInError(boolean bv) {
    m_bFileInError = bv;
  }

  public boolean isFileInError() {
    return m_bFileInError;
  }

  public void analizzaFoto() {
    if (isFileInError()) {
      getLogger().error("Il file \"{}\" e' in errore, non lo tratto", getPath().toString());
      return;
    }
    studiaIlDaFarsi();
    if ( !isDaAggiornare()) {
      getLogger().debug("Nulla da fare con {}", getPath().toString());
      return;
    }
  }

  public void lavoraIlFile() {
    // System.out.println(toString());
    //    String szPath = getPath().toString();
    //    if (szPath.contains("2023-12-04"))
    //      System.out.println("trovato");

    Set<CosaFare> df = getCosaFare();
    // il cambio nome ha priorita perche imposta anche     dtAssunta
    if (df.contains(CosaFare.setNomeFile)) {
      cambiaNomeFile();
      df.remove(CosaFare.setNomeFile);
    }
    for (CosaFare che : df) {
      switch (che) {
        case setDtAcquisizione:
          cambiaExifInfoOnFile();
          break;
        case setDtCreazione:
          cambiaDtCreazione();
          cambiaDtUltModif();
          break;
        case setNomeFile:
          cambiaNomeFile();
          break;
        case setUltModif:
          cambiaDtUltModif();
          break;
      }
    }
  }

  private OffsetDateTime toOfsetDatetime(FileTime ux) {
    LocalDateTime ux1 = LocalDateTime.ofInstant(ux.toInstant(), s_zoneOffSet);
    OffsetDateTime ux2 = ux1.atOffset(s_zoneOffSet);
    return ux2;
  }

  public LocalDateTime getItem(String p_colName) {
    LocalDateTime retDt = null;
    if (getPath().toString().contains("19860723")) {
      System.out.println("FSFoto.getItem() f=" + getPath().toString());
    }
    switch (p_colName) {
      case FSFile.COL04_DTASSUNTA:
        retDt = getDtAssunta();
        break;
      case FSFile.COL05_DTNOMEFILE:
        retDt = getDtNomeFile();
        break;
      case FSFile.COL06_DTCREAZIONE:
        retDt = getDtCreazione();
        break;
      case FSFile.COL07_DTULTMODIF:
        retDt = getDtUltModif();
        break;
      case FSFile.COL08_DTACQUISIZIONE:
        retDt = getDtAcquisizione();
        break;
      case FSFile.COL09_DTPARENTDIR:
        retDt = getDtParentDir();
        break;
    }
    return retDt;
  }

  public boolean isExifParseable() {
    return m_bExifParseable;
  }

  public void setExifParseable(boolean bv) {
    m_bExifParseable = bv;
  }

  public EExifRotation getRotation() {
    return rotation;
  }

  public void setRotation(EExifRotation p_rotation) {
    rotation = p_rotation;
  }
}

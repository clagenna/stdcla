package sm.clagenna.stdcla.geo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.enums.EExifPriority;
import sm.clagenna.stdcla.sys.ex.GeoFileException;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

@Data
public class GeoScanJpg {
  private static final Logger s_log = LogManager.getLogger(GeoScanJpg.class);

  private static final int          TAG_OFFSET_TIME      = 0x9010;
  private static final TagInfoAscii EXIF_TAG_OFFSET_TIME = new TagInfoAscii("OffsetTime", TAG_OFFSET_TIME, 20,
      TiffDirectoryType.TIFF_DIRECTORY_ROOT);

  private GeoList             geolist;
  private boolean             addSimilFoto;
  private boolean             recurseDirs;
  private Path                startDir;
  private EExifPriority       exifPrio;
  private TiffOutputSet       m_outputSet;
  private ImageMetadata       m_metadata;
  private JpegImageMetadata   m_jpegMetadata;
  private TiffImageMetadata   m_exif;
  private TiffOutputDirectory m_rootDir;
  private TiffOutputDirectory m_exifDir;

  public GeoScanJpg(GeoList p_gl) {
    geolist = p_gl;
    exifPrio = EExifPriority.ExifFileDir;
  }

  public GeoList scanDir(Path p_pth) throws GeoFileException {
    setStartDir(p_pth);
    if ( !Files.exists(p_pth)) {
      throw new GeoFileException("Non esiste il dir:" + p_pth.toString());
    }
    if (geolist == null)
      geolist = new GeoList();
    scandir();
    return geolist;
  }

  private void scandir() throws GeoFileException {
    int depth = isRecurseDirs() ? 99 : 1;
    //    try {
    //      Files.walkFileTree(startDir, new FileVisitor<Path>() {
    //
    //        @Override
    //        public FileVisitResult preVisitDirectory(Path p_dir, BasicFileAttributes p_attrs) throws IOException {
    //          // System.out.printf("preVisitDirectory(%s)\n", p_dir.toString());
    //          return FileVisitResult.CONTINUE;
    //        }
    //
    //        @Override
    //        public FileVisitResult visitFile(Path p_file, BasicFileAttributes p_attrs) throws IOException {
    //          // System.out.printf("visitFile(%s)\n", p_file.toString());
    //          String sz = p_file.getFileName().toString().toLowerCase();
    //          if (sz.endsWith(".jpg") || sz.endsWith(".jpeg") || sz.endsWith(".heic"))
    //            gestFileJpg(p_file);
    //          else if (sz.endsWith(".heic"))
    //            gestFileHeic(p_file);
    //          else
    //            s_log.debug("Scarto il file {}", p_file.getFileName());
    //          return FileVisitResult.CONTINUE;
    //        }
    //
    //        @Override
    //        public FileVisitResult visitFileFailed(Path p_file, IOException p_exc) throws IOException {
    //          // System.out.printf("visitFileFailed(%s)\n", p_file.toString());
    //          return FileVisitResult.TERMINATE;
    //        }
    //
    //        @Override
    //        public FileVisitResult postVisitDirectory(Path p_dir, IOException p_exc) throws IOException {
    //          // System.out.printf("postVisitDirectory(%s)\n", p_dir.toString());
    //          return FileVisitResult.CONTINUE;
    //        }
    //
    //      } );
    //    } catch (Exception e) {
    //      s_log.error("Errore scan dir {}, err={}", startDir.toString(), e.getMessage());
    //      throw new GeoFileException("Errore scan:" + startDir.toString(), e);
    //    }
    try (Stream<Path> stre = Files.walk(startDir, depth)) {
      stre //
          .filter(Files::isRegularFile) //
          .forEach(s -> scanFile(s));
    } catch (IOException e) {
      s_log.error("Errore scan dir {}, err={}", startDir.toString(), e.getMessage());
      throw new GeoFileException("Errore scan:" + startDir.toString(), e);
    }

  }

  private void scanFile(Path p_file) {
    String sz = p_file.getFileName().toString().toLowerCase();
    if (sz.endsWith(".jpg") || sz.endsWith(".jpeg"))
      gestFileJpg2(p_file);
    else if (sz.endsWith(".heic"))
      gestFileHeic(p_file);
    else
      s_log.debug("Scarto il file {}", p_file.getFileName());
  }

  private GeoCoord gestFileHeic(Path p_file) {
    GeoCoordFoto geox = new GeoCoordFoto(p_file);
    geox.setExifPriority(EExifPriority.FileDirExif);
    geox.esaminaFotoFile();
    if ( !geolist.contains(geox) || addSimilFoto) {
      geolist.add(geox);
      s_log.debug("Added {}", geox.toStringSimple());
    } else {
      s_log.debug("Discarded {}", geox.getFotoFile().toString());
    }
    return geox;
  }

  public GeoCoord gestFileJpg2(Path p_jpg) {
    GeoCoordFoto geo = new GeoCoordFoto(p_jpg);
    if (exifPrio == null)
      geo.setExifPriority(EExifPriority.FileDirExif);
    else
      geo.setExifPriority(exifPrio);
    geo.esaminaFotoFile();
    if ( !geolist.contains(geo) || addSimilFoto) {
      geolist.add(geo);
      s_log.debug("Added {}", geo.toStringSimple());
    } else {
      s_log.debug("Discarded {}", geo.getFotoFile().toString());
    }
    return geo;
  }

  @SuppressWarnings("unused")
  public GeoCoord gestFileJpg(Path p_jpg) {
    GeoCoord geo = null;
    final int versione = 2;
    try {
      readMetadataJPG(p_jpg);
      if (versione == 1) {
        // se manca EXIF oppure cambia la Priority
        if (null == m_exif || //
            EExifPriority.FileDirExif == exifPrio || //
            EExifPriority.DirFileExif == exifPrio) {
          // cercaInfoDaFileODir(p_jpg);
          GeoCoordFoto geox = new GeoCoordFoto(p_jpg);
          exifPrio = null == exifPrio ? EExifPriority.FileDirExif : exifPrio;
          geox.setExifPriority(exifPrio);
          geox.esaminaFotoFile();
          if ( !geolist.contains(geox) || addSimilFoto) {
            geolist.add(geox);
            s_log.debug("Added {}", geox.toStringSimple());
          } else {
            s_log.debug("Discarded {}", geox.getFotoFile().toString());
          }
          return geox;
        }
      }
    } catch (ImageReadException | ImageWriteException | IOException e) {
      s_log.error("Errore lettura EXIF \"{}\", err={}", p_jpg.toString(), e.getMessage());
    }

    geo = new GeoCoord();
    ParseData prs = new ParseData();
    LocalDateTime dtAcquisizione = null;
    LocalDateTime dtNomeFile = null;
    double longitude = 0;
    double latitude = 0;
    String szDt = null;
    String szZoneOfset = null;
    try {

      String sz = p_jpg.getName(p_jpg.getNameCount() - 1).toString();
      int n = sz.lastIndexOf(".");
      if (n > 0)
        sz = sz.substring(0, n);
      dtNomeFile = prs.guessData(sz);

      String[] arr = null;
      if (null != m_exif)
        arr = m_exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      if (arr != null && arr.length > 0) {
        szDt = arr[0];
      }
      if (szDt != null)
        dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
      if (null != m_exif)
        arr = m_exif.getFieldValue(EXIF_TAG_OFFSET_TIME);
      if (arr != null && arr.length > 0)
        szZoneOfset = arr[0];
      try {
        // provo ad interpretare quello che c'e' nel file
        ZoneOffset zof = null;
        if (null != szZoneOfset)
          zof = ZoneOffset.of(szZoneOfset);
      } catch (Exception e) {
        s_log.error("Errore Zone Offset \"{}\" su  \"{}\", err={}", szZoneOfset, p_jpg.getFileName().toString(), e.getMessage());
        szZoneOfset = null;
      }
      if ( !Utils.isValue(szZoneOfset))
        szZoneOfset = "+01:00";
    } catch (ImageReadException | DateTimeParseException e) {
      // setFileInError(true);
      s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }

    GPSInfo gpsi = null;
    try {
      if (null != m_exif)
        gpsi = m_exif.getGPS();
      if (gpsi != null) {
        longitude = gpsi.getLongitudeAsDegreesEast();
        latitude = gpsi.getLatitudeAsDegreesNorth();
      }
    } catch (ImageReadException | DateTimeParseException e) {
      s_log.error("Errore leggi GPS \"{}\", err={}", p_jpg.getFileName().toString(), e.getMessage());
    }
    if (dtAcquisizione != null
        || dtNomeFile != null /* && longitude * latitude != 0 */) {
      LocalDateTime dt = null != dtAcquisizione ? dtAcquisizione : dtNomeFile;
      geo.setTstamp(dt);
      geo.parseZoneOffset(szZoneOfset);
      geo.setLongitude(longitude);
      geo.setLatitude(latitude);
      geo.setSrcGeo(EGeoSrcCoord.foto);
      geo.setFotoFile(p_jpg);
      if ( !geolist.contains(geo) || addSimilFoto) {
        geolist.add(geo);
        s_log.debug("Geo Added {}", geo.toStringSimple());
      } else {
        s_log.error("Geo Discarded {}", geo.getFotoFile().toString());
      }
    } else {
      s_log.debug("No exif info on {}", p_jpg.toAbsolutePath().toString());
      GeoCoordFoto geox = new GeoCoordFoto(p_jpg);
      geox.setExifPriority(EExifPriority.FileDirExif);
      geox.esaminaFotoFile();
      geo = geox;
    }
    return geo;
  }
  //
  //  private GeoCoord cercaInfoDaFileODir(Path p_jpg) {
  //    switch (exifPrio) {
  //      case ExifFileDir:
  //        s_log.info("Sul file {} mancano completamente le info EXIF!", p_jpg.toString());
  //        break;
  //      default:
  //        s_log.debug("Sul file {} mancano completamente le info EXIF!", p_jpg.toString());
  //        break;
  //    }
  //    //    m_fotonx.setFotoFile(p_jpg);
  //    //    geo = m_fotonx.esaminaFotoFile();
  //    GeoCoordFoto geo = new GeoCoordFoto(p_jpg);
  //    geo.esaminaFotoFile();
  //    if ( !geolist.contains(geo) || addSimilFoto) {
  //      geolist.add(geo);
  //      s_log.debug("Added {}", geo.toStringSimple());
  //    } else {
  //      s_log.debug("Discarded {}", geo.getFotoFile().toString());
  //    }
  //    return geo;
  //  }

  public void cambiaGpsCoordinate(GeoCoord p_geo) {
    if ( !p_geo.hasLonLat() || p_geo.getFotoFile() == null) {
      s_log.debug("Non cambio GPS per {}, ", p_geo.toString());
      return;
    }
    if ( !p_geo.isGuessed()) {
      s_log.debug("Non cambio GPS per {}, non e' interpolato", p_geo.toString());
      return;
    }
    double lon = p_geo.getLongitude();
    double lat = p_geo.getLatitude();
    Path pth = p_geo.getFotoFile();
    try {
      if (Files.notExists(pth) || Files.size(pth) < 5) {
        s_log.error("Il file {} non esiste!", pth.toString());
        return;
      }
    } catch (IOException e) {
      s_log.error("Errore test size di {}, err={}", pth.toString(), e.getMessage());
    }
    s_log.info("Aggiungo coord. GPS per {}", pth.toString());
    boolean bOk = true;
    Path pthCopy = backupFotoFile(pth);
    if (pthCopy == null)
      return;
    File jpegImageFile = pthCopy.toFile();
    File dst = pth.toFile();
    String szDt = null;
    LocalDateTime dtAcquisizione = null;
    ZoneOffset zoneOffset = null;

    try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos);) {
      readMetadataJPG(pthCopy);
      if (null != m_exif) {
        try {
          String[] arr = m_exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
          if (arr != null && arr.length > 0) {
            szDt = arr[0];
          }
          if (szDt != null)
            dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
          arr = m_exif.getFieldValue(EXIF_TAG_OFFSET_TIME);
          if (null != arr && arr.length > 0)
            zoneOffset = ZoneOffset.of(arr[0]);
        } catch (ImageReadException | DateTimeParseException e) {
          s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
        }
      }
      if (null != dtAcquisizione) {
        if (zoneOffset == null)
          zoneOffset = ZoneOffset.of("+01:00");
        s_log.debug("Foto {} dt acquiziz. {} {}", pth.toString(), szDt, zoneOffset != null ? zoneOffset.toString() : " - ");
      }
      m_outputSet.setGPSInDegrees(lon, lat);
      new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, m_outputSet);
      p_geo.setGuessed(false);
    } catch (FileNotFoundException e) {
      bOk = false;
      s_log.error("Errore sul file {}", pthCopy.toString(), e);
    } catch (IOException e) {
      bOk = false;
      s_log.error("Errore I/O sul file {}", pthCopy.toString(), e);
    } catch (ImageReadException | ImageWriteException e) {
      bOk = false;
      s_log.error("Errore lettura EXIF sul file {}", pthCopy.toString(), e);
    }
    try {
      Path pthNew = removeJpgCopy(bOk, pthCopy, pth);
      if (null != pthNew && null != dtAcquisizione) {
        p_geo.setFotoFile(pthNew);
        cambiaAttrFile(pthNew, dtAcquisizione);
      }
    } catch (IOException e) {
      s_log.error("(GPS) Errore cambio attr file {}, err={}", pthCopy.toString(), e.getMessage());
    }
  }

  private Path removeJpgCopy(boolean bOk, Path p_copy, Path p_jpg) {
    Path pthNew = null;
    try {
      if (bOk) {
        Files.delete(p_copy);
        pthNew = p_jpg;
      } else {
        Files.move(p_copy, p_jpg, StandardCopyOption.REPLACE_EXISTING);
        pthNew = p_jpg;
      }
    } catch (Exception e) {
      s_log.error("Errore di cancellazione di {}", p_copy.toString(), e);
    }
    return pthNew;
  }

  public boolean cambiaTStamp(GeoCoord p_updGeo) {
    if (null == p_updGeo.getTstampNew())
      return false;
    if ( !Utils.isChanged(p_updGeo.getTstamp(), p_updGeo.getTstampNew())) {
      p_updGeo.setTstampNew(null);
      return false;
    }
    s_log.info("Cambio dtAcquis da {}  con {}", //
        GeoFormatter.s_fmtTimeZ.format(p_updGeo.getTstamp()), //
        GeoFormatter.s_fmtTimeZ.format(p_updGeo.getTstampNew()));
    p_updGeo.assumeTStampNew();
    cambiaExifDtAcqInfos(p_updGeo);
    return true;
  }

  /**
   * Qui viene fissato la data ottenuta da p_geo.getTstamp() come data da
   * fissare nel EXIF del file. Si arriva qui quando si da' la priorità
   * {@link EExifPriority#FileDirExif}
   *
   * @param p_geo
   * @return
   */
  private GeoCoord cambiaExifDtAcqInfos(GeoCoord p_geo) {
    Path pth = p_geo.getFotoFile();
    try {
      if (Files.notExists(pth) || Files.size(pth) < 5) {
        s_log.error("Il file {} non esiste!", pth.toString());
        return p_geo;
      }
    } catch (IOException e) {
      s_log.error("Errore test size di {}, err={}", pth.toString(), e.getMessage());
    }
    boolean bOk = true;
    Path pthCopy = backupFotoFile(pth);
    if (pthCopy == null)
      return p_geo;
    File jpegImageFile = pthCopy.toFile();
    File dst = pth.toFile();
    String szDt = null;
    LocalDateTime dtAcquisizione = p_geo.getTstamp();
    ZoneOffset zoneOffset = null;

    try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos);) {
      readMetadataJPG(pthCopy);

      szDt = ParseData.s_fmtDtExif.format(dtAcquisizione);
      zoneOffset = ZoneOffset.of("+01:00");
      s_log.debug("Foto {} dt acquiziz. {} {}", pth.toString(), szDt, zoneOffset != null ? zoneOffset.toString() : " - ");

      m_rootDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      m_exifDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      m_exifDir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, szDt);
      new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, m_outputSet);
    } catch (FileNotFoundException e) {
      bOk = false;
      s_log.error("Errore sul file {}", pthCopy.toString(), e);
    } catch (IOException e) {
      bOk = false;
      s_log.error("Errore I/O sul file {}", pthCopy.toString(), e);
    } catch (ImageReadException | ImageWriteException e) {
      bOk = false;
      s_log.error("Errore lettura EXIF sul file {}", pthCopy.toString(), e);
    }
    try {
      Path pthNew = removeJpgCopy(bOk, pthCopy, pth);
      if (null != pthNew) {
        p_geo.setFotoFile(pthNew);
        cambiaAttrFile(pthNew, dtAcquisizione);
      }
    } catch (IOException e) {
      s_log.error("(Chng Exif) Errore cambio attr file {}, err={}", pthCopy.toString(), e.getMessage());
    }
    return p_geo;
  }

  private ImageMetadata readMetadataJPG(Path p_jpg) throws ImageReadException, IOException, ImageWriteException {
    File jpegImageFile = p_jpg.toFile();
    m_exif = null;
    m_outputSet = null;
    m_metadata = null;
    m_jpegMetadata = null;
    m_rootDir = null;
    m_exifDir = null;

    try {
      m_metadata = Imaging.getMetadata(jpegImageFile);
    } catch (IllegalArgumentException | ImageReadException | IOException e) {
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

  public Path renameFile(GeoCoord p_geo) {
    Path pthJpg = null;

    if (null == p_geo || !p_geo.hasFotoFile())
      return pthJpg;
    if (exifPrio == EExifPriority.FileDirExif || //
        exifPrio == EExifPriority.DirFileExif) {
      p_geo = cambiaExifDtAcqInfos(p_geo);
    }
    pthJpg = p_geo.getFotoFile();
    LocalDateTime dtAcq = p_geo.getTstamp();
    String szExt = "jpg";
    String szFilNam = pthJpg.toString();
    int n = szFilNam.lastIndexOf(".");
    if (n > 0)
      szExt = szFilNam.substring(n + 1).toLowerCase();
    String szDt = ParseData.s_fmtDtFile.format(dtAcq);
    try {
      cambiaAttrFile(pthJpg, dtAcq);
    } catch (IOException e) {
      s_log.error("Errore cambia attributi file {}", pthJpg.toString());
    }
    if (szFilNam.toLowerCase().contains(szDt)) {
      s_log.debug("No rename of \"{}\" dt={}", szFilNam, szDt);
      return pthJpg;
    }
    String sep = FileSystems.getDefault().getSeparator();
    String szNew = String.format("%s%s%s.%s", pthJpg.getParent().toString(), sep, szDt, szExt);
    Path pthNew = Paths.get(szNew);
    int k = 1;
    while (Files.exists(pthNew)) {
      szNew = String.format("%s%s%s_%d.%s", pthJpg.getParent().toString(), sep, szDt, k++, szExt);
      pthNew = Paths.get(szNew);
    }
    try {
      Files.move(pthJpg, pthNew);
      cambiaAttrFile(pthNew, dtAcq);
      p_geo.setFotoFile(pthNew);
      s_log.info("Rinominata Foto {} con {}", pthJpg.getFileName().toString(), pthNew.getFileName().toString());
    } catch (IOException e) {
      s_log.error("Errore rename da {} a {}", pthJpg.toString(), pthNew.toString());
      pthNew = pthJpg;
    }
    return pthNew;
  }

  private void cambiaAttrFile(Path pthNew, FileTime timeFi) throws IOException {
    Files.setAttribute(pthNew, "creationTime", timeFi);
    Files.setAttribute(pthNew, "lastAccessTime", timeFi);
    Files.setAttribute(pthNew, "lastModifiedTime", timeFi);
    Files.setAttribute(pthNew, "basic:creationTime", timeFi);
    Files.setAttribute(pthNew, "basic:lastAccessTime", timeFi);
    Files.setAttribute(pthNew, "basic:lastModifiedTime", timeFi);
  }

  private void cambiaAttrFile(Path p_pth, LocalDateTime p_dt) throws IOException {
    FileTime timeFi = FileTime.from(p_dt.toInstant(GeoCoordFoto.s_zoneOffSet));
    cambiaAttrFile(p_pth, timeFi);
  }

  private Path backupFotoFile(Path pth) {
    String szExt = ".jpg";
    int n = pth.toString().lastIndexOf(".");
    if (n > 0)
      szExt = pth.toString().substring(n + 1);
    Path pthCopy = Paths.get(pth.getParent().toString(), UUID.randomUUID().toString() + "." + szExt);
    try {
      Files.copy(pth, pthCopy, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      pthCopy = null;
      s_log.error("Errore {} backup file per {}", e.getMessage(), pth.toString(), e);
    }
    return pthCopy;
  }

}

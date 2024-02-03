package sm.clagenna.stdcla.geo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.sys.ex.GeoFileException;
import sm.clagenna.stdcla.utils.ParseData;

@Data
public class GeoScanJpg {
  private static final Logger s_log = LogManager.getLogger(GeoScanJpg.class);

  private GeoList geolist;
  private Path    startDir;

  public GeoScanJpg(GeoList p_gl) {
    geolist = p_gl;
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
    try {
      Files.walkFileTree(startDir, new FileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path p_dir, BasicFileAttributes p_attrs) throws IOException {
          // System.out.printf("preVisitDirectory(%s)\n", p_dir.toString());
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path p_file, BasicFileAttributes p_attrs) throws IOException {
          // System.out.printf("visitFile(%s)\n", p_file.toString());
          String sz = p_file.getFileName().toString().toLowerCase();
          if (sz.endsWith(".jpg") || sz.endsWith(".jpeg"))
            gestFileJpg(p_file);
          else
            s_log.debug("Scarto il file {}", p_file.getFileName());
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path p_file, IOException p_exc) throws IOException {
          // System.out.printf("visitFileFailed(%s)\n", p_file.toString());
          return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path p_dir, IOException p_exc) throws IOException {
          // System.out.printf("postVisitDirectory(%s)\n", p_dir.toString());
          return FileVisitResult.CONTINUE;
        }

      });
    } catch (Exception e) {
      s_log.error("Errore scan dir {}, err={}", startDir.toString(), e.getMessage());
      throw new GeoFileException("Errore scan:" + startDir.toString(), e);
    }

  }

  private void gestFileJpg(Path p_jpg) {
    ImageMetadata metadata = null;
    LocalDateTime dtAcquisizione = null;
    double longitude = 0;
    double latitude = 0;
    File fi = p_jpg.toFile();
    try {
      metadata = Imaging.getMetadata(fi);
    } catch (ImageReadException | IOException e) {
      // manda in crisi la pipe in FileSystemVisitatore:34
      // setFileInError(true);
      s_log.error("Errore Lettura metadata:" + fi.getAbsolutePath(), e);
      return;
    }
    // s_log.info("-------->" + getPath().getFileName());
    TiffImageMetadata exif = null;
    if (metadata instanceof JpegImageMetadata) {
      exif = ((JpegImageMetadata) metadata).getExif();
    } else if (metadata instanceof TiffImageMetadata) {
      exif = (TiffImageMetadata) metadata;
    } else {
      s_log.info("Sul file {} mancano completamente le info EXIF!", fi.getName());
      // return;
    }
    if (exif == null)
      return;
    String szDt = null;
    try {
      String[] arr = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      if (arr != null && arr.length > 0) {
        szDt = arr[0];
      }
      if (szDt != null)
        dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
    } catch (ImageReadException | DateTimeParseException e) {
      // setFileInError(true);
      s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }

    try {
      GPSInfo gpsi = exif.getGPS();
      if (gpsi != null) {
        longitude = gpsi.getLongitudeAsDegreesEast();
        latitude = gpsi.getLatitudeAsDegreesNorth();
      }
    } catch (ImageReadException | DateTimeParseException e) {
      s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
    }
    if (dtAcquisizione != null /* && longitude * latitude != 0 */) {
      GeoCoord geo = new GeoCoord();
      geo.setTstamp(dtAcquisizione);
      geo.setLongitude(longitude);
      geo.setLatitude(latitude);
      geo.setSrcGeo(EGeoSrcCoord.foto);
      geo.setFotoFile(p_jpg);
      geolist.add(geo);
      s_log.debug("Added {}", geo.toString());
    } else {
      s_log.debug("No exif info on {}", p_jpg.toAbsolutePath().toString());
    }
  }

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

    s_log.info("Aggiungo coord. GPS per {}", pth.toString());
    boolean bOk = true;
    Path pthCopy = backupFotoFile(pth);
    if (pthCopy == null)
      return;
    File jpegImageFile = pthCopy.toFile();
    File dst = pth.toFile();
    String szDt = null;
    LocalDateTime dtAcquisizione = null;
    FileTime timeFi = null;
    BasicFileAttributeView attr = null;
    try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos);) {
      TiffOutputSet outputSet = null;
      // note that metadata might be null if no metadata is found.
      final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
      final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
      TiffImageMetadata exif = null;
      if (null != jpegMetadata) {
        // note that exif might be null if no Exif metadata is found.
        exif = jpegMetadata.getExif();

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

      if (null != exif) {

        try {
          String[] arr = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
          if (arr != null && arr.length > 0) {
            szDt = arr[0];
          }
          if (szDt != null)
            dtAcquisizione = LocalDateTime.from(ParseData.s_fmtDtExif.parse(szDt));
        } catch (ImageReadException | DateTimeParseException e) {
          // setFileInError(true);
          s_log.error("Errore leggi Dt ORIGINAL \"{}\", err={}", szDt, e.getMessage());
        }
      }
      if (null != dtAcquisizione) {
        s_log.debug("Foto {} dt acquiziz. {}", pth.toString(), szDt);
        timeFi = FileTime.from(dtAcquisizione.toInstant(ZoneOffset.UTC));
      }

      // final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
      outputSet.setGPSInDegrees(lon, lat);
      new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
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
      if (bOk) {
        Files.delete(pthCopy);
        if (null != dtAcquisizione) {
          Path pthNew = renameFile(pth, dtAcquisizione);
          attr = Files.getFileAttributeView(pthNew, BasicFileAttributeView.class);
          attr.setTimes(timeFi, timeFi, timeFi);
          p_geo.setFotoFile(pthNew);
        }
      } else
        Files.move(pthCopy, pth, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      s_log.error("Errore di cancellazione di {}", pthCopy.toString(), e);
    }

  }

  private Path renameFile(Path p_pth, LocalDateTime p_dt) {
    String szExt = "jpg";
    String szFilNam = p_pth.toString();
    int n = szFilNam.lastIndexOf(".");
    if (n > 0)
      szExt = szFilNam.substring(n + 1).toLowerCase();
    String szDt = ParseData.s_fmtDtFile.format(p_dt);
    String sep = FileSystems.getDefault().getSeparator();
    String szNew = String.format("%s%s%s.%s", p_pth.getParent().toString(), sep, szDt, szExt);
    Path pthNew = Paths.get(szNew);
    int k = 1;
    while (Files.exists(pthNew)) {
      szNew = String.format("%s%s%s_%d.%s", p_pth.getParent().toString(), sep, szDt, k++, szExt);
      pthNew = Paths.get(szNew);
    }
    try {
      Files.move(p_pth, pthNew);
    } catch (IOException e) {
      s_log.error("Errore rename da {} a {}", p_pth.toString(), pthNew.toString());
      pthNew = p_pth;
    }
    return pthNew;
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

package sm.clagenna.stdcla.geo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
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
          System.out.printf("preVisitDirectory(%s)\n", p_dir.toString());
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
          System.out.printf("visitFileFailed(%s)\n", p_file.toString());

          return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path p_dir, IOException p_exc) throws IOException {
          System.out.printf("postVisitDirectory(%s)\n", p_dir.toString());
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
    if (dtAcquisizione != null && longitude * latitude != 0) {
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

}

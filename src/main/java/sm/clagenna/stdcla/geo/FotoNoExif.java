package sm.clagenna.stdcla.geo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.enums.EExifPriority;
import sm.clagenna.stdcla.utils.ParseData;

@Data
public class FotoNoExif {
  private static final Logger s_log        = LogManager.getLogger(FotoNoExif.class);
  private static ZoneOffset   s_zoneOffSet = ZoneOffset.of("+01:00");

  private Path                fotoFile;
  private EExifPriority       exifPriority;
  private LocalDateTime       dtAssunta;
  private LocalDateTime       dtNomeDir;
  private LocalDateTime       dtNomeFile;
  private LocalDateTime       dtCreazione;
  private LocalDateTime       dtUltModif;
  private List<LocalDateTime> arrDtAssigned;

  public FotoNoExif() {
    setExifPriority(EExifPriority.ExifFileDir);
    arrDtAssigned = new ArrayList<>();
  }

  public FotoNoExif(Path p) {
    fotoFile = p;
    setExifPriority(EExifPriority.ExifFileDir);
    arrDtAssigned = new ArrayList<>();
  }

  public GeoCoord esaminaFotoFile(Path p_fo) {
    setFotoFile(p_fo);
    return esaminaFotoFile();
  }

  public GeoCoord esaminaFotoFile() {
    GeoCoord geo = new GeoCoord();
    geo.setFotoFile(fotoFile);
    geo.setSrcGeo(EGeoSrcCoord.foto);
    LocalDateTime dtAssunta = null;
    leggiDtParentDir();
    interpretaDateTimeDaNomefile();
    leggiFilesAttributes();
    switch (exifPriority) {
      case ExifFileDir:
        dtAssunta = daFileADir();
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
    while (arrDtAssigned.contains(dtAssunta))
      dtAssunta = dtAssunta.plusSeconds(5);
    arrDtAssigned.add(dtAssunta);
    geo.setTstamp(dtAssunta);
    return geo;
  }

  private LocalDateTime daFileADir() {
    LocalDateTime dt = dtNomeFile;
    if (null == dt || dtNomeDir.isBefore(dt))
      dt = dtNomeDir;
    if (null == dt || dtCreazione.isBefore(dt))
      dt = dtCreazione.isBefore(dtUltModif) ? dtCreazione : dtUltModif;
    if (dt.equals(LocalDateTime.MAX))
      dt = null;
    return dt;
  }

  private LocalDateTime daDirAFile() {
    LocalDateTime dt = dtNomeDir;
    if (null == dt)
      dt = dtNomeFile;
    if (null == dt)
      dt = dtCreazione.isBefore(dtUltModif) ? dtCreazione : dtUltModif;
    if (dt.equals(LocalDateTime.MAX))
      dt = null;
    return dt;
  }

  private void leggiDtParentDir() {
    Path parent = fotoFile.getParent();
    String sz = parent.getName(parent.getNameCount() - 1).toString();
    ParseData prs = new ParseData();
    dtNomeDir = prs.guessData(sz);
    if (dtNomeDir == null) {
      s_log.trace("No e' DateTime Parent Dir name :" + sz);
      dtNomeDir = LocalDateTime.MAX;
    }
  }

  private void interpretaDateTimeDaNomefile() {
    dtNomeFile = null;
    String sz = fotoFile.getName(fotoFile.getNameCount() - 1).toString();
    int n = sz.lastIndexOf(".");
    if (n > 0)
      sz = sz.substring(0, n);
    //    if (sz.startsWith("23-"))
    //      System.out.println("Trovato");
    ParseData prs = new ParseData();
    setDtNomeFile(prs.guessData(sz));
    if (null == dtNomeFile) {
      s_log.debug("File name no e' DateTime :" + sz);
      dtNomeFile = LocalDateTime.MAX;
    }

  }

  private void leggiFilesAttributes() {
    BasicFileAttributes attr = null;
    dtUltModif = LocalDateTime.MAX;
    dtCreazione = LocalDateTime.MAX;
    try {
      attr = Files.readAttributes(fotoFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      s_log.error("Errore {} lettura attr file {}", e.getMessage(), fotoFile.toString());
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
}

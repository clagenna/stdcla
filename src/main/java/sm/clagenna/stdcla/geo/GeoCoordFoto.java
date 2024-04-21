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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import lombok.EqualsAndHashCode;
import sm.clagenna.stdcla.enums.EExifPriority;
import sm.clagenna.stdcla.utils.ParseData;

@Data
@EqualsAndHashCode(callSuper = false)
public class GeoCoordFoto extends GeoCoord {
  private static final long   serialVersionUID = 5812233941539440255L;
  private static final Logger s_log            = LogManager.getLogger(GeoCoordFoto.class);
  private static ZoneOffset   s_zoneOffSet     = ZoneOffset.of("+01:00");

  private EExifPriority exifPriority;
  private LocalDateTime dtAssunta;
  private LocalDateTime dtNomeDir;
  private LocalDateTime dtNomeFile;
  private LocalDateTime dtCreazione;
  private LocalDateTime dtUltModif;
  private LocalDateTime dtAcquisizione;

  public GeoCoordFoto() {
    super();
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(Path p_jpg) {
    super(EGeoSrcCoord.foto);
    setFotoFile(p_jpg);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(double p_lat, double p_lon) {
    super(p_lat, p_lon);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(LocalDateTime pdt, double p_lat, double p_lon) {
    super(pdt, p_lat, p_lon);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(LocalDateTime pdt, double p_lat, double p_lon, double p_alt) {
    super(pdt, p_lat, p_lon, p_alt);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoordFoto(EGeoSrcCoord p_v) {
    super(p_v);
    setExifPriority(EExifPriority.ExifFileDir);
  }

  public GeoCoord esaminaFotoFile(Path p_jpg) {
    setFotoFile(p_jpg);
    setSrcGeo(EGeoSrcCoord.foto);
    return esaminaFotoFile();
  }

  public GeoCoord esaminaFotoFile() {
    if (exifPriority == EExifPriority.ExifFileDir)
      leggiExifInfos();
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

  private void leggiExifInfos() {
    GeoList geolist = new GeoList();
    GeoScanJpg scj = new GeoScanJpg(geolist);
    GeoCoord geo = scj.gestFileJpg(getFotoFile());
    if (geo != null)
      this.assign(geo);
  }

  private LocalDateTime daExifADir() {
    LocalDateTime dt = getTstamp();
    if (null == dt || dtNomeFile.isBefore(dt))
      dt = dtNomeFile;
    if (null == dt || dtNomeDir.isBefore(dt))
      dt = dtNomeDir;
    return dt;
  }

  private LocalDateTime daFileADir() {
    // FIXME decidere se privileggiare la data piu antecedente 
    LocalDateTime dt = dtNomeFile;
    if (null == dt  /* || dtNomeDir.isBefore(dt) */)
      dt = dtNomeDir;
    if (null == dt /* || dtCreazione.isBefore(dt) */)
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
    Path parent = getFotoFile().getParent();
    String sz = parent.getName(parent.getNameCount() - 1).toString();
    ParseData prs = new ParseData();
    dtNomeDir = prs.guessData(sz);
    if (dtNomeDir == null) {
      s_log.trace("Parent Dir name No e' DateTime :" + sz);
      dtNomeDir = LocalDateTime.MAX;
    }
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
    ParseData prs = new ParseData();
    setDtNomeFile(prs.guessData(sz));
    if (null == dtNomeFile) {
      if (EExifPriority.DirFileExif == exifPriority || //
          EExifPriority.FileDirExif == exifPriority)
        s_log.warn("File name no e' DateTime :" + sz);
      else
        s_log.trace("File name no e' DateTime :" + sz);
      dtNomeFile = LocalDateTime.MAX;
    }
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

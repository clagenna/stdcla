package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;

public class FSFile implements IFSVisitable {

  public static final String COL01_ATTUALE        = "attuale";
  public static final String COL02_PERCORSO       = "percorso";
  public static final String COL03_NUOVONOME      = "nuovoNome";
  public static final String COL04_DTASSUNTA      = "assunta";
  public static final String COL05_DTNOMEFILE     = "nomeFileDt";
  public static final String COL06_DTCREAZIONE    = "creazione";
  public static final String COL07_DTULTMODIF     = "ultModif";
  public static final String COL08_DTACQUISIZIONE = "acquisizione";
  public static final String COL09_DTPARENTDIR    = "parentDirDt";
  public static final String COL10_LATITUDE       = "latitude";
  public static final String COL11_LONGITUDE      = "longitude";

  private Logger m_log;
  private Path   m_file;
  // private File   m_fiBackup;
  private double                  latitude;
  private double                  longitude;
  /** nel caso che i valori GPS siano risultato di interpolazione */
  @Getter @Setter private boolean interpolato;
  private Path                    m_parent;
  private IImageModel             m_model;

  public FSFile() {
    init();
  }

  private void init() {
    latitude = 0;
    longitude = 0;
  }

  /**
   * Questo viene richiamato sui tipi di files non riconosciuti come foto ma
   * come files generici
   *
   * @param p_fi
   * @throws FileNotFoundException
   */
  public FSFile(Path p_fi) throws FileNotFoundException {
    init();
    setPath(p_fi);
  }

  public String getFileExtention() {
    return null;
  }
  
  @Override
  public void accept(IFSVisitatore p_fsv) {
    try {
      p_fsv.visit(this);
    } catch (FileNotFoundException e) {
      getLogger().error("visita", e);
    }
  }

  public IImageModel getModel() {
    // 2024-09-11 Tolta la dipendenza da AppProperties 
    //    if (m_model != null)
    //      return m_model;
    //    m_model = AppProperties.getInst().getModel();
    return m_model;
  }

  @Override
  public void setModel(IImageModel p_mod) {
    m_model = p_mod;
  }

  @Override
  public void setPath(Path p_fi) throws FileNotFoundException {
    if ( !Files.exists(p_fi, LinkOption.NOFOLLOW_LINKS))
      throw new FileNotFoundException(p_fi.toString());
    m_file = p_fi;
    Path fiParent = m_file.getParent();
    setParent(fiParent);
    String clsNam = getClass().getSimpleName();
    switch (clsNam) {
      case "FSFile":
      case "FSFoto":
      case "FSJpeg":
      case "FSTiff":
      case "FSNef":
      case "FSHeic":
      case "FSCr2":
        getModel().add(this);
        break;
      default:
        break;
    }
  }

  @Override
  public Path getPath() {
    return m_file;
  }

  @Override
  public void setParent(Path p_parent) {
    m_parent = p_parent;
  }

  @Override
  public Path getParent() {
    return m_parent;
  }

  public Logger getLogger() {
    if (m_log == null)
      m_log = LogManager.getLogger(getClass());
    return m_log;
  }

  @Override
  public boolean equals(Object obj) {
    if ( ! (obj instanceof FSFile))
      return false;
    FSFile fsAltro = (FSFile) obj;
    Path pthAltro = fsAltro.getPath();
    if ( (pthAltro == null) || (getPath() == null))
      return false;
    return m_file.equals(pthAltro);
  }

  public String getAttuale() {
    String szRet = getPath().getFileName().toString();
    return szRet;
  }

  public String getPercorso() {
    String szRet = getParent().getFileName().toString();
    return szRet;
  }

  public String getNuovoNome() {
    String szRet = "";
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.creaNomeFile();
    return szRet;
  }

  public String getAssunta() {
    String szRet = "";
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = formatDt(fot.getDtAssunta());
    return szRet;
  }

  public LocalDateTime getNomeFileDt() {
    //    String szRet = "";
    //    if ( ! (this instanceof FSFoto))
    //      return szRet;
    //    FSFoto fot = (FSFoto) this;
    //    szRet = formatDt(fot.getDtNomeFile());
    //    return szRet;
    LocalDateTime szRet = null;
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.getDtNomeFile();
    return szRet;
  }

  public LocalDateTime getCreazione() {
    //    String szRet = "";
    //    if ( ! (this instanceof FSFoto))
    //      return szRet;
    //    FSFoto fot = (FSFoto) this;
    //    szRet = formatDt(fot.getDtCreazione());
    //    return szRet;
    LocalDateTime szRet = null;
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.getDtCreazione();
    return szRet;

  }

  public LocalDateTime getUltModif() {
    //    String szRet = "";
    //    if ( ! (this instanceof FSFoto))
    //      return szRet;
    //    FSFoto fot = (FSFoto) this;
    //    szRet = formatDt(fot.getDtUltModif());
    //    return szRet;
    LocalDateTime szRet = null;
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.getDtUltModif();
    return szRet;

  }

  public LocalDateTime getAcquisizione() {
    //    String szRet = "";
    //    if ( ! (this instanceof FSFoto))
    //      return szRet;
    //    FSFoto fot = (FSFoto) this;
    //    szRet = formatDt(fot.getDtAcquisizione());
    //    return szRet;
    LocalDateTime szRet = null;
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.getDtAcquisizione();
    return szRet;

  }

  public LocalDateTime getParentDirDt() {
    //    String szRet = "";
    //    if ( ! (this instanceof FSFoto))
    //      return szRet;
    //    FSFoto fot = (FSFoto) this;
    //    szRet = formatDt(fot.getDtParentDir());
    //    return szRet;
    LocalDateTime szRet = null;
    if ( ! (this instanceof FSFoto))
      return szRet;
    FSFoto fot = (FSFoto) this;
    szRet = fot.getDtParentDir();
    return szRet;

  }

  public String formatDt(LocalDateTime dt) {
    Date dt2 = null;
    Instant ll = null;
    try {
      if (dt != null) {
        ll = dt.atZone(ZoneId.systemDefault()).toInstant();
        if (ll.toString().indexOf("9999-") < 0)
          dt2 = Date.from(ll);
      }
    } catch (Exception e) {
      return e.getMessage();
    }
    return formatDt(dt2);
  }

  private String formatDt(Date dt2) {
    String szRet = "*";
    if (dt2 != null)
      szRet = ParseData.s_fmt.format(dt2);
    return szRet;
  }

  public boolean isGPS() {
    return (longitude * latitude) != 0;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double vv) {
    latitude = vv;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double vv) {
    longitude = vv;
  }
}

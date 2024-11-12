package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSHeic extends FSFoto {

  private static final String CSZ_EXTENTION = "heic";
  private static Logger       s_log         = LogManager.getLogger(FSHeic.class);

  public FSHeic() {
    setExifParseable(false);
  }

  public FSHeic(Path p_fi) throws FileNotFoundException {
    super(p_fi);
    setExifParseable(false);
  }

  @Override
  public Logger getLogger() {
    return s_log;
  }

  @Override
  public void accept(IFSVisitatore p_fsv) {
    try {
      p_fsv.visit(this);
    } catch (FileNotFoundException e) {
      getLogger().error("visita", e);
    }
  }

  @Override
  public String getFileExtention() {
    return CSZ_EXTENTION;
  }

}

package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSJpeg extends FSFoto {
  private static Logger s_log = LogManager.getLogger(FSJpeg.class);

  public FSJpeg() {
    //
  }

  public FSJpeg(Path p_fi) throws FileNotFoundException {
    super(p_fi);
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
    return "jpg";
  }
}

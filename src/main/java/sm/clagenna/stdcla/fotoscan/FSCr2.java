package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSCr2 extends FSFoto {

  private static Logger s_log = LogManager.getLogger(FSCr2.class);

  public FSCr2() {
    //
  }

  public FSCr2(Path p_fi) throws FileNotFoundException {
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
    return "cr2";
  }

  @Override
  public void cambiaExifInfoOnFile() {
    getLogger().error("Non cambio EXIF per {}", getPath().toString());
    return;
  }

}

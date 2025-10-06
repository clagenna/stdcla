package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSFileFactory {
  private static final Map<String, Class<? extends IFSVisitable>> s_map;
  private static final Logger                                     s_log  = LogManager.getLogger(FSFileFactory.class);
  private static Map<Path, IFSVisitable>                          s_dirs = new HashMap<Path, IFSVisitable>();
  private static FSFileFactory                                    s_inst;
  // FIXME impostare il "model" alla creazione del factory
  private IImageModel m_model;

  static {
    s_map = new HashMap<>();
    s_map.put(".cr2",  FSCr2.class);
    s_map.put(".heic", FSHeic.class);
    s_map.put(".jpeg", FSJpeg.class);
    s_map.put(".jpg",  FSJpeg.class);
    s_map.put(".nef",  FSNef.class);
    s_map.put(".tif",  FSTiff.class);
    s_map.put(".tiff", FSTiff.class);
    s_map.put(".xml",  FSXml.class);
  }

  public FSFileFactory() {
    if (s_inst != null)
      throw new UnsupportedOperationException("FSFileFactory è un singleton !");
    s_inst = this;
  }

  public IFSVisitable get(Path p_pth) {
    IFSVisitable fiRet = null;
    if (p_pth == null)
      return fiRet;
    if (Files.isDirectory(p_pth, LinkOption.NOFOLLOW_LINKS)) {
      fiRet = dammiUnDir(p_pth);
      return fiRet;
    }
    String szExt = p_pth.getName(p_pth.getNameCount() - 1).toString();
    int n = szExt.lastIndexOf(".");
    if (n >= 0)
      szExt = szExt.substring(n);
    szExt = szExt.toLowerCase();
    try {
      if (s_map.containsKey(szExt)) {
        Class<?> cls = s_map.get(szExt);
        fiRet = (IFSVisitable) cls.getDeclaredConstructor().newInstance();
      } else {
        fiRet = new FSFile();
      }
      fiRet.setModel(m_model);
      fiRet.setPath(p_pth);
    } catch (InstantiationException | IllegalAccessException | FileNotFoundException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      s_log.error("Non riesco istanziare", e);
    }
    return fiRet;
  }

  private IFSVisitable dammiUnDir(Path p_pth) {
    IFSVisitable fiRet = null;
    if (s_dirs.containsKey(p_pth))
      return s_dirs.get(p_pth);
    try {
      fiRet = new FSDir(p_pth);
      s_dirs.put(p_pth, fiRet);
    } catch (FileNotFoundException e) {
      s_log.error("Non posso istanziare", e);
    }
    return fiRet;
  }

  public IImageModel getModel() {
    return m_model;
  }

  public void setModel(IImageModel p_model) {
    m_model = p_model;
  }

  public static FSFileFactory getInst() {
    if (s_inst == null)
      throw new UnsupportedOperationException("Non ha mai inizializzato FSFileFactory !");
    return s_inst;
  }

}

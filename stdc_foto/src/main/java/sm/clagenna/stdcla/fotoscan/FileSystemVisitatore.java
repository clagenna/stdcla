package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSystemVisitatore implements IFSVisitatore {

  private static final Logger s_log = LogManager.getLogger(FileSystemVisitatore.class);

  @Override
  public void visit(FSDir p_vis) throws FileNotFoundException {
    // 2024-09-11 Tolto la dipendenza da AppProperties, FSDir indica la ricorsivita
    // ImgModel mod = AppProperties.getInst().getModel();
    // boolean recurse = mod.isRecursive();
    //    if ( !p_vis.isRecursive()) {
    //      // se non corrisponde il path vuol dire che sono in un sub-dir
    //      Path pth = Paths.get(p_vis.getPercorso());
    //      if ( !pth.equals(p_vis.getPath()))
    //        return;
    //    }

    FSFileFactory fact = FSFileFactory.getInst();
    try {
      Files.list(p_vis.getPath()) //
          .map(s -> fact.get(s)) //
          .forEach(s -> s.accept(this));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void visit(FSFile p_vis) {
    s_log.warn("non analizzo FSFile \"{}\"", p_vis.getPath().toString());
  }

  @Override
  public void visit(FSFoto p_vis) {
    p_vis.analizzaFoto();
  }

  @Override
  public void visit(FSTiff p_vis) {
    p_vis.analizzaFoto();
  }

  @Override
  public void visit(FSXml p_vis) {
    s_log.warn("non analizzo FSXml" + p_vis.toString());
  }

}

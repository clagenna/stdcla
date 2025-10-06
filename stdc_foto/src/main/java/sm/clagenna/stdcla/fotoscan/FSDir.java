package sm.clagenna.stdcla.fotoscan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import sm.clagenna.stdcla.utils.ParseData;

public class FSDir extends FSFile {

  // private static Pattern           s_pad  = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}).*");
  // private static DateTimeFormatter s_fmt1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private LocalDate m_dtDir;
  private boolean recursive;

  public FSDir(Path p_dir) throws FileNotFoundException {
    super(p_dir);
  }

  @Override
  public void setPath(Path p_fi) throws FileNotFoundException {
    super.setPath(p_fi);
    analizzaSeDate();
  }

  private void analizzaSeDate() {
    String szNome = getPath().getFileName().toString();
    LocalDateTime dt = ParseData.guessData(szNome);
    //    Matcher mtch = s_pad.matcher(szNome);
    //    if ( !mtch.matches()) {
    //      getLogger().warn("Il dir non contiene una data:" + szNome);
    //      return;
    //    }
    //    String szDt = mtch.group(1);
    //    m_dtDir = LocalDate.from(s_fmt1.parse(szDt));
    if (dt == null) {
      getLogger().warn("Il dir non contiene una data:" + szNome);
    } else
      m_dtDir = dt.toLocalDate();
  }

  public LocalDate getDtDir() {
    return m_dtDir;
  }

  @Override
  public void accept(IFSVisitatore p_fsv) {
    try {
      p_fsv.visit(this);
    } catch (FileNotFoundException e) {
      getLogger().error("Errore Visita", e);
    }
  }

  public Collection<IFSVisitable> getChildrens() throws IOException {
    FSFileFactory fact = FSFileFactory.getInst();
    List<IFSVisitable> coll = Files.list(getPath()) //
        .map(fact::get) //
        .collect(Collectors.toList());
    return coll;
  }

  public boolean isRecursive() {
    return recursive;
  }

  public void setRecursive(boolean p_recursive) {
    recursive = p_recursive;
  }
}

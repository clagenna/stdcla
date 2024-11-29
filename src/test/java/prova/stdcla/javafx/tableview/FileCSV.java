package prova.stdcla.javafx.tableview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class FileCSV {

  @Getter @Setter
  private SimpleStringProperty  name;
  @Getter @Setter
  private SimpleStringProperty  relPath;
  @Getter @Setter
  private SimpleIntegerProperty size;
  @Getter @Setter
  private SimpleIntegerProperty qtaRows;
  @Getter @Setter
  private SimpleStringProperty  datereg;

  public FileCSV() {
    name = new SimpleStringProperty();
    relPath = new SimpleStringProperty();
    size = new SimpleIntegerProperty();
    qtaRows = new SimpleIntegerProperty();
    datereg = new SimpleStringProperty();
  }

  public FileCSV assignPath(Path pth) {
    int n = pth.getNameCount();
    String szNam = pth.getFileName().toString();
    name.set(szNam);
    relPath.set(pth.subpath(n - 2, n - 1).toString());
    try {
      size.set((int) Files.size(pth));
    } catch (IOException e) {
      e.printStackTrace();
    }

    qtaRows.set(ThreadLocalRandom.current().nextInt(3, 520));
    datereg.set("2023-07-23 15:32:44");
    return this;
  }

}

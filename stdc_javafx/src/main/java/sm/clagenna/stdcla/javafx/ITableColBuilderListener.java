package sm.clagenna.stdcla.javafx;

import javafx.scene.control.TableColumn;

public interface ITableColBuilderListener<S,T> {
  void tableColBuilded(TableColumn<S, T> col);
}

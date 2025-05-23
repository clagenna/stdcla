package sm.clagenna.stdcla.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * @see https://stackoverflow.com/questions/19924852/autocomplete-combobox-in-javafx#answer-20282301
 * @param <T>
 */
public class AutoCompleteComboBoxListener<T> implements EventHandler<KeyEvent> {

  private ComboBox<T> comboBox;
  // private StringBuilder sb;
  private ObservableList<T> data;
  private boolean           moveCaretToPos = false;
  private int               caretPos;

  public AutoCompleteComboBoxListener(final ComboBox<T> comboBox) {
    this.comboBox = comboBox;
    // sb = new StringBuilder();
    data = comboBox.getItems();

    this.comboBox.setEditable(true);
    this.comboBox.setOnKeyPressed(new EventHandler<KeyEvent>() {

      @Override
      public void handle(KeyEvent t) {
        comboBox.hide();
      }
    });
    this.comboBox.setOnKeyReleased(AutoCompleteComboBoxListener.this);
  }

  @Override
  public void handle(KeyEvent event) {

    if (event.getCode() == KeyCode.UP) {
      caretPos = -1;
      moveCaret(comboBox.getEditor().getText().length());
      return;
    }
    if (event.getCode() == KeyCode.DOWN) {
      if ( !comboBox.isShowing()) {
        comboBox.show();
      }
      caretPos = -1;
      moveCaret(comboBox.getEditor().getText().length());
      return;
    }
    if (event.getCode() == KeyCode.BACK_SPACE) {
      moveCaretToPos = true;
      caretPos = comboBox.getEditor().getCaretPosition();
    } else if (event.getCode() == KeyCode.DELETE) {
      moveCaretToPos = true;
      caretPos = comboBox.getEditor().getCaretPosition();
    }

    if (event.getCode() == KeyCode.RIGHT || //
        event.getCode() == KeyCode.LEFT || //
        event.isControlDown() || //
        event.getCode() == KeyCode.HOME || //
        event.getCode() == KeyCode.END || //
        event.getCode() == KeyCode.TAB) {
      return;
    }

    ObservableList<T> list = FXCollections.observableArrayList();
    for (int i = 0; i < data.size(); i++) {
      if (null == data.get(i))
        continue;
      if (data.get(i) //
          .toString() //
          .toLowerCase() //
          .startsWith(comboBox //
              .getEditor() //
              .getText() //
              .toLowerCase())) {
        list.add(data.get(i));
      }
    }
    String t = comboBox.getEditor().getText();

    comboBox.setItems(list);
    comboBox.getEditor().setText(t);
    if ( !moveCaretToPos) {
      caretPos = -1;
    }
    moveCaret(t.length());
    if ( !list.isEmpty()) {
      comboBox.show();
    }
  }

  private void moveCaret(int textLength) {
    if (caretPos == -1) {
      comboBox.getEditor().positionCaret(textLength);
    } else {
      comboBox.getEditor().positionCaret(caretPos);
    }
    moveCaretToPos = false;
  }
}

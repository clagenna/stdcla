package prova.cleanup;

public class BugCleanUpRemovesVar {
  // cleanUp will remove this field !!
  @SuppressWarnings("unused")
  private boolean overwrite;

  public BugCleanUpRemovesVar() {
    overwrite = false;
  }

}

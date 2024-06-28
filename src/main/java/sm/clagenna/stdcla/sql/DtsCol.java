package sm.clagenna.stdcla.sql;

import lombok.Getter;
import lombok.Setter;

class DtsCol {
  /** Il nome <b>univoco</b> e <b>case insensitive</b> della colonna */
  @Getter @Setter private String name;
  /** la posizione 0-based nel dataset */
  @Getter @Setter private int    index;
  @Getter private SqlTypes       type;
  @Getter @Setter private String format;
  @Getter private boolean        inferredDate;

  public DtsCol() {
    index = -1;
  }

  public void setInferredDate(boolean bv) {
    // la prima data valida inferisce
    inferredDate |= bv;
  }

  public void setType(SqlTypes p_ty) {
    type = p_ty;
  }

  @Override
  public String toString() {
    String szRet = "col:";
    szRet += name != null ? name : "??name??";
    szRet += index >= 0 ? String.format("(%d)", index) : "(?)";
    szRet += type != null ? "[" + type + "]" : "[??type??]";
    return szRet;
  }
}

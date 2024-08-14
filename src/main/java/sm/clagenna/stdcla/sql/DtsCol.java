package sm.clagenna.stdcla.sql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;

class DtsCol implements Cloneable {

  private static final Logger    s_log = LogManager.getLogger(DtsCol.class);
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

  @Override
  protected Object clone() throws CloneNotSupportedException {
    DtsCol ret = new DtsCol();
    ret.name = name;
    ret.index = index;
    ret.type = type;
    ret.format = format;
    ret.inferredDate = inferredDate;
    return ret;
  }

  public void setInferredDate(boolean bv) {
    // la prima data valida inferisce
    inferredDate |= bv;
  }

  public void setType(SqlTypes p_ty) {
    type = p_ty;
  }

  public Object parse(String p_szv) {
    Object obj = null;
    switch (type) {
      case SqlTypes.SMALLINT:
      case SqlTypes.INTEGER:
        obj = Integer.parseInt(p_szv);
        break;
      case SqlTypes.VARCHAR:
        obj = p_szv;
        break;
      case SqlTypes.NUMERIC:
      case SqlTypes.DECIMAL:
      case SqlTypes.FLOAT:
      case SqlTypes.DOUBLE:
      case SqlTypes.REAL:
        obj = Double.parseDouble(p_szv.replace(",", "."));
        break;
      case SqlTypes.DATE:
        obj = ParseData.parseData(p_szv);
        break;
      default:
        s_log.error("Non interpreto tipo {} per col {}", type, name);
        break;
    }
    return obj;
  }

  @Override
  public String toString() {
    StringBuilder szRet = new StringBuilder("col:");
    szRet.append(name != null ? name : "??name??");
    szRet.append(index >= 0 ? String.format("(%d)", index) : "(?)");
    szRet.append(type != null ? "[" + type + "]" : "[??type??]");
    return szRet.toString();
  }
}

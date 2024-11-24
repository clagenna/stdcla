package sm.clagenna.stdcla.sql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class DtsCol implements Cloneable {

  private static final Logger s_log = LogManager.getLogger(DtsCol.class);
  /** Il nome <b>univoco</b> e <b>case insensitive</b> della colonna */
  @Getter @Setter
  private String              name;
  /** la posizione 0-based nel dataset */
  @Getter @Setter
  private int                 index;
  @Getter
  private SqlTypes            type;
  @Getter @Setter
  private String              format;
  @Getter
  private boolean             inferredDate;

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
    String sz2 = null;
    try {
      switch (type) {
        case SqlTypes.SMALLINT:
        case SqlTypes.INTEGER:
          if (null != p_szv)
            sz2 = p_szv.trim();
          if (null != sz2 && sz2.length() > 0)
            obj = Integer.parseInt(sz2);
          break;
        case SqlTypes.VARCHAR:
          obj = p_szv;
          break;
        case SqlTypes.NUMERIC:
        case SqlTypes.DECIMAL:
        case SqlTypes.FLOAT:
        case SqlTypes.DOUBLE:
        case SqlTypes.REAL:
          sz2 = p_szv.trim();
          if (sz2.length() > 0) {
            //            sz2 = sz2.replace(",", ".");
            //            obj = Double.parseDouble(sz2);
            obj = Utils.parseDouble(sz2);
          }
          break;
        case SqlTypes.DATE:
          if (p_szv.startsWith("\""))
            p_szv = p_szv.replaceAll("\"", "");
          obj = ParseData.parseData(p_szv);
          break;
        case SqlTypes.TIMESTAMP:
          if (p_szv.startsWith("\""))
            p_szv = p_szv.replaceAll("\"", "");
          obj = ParseData.parseData(p_szv);
          break;
        default:
          s_log.error("Non interpreto tipo {} per col {}", type, name);
          break;
      }
    } catch (Exception e) {
      s_log.error("Excp: interpreto tipo {} per col {}, err={}", type, name, e.getMessage());
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

package sm.clagenna.stdcla.sql;

import java.sql.Types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.ECurrencies;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class DtsCol implements Cloneable {
  private static final Logger s_log = LogManager.getLogger(DtsCol.class);
  // campi per la decoType()
  @Getter @Setter
  private static String       szFmt;
  @Getter @Setter
  private static String       szTyp;
  @Getter @Setter
  private static int          decPlace;

  /** Il nome <b>univoco</b> e <b>case insensitive</b> della colonna */
  @Getter @Setter
  private String   name;
  /** la posizione 0-based nel dataset */
  @Getter @Setter
  private int      index;
  @Getter
  private SqlTypes type;
  @Getter @Setter
  private String   format;
  @Getter
  private boolean  inferredDate;

  public DtsCol() {
    index = -1;
  }

  public DtsCol(String pName, SqlTypes pType) {
    index = -1;
    setName(pName);
    setType(pType);
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
          obj = Double.valueOf(0);
          if (null != p_szv) {
            sz2 = p_szv.trim();
            if (sz2.contains(ECurrencies.Euro.getSymbol()))
              sz2 = sz2.replaceAll(ECurrencies.Euro.getSymbol(), "");
            if (sz2.contains(ECurrencies.Dollar.getSymbol()))
              sz2 = sz2.replaceAll(ECurrencies.Dollar.getSymbol(), "");
            sz2 = sz2.trim();

            if (sz2.length() > 0) {
              //            sz2 = sz2.replace(",", ".");
              //            obj = Double.parseDouble(sz2);
              obj = Utils.parseDouble(sz2);
            }
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

  public static SqlTypes decoType(int nTyp) {
    SqlTypes sqlt = SqlTypes.decode(nTyp);
    szFmt = null;
    szTyp = null;
    decPlace = 6;
    int colWidth = DtsCols.getColWidth();
    
    
    
    
    
    
    
    
    
    
    switch (nTyp) {
      case Types.SMALLINT:
        szFmt = String.format("%%%dd ", colWidth);
        szTyp = "INTEGER";
        break;
      case Types.INTEGER:
        szFmt = String.format("%%%dd ", colWidth);
        szTyp = "INTEGER";
        break;
      case Types.NVARCHAR:
      case Types.VARCHAR:
        szFmt = String.format("%%-%ds ", colWidth);
        szTyp = "VARCHAR";
        break;
      case Types.NUMERIC:
        szFmt = String.format("%%-%ds ", colWidth);
        szTyp = "NUMERIC";
        break;
      case Types.DECIMAL:
        szFmt = String.format("%%%d.%df ", colWidth, decPlace);
        szTyp = "DECIMAL";
        break;
      case Types.FLOAT:
        szFmt = String.format("%%%d.%df ", colWidth, decPlace);
        szTyp = "FLOAT";
        break;
      case Types.DOUBLE:
        szFmt = String.format("%%%d.%df ", colWidth, decPlace);
        szTyp = "DOUBLE";
        break;
      case Types.REAL:
        szFmt = String.format("%%%d.%df ", colWidth, decPlace);
        szTyp = "REAL";
        break;
      case Types.DATE:
        szFmt = String.format("%%%ds ", colWidth);
        szTyp = "DATE";
        break;
      case Types.TIMESTAMP:
        szFmt = String.format("%%%ds ", colWidth);
        szTyp = "DATE";
        break;
      default:
        s_log.error("Non interpreto tipo {}", nTyp);
        break;
    }
//    if (null != szTyp)
//      sqlt = SqlTypes.parse(szTyp);
    return sqlt;
  }
  
  public static int decoType(SqlTypes pSql) {
    int ret = 0;
    switch( pSql) {
      case SMALLINT:
        ret = Types.SMALLINT;
        break;
      case BIGINT:
        ret = Types.BIGINT;
        break;
      case BINARY:
        ret = Types.BINARY;
        break;
      case BIT:
        ret = Types.BIT;
        break;
      case BLOB:
        ret = Types.BLOB;
        break;
      case BOOLEAN:
        ret = Types.BOOLEAN;
        break;
      case CHAR:
        ret = Types.CHAR;
        break;
      case CLOB:
        ret = Types.CLOB;
        break;
      case DATALINK:
        ret = Types.DATALINK;
        break;
      case DATE:
        ret = Types.DATE;
        break;
      case DECIMAL:
        ret = Types.DECIMAL;
        break;
      case DISTINCT:
        ret = Types.DISTINCT;
        break;
      case DOUBLE:
        ret = Types.DOUBLE;
        break;
      case FLOAT:
        ret = Types.FLOAT;
        break;
      case INTEGER:
        ret = Types.INTEGER;
        break;
      case JAVA_OBJECT:
        ret = Types.JAVA_OBJECT;
        break;
      case LONGNVARCHAR:
        ret = Types.LONGNVARCHAR;
        break;
      case LONGVARBINARY:
        ret = Types.LONGVARBINARY;
        break;
      case LONGVARCHAR:
        ret = Types.LONGVARCHAR;
        break;
      case NCHAR:
        ret = Types.NCHAR;
        break;
      case NCLOB:
        ret = Types.NCLOB;
        break;
      case NULL:
        ret = Types.NULL;
        break;
      case NUMERIC:
        ret = Types.NUMERIC;
        break;
      case NVARCHAR:
        ret = Types.BIGINT;
        break;
      case OTHER:
        ret = Types.BIGINT;
        break;
      case REAL:
        ret = Types.BIGINT;
        break;
      case REF:
        ret = Types.BIGINT;
        break;
      case REF_CURSOR:
        ret = Types.BIGINT;
        break;
      case ROWID:
        ret = Types.BIGINT;
        break;
      case SQLXML:
        ret = Types.BIGINT;
        break;
      case STRUCT:
        ret = Types.BIGINT;
        break;
      case TIME:
        ret = Types.BIGINT;
        break;
      case TIMESTAMP:
        ret = Types.BIGINT;
        break;
      case TIMESTAMP_WITH_TIMEZONE:
        ret = Types.BIGINT;
        break;
      case TIME_WITH_TIMEZONE:
        ret = Types.BIGINT;
        break;
      case TINYINT:
        ret = Types.BIGINT;
        break;
      case VARBINARY:
        ret = Types.BIGINT;
        break;
      case VARCHAR:
        ret = Types.BIGINT;
        break;
      default:
        break;
    }
    return ret;
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

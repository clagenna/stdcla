package sm.clagenna.stdcla.sql;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public enum SqlTypes {
  // reinterpretato java.sql.Types
  ARRAY(2003), //
  BIGINT( -5), //
  BINARY( -2), //
  BIT( -7), //
  BLOB(2004), //
  CHAR(1), //
  CLOB(2005), //
  DATE(91), //
  DECIMAL(3), //
  DISTINCT(2001), //
  DOUBLE(8), //
  FLOAT(6), //
  INTEGER(4), //
  JAVA_OBJECT(2000), //
  LONGVARBINARY( -4), //
  LONGVARCHAR( -1), //
  NULL(0), //
  NUMERIC(2), //
  OTHER(1111), //
  REAL(7), //
  REF(2006), //
  SMALLINT(5), //
  STRUCT(2002), //
  TIME(92), //
  TIMESTAMP(93), //
  TINYINT( -6), //
  VARBINARY( -3), //
  VARCHAR(12), //
  //
  BOOLEAN(16), //
  DATALINK(70), //
  LONGNVARCHAR( -16), //
  NCHAR( -15), //
  NCLOB(2011), //
  NVARCHAR( -9), //
  REF_CURSOR(2012), //
  ROWID( -8), //
  SQLXML(2009), //
  TIMESTAMP_WITH_TIMEZONE(2014), //
  TIME_WITH_TIMEZONE(2013); //

  private static Map<Integer, SqlTypes> s_map;
  private int                           code;

  static {
    s_map = new HashMap<>();
    for (SqlTypes ty : SqlTypes.values()) {
      Integer ii = ty.code;
      s_map.put(ii, ty);
    }
  }

  private SqlTypes(int no) {
    code = no;
  }

  public int code() {
    return code;
  }

  public static SqlTypes decode(int i) {
    return s_map.get(i);
  }

  public static SqlTypes translate(Object o) {
    SqlTypes ret = null;
    if (null == o)
      return ret;
    String szCls = o.getClass().getSimpleName().toLowerCase();
    switch (szCls) {

      case "character":
        ret = CHAR;
        break;

      case "string":
        ret = NVARCHAR;
        break;

      case "integer":
        ret = INTEGER;
        break;

      case "long":
        ret = INTEGER;
        break;

      case "bigdecimal":
        ret = DECIMAL;
        break;

      case "float":
      case "double":
        ret = DOUBLE;
        break;

      case "localdatetime":
      case "timestamp":
      case "date":
        ret = DATE;
        break;

      default:
        throw new UnsupportedOperationException(String.format("Translate SqlType \"%s\" Unknown", szCls));
    }
    return ret;
  }

  public static Object defval(SqlTypes tt) {
    Object obj = null;
    switch (tt) {
      case ARRAY:
        break;
      case BIGINT:
        obj = BigInteger.ZERO;
        break;
      case BINARY:
        break;
      case BIT:
        break;
      case BLOB:
        break;
      case BOOLEAN:
        obj = Boolean.FALSE;
        break;
      case CHAR:
        obj = Character.valueOf('0');
        break;
      case CLOB:
        break;
      case DATALINK:
        break;
      case DATE:
      case TIME:
      case TIMESTAMP:
        obj = new Date(0);
        break;
      case DECIMAL:
      case DOUBLE:
      case FLOAT:
      case NUMERIC:
      case REAL:
        obj = Double.valueOf(0);
        break;
      case DISTINCT:
        break;
      case INTEGER:
      case SMALLINT:
      case TINYINT:
        obj = Integer.valueOf(0);
        break;
      case JAVA_OBJECT:
        break;
      case LONGNVARCHAR:
      case LONGVARBINARY:
      case LONGVARCHAR:
      case NCHAR:
      case NVARCHAR:
      case VARCHAR:
        obj = new String();
        break;
      case NCLOB:
        break;
      case NULL:
        break;
      case OTHER:
        break;
      case REF:
        break;
      case REF_CURSOR:
        break;
      case ROWID:
        break;
      case SQLXML:
        break;
      case STRUCT:
        break;
      case TIMESTAMP_WITH_TIMEZONE:
      case TIME_WITH_TIMEZONE:
        obj = LocalDateTime.now();
        break;
      case VARBINARY:
        break;
      default:
        break;
    }
    return obj;
  }
}

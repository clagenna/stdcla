package sm.clagenna.stdcla.sql;

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
}

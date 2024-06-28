package sm.clagenna.stdcla.sql;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sm.clagenna.stdcla.sys.ex.DatasetException;
import sm.clagenna.stdcla.utils.ParseData;

public class DtsRow implements Cloneable {
  private static final Logger s_log = LogManager.getLogger(DtsRow.class);
  private List<Object>        valori;
  private Dataset             dataset;
  private ParseData           parsedt;

  private DtsRow() {

  }

  public DtsRow(Dataset p_dts) throws DatasetException {
    dataset = p_dts;
    List<Object> li = Collections.nCopies(dataset.getQtaCols(), (Object) null);
    valori = new ArrayList<>(li);
    if (dataset.getTipoServer().isDateAsString())
      parsedt = new ParseData();
  }

  public void addRow(ResultSet p_res) throws DatasetException {
    for (DtsCol col : dataset.getColumns().getColumns()) {
      int nCol = col.getIndex() + 1;
      Object val = null;
      try {
        switch (col.getType()) {
          case NCHAR:
          case VARCHAR:
          case CHAR:
            val = p_res.getString(nCol);
            if (val != null && dataset.getTipoServer().isDateAsString()) {
              val = provaSeData(val);
              col.setInferredDate(val != null);
            }
            break;

          case BIGINT:
          case INTEGER:
          case SMALLINT:
          case TINYINT:
            val = p_res.getInt(nCol);
            break;
          case FLOAT:
            val = p_res.getFloat(nCol);
            break;
          case DOUBLE:
            val = p_res.getDouble(nCol);
            break;
          case DATE:
            val = p_res.getDate(nCol);
            break;
          case TIMESTAMP:
            val = p_res.getTimestamp(nCol);
            break;
          case REAL:
            val = p_res.getDouble(nCol);
            break;

          default:
            s_log.error("Non riconosco tipo della col {}", col);
        }
      } catch (Exception e) {
        String szMsg = String.format("Errore in lettura col \"%s\" con err=%s", col, e.getMessage());
        s_log.error(szMsg, e);
        throw new DatasetException(szMsg, e);
      }
      valori.set(nCol - 1, val);
    }
  }

  public int addRow(List<Object> p_lio) {
    int nRet = -1;
    if (null == p_lio || p_lio.size() == 0)
      return nRet;
    int nCol = 0;
    for (Object obj : p_lio) {
      valori.set(nCol++, obj);
    }
    return nRet;
  }

  public Object get(int nCol) {
    return valori.get(nCol);
  }

  public Object get(String col) {
    Object ret = null;
    int ii = dataset.getColumNo(col);
    ret = valori.get(ii);
    return ret;
  }

  public void set(String col, Object val) {
    int ii = dataset.getColumNo(col);
    while (valori.size() < ii)
      valori.add((Object) null);
    valori.set(ii, val);
  }

  private Object provaSeData(Object p_val) {
    if (p_val == null)
      return p_val;
    String szVal = p_val.toString();
    LocalDateTime dt = parsedt.parseData(szVal);
    if (dt != null) {
      p_val = java.sql.Timestamp.valueOf(dt);
      s_log.trace("Convertito {} in Timestamp {}", szVal, ParseData.s_fmtDtExif.format(dt));
    }
    return p_val;
  }

  public void addVal(String p_nam, Object p_v) {
    int nCol = dataset.getColumNo(p_nam);
    valori.set(nCol - 1, p_v);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    DtsRow ret = new DtsRow();
    ret.dataset = dataset;
    ret.parsedt = parsedt;
    if (null != valori)
      ret.valori = Arrays.asList(valori.toArray());
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (DtsCol col : dataset.getColumns().getColumns()) {
      Object vv = valori.get(col.getIndex());
      String szv = String.format(DtsCols.getColFmtL(), "*null*");
      if (vv != null) {
        //        if (vv instanceof LocalDateTime) {
        //          szv = ParseData.s_fmtDtExif.format((TemporalAccessor) vv);
        //          szv = szv.replace(" 00:00:00", "");
        //        } else
        //          szv = String.format(col.getFormat(), vv);
        //
        //        szv = switch ( vv) {
        //          case Integer i ->  String.format(col.getFormat(), i);
        //          case Timestamp ts -> ParseData.s_fmtDtExif.format(ts.toInstant());
        //          default String.format(col.getFormat(), vv);
        //        };
        String szClss = vv != null ? vv.getClass().getSimpleName() : "Object";
        switch (szClss) {

          case "LocalDateTime":
            szv = ParseData.s_fmtDtExif.format((LocalDateTime) vv);
            szv = szv.replace(" 00:00:00", "");
            szv = String.format(DtsCols.getColFmtR(), szv);
            break;

          case "Timestamp":
            szv = ParseData.s_fmtDtDate.format((Timestamp) vv);
            szv = szv.replace(" 00:00:00", "");
            szv = String.format(DtsCols.getColFmtR(), szv);
            break;

          default:
            szv = String.format(DtsCols.getColFmt(), vv);
            break;
        }

      }
      sb.append(szv);
    }
    return sb.toString();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> mp = new LinkedHashMap<>();
    for (DtsCol col : dataset.getColumns().getColumns()) {
      String szNam = col.getName();
      Object obj = valori.get(col.getIndex());
      mp.put(szNam, obj);
    }
    return mp;
  }

}

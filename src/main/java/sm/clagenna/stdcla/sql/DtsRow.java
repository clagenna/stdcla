package sm.clagenna.stdcla.sql;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.sys.ex.DatasetException;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class DtsRow implements Cloneable {
	private static final Logger s_log = LogManager.getLogger(DtsRow.class);
	private List<Object> valori;
	@Getter
	@Setter
	private Dataset dataset;

	private DtsRow() {

	}

	public DtsRow(Dataset p_dts) {
		dataset = p_dts;
		List<Object> li = Collections.nCopies(dataset.getQtaCols(), (Object) null);
		valori = new ArrayList<>(li);
	}

	public void addRow(ResultSet p_res) throws DatasetException {
		for (DtsCol col : dataset.getColumns().getColumns()) {
			int nCol = col.getIndex() + 1;
			Object val = null;
			Object valo = null;
			try {
				switch (col.getType()) {
				case NCHAR:
				case NVARCHAR:
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
					valo = p_res.getObject(nCol);
					if (valo != null && !valo.getClass().getSimpleName().equals("Date"))
						val = ParseData.parseData(valo.toString());
					else
						val = p_res.getDate(nCol);
					break;
				case TIMESTAMP:
					val = p_res.getTimestamp(nCol);
					break;
				case REAL:
					val = p_res.getDouble(nCol);
					break;
				case DECIMAL:
					val = p_res.getDouble(nCol);
					break;
				case NUMERIC:
					val = p_res.getObject(nCol);
					if (null != val)
						val = val.toString();
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

	public DtsRow addRow(List<Object> p_lio) {
		// int nRet = -1;
		if (null == p_lio || p_lio.size() == 0)
			return null;
		int nCol = 0;
		for (Object obj : p_lio) {
			valori.set(nCol++, obj);
		}
		return this;
	}

	/**
	 * Dato un {@link List<String>} dalla lettura di un CSV popolo il row attuale
	 *
	 * @param p_rec
	 * @return
	 */
	public int parseRow(List<String> p_rec) {
		int nRet = -1;
		if (null == p_rec || p_rec.size() == 0)
			return nRet;
		int nMaxCol = dataset.getQtaCols();
		int nCol = 0;
		boolean bOnlyKCols = dataset.isOnlyKnownCols();
		// String szDebugCol = "27";
		for (String szv : p_rec) {
			if (bOnlyKCols && nCol >= nMaxCol)
				break;
			// if (null != szDebugCol && szv.contains(szDebugCol))
			// System.out.println("DtsRow.parseRow()");
			if (nCol < nMaxCol) {
				DtsCol col = dataset.getColum(nCol);
				Object o = col.parse(szv);
				valori.set(nCol++, o);
			} else if (null != szv && szv.trim().length() > 0)
				s_log.warn("Too many cols (n={}) on Row {}", nCol, p_rec.toString());
		}
		return nRet;

	}

	public Object get(int nCol) {
		return valori.get(nCol);
	}

	public Object get(String col) {
		Object ret = null;
		int ii = dataset.getColumNo(col);
		if (ii < 0)
			return ret;
		ret = valori.get(ii);
		return ret;
	}

	public void set(String col, Object val) {
		int ii = dataset.getColumNo(col);
		while (valori.size() < ii)
			valori.add((Object) null);
		valori.set(ii, val);
	}

	public void addCol(SqlTypes p_ty) {
		valori.add(SqlTypes.defval(p_ty));
	}

	private Object provaSeData(Object p_val) {
		if (p_val == null)
			return p_val;
		String szVal = p_val.toString();
		LocalDateTime dt = ParseData.parseData(szVal);
		if (dt != null) {
			p_val = java.sql.Timestamp.valueOf(dt);
			s_log.trace("Convertito {} in Timestamp {}", szVal, ParseData.s_fmtDtExif.format(dt));
		}
		return p_val;
	}

	public void addVal(String p_nam, Object p_v) {
		int nCol = dataset.getColumNo(p_nam);
		valori.set(nCol, p_v);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		DtsRow ret = new DtsRow();
		ret.dataset = dataset;
		if (null != valori) {
			// ret.valori = Arrays.asList(valori.toArray());
			ret.valori = new ArrayList<>();
			ret.valori.addAll(valori);
		}
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		// sb.append(dataset.getColumns().getIntestazione()).append("\n");
		for (DtsCol col : dataset.getColumns().getColumns()) {
			Object vv = valori.get(col.getIndex());
			String szv = String.format(DtsCols.getColFmtL(), "*null*");
			if (vv != null) {
				// if (vv instanceof LocalDateTime) {
				// szv = ParseData.s_fmtDtExif.format((TemporalAccessor) vv);
				// szv = szv.replace(" 00:00:00", "");
				// } else
				// szv = String.format(col.getFormat(), vv);
				//
				// szv = switch ( vv) {
				// case Integer i -> String.format(col.getFormat(), i);
				// case Timestamp ts -> ParseData.s_fmtDtExif.format(ts.toInstant());
				// default String.format(col.getFormat(), vv);
				// };
				String szClss = vv != null ? vv.getClass().getSimpleName() : "Object";
				switch (szClss) {

				case "LocalDateTime":
					szv = ParseData.s_fmtTs.format((LocalDateTime) vv);
					szv = szv.replace(" 00:00:00", "");
					szv = String.format(DtsCols.getColFmtR(), szv);
					break;

				case "Integer":
					szv = String.format(DtsCols.getColFmtR(), vv);
					break;

				case "Double":
					szv = String.format(DtsCols.getColFmtDbl(), vv);
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

	public String toCsv(String csvdelim) {
		StringBuilder sb = new StringBuilder();
		String escsep = String.format("\\%s", csvdelim);

		for (DtsCol col : dataset.getColumns().getColumns()) {
			Object vv = valori.get(col.getIndex());
			String szv = "";
			if (sb.length() > 0)
				sb.append(csvdelim);
			if (vv != null) {
				String szClss = vv != null ? vv.getClass().getSimpleName() : "Object";
				switch (szClss) {

				case "LocalDateTime":
					szv = ParseData.s_fmtTs.format((LocalDateTime) vv);
					szv = szv.replace(" 00:00:00", "");
					// szv = String.format(DtsCols.getColFmtR(), szv);
					break;

				case "Integer":
					szv = String.format(DtsCols.getColFmtR(), vv).trim();
					if (( (Integer)vv ) == 0 && dataset.isCsvBlankOnZero())
						szv="";
					break;

				case "Float":
					szv = String.format(DtsCols.getColFmtDbl(), vv);
					// szv = String.format(DtsCols.getColFmtR(), szv);
					if (( (Float)vv ) == 0 && dataset.isCsvBlankOnZero())
						szv="";
					break;

				case "Double":
					szv = String.format(DtsCols.getColFmtDbl(), vv);
					if (( (Double)vv ) == 0 && dataset.isCsvBlankOnZero())
						szv="";
					// szv = String.format(DtsCols.getColFmtR(), szv);
					break;

				case "Timestamp":
					szv = ParseData.s_fmtDtDate.format((Timestamp) vv);
					szv = szv.replace(" 00:00:00", "");
					// szv = String.format(DtsCols.getColFmtR(), szv);
					break;

				default:
					szv = vv.toString().replaceAll(csvdelim, escsep);
					break;
				}
				sb.append(szv);
			}
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

	public List<Object> getValues() {
		return valori;
	}

	public List<Object> getValues(boolean bEdit) {
		if (!bEdit)
			return valori;
		List<Object> loc = new ArrayList<Object>();
		for (Object o : valori) {
			Object ret = o;
			String szcls = null != o ? o.getClass().getSimpleName() : "*NULL*";
			switch (szcls) {
			case "Timestamp":
				Date dt = new Date(((Timestamp) o).getTime());
				ret = ParseData.s_fmtY4MD.format(dt.toInstant());
				break;
			default:
				break;
			}
			// String sz = null != o ? o.toString() : "";
			// System.out.printf("DtsRow.getValues([%s]=\"%s\")\n", szcls, sz);
			loc.add(ret);
		}
		return loc;
	}

}

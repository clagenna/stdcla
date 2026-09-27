package sm.clagenna.stdcla.sql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

/**
 * Classe di supporto per i {@link PreparedStatement} per la visualizzazione di
 * della sua query {@link #toString()} con parametri posizionali
 * 
 * @author clagenna
 *
 */
@Data
public class StmtShowQuery {
  @SuppressWarnings("unused")
  private static final Logger s_log = LogManager.getLogger(StmtShowQuery.class);

  private String       query;
  private List<String> params;

  public StmtShowQuery() {
    // s_log.debug("StmtShowQuery()");
  }

  public StmtShowQuery(String p_qry) {
    assign(p_qry);
  }

  public void assign(String p_qry) {
    query = p_qry;
    init();
  }

  private void init() {
    initParams();
  }

  /**
   * Inizializza la lista dei parametri andando alla ricerca dei parametri
   * posizionali '?' per contare la loro quantita per poi inizializzare l'array
   * con valori null
   */
  private void initParams() {
    StringBuilder sb = new StringBuilder(query);
    params = new ArrayList<>();
    int idx = sb.indexOf("?");
    String nullVal = "*null*";
    while (idx >= 0) {
      params.add(nullVal);
      sb.replace(idx, idx + 1, nullVal);
      idx = sb.indexOf("?");
    }
  }

  /**
   * Assegna un parametro posizionale alla query con le seguenti assunzioni:
   * <ul>
   * <li>i parametri siano posizionali e che siano indicati con il carattere
   * '?'.</li>
   * <li>l'indice del parametro sia 1-based (il primo parametro ha indice
   * 1).</li>
   * <li>se l'indice è 1 (primo) allora inizia una nuova assegnazione</li>
   * <li>il valore del parametro sia un oggetto di tipo String, Integer, Long,
   * Double, BigDecimal, java.util.Date, java.sql.Date, LocalDateTime.</li>
   * <li>se il valore del parametro e' null, allora il parametro viene
   * valorizzato con la stringa "*null*".</li>
   * <li>se il valore del parametro e' di tipo Double o BigDecimal, allora viene
   * formattato con 2 decimali.</li>
   * <li>se il valore del parametro e' di tipo java.util.Date, java.sql.Date o
   * LocalDateTime, allora viene formattato con il formato "yyyy-MM-dd
   * HH:mm:ss".</li>
   * </ul>
   * 
   * @param pIndx
   *          indice del parametro (1-based)
   * @param pVal
   *          valore del parametro
   */
  public void assignParameter(int pIndx, Object pVal) {
    // RICORDA ! pIndx e' 1-based!
    if (pIndx == 1)
      params = new ArrayList<String>();
    String szVal = "*null*";
    // lo riempio di *null* almeno fino all'indice pIndx-1, cosi' se non viene valorizzato un parametro, lo vedo come *null*
    while (params.size() < pIndx - 1)
      params.add(szVal);
    if (null == pVal) {
      params.add(pIndx - 1, szVal);
      return;
    }
    // converto il parametro in stringa
    szVal = switch (pVal) {
      case String str -> str;
      case Integer ii -> String.valueOf(ii);
      case Long li -> String.valueOf(li);
      case Double dbl -> Utils.formatDouble(dbl);
      case BigDecimal bd -> Utils.formatDouble(bd.doubleValue());
      case java.util.Date dt -> ParseData.formatDate(dt);
      // case java.sql.Date dtq -> ParseData.formatDate(dt);
      case LocalDateTime dtt -> ParseData.formatDate(dtt);
      //
      default -> szVal;
    };
    params.add(pIndx - 1, szVal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(query);
    for (int i = 0; i < params.size(); i++) {
      String szVal = params.get(i);
      int idx = sb.indexOf("?");
      if (idx >= 0) {
        sb.replace(idx, idx + 1, "'" + szVal + "'");
      }
    }
    return sb.toString();
  }

}

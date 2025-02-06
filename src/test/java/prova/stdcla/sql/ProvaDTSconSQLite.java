package prova.stdcla.sql;

import java.io.IOException;

import org.junit.Test;

import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.sql.DBConn;
import sm.clagenna.stdcla.sql.DBConnFactory;
import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sys.TimerMeter;

public class ProvaDTSconSQLite {

  private DBConnFactory dbfact;

  public ProvaDTSconSQLite() {
    //
  }

  /**
   * Purtroppo ci sono due ordini di grandezza tra SQLite e SQLserver...
   * <pre>
   *  DB Prima su F:.qtaRows=4582
   *  Passato <b>41,3780</b>,  "DB Prima su F:"
   *  
   *  DB Dopo su F:.qtaRows=4520
   *  Passato 33,2810,  "DB Dopo su F:"
   *  
   *  DB Prima su C:.qtaRows=4582
   *  Passato 40,0090,  "DB Prima su C:"
   *  
   *  DB Dopo su C:.qtaRows=4520
   *  Passato 32,9720,  "DB Dopo su C:"
   *  
   *  DB SQLServer.qtaRows=4520
   *  Passato <b>0,2370</b>, "DB SQLServer"
   * </pre> 
   * @throws IOException
   */
  @Test
  public void doTheJob() throws IOException {
    DBConnFactory.setSingleton(false);
    dbfact = new DBConnFactory();
    String qry1 = "SELECT * FROM ListaMovimentiUNION ORDER BY dtmov";
    String qry2 = "SELECT * FROM ListaMovimenti ORDER BY dtmov";

    String p_db1F = "datiProva\\Banca_Prima.db";
    String p_db2F = "datiProva\\Banca_Dopo.db";
    String p_db1C = "C:\\Winapp\\Banca\\Banca_Prima.db";
    String p_db2C = "C:\\Winapp\\Banca\\Banca_Dopo.db";

    openDtsDB("DB Prima su F:", p_db1F, qry1);
    openDtsDB("DB Dopo su F:", p_db2F, qry2);

    openDtsDB("DB Prima su C:", p_db1C, qry1);
    openDtsDB("DB Dopo su C:", p_db2C, qry2);

    openDtsDBSqlServer("DB SQLServer", "Banca", qry2);
  }

  private void openDtsDB(String p_id, String p_dbFile, String p_qry) {
    try (DBConn dbcon = dbfact.get(EServerId.SQLite3)) {
      dbcon.setDbname(p_dbFile);
      dbcon.doConn();
      try (Dataset dts = new Dataset(dbcon)) {
        dbcon.beginTrans();
        TimerMeter tm = new TimerMeter(p_id);
        dts.executeQuery(p_qry);
        System.out.printf("%s.qtaRows=%d\n", p_id, dts.size());
        System.out.println(tm.stop());
        System.out.println();
        dbcon.commitTrans();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void openDtsDBSqlServer(String p_id, String p_dbFile, String p_qry) {
    try (DBConn dbcon = dbfact.get(EServerId.SqlServer)) {
      dbcon.setDbname(p_dbFile);
      dbcon.setHost("locahost");
      dbcon.setService(1433);
      dbcon.setUser("sqlgianni");
      dbcon.setPasswd("sicuelserver");
      
      dbcon.doConn();
      try (Dataset dts = new Dataset(dbcon)) {
        dbcon.beginTrans();
        TimerMeter tm = new TimerMeter(p_id);
        dts.executeQuery(p_qry);
        System.out.printf("%s.qtaRows=%d\n", p_id, dts.size());
        System.out.println(tm.stop());
        System.out.println();
        dbcon.commitTrans();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}

package prova.stdcla.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import sm.clagenna.stdcla.sql.DBConnSQL;

public class ProvaDBConnSqlServ {

  private static final String QRY = //
      " SELECT ROW_NUMBER() OVER (ORDER BY dtmov,tipo) as rigaNo" + //
          "        ,tipo" + //
          "        ,idfile" + //
          "       ,dtmov" + //
          "       ,dare" + //
          "       ,avere" + //
          "       ,descr" + //
          "       ,abicaus" + //
          "       ,descrcaus" + //
          "   FROM Banca.dbo.ListaMovimentiUNION" + //
          "   WHERE 1=1" + //
          "     AND dtmov BETWEEN '01/01/2023' AND '31/01/2025'" + //
          "   ORDER BY dtmov,tipo";

  public ProvaDBConnSqlServ() {
    //  nothing to do
  }

  @Test
  public void doTheJob() {
    try (DBConnSQL dbc = new DBConnSQL()) {
      dbc.setHost("localhost");
      dbc.setService(1433);
      dbc.setDbname("banca");
      dbc.setUser("sqlgianni");
      dbc.setPasswd("sicuelserver");

      Connection conn = dbc.doConn();
      provaDescr(conn);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void provaDescr(Connection conn) {
    try (PreparedStatement stmt = conn.prepareStatement(QRY); ResultSet res = stmt.executeQuery()) {
      if (null == res || res.isClosed())
        return;

      int ncols = res.getMetaData().getColumnCount();
      while (res.next()) {
        StringBuilder sb = new StringBuilder();
        String vir = "";
        for (int k = 1; k <= ncols; k++) {
          sb.append(vir).append(String.valueOf(res.getObject(k)));
          vir = ",";
        }
        System.out.println(sb.toString());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}

package prova.stdcla.dts;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.opencsv.exceptions.CsvException;

import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class ProvaDatasetCSV {
  public ProvaDatasetCSV() {
    //
  }

  @Test
  public void provalo() throws DatasetException, CsvException {
    Path pthFi = Paths.get("src", "test", "resources", "prova", "stdcla", "dts", "abitanti2.csv");
    Path pthCopia = Paths.get("src", "test", "resources", "prova", "stdcla", "dts", "abitanti_Copia.csv");
    try (Dataset dts = new Dataset()) {
      // lettura del file CSV
      int qta = dts.readcsv(pthFi).size();
      System.out.printf("\nLetti %d records\n", qta);
      // elenco colonne
      System.out.println(dts.getColumns());

      dts.savecsv(pthCopia);
      System.out.println("Scritto:" + pthCopia);

      // calcolo delle medie
      System.out.printf("%20s:%f\n", "Eta media", dts.mean("eta"));
      System.out.printf("%20s:%f\n", "Eta media quardr", dts.sqrtmean("eta"));
      System.out.println();

      // provo il filtro solo F e M
      Dataset dtsF = dts.filter("sesso", s -> s.equals("F"));
      Dataset dtsM = dts.filter("sesso", s -> s.equals("M"));

      System.out.printf("%20s:%f\n", "Alt F media", dtsF.mean("altezza"));
      System.out.printf("%20s:%f\n", "Alt F media quardr", dtsF.sqrtmean("altezza"));
      System.out.println();

      System.out.printf("%20s:%f\n", "Alt M media", dtsM.mean("altezza"));
      System.out.printf("%20s:%f\n", "Alt M media quardr", dtsM.sqrtmean("altezza"));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}

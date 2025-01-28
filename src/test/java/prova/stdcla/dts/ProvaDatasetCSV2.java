package prova.stdcla.dts;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.opencsv.exceptions.CsvException;

import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class ProvaDatasetCSV2 {

  public ProvaDatasetCSV2() {
    //
  }

  @Test
  public void provalo() throws DatasetException, CsvException {
    Path pthFi = Paths.get( "F:\\Google Drive\\gennari\\Banche\\Amazon\\estratto_amzn_2412_cla.csv");
    Path pthCopia = Paths.get( "F:\\Google Drive\\gennari\\Banche\\Amazon\\nullo_amzn_2412_cla.csv");

    try (Dataset dts = new Dataset()) {
      dts.setCsvdelim(",");
      dts.setCsvBlankOnZero(true);
      // lettura del file CSV
      int qta = dts.readcsv(pthFi).size();
      System.out.println("Rec letti:" + qta);
      System.out.printf("QtaCols:%d\n%s\n",dts.getColumns().size(), dts.getColumns());
      System.out.println();
      System.out.println(dts.toString());
      // con la virgola s'impapina coi double italiani
      dts.setCsvdelim(";");
      dts.savecsv(pthCopia);
    } catch (IOException | CsvException e) {
      e.printStackTrace();
    }
  }

}

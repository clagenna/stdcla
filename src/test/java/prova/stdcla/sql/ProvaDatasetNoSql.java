package prova.stdcla.sql;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.sql.DtsCols;
import sm.clagenna.stdcla.sql.SqlTypes;
import sm.clagenna.stdcla.sys.ex.DatasetException;

public class ProvaDatasetNoSql {

  @Test
  public void provalo() throws DatasetException {
    DtsCols.setWidthCh(20);
    Dataset dts = new Dataset();
    // un linkedHashMap che mantiene l'ordine d'inserimento nel keySet
    Map<String, SqlTypes> map = new LinkedHashMap<String, SqlTypes>();
    map.put("Name", SqlTypes.VARCHAR);
    map.put("Weight", SqlTypes.DOUBLE);
    map.put("Height", SqlTypes.DOUBLE);
    map.put("Gender", SqlTypes.CHAR);
    dts.creaCols(map);

    addRow(dts, "Alice", 60.32, 165.10, 'F');
    addRow(dts, "Bob", 72.56, 182.88, 'M');
    addRow(dts, "Charlie", 68.93, 177.80, 'M');
    addRow(dts, "Diana", 54.42, 152.40, 'F');
    System.out.println(dts.toString());
    // -----------------------------------------------
    List<Map<String, Object>> lim = dts.toList();
    lim.stream().forEach(System.out::println);
    System.out.println("\n------- mean -------");
    // -------------- get array colums ---------------
    String szColNo = "Weight";
    List<Object> colv = dts.colArray(szColNo);
    // .............. Mean of column -----------------
    double somma = colv //
        .stream() //
        .mapToDouble(s -> Double.parseDouble(s.toString())) //
        .sum();
    double mean = somma / dts.size();
    System.out.printf("Media di %s\t\t= %.2f\n", szColNo, mean);
    // .............. Mean of column -----------------
    final double meanWe = dts.mean(szColNo);
    System.out.printf("Media(2) di %s\t= %.2f\n", szColNo, meanWe);
    // .............. Mean of column -----------------
    szColNo = "Height";
    final double meanHe = dts.mean(szColNo);
    System.out.printf("Media(3) di %s\t= %.2f\n", szColNo, meanHe);
    // ------------- conv Gender -----------------------
    System.out.println("\n------ conv Gender --------------");
    Dataset dt2 = dts.convert("Gender", s -> s.equals('F') ? 1 : 0);
    System.out.println(dt2.toString());
    // ------------ Norm Weight------------------------------
    System.out.println("\n------ norm Weight --------------");
    Dataset dt3 = dt2.convert("Weight", s -> (Double) s - meanWe);
    System.out.println(dt3.toString());
    // ------------ Norm Height------------------------------
    System.out.println("\n------ norm Height --------------");
    Dataset dt4 = dt3.convert("Height", s -> (Double) s - meanHe);
    System.out.println(dt4.toString());
  }

  private void addRow(Dataset dt, Object... p_li) throws DatasetException {
    List<Object> lio = Arrays.asList(p_li);
    dt.addRow(lio);
  }

}

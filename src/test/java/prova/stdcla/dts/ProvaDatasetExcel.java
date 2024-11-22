package prova.stdcla.dts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import sm.clagenna.stdcla.sql.Dataset;
import sm.clagenna.stdcla.utils.Utils;

public class ProvaDatasetExcel {

  public ProvaDatasetExcel() {
    //
  }

  @Test
  public void provalo() {
    Path pthExcel = Paths.get("datiProva/estrattoconto_SMAC_2024-11-22_cla.xls");
    Path pthCsv = Paths.get("datiProva/estrattoconto_SMAC_2024-11-22_cla.csv");
    // readExcel(pthExcel);
    try (Dataset dts = new Dataset()) {
      // lettura del file EXCEL
      int qta = dts.readexcel(pthExcel).size();
      System.out.println("ProvaDatasetExcel righe=" + qta);
      dts.savecsv(pthCsv);
      System.out.println("ProvaDatasetExcel Salvato su:" + pthCsv.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unused")
  private void readExcel(Path pthExcel) {
    String szExt = Utils.getFileExtention(pthExcel);
    switch (szExt) {
      case ".xls":
        readExcelHSSF(pthExcel);
        break;
      case ".xlsx":
        readExcelXML(pthExcel);
        break;
    }
  }

  private void readExcelXML(Path pthExcel) {
    try (FileInputStream file = new FileInputStream(pthExcel.toFile())) {
      //Create Workbook instance holding reference to .xlsx file
      try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
        //Get first/desired sheet from the workbook
        XSSFSheet sheet = workbook.getSheetAt(0);
        //Iterate through each rows one by one
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
          Row row = rowIterator.next();
          //For each row, iterate through all the columns
          Iterator<Cell> cellIterator = row.cellIterator();
          while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            //Check the cell type and format accordingly
            switch (cell.getCellType()) {
              case CellType.NUMERIC:
                System.out.print(cell.getNumericCellValue() + "\t");
                break;
              case CellType.STRING:
                System.out.print(cell.getStringCellValue() + "\t");
                break;
              default:
                System.out.print(cell.getStringCellValue() + "??\t");
                break;
            }
          }
          System.out.println("");
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void readExcelHSSF(Path pthExcel) {
    try (FileInputStream file = new FileInputStream(pthExcel.toFile())) {
      //Create Workbook instance holding reference to .xlsx file
      try (HSSFWorkbook workbook = new HSSFWorkbook(file)) {
        //Get first/desired sheet from the workbook
        HSSFSheet sheet = workbook.getSheetAt(0);
        //Iterate through each rows one by one
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
          Row row = rowIterator.next();
          //For each row, iterate through all the columns
          Iterator<Cell> cellIterator = row.cellIterator();
          while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            //Check the cell type and format accordingly
            switch (cell.getCellType()) {
              case CellType.NUMERIC:
                System.out.print(cell.getNumericCellValue() + "\t");
                break;
              case CellType.STRING:
                System.out.print(cell.getStringCellValue() + "\t");
                break;
              default:
                System.out.print(cell.getStringCellValue() + "??\t");
                break;
            }
          }
          System.out.println("");
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}

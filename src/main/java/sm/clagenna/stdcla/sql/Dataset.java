package sm.clagenna.stdcla.sql;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.enums.EServerId;
import sm.clagenna.stdcla.sys.ex.DatasetException;
import sm.clagenna.stdcla.utils.ECurrencies;
import sm.clagenna.stdcla.utils.ParseData;
import sm.clagenna.stdcla.utils.Utils;

public class Dataset implements Closeable {
  private static final Logger s_log             = LogManager.getLogger(Dataset.class);
  private static final String DEFAULT_CSV_DELIM = ";";
  @Getter @Setter
  private EServerId           tipoServer;
  private DtsCols             columns;
  @Getter @Setter
  private DBConn              db;
  @Getter @Setter
  private String              csvdelim;
  @Getter @Setter
  private boolean             csvBlankOnZero;
  @Getter
  private List<DtsRow>        righe;
  /**
   * scandisce solo la quantita di colonne conosciute (vedi
   * {@link #getQtaCols()} )
   */
  @Getter @Setter
  private boolean             onlyKnownCols;
  /**
   * fa si che anche gli interi durante la {@link #guessSqlType(String)} vengano
   * interpretati come double
   */
  @Getter @Setter
  private boolean             intToDouble;
  private int                 skipRows;

  public Dataset() {
    setTipoServer(EServerId.SqlServer);
  }

  public Dataset(EServerId p_serverId) {
    setTipoServer(p_serverId);
  }

  public Dataset(DBConn p_con) {
    setDb(p_con);
    setTipoServer(p_con.getServerId());
  }

  public boolean executeQuery(String p_qry) {
    boolean bRet = false;
    Connection conn = db.getConn();
    // TimerMeter tm1 = new TimerMeter("prep qry" + p_qry);
    try (PreparedStatement stmt = conn.prepareStatement(p_qry); //
        ResultSet res = stmt.executeQuery()) {
      creaCols(stmt);
      addRows(res);
      bRet = true;
      // System.out.println(tm1.stop());
    } catch (SQLException | DatasetException e) {
      s_log.error("Error execute Query, err={}", e.getMessage());
    }
    return bRet;
  }

  /**
   * Legge un file CSV dove sono presenti i nomi di colonna nella prima riga.
   * Inoltre il separatore di campi e' specificato nel campo csvdelim (default
   * semicolon ';')
   *
   * @param p_csvFil
   * @return qta di records letti
   * @throws IOException
   * @throws CsvException
   */
  public Dataset readcsv(Path p_csvFil) throws IOException, CsvException {
    if (null == csvdelim)
      csvdelim = DEFAULT_CSV_DELIM;
    if (null == columns || columns.size() == 0)
      creaCsvCols2(p_csvFil);
    readCsvFile2(p_csvFil);
    return this;
  }

  public void savecsv(Path p_fil) throws DatasetException {
    if (null == csvdelim)
      csvdelim = DEFAULT_CSV_DELIM;
    if (null == columns || columns.size() == 0)
      throw new DatasetException("Nothing to save");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(p_fil.toFile(), false))) {
      bw.write(columns.csvIntestazione(csvdelim));
      bw.newLine();
      for (DtsRow row : righe) {
        bw.write(row.toCsv(csvdelim));
        bw.newLine();
      }
    } catch (IOException e) {
      throw new DatasetException("Write :" + p_fil.toString(), e);
    }
    s_log.info("Salvato file CSV {}", p_fil.toString());
  }

  @SuppressWarnings("unused")
  private void creaCsvCols(Path p_csvFil) throws IOException {
    // FIXME Impostare una var che tiene conto se l'elenco di colonne finisce con ';' finale vuoto
    List<List<String>> recs = null;
    // leggo le prime 4 per capire il nome delle colonne
    // ogni riga e' split(csvDelim) in un Array
    try (BufferedReader reader = Files.newBufferedReader(p_csvFil, Charset.defaultCharset())) {
      recs = reader.lines() //
          .limit(4) //
          .map(li -> Arrays.asList(li.split(csvdelim))) //
          .collect(Collectors.toList());
    } catch (Exception e) {
      s_log.error("Errore Read CSV file {}, err={}", p_csvFil.getFileName().toString(), e.getMessage());
    }
    // test se inizia il file con la specifica "sep=x"
    if (recs.size() > 0) {
      List<String> li = recs.get(0);
      // se c'e' 1 solo elem allora probabile la direttiva "sep=x"
      if (li.size() == 1 && null != li.get(0)) {
        // tolgo l'eventuale UTF-8 BOM (maledetto!)
        String sz = checkBOM(li.get(0).toLowerCase());
        // forse ho una specifica "sep=c", quindi la interpreto
        if (sz.contains("sep=")) {
          sz = sz.replace("sep=", "");
          if (sz.length() > 0)
            setCsvdelim(sz);
          else
            sz = getCsvdelim();
          s_log.warn("CSV file {} contiene la specifica \"sep={}\", imposto questa!", p_csvFil.getFileName().toString(), sz);
        }
      }
    }
    // analizzo i primi 20 records
    try (BufferedReader reader = Files.newBufferedReader(p_csvFil)) {
      recs = reader.lines() //
          .limit(20) //
          .map(li -> Arrays.asList(li.split(csvdelim))) //
          .collect(Collectors.toList());

      List<String> liNames = recs.get(0);
      skipRows = 1;
      if (liNames.size() == 1 && checkBOM(liNames.get(0).toString()).contains("sep=")) {
        liNames = recs.get(1);
        skipRows++;
      }
      // analizzo i nomi delle colonne
      int k = 0;
      for (String sz : liNames) {
        // solo per il primo nome di colonna (epuro il BOM)
        if (k == 0) {
          String sz1 = checkBOM(sz);
          // se c'è il BOM lo epuro ! (Maledetto!!)
          if ( !sz1.equals(sz)) {
            liNames.set(0, sz1);
            sz = sz1;
          }
        }
        // epuro le virgolette se ci sono
        if (sz.startsWith("\""))
          liNames.set(k, sz.replace("\"", ""));
        k++;
      }
      // indovino le tipologie delle colonne
      List<SqlTypes> liTypes = guessSqlTypes(recs);
      int diff = liNames.size() - liTypes.size();
      for (; diff > 0; diff--)
        liTypes.add(SqlTypes.VARCHAR);

      Map<String, SqlTypes> mpCols = new LinkedHashMap<>();
      int i = 0;
      try {
        for (String na : liNames)
          mpCols.put(na.trim(), liTypes.get(i++));
      } catch (Exception e) {
        s_log.error("Errore crea DataSet su file {}, err={}", p_csvFil.getFileName().toString(), e.getMessage());
      }
      creaCols(mpCols);
    }
  }

  private void creaCsvCols2(Path p_csvFil) throws IOException {
    // FIXME Impostare una var che tiene conto se l'elenco di colonne finisce con ';' finale vuoto
    List<List<String>> recs = null;
    // leggo le prime 4 per capire il nome delle colonne
    // ogni riga e' split(csvDelim) in un Array
    skipRows = 0;
    CSVParser csvParser = new CSVParserBuilder().withSeparator(csvdelim.charAt(0)).build(); // custom separator
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(p_csvFil.toFile())).withCSVParser(csvParser) // custom CSV parser
        .withSkipLines(skipRows) // skip the first line, header info
        .build()) {
      recs = reader.readAll() //
          .stream() //
          .limit(4) //
          // .map(li -> Arrays.asList(li.split(csvdelim))) //
          .map(li -> Arrays.asList(li)) //
          .collect(Collectors.toList());
    } catch (CsvException e) {
      s_log.error("Errore Read CSV file {}, err={}", p_csvFil.getFileName().toString(), e.getMessage());
    }

    // test se inizia il file con la specifica "sep=x"
    if (recs.size() > 0) {
      List<String> li = recs.get(0);
      // se c'e' 1 solo elem allora probabile la direttiva "sep=x"
      if (li.size() <= 2 && null != li.get(0)) {
        // tolgo l'eventuale UTF-8 BOM (maledetto!)
        String sz = checkBOM(li.get(0).toLowerCase());
        // forse ho una specifica "sep=c", quindi la interpreto
        if (sz.contains("sep=")) {
          sz = sz.replace("sep=", "");
          if (sz.length() == 0 && li.size() == 2) {
            // qui e il caso sep='csvdelim'
            skipRows++;
          } else if (sz.length() > 0) {
            setCsvdelim(sz);
            skipRows++;
          } else
            sz = getCsvdelim();
          s_log.warn("CSV file {} contiene la specifica \"sep={}\", imposto questa!", p_csvFil.getFileName().toString(), sz);
        }
      }
    }
    // analizzo i primi 20 records

    csvParser = new CSVParserBuilder().withSeparator(csvdelim.charAt(0)).build(); // custom separator
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(p_csvFil.toFile())).withCSVParser(csvParser) // custom CSV parser
        .withSkipLines(skipRows) // skip the first line, header info
        .build()) {
      recs = reader.readAll() //
          .stream() //
          .limit(20) //
          // .map(li -> Arrays.asList(li.split(csvdelim))) //
          .map(li -> Arrays.asList(li)) //
          .collect(Collectors.toList());

      List<String> liNames = recs.get(0);
      skipRows++;
      if (liNames.size() == 1 && checkBOM(liNames.get(0).toString()).contains("sep=")) {
        liNames = recs.get(1);
        skipRows++;
      }
      // analizzo i nomi delle colonne
      int k = 0;
      for (String sz : liNames) {
        // solo per il primo nome di colonna (epuro il BOM)
        if (k == 0) {
          String sz1 = checkBOM(sz);
          // se c'è il BOM lo epuro ! (Maledetto!!)
          if ( !sz1.equals(sz)) {
            liNames.set(0, sz1);
            sz = sz1;
          }
        }
        // epuro le virgolette se ci sono
        if (sz.startsWith("\""))
          liNames.set(k, sz.replace("\"", ""));
        k++;
      }
      // indovino le tipologie delle colonne
      List<SqlTypes> liTypes = guessSqlTypes(recs);
      int diff = liNames.size() - liTypes.size();
      for (; diff > 0; diff--)
        liTypes.add(SqlTypes.VARCHAR);

      Map<String, SqlTypes> mpCols = new LinkedHashMap<>();
      int i = 0;
      try {
        for (String na : liNames)
          mpCols.put(na.trim(), liTypes.get(i++));
      } catch (Exception e) {
        s_log.error("Errore crea DataSet su file {}, err={}", p_csvFil.getFileName().toString(), e.getMessage());
      }
      creaCols(mpCols);
    } catch (CsvException e1) {
      s_log.error("Errore parse CSV file {}, err={}", p_csvFil.getFileName().toString(), e1.getMessage());
    }
  }

  /**
   * Il Byte Order Mark (BOM), per UTF-8 è EF BB BF ( 0xEF 0xBB 0xBF ). indica
   * la codifica di un file ma può causare problemi se non lo gestiamo
   * correttamente, specialmente quando elaboriamo dati di testo. Per questo, se
   * presente lo tolgo
   *
   * @param psz
   * @return
   */
  private String checkBOM(String psz) {
    if (null == psz)
      return psz;
    psz = psz.trim();
    if (psz.length() == 0)
      return psz;
    String szX = Integer.toHexString(psz.charAt(0));
    if (szX.equalsIgnoreCase("feff"))
      psz = psz.substring(1);
    return psz;
  }

  public void addCol(String szNam, SqlTypes p_ty) {
    if (null == columns)
      columns = new DtsCols(this);
    columns.addCol(szNam, p_ty);
    if (null == righe)
      return;
    for (DtsRow row : righe) {
      row.setDataset(this);
      row.addCol(p_ty);
    }

  }

  @SuppressWarnings("unused")
  private void readCsvFile(Path p_csvFil) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(p_csvFil)) {
      reader //
          .lines() //
          .skip(skipRows) //
          .map(li -> Arrays.asList(li.split(csvdelim))) //
          .forEach(s -> parseRow(s));
    }
  }

  private void readCsvFile2(Path p_csvFil) throws IOException, CsvException {
    CSVParser csvParser = new CSVParserBuilder().withSeparator(csvdelim.charAt(0)).build(); // custom separator
    //    try (CSVReader reader = new CSVReaderBuilder(new FileReader(p_csvFil.toFile())).withCSVParser(csvParser) // custom CSV parser
    //        .withSkipLines(skipRows) // skip the first line, header info
    //        .build()) {
    //      List<String[]> recs = reader.readAll();
    //
    //      recs.forEach(s -> parseRow(Arrays.asList(s)));
    //    }
    //
    // List<List<String>> recs;
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(p_csvFil.toFile())).withCSVParser(csvParser) // custom CSV parser
        .withSkipLines(skipRows) // skip the first line, header info
        .build()) {
      reader.readAll() //
          .stream() //
          .skip(skipRows) //
          .map(li -> Arrays.asList(li)) //
          .forEach(s -> parseRow(s));
    }
  }

  public Dataset readexcel(Path p_excelFil) throws IOException {
    String szExt = Utils.getFileExtention(p_excelFil);
    switch (szExt) {
      case ".xls":
        // formato Excel 1995 -
        readExcelHSSF(p_excelFil);
        break;
      case ".xlsx":
        readExcelXSSF(p_excelFil);
        break;
    }
    return this;
  }

  private void readExcelHSSF(Path p_excelFil) throws FileNotFoundException, IOException {
    if (null == columns || columns.size() == 0)
      creaExcelHSSFCols(p_excelFil);
    readExcelHSSFFile(p_excelFil);
  }

  private void creaExcelHSSFCols(Path p_excelFil) throws FileNotFoundException, IOException {
    List<List<String>> recs = readExcelHSSFFile(p_excelFil, 20);
    List<String> liNames = recs.get(0);
    skipRows = 1;
    if (liNames.size() == 1) {
      liNames = recs.get(1);
      skipRows++;
    }
    int k = 0;
    for (String sz : liNames) {
      if (sz.startsWith("\""))
        liNames.set(k, sz.replace("\"", ""));
      // se ci sono parentesi nei nomi tolgo tutto
      if (sz.contains("(")) {
        int n = sz.indexOf("(");
        if (n > 0) {
          sz = sz.substring(0, n);
          liNames.set(k, sz);
        }
      }
      k++;
    }
    List<SqlTypes> liTypes = guessSqlTypes(recs);
    int diff = liNames.size() - liTypes.size();
    for (; diff > 0; diff--)
      liTypes.add(SqlTypes.VARCHAR);

    Map<String, SqlTypes> mpCols = new LinkedHashMap<>();
    int i = 0;
    try {
      for (String na : liNames)
        mpCols.put(na.trim(), liTypes.get(i++));
    } catch (Exception e) {
      s_log.error("Errore crea DataSet su file {}, err={}", p_excelFil.getFileName().toString(), e.getMessage());
    }
    creaCols(mpCols);
  }

  private List<List<String>> readExcelHSSFFile(Path p_excelFil) throws FileNotFoundException, IOException {
    return readExcelHSSFFile(p_excelFil, -1);
  }

  /**
   * Leggo il foglio Excel formato Excel 97-2003 (poi HSSF)
   *
   * @param p_excelFil
   *          il file formato Excel 97-2003 (poi HSSF) da leggere
   * @param qtaMax
   *          se -1 letto tutto il foglio e riga per riga la aggiungo chiamendo
   *          {@link #parseRow()} altrimenti ritorno un List<> con qtaMax righe
   *          lette
   * @return se qtaMax > 0 torna qtaMax righe altrimenti List<> vuoto
   * @throws FileNotFoundException
   * @throws IOException
   */
  private List<List<String>> readExcelHSSFFile(Path p_excelFil, int qtaMax) throws FileNotFoundException, IOException {
    List<List<String>> righe = new ArrayList<>();
    try (FileInputStream file = new FileInputStream(p_excelFil.toFile())) {
      try (HSSFWorkbook workbook = new HSSFWorkbook(file)) {
        HSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        int qtaRighe = 0;
        while (rowIterator.hasNext()) {
          Row row = rowIterator.next();
          if (qtaRighe++ < skipRows)
            continue;
          Iterator<Cell> cellIterator = row.cellIterator();
          List<String> liRiga = new ArrayList<>();
          while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            int nCol = cell.getColumnIndex();
            while (liRiga.size() < nCol)
              liRiga.add("");
            switch (cell.getCellType()) {
              case CellType.NUMERIC:
                liRiga.add(Utils.formatDouble(cell.getNumericCellValue()));
                break;
              case CellType.STRING:
              case CellType.BLANK:
                liRiga.add(cell.getStringCellValue());
                break;
              default:
                s_log.warn("Cella del file {} HSSF non riconosciuta Riga.{} Col.{}", p_excelFil.toString(), qtaRighe,
                    cell.getColumnIndex());
                liRiga.add(null);
                break;
            }
          }
          if (qtaMax < 0) {
            // leggo tutto il foglio Excel
            parseRow(liRiga);
          } else {
            // le righe le tratto dopo
            righe.add(liRiga);
            if (righe.size() > 20)
              break;
          }
        }
      }
    }
    return righe;
  }

  private void readExcelXSSF(Path p_excelFil) throws FileNotFoundException, IOException {
    if (null == columns || columns.size() == 0)
      creaExcelXSSFCols(p_excelFil);
    readExcelXSSFFile(p_excelFil);
  }

  private void creaExcelXSSFCols(Path p_excelFil) throws FileNotFoundException, IOException {
    List<List<String>> recs = readExcelXSSFFile(p_excelFil, 20);
    List<String> liNames = recs.get(0);
    skipRows = 1;
    if (liNames.size() == 1) {
      liNames = recs.get(1);
      skipRows++;
    }
    int k = 0;
    for (String sz : liNames) {
      if (sz.startsWith("\""))
        liNames.set(k, sz.replace("\"", ""));
      // se ci sono parentesi nei nomi tolgo tutto
      if (sz.contains("(")) {
        int n = sz.indexOf("(");
        if (n > 0) {
          sz = sz.substring(0, n);
          liNames.set(k, sz);
        }
      }
      k++;
    }
    List<SqlTypes> liTypes = guessSqlTypes(recs);
    int diff = liNames.size() - liTypes.size();
    for (; diff > 0; diff--)
      liTypes.add(SqlTypes.VARCHAR);

    Map<String, SqlTypes> mpCols = new LinkedHashMap<>();
    int i = 0;
    try {
      for (String na : liNames)
        mpCols.put(na.trim(), liTypes.get(i++));
    } catch (Exception e) {
      s_log.error("Errore crea DataSet su file {}, err={}", p_excelFil.getFileName().toString(), e.getMessage());
    }
    creaCols(mpCols);
  }

  private List<List<String>> readExcelXSSFFile(Path p_excelFil) throws FileNotFoundException, IOException {
    return readExcelXSSFFile(p_excelFil, -1);
  }

  private List<List<String>> readExcelXSSFFile(Path p_excelFil, int qtaMax) throws FileNotFoundException, IOException {
    List<List<String>> righe = new ArrayList<>();
    try (FileInputStream file = new FileInputStream(p_excelFil.toFile())) {
      try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        int qtaRighe = 0;
        while (rowIterator.hasNext()) {
          Row row = rowIterator.next();
          if (qtaRighe++ < skipRows)
            continue;
          Iterator<Cell> cellIterator = row.cellIterator();
          List<String> liRiga = new ArrayList<>();
          while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            int nCol = cell.getColumnIndex();
            while (liRiga.size() < nCol)
              liRiga.add("");
            switch (cell.getCellType()) {
              case CellType.NUMERIC:
                String tipo = cell.getCellStyle().getDataFormatString();
                if (null != tipo && tipo.toLowerCase().contains("yy")) {
                  Date dt = cell.getDateCellValue();
                  String sz = ParseData.s_fmtDtDate.format(dt);
                  liRiga.add(sz);
                } else
                  liRiga.add(Utils.formatDouble(cell.getNumericCellValue()));
                break;
              case CellType.STRING:
                liRiga.add(cell.getStringCellValue());
                break;
              case CellType.FORMULA: {
                FormulaEvaluator eval = workbook.getCreationHelper().createFormulaEvaluator();
                switch (eval.evaluateFormulaCell(cell)) {
                  case STRING:
                    liRiga.add(cell.getStringCellValue());
                    break;
                  case NUMERIC:
                    liRiga.add(Utils.formatDouble(cell.getNumericCellValue()));
                    break;
                  default:
                    s_log.warn("Cella Formula del file {} XSSF non riconosciuta Riga.{} Col.{}", p_excelFil.toString(), qtaRighe,
                        cell.getColumnIndex());
                    liRiga.add(null);
                    break;
                }
              }
                break;
              default:
                s_log.warn("Cella del file {} XSSF non riconosciuta Riga.{} Col.{}", p_excelFil.toString(), qtaRighe,
                    cell.getColumnIndex());
                liRiga.add(null);
                break;
            }
          }
          if (qtaMax < 0) {
            // leggo tutto il foglio Excel
            parseRow(liRiga);
          } else {
            // le righe le tratto dopo
            righe.add(liRiga);
            if (righe.size() > 20)
              break;
          }
        }
      }
    }
    return righe;
  }

  /**
   * Cerca di indovinare il {@link SqlTypes} di ogni colonna facendo una analisi
   * con precedenza di difficolta' del tipo confrontando il .code()
   *
   * @param p_recs
   * @return
   */
  private List<SqlTypes> guessSqlTypes(List<List<String>> p_recs) {
    List<SqlTypes> liTy = new ArrayList<SqlTypes>();
    int k = 0;
    for (List<String> riga : p_recs) {
      // salto il "sep="
      // salto i "nomi colonna"
      if (riga.size() <= 1 || k++ < 1)
        continue;
      if (k > 20)
        break;
      int col = 0;
      for (String sz : riga) {
        SqlTypes ty = guessSqlType(sz);
        if (liTy.size() <= col)
          liTy.add(ty);
        else {
          SqlTypes prevty = liTy.get(col);
          if (prevty.code() > ty.code())
            liTy.set(col, ty);
        }
        col++;
      }
    }
    return liTy;
  }

  private SqlTypes guessSqlType(String p_sz) {
    // ******   date *********
    String lsz = p_sz.replace("\"", "");
    if (lsz.contains(ECurrencies.Euro.getSymbol()))
      lsz = lsz.replaceAll(ECurrencies.Euro.getSymbol(), "");
    if (lsz.contains(ECurrencies.Dollar.getSymbol()))
      lsz = lsz.replaceAll(ECurrencies.Dollar.getSymbol(), "");
    LocalDateTime dt = ParseData.parseData(lsz);
    if (null != dt)
      return SqlTypes.DATE;
    // ******* double ********
    if (lsz.contains(".") || lsz.contains(",") || isIntToDouble()) {
      Double dbl = null;
      try {
        dbl = Double.parseDouble(lsz.replace(",", "."));
        if (null != dbl)
          return SqlTypes.DOUBLE;
      } catch (NumberFormatException e) {
        //
      }
    }
    // ***** integer *******
    Integer ii = null;
    try {
      ii = Integer.parseInt(lsz);
      if (ii != null)
        return SqlTypes.INTEGER;
    } catch (NumberFormatException e) {
      //
    }
    return SqlTypes.VARCHAR;
  }

  public void creaCols(PreparedStatement p_stmt) throws DatasetException {
    columns = new DtsCols(this);
    columns.parseColsStatement(p_stmt);
  }

  public void creaCols(Map<String, SqlTypes> p_map) {
    columns = new DtsCols(this);
    columns.creaCols(p_map);
  }

  private void creaCols(DtsCols p_columns) {
    try {
      columns = (DtsCols) p_columns.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

  public int addRows(ResultSet p_stmt) throws DatasetException {
    int nRet = -1;
    try {
      while (p_stmt.next()) {
        DtsRow row = new DtsRow(this);
        row.addRow(p_stmt);
        if (s_log.isTraceEnabled()) {
          System.out.printf("%s\n%s\n", columns.getIntestazione(), row.toString());
        }
        addRow(row);
      }
      if (null != righe)
        nRet = righe.size();
    } catch (SQLException e) {
      s_log.error("Errore in addRow(resultset), msg={}", e.getMessage());
      throw new DatasetException("Errore in addRow(resultset)", e);
    }
    return nRet;
  }

  public int addRow(DtsRow p_r) {
    if (righe == null)
      righe = new ArrayList<>();
    // System.out.println(p_r);
    righe.add(p_r);
    return righe.size();
  }

  public void parseRow(List<String> p_rec) {
    DtsRow row = new DtsRow(this);
    row.parseRow(p_rec);
    addRow(row);
  }

  public int addRow(List<Object> p_lio) throws DatasetException {
    DtsRow row = new DtsRow(this);
    row.addRow(p_lio);
    addRow(row);
    return righe.size();
  }

  public int getQtaCols() {
    return columns.size();
  }

  public DtsCols getColumns() {
    return columns;
  }

  public DtsCol getColum(int p_i) {
    if (null == columns || columns.size() <= p_i)
      throw new UnsupportedOperationException("Col no of bound:" + p_i);
    return columns.getCol(p_i);
  }

  /**
   * Torna l'indice della colonna con quel nome
   * 
   * @param p_nam
   *          nome colonna
   * @return l'indice colonnoa, oppure -1 se non trovata
   */
  public int getColumNo(String p_nam) {
    return columns.getColIndex(p_nam);
  }

  public DtsRow getRow(int p_i) throws DatasetException {
    if (null == righe || p_i >= righe.size())
      throw new DatasetException("Index " + p_i + " out of bound");
    return righe.get(p_i);
  }

  /**
   * Torna una List<> dei valori della sola colonna p_colNam
   *
   * @param p_colNam
   *          la colonna da estrarre
   * @return
   */
  public List<Object> colArray(String p_colNam) {
    List<Object> li = new ArrayList<>();
    int ii = columns.getColIndex(p_colNam);
    for (DtsRow row : righe) {
      li.add(row.get(ii));
    }
    return li;
  }

  public Dataset convert(String p_col, IDtsConvCol<Object> p_conv) {
    Dataset ret = new Dataset();
    ret.creaCols(columns);
    SqlTypes newty = null;
    try {
      for (DtsRow row : righe) {
        DtsRow r = (DtsRow) row.clone();
        Object vv = r.get(p_col);
        Object cnv = p_conv.converti(vv);
        if (null == newty) {
          newty = SqlTypes.translate(cnv);
          DtsCol co = ret.columns.getCol(p_col);
          co.setType(newty);
        }
        r.set(p_col, cnv);
        ret.addRow(r);
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return ret;
  }

  public Dataset filter(String p_col, IDtsFiltra<Object> p_conv) {
    Dataset ret = new Dataset();
    ret.creaCols(columns);
    try {
      for (DtsRow row : righe) {
        DtsRow r = (DtsRow) row.clone();
        Object vv = r.get(p_col);
        if (p_conv.filtra(vv))
          ret.addRow(r);
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return ret;
  }

  /**
   * Torna un {@link Dataset} con le sole colonne (case-insensitive) specificate
   *
   * @param p_colList
   *          elenco di colonne da includere
   * @return
   * @throws DatasetException
   */
  public Dataset colSubset(List<String> p_colList) throws DatasetException {
    Dataset ret = new Dataset();
    DtsCols cols = new DtsCols(ret);
    try {
      for (DtsCol col : getColumns().getColumns()) {
        if (p_colList.stream().anyMatch(col.getName()::equalsIgnoreCase)) {
          cols.addCol((DtsCol) col.clone());
        }
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    if (cols.size() == 0)
      throw new DatasetException("No cols for " + p_colList.toString());
    ret.creaCols(cols);
    for (DtsRow row : righe) {
      DtsRow newro = new DtsRow(ret);
      for (String szc : p_colList) {
        newro.addVal(szc, row.get(szc));
      }
      ret.addRow(newro);
    }
    return ret;
  }

  /**
   * Creo un nuovo {@link Dataset} con i records specificati. <code>p_ini</code>
   * inclusive, <code>p_fin</code> <b>ex</b>clusive.<br/>
   * Se <code>p_fin</code> oltre <b>this.size()</b> il subset si ferma
   * all'ultimo record.
   *
   * @param p_ini
   *          index record iniziale
   * @param p_fin
   *          index record finale escluso
   * @return
   * @throws DatasetException
   */
  public Dataset subset(int p_ini, int p_fin) throws DatasetException {
    if (p_ini < 0 || p_ini >= p_fin)
      throw new DatasetException("Indexes incongruent");
    Dataset ret = new Dataset();
    try {
      DtsCols cols = (DtsCols) getColumns().clone();
      ret.creaCols(cols);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    int i = 0;
    try {
      for (DtsRow row : getRighe()) {
        if (i >= p_ini && i < p_fin)
          ret.addRow((DtsRow) row.clone());
        if (i++ >= p_fin)
          break;
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return ret;
  }

  /**
   * Torna un nuovo dataset contenente le sole colonne specificate in
   * <code>p_colList</code> e un solo row con la somma delle colonne specificate
   *
   * @param p_colList
   *          le colonne da sommare
   * @return
   * @throws DatasetException
   */
  public Dataset sum(List<String> p_colList) throws DatasetException {
    Dataset ret = new Dataset();
    DtsCols cols = new DtsCols(ret);
    try {
      for (DtsCol col : getColumns().getColumns()) {
        // ??? non so perche' non funziona
        //        if (p_colList.stream().anyMatch(col.getName()::equalsIgnoreCase))
        //          cols.addCol((DtsCol) col.clone());
        // !! Scoperto !!
        //        szColNam.getBytes()
        //        (byte[]) [-17, -69, -65, 99, 111, 100, 105, 115, 115]
        //                  ^????????????^  <-- cos'e' sta roba ?!? E' il BOM (Maledetto!)
        //     p_colList.get(0).getBytes()
        //        (byte[]) [               99, 111, 100, 105, 115, 115]      szColNam.getBytes()
        //        String szColNam = col.getName();
        //        if (p_colList.contains(szColNam))
        //          cols.addCol((DtsCol) col.clone());
        if (p_colList.stream().anyMatch(col.getName()::equalsIgnoreCase))
          cols.addCol((DtsCol) col.clone());
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    if (cols.size() == 0)
      throw new DatasetException("No cols for " + p_colList.toString());
    ret.creaCols(cols);
    DtsRow newro = new DtsRow(ret);

    for (String colnam : p_colList) {
      List<Object> colv = colArray(colnam);
      double somma = colv //
          .stream() //
          .mapToDouble(s -> Double.parseDouble(s.toString())) //
          .sum();
      newro.set(colnam, somma);
    }
    ret.addRow(newro);
    return ret;
  }

  public double mean(String p_colNam) throws DatasetException {
    DtsCol col = columns.getCol(p_colNam);
    if (null == col)
      throw new DatasetException("no col:" + p_colNam);
    if (0 == size())
      throw new DatasetException("mean on no data");
    SqlTypes typ = col.getType();
    switch (typ) {
      case TINYINT:
      case SMALLINT:
      case INTEGER:
      case NUMERIC:
      case DECIMAL:
      case FLOAT:
      case DOUBLE:
        break;
      default:
        throw new DatasetException("Mean on not numeric:" + p_colNam);
    }
    List<Object> colv = colArray(p_colNam);
    double somma = colv //
        .stream() //
        .mapToDouble(s -> Double.parseDouble(s.toString())) //
        .sum();
    return somma / size();
  }

  public double sqrtmean(String p_colNam) throws DatasetException {
    DtsCol col = columns.getCol(p_colNam);
    if (null == col)
      throw new DatasetException("no col:" + p_colNam);
    if (0 == size())
      throw new DatasetException("mean on no data");
    SqlTypes typ = col.getType();
    switch (typ) {
      case TINYINT:
      case SMALLINT:
      case INTEGER:
      case NUMERIC:
      case DECIMAL:
      case FLOAT:
      case DOUBLE:
        break;
      default:
        throw new DatasetException("sqrtMean on not numeric:" + p_colNam);
    }
    List<Object> colv = colArray(p_colNam);
    double somma = colv //
        .stream() //
        .mapToDouble(s -> Double.parseDouble(s.toString())) //
        .map(s -> s * s) //
        .sum();
    return Math.sqrt(somma / size());
  }

  public int size() {
    if (null == righe)
      return 0;
    return righe.size();
  }

  @Override
  public void close() throws IOException {
    //
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(columns.getIntestazione()).append("\n");
    if (null != righe) {
      for (DtsRow row : righe)
        sb.append(row.toString()).append("\n");
    } else
      sb.append("** no rows **");
    return sb.toString();
  }

  public List<Map<String, Object>> toList() {
    List<Map<String, Object>> lim = new ArrayList<Map<String, Object>>();
    for (DtsRow row : righe) {
      Map<String, Object> mp = row.toMap();
      lim.add(mp);
    }
    return lim;
  }

}

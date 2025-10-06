package prova.stdcla.dts;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

public class ProvaOpenCSV {
  private static final String CSZ_CSV = "datiProva/Estrattoconto_paypal-202401.CSV";

  public ProvaOpenCSV() {
    //
  }

  @Test
  public void provalo() throws FileNotFoundException, IOException, CsvException {
    CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build(); // custom separator
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(CSZ_CSV)).withCSVParser(csvParser) // custom CSV parser
        .withSkipLines(1) // skip the first line, header info
        .build()) {
      List<String[]> r = reader.readAll();
      r.forEach(x -> System.out.println(Arrays.toString(x)));
    }

  }

}

package prova.date;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import sm.clagenna.stdcla.utils.ParseData;

public class ProvaConvDatetime2Date {

  @Test
  public void doTheJob() {
    String szDtConHms = "2024-06-01T12:34:56.789";
    LocalDateTime d1 = ParseData.parseData(szDtConHms);
    System.out.printf("a) src = %s\t d1 = %s\n", szDtConHms, ParseData.formatDate(d1));
    // --------------------------------------------------------------------------------
    LocalDate dtSolo = d1.toLocalDate();
    LocalDateTime d2 = dtSolo.atStartOfDay();
    System.out.printf("b) dsolo = %s\n", ParseData.formatDate(d2));
  }

}

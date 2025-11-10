package prove.stdc_pdf.ansan;

import java.time.LocalDate;

import lombok.Data;
import sm.clagenna.stdcla.utils.Utils;

@Data
public class AnSanRow {
  private LocalDate dtExam;
  private String    esame;
  private double    esito;
  private boolean   alarme;
  private String    unMis;
  private double    refMin;
  private double    refMax;

  public AnSanRow() {
    //
  }

  public AnSanRow(String esame, //
      double value, //
      boolean alarm, //
      String unMis, //
      double refMin, //
      double refMax) {
    setEsame(esame);
    setEsito(esito);
    setAlarme(alarm);
    setUnMis(unMis);
    setRefMin(refMin);
    setRefMax(refMax);
  }

  @Override
  public final String toString() {
    String sz2 = esame;
    final int len = 20;
    String szFmt = String.format("%%-%20ds %%6s %%s %%-10s %%6s %%6s", len);
    if (null != sz2 && sz2.length() > len)
      sz2 = sz2.substring(0, len - 3) + "...";
    String sz = String.format(szFmt, //
        sz2, //
        Utils.formatDouble(esito), //
        alarme ? "*" : " ", //
        unMis, //
        Utils.formatDouble(refMin), //
        Utils.formatDouble(refMax));
    return sz;
  }
}

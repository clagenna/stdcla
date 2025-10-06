package prova.utils;

import sm.clagenna.stdcla.utils.Utils;

public class ProvaFmt {
  
  public static void main(String[] args) {
    var app = new ProvaFmt();
    app.doIt();
  }

  private void doIt() {
    int ii = 12345;
    String sz = Utils.s_fmtInt.format(ii);
    System.out.printf("ProvaFmt.doIt(%d=%s)\n", ii, sz);
  }
  

}

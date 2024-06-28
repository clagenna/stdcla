package prova.stdcla.geo;

import org.junit.Test;

import sm.clagenna.stdcla.utils.Utils;

public class ProvaNumerics {

  String[] test = { "10" //
      , "-20" //
      , "1.230" //
      , "+231.230" //
      , "-7.654.230" //
      , "-20,745" //
      , " 20 " //
  };

  @Test
  public void provalo() {
    for (String sz : test) {
      System.out.printf("--%s------------\n", sz);
      Object obj = Utils.parseDouble(sz);
      System.out.printf("parseDouble(%s) = %s [%s]\n", sz, obj, null != obj ? obj.getClass().getSimpleName() : "*NULL*");
      obj = Utils.parseLong(sz);
      System.out.printf("parseLong(%s) = %s [%s]\n", sz, obj, null != obj ? obj.getClass().getSimpleName() : "*NULL*");
      obj = Utils.parseInt(sz);
      System.out.printf("parseInt(%s) = %s [%s]\n", sz, obj, null != obj ? obj.getClass().getSimpleName() : "*NULL*");
    }

  }
}

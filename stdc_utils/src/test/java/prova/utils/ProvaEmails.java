package prova.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import sm.clagenna.stdcla.utils.Utils;

public class ProvaEmails {
  private static final String arrEmails[] = { //
      "errore@boh", //
      "errore.boh.mah", //
      "clagenna@gmail.com", //
      "clagenna_!#$%&'*+/=?`{|}~^-@gmail.com", //
      "cla:genna@gmail.com", //
      "nome.cognome@dominio.com", //
      "utente@mail.subdominio.dominio.it", //
      "nome-utente-123@dominio-web.net", //
      "1234567890@dominio.org", //
      "nome_utente@dominio.com", //
      "primo.secondo.terzo@dominio.com", //
      "utente+mailinglist@dominio.com", //
      "utente@dominio.travel", //
      "utente@[192.168.1.1]", //
      "\"molto insolito\"@dominio.com", //
      "UTENTE@Dominio.com" //
  };

  @Test
  public void doIt() {
    // Pattern pat_e1 = Pattern.compile("[a-z][a-z0-9 \\.\\-]+@[a-z][0-9a-z\\-]+\\.[a-z]+");
    String szCla = "" //
        + "([a-z0-9\\-" //
        + "]+)" //
        + "(?:\\.[a-z0-9\\-])*" //
        + "@([a-z][0-9a-z\\-]+?:\\.)*\\.[a-z]{2,6}";
    szCla = "([a-z0-9\\-]+)(\\.[a-z0-9\\-]+)*@([a-z][0-9a-z\\-]+)*\\.[a-z]{2,6}.*";
    Pattern pat_mio = Pattern.compile(szCla);
    final String szGem = "" //
        + "^" // 
        + "[a-zA-Z0-9" // lettera e num
        + "_!#$%&'*+/=?`{|}~^-" // chars
        + "]+" // da 1-...
        + "(?:\\." //
        + "[a-zA-Z0-9" //
        + "_!#$%&'*+/=?`{|}~^-" //
        + "]+)*" //
        + "@" //
        + "(?:[a-zA-Z0-9-]+\\.)+" //
        + "[a-zA-Z]{2,6}" //
        + "$";
    Pattern pat_gemini = Pattern.compile(szGem);
    System.out.printf("%-40s %-6s %-6s %-6s\n", "email", "Mia", "gemini", "util");
    for (String email : arrEmails) {
      String low = email.toLowerCase();
      Matcher mtch = pat_mio.matcher(low);
      String e1 = mtch.find() ? "Ok" : "ERROR";
      mtch = pat_gemini.matcher(low);
      String gem = mtch.find() ? "Ok" : "ERROR";
      String uti = Utils.isEmail(low) ? "Ok" : "ERROR";
      System.out.printf("%-40s %-6s %-6s %-6s\n", email, e1, gem, uti);
    }
  }

}

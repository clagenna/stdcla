package prova.utils;

import sm.clagenna.stdcla.utils.SecPwd;

public class ProvaSecPwd {

  public ProvaSecPwd() {
    // 
  }

  public static void main(String[] args) {
    ProvaSecPwd prova = new ProvaSecPwd();
    prova.doTheJob();
  }

  private void doTheJob() {
    String password = "sicuelserver";
    SecPwd secPwd = new SecPwd();
    String encryptedPassword = secPwd.encrypt(password);
    String decryptedPassword = secPwd.decrypt(encryptedPassword);
    System.out.println("Original Password: " + password);
    System.out.println("Encrypted Password: " + encryptedPassword);
    System.out.println("Decrypted Password: " + decryptedPassword);

  }

}

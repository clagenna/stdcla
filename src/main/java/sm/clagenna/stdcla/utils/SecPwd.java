package sm.clagenna.stdcla.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecPwd {
  private static String        baseKey   = "Non!Divulgare";
  private static SecretKeySpec secretKey;
  private static byte[]        key;
  private static final String  ALGORITHM = "AES";

  public SecPwd() {
    //
  }

  public void prepareSecreteKey(String myKey) {
    MessageDigest sha = null;
    try {
      key = myKey.getBytes(StandardCharsets.UTF_8);
      sha = MessageDigest.getInstance("SHA-1");
      key = sha.digest(key);
      key = Arrays.copyOf(key, 16);
      secretKey = new SecretKeySpec(key, ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public String encrypt(String strToEncrypt) {
    try {
      prepareSecreteKey(baseKey);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
    } catch (Exception e) {
      System.out.println("Error while encrypting: " + e.toString());
    }
    return null;
  }

  public String decrypt(String strToDecrypt) {
    try {
      prepareSecreteKey(baseKey);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    } catch (Exception e) {
      System.out.println("Error while decrypting: " + e.toString());
    }
    return null;
  }

  public static void main(String[] args) {

    String originalString = "Claudio!Genna";

    SecPwd aesEncryptionDecryption = new SecPwd();
    String encryptedString = aesEncryptionDecryption.encrypt(originalString);
    String decryptedString = aesEncryptionDecryption.decrypt(encryptedString);

    System.out.println(originalString);
    System.out.println(encryptedString);
    System.out.println(decryptedString);
  }
}

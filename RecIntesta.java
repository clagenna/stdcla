package pippo;

import lombok.Getter;
import lombok.Setter;
import sm.clagenna.stdcla.utils.Utils;
public class RecIntesta {
`n@Getter @Setter
private int codiss;
 @Getter @Setter
private String nome;
 @Getter @Setter
private String startPath;
`n@Override
public String toString() {
  StringBuilder sb = new StringBuilder();`nsb.append(String.format("\t%-16s %d\n", "codiss", codiss));
 sb.append(String.format("\t%-16s %s\n", "nome", nome));
 sb.append(String.format("\t%-16s %s\n", "startPath", startPath));
`n     return sb.toString();
  }
}

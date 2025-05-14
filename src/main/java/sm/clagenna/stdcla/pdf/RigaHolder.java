package sm.clagenna.stdcla.pdf;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Il responsabile del numero di riga durante lo scansionamento delle sequenze
 *
 * @author claudio
 *
 */
public class RigaHolder {
  private List<ValoreBySeq> m_liSeq;
  @Getter
  private int               riga;

  public RigaHolder() {
    riga = 0;
  }

  public void addRiga() {
    riga++;
  }

  public void addSeq(ValoreBySeq p_sq) {
    if (m_liSeq == null)
      m_liSeq = new ArrayList<>();
    m_liSeq.add(p_sq);
  }

  @Override
  public String toString() {
    String sz = "RigaH=*none*";
    if (m_liSeq == null)
      return sz;
    sz = "RigaH[";
    String vir = "";
    for (ValoreBySeq seq : m_liSeq) {
      sz += vir + seq.getNumSeq();
      vir = ",";
    }
    sz += "] riga=" + riga;
    return sz;
  }

  public List<ValoreBySeq> getListSeq() {
    return m_liSeq;
  }
}

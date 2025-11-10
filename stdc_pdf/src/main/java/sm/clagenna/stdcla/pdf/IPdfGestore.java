package sm.clagenna.stdcla.pdf;

/**
 * Interfaccia per la classe che gestisce il trascorrere delle pagine del PDF,
 * memorizzando il No della pagina corrente e quale (massima) posY e' stata
 * raggiunta su quella pagina
 */
public interface IPdfGestore {
  int getNPage();

  void setNPage(int vPage);

  double getMaxTop();

  void setMaxTop(double vVal);
}

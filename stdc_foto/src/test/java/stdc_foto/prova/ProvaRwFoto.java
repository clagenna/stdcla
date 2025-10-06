package stdc_foto.prova;

import java.io.IOException;

import org.apache.commons.imaging.ImagingException;
import org.junit.Test;

import sm.clagenna.stdcla.fotoscan.FotoReader;

public class ProvaRwFoto {
  private static final String fotoFile = "D:\\temp\\foto\\2025-07-23 Slovenia\\prove\\20250723_112702.jpg";
  private static final String fotoOut1 = "D:\\temp\\foto\\2025-07-23 Slovenia\\prove\\20250723_112702_1.jpg";
  private static final String fotoOut2 = "D:\\temp\\foto\\2025-07-23 Slovenia\\prove\\20250723_112702_2.jpg";

  public ProvaRwFoto() {
    // 
  }

  @Test
  public void doit() throws ImagingException, IOException {
    // scrittura foto a 1024 pixels
    FotoReader rwFo1 = new FotoReader(fotoFile);
    rwFo1.setDesiredSize(1024);
    rwFo1.readFotoFile();
    rwFo1.writeFotoFile(fotoOut1, "jpg" );

    rwFo1 = new FotoReader(fotoFile);
    rwFo1.setDesiredSize(1024);
    rwFo1.setGrayed(true);
    rwFo1.readFotoFile();
    rwFo1.writeFotoFile(fotoOut2, "jpg" );
  }

}

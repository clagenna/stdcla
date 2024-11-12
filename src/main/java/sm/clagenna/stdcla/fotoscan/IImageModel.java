package sm.clagenna.stdcla.fotoscan;

import sm.clagenna.stdcla.enums.EExifPriority;

public interface IImageModel {
  int add(FSFile p_fi);

  EExifPriority getPriority();

  void setPriority(EExifPriority prio);
}

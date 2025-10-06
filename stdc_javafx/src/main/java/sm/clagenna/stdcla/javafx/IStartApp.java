package sm.clagenna.stdcla.javafx;

import sm.clagenna.stdcla.utils.AppProperties;

public interface IStartApp {
  void initApp(AppProperties p_props);

  void changeSkin();

  void closeApp(AppProperties p_props);
}

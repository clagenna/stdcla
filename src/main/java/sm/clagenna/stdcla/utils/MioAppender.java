package sm.clagenna.stdcla.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "MioAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class MioAppender extends AbstractAppender {

  private static MioAppender s_inst;

  private static List<ILog4jReader> s_logRead;
  private final ReadWriteLock       rwLock   = new ReentrantReadWriteLock();
  private final Lock                readLock = rwLock.readLock();
  private String                    m_lastMsg;

  public MioAppender(String p_name, Filter p_filter, Layout<? extends Serializable> p_layout, boolean p_ignoreExceptions,
      Property[] p_properties) {
    super(p_name, p_filter, p_layout, p_ignoreExceptions, p_properties);
    s_inst = this;
  }

  public static MioAppender getInst() {
    return s_inst;
  }

  public static void setLogReader(ILog4jReader p_lr) {
    if (s_logRead == null)
      s_logRead = new ArrayList<>();
    s_logRead.add(p_lr);
  }

  public static void removeLogReader(ILog4jReader p_lr) {
    if (s_logRead == null)
      return;
    s_logRead.remove(p_lr);
    if (s_logRead.size() == 0)
      s_logRead = null;
  }

  @PluginFactory
  public static MioAppender createAppender(//
      @PluginAttribute("name")
      String name, //
      @PluginElement("Layout")
      Layout<? extends Serializable> layout, //
      @PluginElement("Filter")
      final Filter filter, //
      @PluginAttribute("otherAttribute")
      String otherAttribute) {
    if (name == null) {
      LOGGER.error("Non hai fornito nessun nome al appender MioAppender");
      return null;
    }
    if (layout == null) {
      layout = PatternLayout.createDefaultLayout();
    }
    return new MioAppender(name, filter, layout, true, null);
  }

  @Override
  public void append(LogEvent p_event) {
    readLock.lock();
    try {
      // Level lev = p_event.getLevel();
      // String sz = p_event.getMessage().toString();
      final byte[] by = getLayout().toByteArray(p_event);
      String sz2 = new String(by, "UTF-8");
      String[] arr = sz2.split("\\t");
      // System.out.printf("MioLog:%s:\t%s\n", lev.name(), sz2);
      m_lastMsg = "";
      if (arr != null && arr.length > 3) {
        for (int i = 3; i < arr.length; i++) {
          m_lastMsg = m_lastMsg.length() > 0 ? m_lastMsg + " " : "";
          m_lastMsg += arr[i];
        }
      }
      broadcast(arr);
    } catch (Exception l_e) {
      l_e.printStackTrace();
    } finally {
      readLock.unlock();
    }
  }

  private void broadcast(String[] p_arr) {
    if (s_logRead == null)
      return;
    for (ILog4jReader lr : s_logRead)
      lr.addLog(p_arr);
  }

  public String lastMsg() {
    return m_lastMsg;
  }

}

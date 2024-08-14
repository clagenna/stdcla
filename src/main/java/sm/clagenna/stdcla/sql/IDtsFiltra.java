package sm.clagenna.stdcla.sql;

@FunctionalInterface
public interface IDtsFiltra<T> {
  
   boolean filtra(T o);

}

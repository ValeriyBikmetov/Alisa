package valeriy.bikmetov.alisa.model;

public interface IObservable {
    void notifyObservers(Object obj);
    void register (IObserver obs);
    void unRegister(IObserver obs);
}

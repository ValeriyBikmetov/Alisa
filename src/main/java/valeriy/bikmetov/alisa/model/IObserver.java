package valeriy.bikmetov.alisa.model;

public interface IObserver {
    void refreshData(IObservable subject, Object object);
}

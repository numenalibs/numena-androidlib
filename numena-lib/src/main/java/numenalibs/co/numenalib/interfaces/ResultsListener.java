package numenalibs.co.numenalib.interfaces;


public interface ResultsListener<T> {

    public void onCompletion(T result);

    public void onFailure(Throwable throwable);
}
package numenalibs.co.numenalib.interfaces;


public interface ResultsListener<T> {

    public void onSuccess(T result);
    public void onFailure(Throwable e, String response);

}
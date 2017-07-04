package numenalibs.co.numenalib.models;

import java.util.Arrays;
import java.util.concurrent.Callable;


import java.util.concurrent.Callable;

import numenalibs.co.numenalib.interfaces.ResultsListener;

public abstract class NumenaMethod<T> implements Callable<Void> {

    ResultsListener<byte[]> listener;

    T result;

    public void setResult (T result) {
        this.result = result;
    }

    public T getResult(){
        return result;
    }

    public abstract Void call ();

    public ResultsListener<byte[]> getListener() {
        return listener;
    }

    public void setListener(ResultsListener<byte[]> listener) {
        this.listener = listener;
    }
}

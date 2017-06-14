package numenalibs.co.numenalib.models;

import java.util.Arrays;
import java.util.concurrent.Callable;


import java.util.concurrent.Callable;

public abstract class NumenaMethod<T> implements Callable<Void> {
    T result;

    public void setResult (T result) {
        this.result = result;
    }

    public T getResult(){
        return result;
    }

    public abstract Void call ();
}

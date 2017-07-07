package numenalibs.co.numenalib.models;

import android.os.AsyncTask;
import android.os.Looper;


public class WorkerThread extends Thread {

    private NumenaMethod numenaMethod;

    public void setNumenaMethod(NumenaMethod numenaMethod) {
        this.numenaMethod = numenaMethod;
    }
    public void run() {
        if(Looper.myLooper() == null){
            Looper.prepare();
        }
        numenaMethod.call();
        Looper.loop();
    }
}

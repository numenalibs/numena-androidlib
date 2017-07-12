package numenalibs.co.numenalib.models;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import numenalibs.co.numenalib.NumenaLibDebug;

import static numenalibs.co.numenalib.NumenaMessageHelper.isLocked;


public class WorkerThread extends Thread {

    private NumenaMethod numenaMethod;

    public void setNumenaMethod(NumenaMethod numenaMethod) {
        this.numenaMethod = numenaMethod;
    }

    public void run() {
        numenaMethod.call();
    }
}

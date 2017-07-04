package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Numena numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);
//        numena.getMessageHandler().register("librarytest3123", "Numena".getBytes(), "oaskd".getBytes(), new ResultsListener<byte[]>() {
//            @Override
//            public void onSuccess(byte[] result) {
//                Log.d("REGISTER", "SUCCESS");
//
//            }
//
//            @Override
//            public void onFailure(Throwable e, String response) {
//                Log.d("REGISTER", "FAILURE");
//                e.printStackTrace();
//            }
//        });

        numena.getMessageHandler().getUsers("t", new ResultsListener<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                Log.d("GETUSERS", "SUCCESS");
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.d("GETUSERS", "FAILURE");
            }
        });


    }
}

package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaResponse;

public class NextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        Numena numena = Numena.getInstance();
        numena.getMessageHandler().getUsers("t", new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                Log.d("STATUS", result.getStatus());
            }
        });
    }
}

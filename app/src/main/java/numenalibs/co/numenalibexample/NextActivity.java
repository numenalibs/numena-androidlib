package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaResponse;

public class NextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        final Numena numena = Numena.getInstance();
        Button button = (Button) findViewById(R.id.getUsersButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().getUsers("t", new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        Log.d("STATUS", result.getStatus());
                    }
                });
            }
        });
//        numena.getMessageHandler().getUsers("t", new ResultsListener<NumenaResponse>() {
//            @Override
//            public void onCompletion(NumenaResponse result) {
//                Log.d("STATUS", result.getStatus());
//            }
//        });
    }
}

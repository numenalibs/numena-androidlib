package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import de.tavendo.autobahn.WebSocketConnection;
import numenalibs.co.numenalib.Numena;

public class NextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        final Numena numena = Numena.getInstance();
    }
}

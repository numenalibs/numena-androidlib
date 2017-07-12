package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import de.tavendo.autobahn.WebSocketConnection;
import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.networking.NumenaChatSocket;

public class NextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        final Numena numena = Numena.getInstance();
        WebSocketConnection web = numena.getChatSocket();
        NumenaChatSocket numenaChatSocket = numena.getChatSocketHandler(web);
    }
}

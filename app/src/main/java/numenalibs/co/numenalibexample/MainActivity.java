package numenalibs.co.numenalibexample;

import android.content.Intent;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaChatHandler;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

public class MainActivity extends AppCompatActivity {

    private Numena numena;
    private static byte[] TESTORG = "LIBTESTman2".getBytes();
    private static byte[] TESTAPPDATA = "LIBTESTman2".getBytes();
    private static byte[] TESTAPPID = "LIBTEST".getBytes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);

        Button regButton = (Button) findViewById(R.id.registerButton);
        Button unregButton = (Button) findViewById(R.id.unregisterButton);
        Button getUsersButton = (Button) findViewById(R.id.getUsersButton);
        Button nextActButton = (Button) findViewById(R.id.nextActivityButton);
        Button storeToSelf = (Button) findViewById(R.id.storeSelfbutton);

        final EditText username = (EditText) findViewById(R.id.userNameEditText);
        final EditText query = (EditText) findViewById(R.id.getUsersEditText);
        final EditText msgEditText = (EditText) findViewById(R.id.messageEditText);

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().register(null, null, username.getText().toString(), TESTORG, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        Log.d("REGISTER UI1", result.getStatus());
                    }
                });
            }
        });

        unregButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    numena.getMessageHandler().unregister(null, null, username.getText().toString(), TESTORG, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
                        @Override
                        public void onCompletion(NumenaResponse result) {
                            Log.d("UNREGISTER UI1", result.getStatus());
                        }
                    });
            }
        });

        getUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });

        storeToSelf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<NumenaUser> numenaUsers = new ArrayList<>();
                byte[] content = msgEditText.getText().toString().getBytes();
                byte[] organisationId = TESTORG;
                byte[] appId = TESTAPPID;
                boolean writePermission = true;
                boolean readPermission = true;
                ValuesManager valuesManager = ValuesManager.getInstance();
                byte[] publicKey = valuesManager.getClientIdentityPublicKey();
                NumenaUser numenaUser = new NumenaUser(username.getText().toString(),TESTAPPDATA, publicKey,TESTORG);
                numenaUsers.add(numenaUser);

                numena.getMessageHandler().storeObject(numenaUsers, content, organisationId, appId, writePermission, readPermission, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        Log.d("STORE", result.getStatus());
                    }
                });
            }
        });

        nextActButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumenaChatHandler k = new NumenaChatHandler() {
                    @Override
                    public void onMessage(byte[] msg) {
                        Log.d("GOT A NEW MESSAGE", new String(msg));
                    }
                };

                numena.getMessageHandler().subscribe(null, null, TESTORG, "Helium".getBytes(),k, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        Log.d("SUBSCRIBE", result.getStatus());
                    }
                });
            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        numena.getMessageHandler().closeSocket();
    }
}

package numenalibs.co.numenalibexample;

import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;

public class MainActivity extends AppCompatActivity {

    private Numena numena;
    private static byte[] TESTORG = "LIBTESTman2".getBytes();
    private static byte[] TESTAPPDATA = "LIBTESTman2".getBytes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);

        Button regButton = (Button) findViewById(R.id.registerButton);
        Button unregButton = (Button) findViewById(R.id.unregisterButton);
        Button getUsersButton = (Button) findViewById(R.id.getUsersButton);

        final EditText username = (EditText) findViewById(R.id.userNameEditText);
        final EditText query = (EditText) findViewById(R.id.getUsersEditText);

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
                numena.getMessageHandler().getUsers(query.getText().toString(), new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        String status = result.getStatus();
                        Log.d("GETUSERS", status);
                        List<NumenaObject> numenaObjectList = result.getNumenaObjects();
                        for (NumenaObject numenaObject : numenaObjectList) {
                            NumenaUser numenaUser = (NumenaUser) numenaObject;
                            Log.d("NumenaUSER", numenaUser.getUsername() + " Organisation " + new String(numenaUser.getOrganisationId()) + " Publickey " + Utils.printByteArray(numenaUser.getPublicKey()));
                        }

                        if(!numenaObjectList.isEmpty()){
                            final NumenaUser numenaUser = (NumenaUser) numenaObjectList.get(0);
                            final NumenaUser self = new NumenaUser(username.getText().toString(),TESTAPPDATA, TESTORG);
                            numena.getMessageHandler().addContact(self, numenaUser, new ResultsListener<NumenaResponse>() {
                                @Override
                                public void onCompletion(NumenaResponse result) {
                                    Log.d("ADDCONTACT", result.getStatus());
                                    numena.getMessageHandler().removeContact(self, numenaUser, new ResultsListener<NumenaResponse>() {
                                        @Override
                                        public void onCompletion(NumenaResponse result) {
                                            Log.d("REMOVECONTACT", result.getStatus());
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}

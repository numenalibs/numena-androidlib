package numenalibs.co.numenalibexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.interfaces.NumenaChatHandler;
import numenalibs.co.numenalib.models.NumenaKey;
import numenalibs.co.numenalib.models.NumenaKeyPair;
import numenalibs.co.numenalib.tools.NumenaCryptoBox;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;

public class MainActivity extends AppCompatActivity {

    private Button regButton, unregButton;
    private EditText userName;
    private String userNameText;
    private Activity activity;
    public static byte[] TESTORGANISATION = "HAHAH".getBytes();
    public static byte[] TESTAPPID = "LOL".getBytes();
    private Numena numena;
    NumenaCryptoBox cryptoBox;
    private JSONObject jsonAppData;
    private NumenaKeyPair keyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);
        setupValues();
        initLayout();

        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userNameText = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().register(null, null, userNameText, TESTORGANISATION, jsonAppData.toString().getBytes(), new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "REGISTER " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, UserSelectActivity.class);
                        intent.putExtra("PBKEY", keyPair.getPublicKey().getKeyValue());
                        intent.putExtra("SKKEY", keyPair.getSecretKey().getKeyValue());
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Toast.makeText(activity, "REGISTER " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        unregButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().unregister(null, null, userNameText, TESTORGANISATION, jsonAppData.toString().getBytes(), new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "UNREGISTER " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Toast.makeText(activity, "UNREGISTER " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void setupValues(){
        cryptoBox = numena.getMessageHandler().getNumenaCryptoBox();
        keyPair = cryptoBox.generateAppKey("PBKEY","SKKEY");
        NumenaKey publicKey = keyPair.getPublicKey();
        List<NumenaKey> keys = new ArrayList<>();
        keys.add(keyPair.getSecretKey());
        cryptoBox.refreshSecretKeyList(keys);
        jsonAppData = new JSONObject();
        try {
            jsonAppData.put("appPublicKey", new String(publicKey.getKeyValue(), "ISO-8859-1"));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void initLayout() {
        regButton = (Button) findViewById(R.id.registerButton);
        unregButton = (Button) findViewById(R.id.unregisterButton);
        userName = (EditText) findViewById(R.id.userName);
    }

    @Override
    protected void onStop() {
        numena.getMessageHandler().closeSocket(this);
        super.onStop();
    }
}
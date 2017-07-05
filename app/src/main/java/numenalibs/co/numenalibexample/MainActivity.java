package numenalibs.co.numenalibexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;

public class MainActivity extends AppCompatActivity {

    private  Numena numena;
    private boolean firstDone = false;
    private static String TESTNAME = "LIBTESTman1";
    private static byte[] TESTORG = "LIBTESTman1".getBytes();
    private static byte[] TESTAPPDATA = "LIBTESTman2".getBytes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);
        register();
        //getUsers("t");
    }

    private void getUsers(String query){
        numena.getMessageHandler().getUsers(query, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                String status = result.getStatus();
                Log.d("GETUSERS", status);
                List<NumenaObject> numenaObjectList = result.getNumenaObjects();
                for(NumenaObject numenaObject : numenaObjectList){
                    NumenaUser numenaUser = (NumenaUser) numenaObject;
                    Log.d("NumenaUSER", numenaUser.getUsername() + " Organisation " + new String(numenaUser.getOrganisationId()) + " Publickey " + Utils.printByteArray(numenaUser.getPublicKey()));
                }
                if(!firstDone){
                    firstDone = true;
                    getUsers("r");
                }
            }

        });
    }

    private void register(){
        numena.getMessageHandler().register(null, null, TESTNAME, TESTORG, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                Log.d("REGISTER", "SUCCESS");
                numena.getMessageHandler().unregister(null, null, TESTNAME, TESTORG, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        Log.d("UNREGISTER", result.getStatus());
                    }
                });
            }
        });
    }


}

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

public class MainActivity extends AppCompatActivity {

    private  Numena numena;
    private boolean firstDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);
        getUsers("t");

    }

    private void getUsers(String query){
        numena.getMessageHandler().getUsers(query, new ResultsListener<NumenaResponse>() {
            @Override
            public void onSuccess(NumenaResponse result) {

                String status = result.getStatus();
                Log.d("GETUSERS", status);
                List<NumenaObject> numenaObjectList = result.getNumenaObjects();
                for(NumenaObject numenaObject : numenaObjectList){
                    NumenaUser numenaUser = (NumenaUser) numenaObject;
                    Log.d("NumenaUSER", numenaUser.getUsername());
                }
                if(!firstDone){
                    firstDone = true;
                    getUsers("r");
                }

            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.d("GETUSERS", "FAILURE");
            }
        });
    }

    private void register(){
        numena.getMessageHandler().register("librarytest3123123", "Numena".getBytes(), "oaskd".getBytes(), new ResultsListener<NumenaResponse>() {
            @Override
            public void onSuccess(NumenaResponse result) {
                Log.d("REGISTER", "SUCCESS");

            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.d("REGISTER", "FAILURE");
                e.printStackTrace();
            }
        });
    }
}

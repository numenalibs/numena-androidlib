package numenalibs.co.numenalib;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Constants;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    public static byte[] TESTORGANISATION = "ExAmPleUnitTest".getBytes();
    public static byte[] TESTAPPID = "ExAmPleUnitTest".getBytes();
    public static byte[] TESTAPPDATA = "ExAmPleUnitTest".getBytes();
    public static String TESTNAME = "ExAmPleUnitTestss";

    private Numena numena = null;


    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        Looper.prepare();
        numena = Numena.getInstance();
        numena.setupNumenaLibrary(appContext);

        testGetUsers();
        testRegisterAndUnRegister();

    }

    private void testRegisterAndUnRegister() {
        numena.getMessageHandler().register(null, null, TESTNAME, TESTORGANISATION, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                if (result.getStatus().equals(Constants.RESPONSE_SUCCESS)) {
                    testUnRegister();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                assertEquals(true, false);

            }
        });
    }


    private void testUnRegister() {
        numena.getMessageHandler().unregister(null, null, TESTNAME, TESTORGANISATION, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                boolean value = false;
                if (result.getStatus().equals(Constants.RESPONSE_SUCCESS)) {
                    value = true;
                }
                assertEquals(true, value);
            }

            @Override
            public void onFailure(Throwable throwable) {
                assertEquals(true, false);
            }
        });

    }


    private void testGetUsers() {
        numena.getMessageHandler().getUsers("", TESTORGANISATION, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                boolean value = false;
                if (result.getStatus().equals(Constants.RESPONSE_SUCCESS)) {
                    value = true;
                }
                assertEquals(true, value);
                List<NumenaUser> numenaUserList = new ArrayList<NumenaUser>();
                for(NumenaObject object : result.getNumenaObjects()){
                    numenaUserList.add((NumenaUser) object);
                }
                sendMessageToUser(numenaUserList);

            }

            @Override
            public void onFailure(Throwable throwable) {
                assertEquals(true, false);
            }
        });
    }

    private void sendMessageToUser(List<NumenaUser> numenaUser){
        numena.getMessageHandler().storeObject(numenaUser, "test".getBytes(), TESTORGANISATION, TESTAPPID, true, true, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {
                boolean value = false;
                if (result.getStatus().equals(Constants.RESPONSE_SUCCESS)) {
                    value = true;
                }
                assertEquals(true, value);
            }

            @Override
            public void onFailure(Throwable throwable) {
                assertEquals(true, false);
            }
        });

    }
}

package numenalibs.co.numenalib;


import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;


import messages.Basemessage;
import messages.Clienthello;
import messages.Serverhello;
import messages.Statusmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.ValuesManager;


public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;
    private NumenaMessageHelper numenaMessageHelper;

    public NumenaMessageHandler() {
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();
        numenaMessageHelper = new NumenaMessageHelper(encryptionManager, protocolManager, singleMessageManager);
    }

    public void startNumenaCommunication(Context context) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        valuesManager.initDatabase(context);
        encryptionManager.setupKeys();
       // numenaMessageHelper.initConnection(listener);
    }

    public void getUsers(final String query,final ResultsListener<NumenaResponse> listener) {
        if (numenaMessageHelper.isConnectionEstablished()) {
            executeGetUsersCall(query, listener);
        } else {
            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                @Override
                public void onSuccess(NumenaResponse result) {
                    executeGetUsersCall(query, listener);
                }

                @Override
                public void onFailure(Throwable e, String response) {
                    listener.onFailure(new NumenaLibraryException("Failure: Could not register"), e.getMessage());
                }
            });
        }
    }

    public void register(final String title,final byte[] organisationId,final byte[] appData,final ResultsListener<NumenaResponse> listener) {
        if (numenaMessageHelper.isConnectionEstablished()) {
            executeRegisterCall(title, organisationId, appData, listener);
        } else {
            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                @Override
                public void onSuccess(NumenaResponse result) {
                    executeRegisterCall(title, organisationId, appData, listener);
                }

                @Override
                public void onFailure(Throwable e, String response) {
                    listener.onFailure(new NumenaLibraryException("Failure: Could not register"), e.getMessage());
                }
            });
        }

    }

    private void executeGetUsersCall(String query, ResultsListener<NumenaResponse> listener){
        numenaMessageHelper.buildAndSendGetUsers(query, listener);

    }

    private void executeRegisterCall(String title, byte[] organisationId, byte[] appData, ResultsListener<NumenaResponse> listener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] publicKey = valuesManager.getClientIdentityPublicKey();
        numenaMessageHelper.buildAndSendRegister(title, publicKey, organisationId, appData, listener);
    }


}

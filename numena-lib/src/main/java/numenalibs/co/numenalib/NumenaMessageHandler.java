package numenalibs.co.numenalib;


import android.content.Context;
import android.support.annotation.Nullable;
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
    }

    public void getUsers(final String query, final ResultsListener<NumenaResponse> listener) {
        if (numenaMessageHelper.isConnectionEstablished()) {
            executeGetUsersCall(query, listener);
        } else {
            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                @Override
                public void onCompletion(NumenaResponse result) {
                    executeGetUsersCall(query, listener);
                }
            });
        }
    }

    /**
     * Method used for registering a user to the connected server.
     * If Public- and secretkey is nulled the first forged identitykeys are used
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */

    public void register(@Nullable final byte[] publicKey,@Nullable final byte[] secretKey, final String title, final byte[] organisationId,final byte[] appData,final ResultsListener<NumenaResponse> listener) {
        if (numenaMessageHelper.isConnectionEstablished()) {
            executeRegisterCall(publicKey, secretKey, title, organisationId, appData, listener);
        } else {
            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                @Override
                public void onCompletion(NumenaResponse result) {
                    executeRegisterCall(publicKey, secretKey, title, organisationId, appData, listener);
                }
            });
        }
    }

    /**
     * Method used for unregistering a user from the connected server.
     * If Public- and secretkey is nulled the first forged identitykeys are used
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */

    public void unregister(@Nullable final byte[] publicKey,@Nullable final byte[] secretKey, final String title,final byte[] organisationId,final byte[] appData, final ResultsListener<NumenaResponse> listener) {
        if (numenaMessageHelper.isConnectionEstablished()) {
            executeUnregisterCall(publicKey, secretKey, title, organisationId, appData, listener);
        } else {
            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                @Override
                public void onCompletion(NumenaResponse result) {
                    executeUnregisterCall(publicKey, secretKey, title, organisationId, appData, listener);
                }
            });
        }
    }

    private void executeGetUsersCall(final String query, ResultsListener<NumenaResponse> listener){
        numenaMessageHelper.buildAndSendGetUsers(query, listener);

    }

    private void executeRegisterCall(@Nullable byte[] publicKey, @Nullable byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> listener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] usedPubKey = publicKey;
        byte[] usedSecretKey = secretKey;
        if(publicKey == null){
            usedPubKey = valuesManager.getClientIdentityPublicKey();
        }
        if(secretKey == null){
            usedSecretKey = valuesManager.getClientIdentitySecretKey();
        }
        numenaMessageHelper.buildAndSendRegister(usedPubKey,usedSecretKey, title, organisationId, appData, listener);
    }

    private void executeUnregisterCall(@Nullable byte[] publicKey, @Nullable byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> listener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] usedPubKey = publicKey;
        byte[] usedSecretKey = secretKey;
        if(publicKey == null){
            usedPubKey = valuesManager.getClientIdentityPublicKey();
        }
        if(secretKey == null){
            usedSecretKey = valuesManager.getClientIdentitySecretKey();
        }
        numenaMessageHelper.buildAndSendUnRegister(usedPubKey,usedSecretKey, title, organisationId, appData, listener);
    }


}

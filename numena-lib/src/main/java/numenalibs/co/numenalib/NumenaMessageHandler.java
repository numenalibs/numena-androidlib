package numenalibs.co.numenalib;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.models.WorkerThread;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.BroadCaster;
import numenalibs.co.numenalib.tools.CallbackManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

import static numenalibs.co.numenalib.NumenaMessageHelper.isLocked;
import static numenalibs.co.numenalib.tools.Constants.BROADCASTCODE;
import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_ADD;
import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_REMOVE;


public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;
    private NumenaMessageHelper numenaMessageHelper;
    private CallbackManager callbackManager;
    private Queue<NumenaMethod> forExecute = new LinkedList<NumenaMethod>();

    /**
     * Handler used for executing a thread once a NumenaMethod is polled from the queue.
     */

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle b = msg.getData();
            int response = b.getInt(BROADCASTCODE);
            if (response == Constants.EXECUTEWORKERTHREAD) {
                if (!isLocked) {
                    NumenaMethod method = forExecute.poll();
                    if (method != null) {
                        isLocked = true;
                        final WorkerThread workerThread = new WorkerThread();
                        workerThread.setNumenaMethod(method);
                        if(!numenaMessageHelper.isConnectionEstablished()){
                            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                                @Override
                                public void onCompletion(NumenaResponse result) {
                                    workerThread.start();
                                }
                            });
                        }else {
                            workerThread.start();
                        }
                    }
                }
            }
        }
    };

    public NumenaMessageHandler() {
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();
        BroadCaster.getBroadCaster().registerObserver(mHandler);
        numenaMessageHelper = new NumenaMessageHelper(encryptionManager, protocolManager, singleMessageManager);
        callbackManager = new CallbackManager(numenaMessageHelper);
    }

    public void initNumenaValues(Context context) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        valuesManager.initDatabase(context);
        encryptionManager.setupKeys();
    }

    public void closeSocket(){
        numenaMessageHelper.closeConnection();

    }

    /**
     * Method for adding a getObjectCallback to the queue
     *  If Publickey is nulled the first forged identitykey is used
     * @param publicKey
     * @param appId
     * @param messageHash
     * @param limit
     * @param clientlistener
     */

    public void getObject(@Nullable  byte[] publicKey, byte[] appId, byte[] messageHash, int limit, ResultsListener<NumenaResponse> clientlistener){
        CallbackManager.GetObjectCallback getObjectCallback = callbackManager.makeGetObjectCallback(publicKey,appId,messageHash,limit, clientlistener);
        forExecute.add(getObjectCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }


    /**
     * Method for adding a storeObjectCallback to the queue
     * @param numenaUsers
     * @param content
     * @param organisationId
     * @param appId
     * @param writePermission
     * @param readPermission
     * @param listener
     */

    public void storeObject(List<NumenaUser> numenaUsers, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener<NumenaResponse> listener) {
        CallbackManager.StoreObjectCallback storeObjectCallback = callbackManager.makeStoreObjectCallback(numenaUsers, content, organisationId, appId, writePermission, readPermission, listener);
        forExecute.add(storeObjectCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method for adding an ContactCallback with type CONTACTTYPE_REMOVE to the queue
     *
     * @param self
     * @param numenaUser
     * @param listener
     */

    public void removeContact(NumenaUser self, NumenaUser numenaUser, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.ContactCallback contactCallback = callbackManager.makeContactsCallback(self, numenaUser, CONTACTTYPE_REMOVE, listener);
        forExecute.add(contactCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method for adding an ContactCallback with type CONTACTTYPE_ADD to the queue
     *
     * @param self
     * @param numenaUser
     * @param listener
     */

    public void addContact(NumenaUser self, NumenaUser numenaUser, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.ContactCallback contactCallback = callbackManager.makeContactsCallback(self, numenaUser, CONTACTTYPE_ADD, listener);
        forExecute.add(contactCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method for adding a getUsers callback to the queue
     *
     * @param query
     * @param listener
     */

    public void getUsers(final String query, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.GetUsersCallback getUsersCallback = callbackManager.makeGetUsersCallback(query, listener);
        forExecute.add(getUsersCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method for adding a register callback to the queue
     * If Public- and secretkey is nulled the first forged identitykeys are used
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */

    public void register(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.RegisterCallback registerCallback = callbackManager.makeRegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
        forExecute.add(registerCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method for adding a unregister callback to the queue
     * If Public- and secretkey is nulled the first forged identitykeys are used
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */

    public void unregister(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.UnRegisterCallback unRegisterCallback = callbackManager.makeUnRegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
        forExecute.add(unRegisterCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }
}

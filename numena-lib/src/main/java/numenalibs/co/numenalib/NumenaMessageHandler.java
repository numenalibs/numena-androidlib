package numenalibs.co.numenalib;


import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.interfaces.NumenaChatHandler;
import numenalibs.co.numenalib.models.NumenaKey;
import numenalibs.co.numenalib.tools.ConnectionChangeReceiver;
import numenalibs.co.numenalib.tools.NumenaCryptoBox;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.models.WorkerThread;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.BroadCaster;
import numenalibs.co.numenalib.tools.CallbackManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.NumenaProviderClient;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

import static numenalibs.co.numenalib.NumenaMessageHelper.isLocked;
import static numenalibs.co.numenalib.tools.Constants.BROADCASTCODE;
import static numenalibs.co.numenalib.tools.Constants.CLIENT_IDENTITY_PUBLICKEY;
import static numenalibs.co.numenalib.tools.Constants.CLIENT_IDENTITY_SECRETKEY;
import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_ADD;
import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_REMOVE;
import static numenalibs.co.numenalib.tools.Constants.NO_CONNECTION_AVAILABLE;


public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;
    private NumenaMessageHelper numenaMessageHelper;
    private CallbackManager callbackManager;
    private NumenaCryptoBox numenaCryptoBox;
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
                        if (!numenaMessageHelper.isConnectionEstablished()) {
                            numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                                @Override
                                public void onCompletion(NumenaResponse result) {
                                    workerThread.start();
                                }

                                @Override
                                public void onFailure(Throwable throwable) {

                                }
                            });
                        } else {
                            workerThread.start();
                        }
                    }
                }
            } else if (response == Constants.RESETCONNECTIONVALUES) {
                numenaMessageHelper.resetValues();
            }
        }
    };

    public NumenaMessageHandler() {
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();
        numenaMessageHelper = new NumenaMessageHelper(encryptionManager, protocolManager, singleMessageManager);
        callbackManager = new CallbackManager(numenaMessageHelper);
        numenaCryptoBox = new NumenaCryptoBox(encryptionManager, protocolManager);
        BroadCaster.getBroadCaster().registerObserver(mHandler);
    }

    public NumenaCryptoBox getNumenaCryptoBox() {
        return numenaCryptoBox;
    }

    /**
     * Uses the context to initialise a SQLite database
     * Also creates a pair of identitykeys if needed.
     *
     * @param context
     */

    public void initNumenaValues(Context context) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        valuesManager.initDatabase(context);
        keyRead(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectionChangeReceiver connectionChangeReceiver = new ConnectionChangeReceiver();
            context.registerReceiver(connectionChangeReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    /**
     * Tries to find a provider with ID keys, if not then it creates new and sets them in
     * ValuesManager and in the database.
     *
     * @param context
     */

    private void keyRead(Context context) {
        NumenaProviderClient numenaProviderClient = new NumenaProviderClient(context);
        List<NumenaKey> numenaKeys = numenaProviderClient.lookupKeysFromProviders();
        if (numenaKeys.isEmpty()) {
            Log.d("No keys found", "Creating new");
            encryptionManager.setupKeys();
        } else {
            Log.d("keys found", "Setting them");
            ValuesManager vm = ValuesManager.getInstance();
            for (NumenaKey key : numenaKeys) {
                byte[] keyValue = key.getKeyValue();
                if (key.getKeyName().equals(CLIENT_IDENTITY_PUBLICKEY)) {
                    vm.setClientIdentityPublicKey(keyValue);
                }
                if (key.getKeyName().equals(CLIENT_IDENTITY_SECRETKEY)) {
                    vm.setClientIdentitySecretKey(keyValue);
                }
            }
        }
    }

    public void closeSocket() {
        numenaMessageHelper.closeConnection();
    }


    /**
     * Method for adding a subscribe callback to the queue
     * If Public- and secretkey is nulled the first forged identitykeys are used
     *
     * @param identityPublicKey
     * @param identitySecretKey
     * @param organisationId
     * @param appId
     * @param listener
     */

    public void subscribe(@Nullable byte[] identityPublicKey, @Nullable byte[] identitySecretKey, byte[] organisationId, byte[] appId, NumenaChatHandler chatHandler, ResultsListener<NumenaResponse> listener) {
        CallbackManager.SubscribeCallback subscribeCallback = callbackManager.makeSubscribeCallback(identityPublicKey, identitySecretKey, organisationId, appId, chatHandler, listener);
        addForQueue(subscribeCallback, listener);
    }

    /**
     * Method for adding a getObjectCallback to the queue
     * If Publickey is nulled the first forged identitykey is used
     *
     * @param publicKey
     * @param appId
     * @param messageHash
     * @param limit
     * @param listener
     */

    public void getObject(@Nullable byte[] publicKey, byte[] appId, byte[] messageHash, int limit, ResultsListener<NumenaResponse> listener) {
        CallbackManager.GetObjectCallback getObjectCallback = callbackManager.makeGetObjectCallback(publicKey, appId, messageHash, limit, listener);
        addForQueue(getObjectCallback, listener);
    }


    /**
     * Method for adding a storeObjectCallback to the queue
     *
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
        addForQueue(storeObjectCallback, listener);
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
        addForQueue(contactCallback, listener);
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
        addForQueue(contactCallback, listener);
    }

    /**
     * Method for adding a getUsers callback to the queue
     *
     * @param query
     * @param listener
     */

    public void getUsers(final String query, byte[] organisationId, final ResultsListener<NumenaResponse> listener) {
        CallbackManager.GetUsersCallback getUsersCallback = callbackManager.makeGetUsersCallback(query, organisationId, listener);
        addForQueue(getUsersCallback, listener);
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
        addForQueue(registerCallback, listener);
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
        addForQueue(unRegisterCallback, listener);
    }


    /**
     * Method for add a numenamethod for to the queue of methods to be executes.
     * Does not add a method if there is no ethernet connection present.
     *
     * @param numenaMethod
     * @param listener
     */

    private void addForQueue(NumenaMethod numenaMethod, ResultsListener listener) {
        if (ConnectionChangeReceiver.hasEthernetConnection) {
            forExecute.add(numenaMethod);
            BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
        } else {
            listener.onFailure(new NumenaLibraryException("Failing: " + NO_CONNECTION_AVAILABLE));
        }
    }
}

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
                        WorkerThread workerThread = new WorkerThread();
                        workerThread.setNumenaMethod(method);
                        workerThread.start();
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
    }

    public void startNumenaCommunication(Context context) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        valuesManager.initDatabase(context);
        encryptionManager.setupKeys();
    }

    public void storeObject(List<NumenaUser> numenaUsers, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener<NumenaResponse> listener) {
        StoreObjectCallback storeObjectCallback = new StoreObjectCallback(numenaUsers, content, organisationId, appId, writePermission, readPermission, listener);
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
        ContactCallback contactCallback = new ContactCallback(self, numenaUser, CONTACTTYPE_REMOVE, listener);
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
        ContactCallback contactCallback = new ContactCallback(self, numenaUser, CONTACTTYPE_ADD, listener);
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
        GetUsersCallback getUsersCallback = new GetUsersCallback(query, listener);
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
        RegisterCallback registerCallback = new RegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
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
        UnRegisterCallback unregisterCallback = new UnRegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
        forExecute.add(unregisterCallback);
        BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
    }

    /**
     * Method that executes the call for register.
     * Using NumenaMessageHelper to build and send a basemessage with type LEDGER
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */

    private void executeRegisterCall(@Nullable byte[] publicKey, @Nullable byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> listener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] usedPubKey = publicKey;
        byte[] usedSecretKey = secretKey;
        if (publicKey == null) {
            usedPubKey = valuesManager.getClientIdentityPublicKey();
        }
        if (secretKey == null) {
            usedSecretKey = valuesManager.getClientIdentitySecretKey();
        }
        numenaMessageHelper.buildAndSendRegister(usedPubKey, usedSecretKey, title, organisationId, appData, listener);
    }

    /**
     * Method that executes the call for unregister.
     * Using NumenaMessageHelper to build and send a basemessage with type LEDGER
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param listener
     */
    private void executeUnregisterCall(@Nullable byte[] publicKey, @Nullable byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> listener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] usedPubKey = publicKey;
        byte[] usedSecretKey = secretKey;
        if (publicKey == null) {
            usedPubKey = valuesManager.getClientIdentityPublicKey();
        }
        if (secretKey == null) {
            usedSecretKey = valuesManager.getClientIdentitySecretKey();
        }
        numenaMessageHelper.buildAndSendUnRegister(usedPubKey, usedSecretKey, title, organisationId, appData, listener);
    }

    /**
     * Method that executes the call for getUsers
     * Using NumenaMessageHelper to build and send a basemessage with type LEDGER
     *
     * @param self
     * @param numenaUser
     * @param listener
     */

    private void executeContactCall(NumenaUser self, NumenaUser numenaUser, int type, ResultsListener<NumenaResponse> listener) {
        ValuesManager vm = ValuesManager.getInstance();
        self.setPublicKey(vm.getClientIdentityPublicKey());
        self.setSecretKey(vm.getClientIdentitySecretKey());
        if (type == CONTACTTYPE_ADD) {
            numenaMessageHelper.buildAndSendAddContact(self, numenaUser, listener);
        } else if (type == CONTACTTYPE_REMOVE) {
            numenaMessageHelper.buildAndSendRemoveContact(self, numenaUser, listener);
        }
    }

    /**
     * Method that executes the call for getUsers
     * Using NumenaMessageHelper to build and send a basemessage with type LEDGER
     *
     * @param query
     * @param listener
     */

    private void executeGetUsersCall(final String query, ResultsListener<NumenaResponse> listener) {
        numenaMessageHelper.buildAndSendGetUsers(query, listener);
    }

    private void executeStoreObjectCall(List<NumenaUser> numenaUsers, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener listener) {
        numenaMessageHelper.buildAndStoreObject(numenaUsers, content, organisationId, appId, writePermission, readPermission, listener);
    }


    /*************************************************************
     * CALLBACKS
     *************************************************************/

    private class RegisterCallback extends NumenaMethod {

        private byte[] publicKey, secretKey, organisationId, appData;
        private String title;
        private ResultsListener listener;

        public RegisterCallback(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
            this.publicKey = publicKey;
            this.secretKey = secretKey;
            this.title = title;
            this.organisationId = organisationId;
            this.appData = appData;
            this.listener = listener;
        }

        @Override
        public Void call() {
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
            return null;
        }
    }

    private class UnRegisterCallback extends NumenaMethod {

        private byte[] publicKey, secretKey, organisationId, appData;
        private String title;
        private ResultsListener listener;

        public UnRegisterCallback(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
            this.publicKey = publicKey;
            this.secretKey = secretKey;
            this.title = title;
            this.organisationId = organisationId;
            this.appData = appData;
            this.listener = listener;
        }

        @Override
        public Void call() {
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
            return null;
        }
    }

    private class GetUsersCallback extends NumenaMethod {

        private String query;
        private ResultsListener listener;

        public GetUsersCallback(String query, ResultsListener listener) {
            this.query = query;
            this.listener = listener;
        }

        @Override
        public Void call() {
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
            return null;
        }
    }

    private class StoreObjectCallback extends NumenaMethod {

        private List<NumenaUser> numenaUserList = new ArrayList<>();
        private ResultsListener listener;
        private byte[] organisationId, appId, content;
        private boolean writePermission, readPermission;

        public StoreObjectCallback(List<NumenaUser> numenaUserList, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, final ResultsListener listener) {
            this.numenaUserList.addAll(numenaUserList);
            this.listener = listener;
            this.organisationId = organisationId;
            this.appId = appId;
            this.content = content;
            this.writePermission = writePermission;
            this.readPermission = readPermission;
        }

        @Override
        public Void call() {
            if (numenaMessageHelper.isConnectionEstablished()) {
                executeStoreObjectCall(numenaUserList, content, organisationId, appId, writePermission, readPermission, listener);
            } else {
                numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        executeStoreObjectCall(numenaUserList, content, organisationId, appId, writePermission, readPermission, listener);
                    }
                });
            }
            return null;
        }
    }

    private class ContactCallback extends NumenaMethod {

        private NumenaUser numenaUser, self;
        private ResultsListener listener;
        private int type;

        public ContactCallback(NumenaUser self, NumenaUser numenaUser, int type, ResultsListener listener) {
            this.numenaUser = numenaUser;
            this.self = self;
            this.type = type;
            this.listener = listener;
        }

        @Override
        public Void call() {
            if (numenaMessageHelper.isConnectionEstablished()) {
                executeContactCall(self, numenaUser, type, listener);
            } else {
                numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        executeContactCall(self, numenaUser, type, listener);
                    }
                });
            }
            return null;
        }
    }

}

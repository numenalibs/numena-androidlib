package numenalibs.co.numenalib;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;


import java.util.LinkedList;
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
            int response = b.getInt("KEY");
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

    /**
     * Method for adding an addContactCallback to the queue
     * @param self
     * @param numenaUser
     * @param listener
     */

    public void addContact(NumenaUser self, NumenaUser numenaUser,final ResultsListener<NumenaResponse> listener){
        AddContactCallback addContactCallback = new AddContactCallback(self, numenaUser, listener);
        forExecute.add(addContactCallback);
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
     * @param self
     * @param numenaUser
     * @param listener
     */

    private void executeAddContactCall(NumenaUser self, NumenaUser numenaUser, ResultsListener<NumenaResponse> listener) {
        ValuesManager vm = ValuesManager.getInstance();
        self.setPublicKey(vm.getClientIdentityPublicKey());
        self.setSecretKey(vm.getClientIdentitySecretKey());
        numenaMessageHelper.buildAndSendAddContact(self,numenaUser,listener);
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

    private class AddContactCallback extends NumenaMethod {

        private NumenaUser numenaUser, self;
        private ResultsListener listener;

        public AddContactCallback(NumenaUser self, NumenaUser numenaUser, ResultsListener listener) {
            this.numenaUser = numenaUser;
            this.self = self;
            this.listener = listener;
        }

        @Override
        public Void call() {
            if (numenaMessageHelper.isConnectionEstablished()) {
                executeAddContactCall(self,numenaUser, listener);
            } else {
                numenaMessageHelper.initConnection(new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse result) {
                        executeAddContactCall(self,numenaUser, listener);
                    }
                });
            }
            return null;
        }
    }

}

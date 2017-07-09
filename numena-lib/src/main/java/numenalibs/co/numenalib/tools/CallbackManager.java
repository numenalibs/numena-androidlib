package numenalibs.co.numenalib.tools;


import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.NumenaMessageHelper;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;

import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_ADD;
import static numenalibs.co.numenalib.tools.Constants.CONTACTTYPE_REMOVE;

public class CallbackManager {

    private NumenaMessageHelper numenaMessageHelper;

    public CallbackManager(NumenaMessageHelper numenaMessageHelper) {
        this.numenaMessageHelper = numenaMessageHelper;
    }

    /*******************************************************************************
     * METHODS FOR GENERATING CALLBACKS
     * *****************************************************************************
     */

    public RegisterCallback makeRegisterCallback(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
        return new RegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
    }

    public UnRegisterCallback makeUnRegisterCallback(@Nullable final byte[] publicKey, @Nullable final byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, final ResultsListener<NumenaResponse> listener) {
        return new UnRegisterCallback(publicKey, secretKey, title, organisationId, appData, listener);
    }

    public GetUsersCallback makeGetUsersCallback(String query, ResultsListener<NumenaResponse> listener){
        return new GetUsersCallback(query, listener);
    }

    public ContactCallback makeContactsCallback(NumenaUser self, NumenaUser contact, int type, ResultsListener<NumenaResponse> listener){
        return new ContactCallback(self, contact, type, listener);
    }

    public StoreObjectCallback makeStoreObjectCallback(List<NumenaUser> numenaUsers, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener<NumenaResponse> listener) {
        return new StoreObjectCallback(numenaUsers, content, organisationId, appId, writePermission, readPermission, listener);
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

    /**
     * Method that executes the call for storeObject
     * Using NumenaMessageHelper to build and send a basemessage with type DATABASE
     *
     * @param numenaUsers
     * @param content
     * @param organisationId
     * @param appId
     * @param writePermission
     * @param readPermission
     * @param listener
     */

    private void executeStoreObjectCall(List<NumenaUser> numenaUsers, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener listener) {
        numenaMessageHelper.buildAndStoreObject(numenaUsers, content, organisationId, appId, writePermission, readPermission, listener);
    }

    /****************************************************************************************
     * CALLBACKS
     * **************************************************************************************
     */

    public class RegisterCallback extends NumenaMethod {

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

    public class UnRegisterCallback extends NumenaMethod {

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

    public class GetUsersCallback extends NumenaMethod {

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

    public class StoreObjectCallback extends NumenaMethod {

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

    public class ContactCallback extends NumenaMethod {

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

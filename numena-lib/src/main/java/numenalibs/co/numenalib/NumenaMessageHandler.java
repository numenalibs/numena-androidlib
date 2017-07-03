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
        numenaMessageHelper.initConnection();
    }

    public void getUsers(String query){
        numenaMessageHelper.buildAndSendGetUsers(query);
    }



}

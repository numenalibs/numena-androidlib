package numenalibs.co.numenalibexample;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.NumenaChatHandler;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaKey;
import numenalibs.co.numenalib.models.NumenaKeyPair;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.NumenaCryptoBox;

import static numenalibs.co.numenalibexample.MainActivity.TESTAPPID;
import static numenalibs.co.numenalibexample.MainActivity.TESTORGANISATION;

public class ChatActivity extends AppCompatActivity {

    private Numena numena;
    private byte[] skey;
    private byte[] pbkey;
    private byte[] userKey;
    private NumenaCryptoBox cryptoBox;
    private EditText messageField;
    private Button send;
    private ListView chatView;
    private List<String> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private String messageText;
    private byte[] selectedPublicKey;
    private NumenaUser numenaUser;

    NumenaChatHandler handler = new NumenaChatHandler() {
        @Override
        public void onMessage(byte[] bytes) {
            byte[] decrypted = null;
            try {
                decrypted = cryptoBox.decryptAppMessage(bytes,null);
            } catch (NumenaLibraryException e) {
                e.printStackTrace();
            }
            messages.add(new String(decrypted));
            adapter.refreshData(messages);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        numena = Numena.getInstance();
        Bundle bundle = getIntent().getExtras();
        pbkey = bundle.getByteArray("PBKEY");
        skey = bundle.getByteArray("SKKEY");
        userKey  = bundle.getByteArray("USERAPPKEY");
        selectedPublicKey = bundle.getByteArray("USERKEY");
        byte[] userData = bundle.getByteArray("USERAPPDATA");
        byte[] userOrg = bundle.getByteArray("USERORG");
        String userName = bundle.getString("USERNAME");
        numenaUser = new NumenaUser(userName,userData,selectedPublicKey, userOrg);
        setupValues();
        initLayout();
        initControls();
    }

    private void initControls(){

        numena.getMessageHandler().subscribe(null, null, TESTORGANISATION, TESTAPPID, handler, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse numenaResponse) {
                Toast.makeText(ChatActivity.this, "SUBSCRIBE " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(ChatActivity.this, "SUBSCRIBE " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initLayout(){
        messageField = (EditText) findViewById(R.id.conversation_mymessage_edittext);
        send = (Button) findViewById(R.id.sendButton);
        chatView = (ListView) findViewById(R.id.conversation_recyclerView);
        adapter = new MessageAdapter(this);
        chatView.setAdapter(adapter);

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                messageText = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<NumenaUser> numenaUsers = new ArrayList<NumenaUser>();
                numenaUsers.add(numenaUser);
                byte[] appMessage = cryptoBox.createEncryptedAppMessage(pbkey,userKey,skey, messageText.getBytes());
                numena.getMessageHandler().storeObject(numenaUsers, appMessage, TESTORGANISATION, TESTAPPID, true, true, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(ChatActivity.this, "STOREOBJECT " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Toast.makeText(ChatActivity.this, "STOREOBJECT " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        numena.getMessageHandler().closeSocket(this);
    }

    private void setupValues(){
        cryptoBox = numena.getMessageHandler().getNumenaCryptoBox();
        List<NumenaKey> keys = new ArrayList<>();
        NumenaKey sk = new NumenaKey(skey);
        keys.add(sk);
        cryptoBox.refreshSecretKeyList(keys);
    }


    public class MessageAdapter extends BaseAdapter {

        private List<String> messageList = new ArrayList<>();
        private Context context;
        private LayoutInflater layoutInflater;

        public MessageAdapter(Context context) {
            this.context = context;
            layoutInflater = LayoutInflater.from(context);

        }

        public void refreshData(List<String> messages) {
            messageList.clear();
            messageList.addAll(messages);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return messageList.size();
        }

        @Override
        public String getItem(int position) {
            return messageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final String message = (String) getItem(position);
            convertView = layoutInflater.inflate(R.layout.listitem_numenauser, null);
            TextView txt = (TextView) convertView.findViewById(R.id.numenaUserName);
            txt.setText(message);
            return convertView;
        }
    }
}

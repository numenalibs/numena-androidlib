package numenalibs.co.numenalibexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaChatHandler;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

public class MainActivity extends AppCompatActivity {

    private Button regButton, unregButton, subscribeButton, sendMessageButton, getUsersButton;
    private TextView textMessage;
    private EditText userName, message, queryText;
    private ListView listView;
    private String userNameText, query, messageText;
    private Activity activity;
    private UserAdapter adapter;
    private NumenaUser selectedUser;
    private byte[] TESTORGANISATION = "HAHAH".getBytes();
    private byte[] TESTAPPID = "LOL".getBytes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        final Numena numena = Numena.getInstance();
        numena.setupNumenaLibrary(this);
        initLayout();
        adapter = new UserAdapter(this);
        listView.setAdapter(adapter);

        message.addTextChangedListener(new TextWatcher() {
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


        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userNameText = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        queryText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().register(null, null, userNameText, TESTORGANISATION, userNameText.getBytes(), new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "REGISTER " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        unregButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().unregister(null, null, userNameText, TESTORGANISATION, userNameText.getBytes(), new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "UNREGISTER " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        getUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().getUsers(query, TESTORGANISATION, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "QUERY " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                        adapter.refreshData(numenaResponse.getNumenaObjects());
                    }
                });
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<NumenaUser> numenaUsers = new ArrayList<NumenaUser>();
                numenaUsers.add(selectedUser);
                numena.getMessageHandler().storeObject(numenaUsers, messageText.getBytes(), TESTORGANISATION, TESTAPPID, true, true, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "STOREOBJECT " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

        final NumenaChatHandler handler = new NumenaChatHandler() {
            @Override
            public void onMessage(byte[] bytes) {
                textMessage.setText(new String(bytes));
            }
        };

        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().subscribe(null, null, TESTORGANISATION, TESTAPPID, handler, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "SUBSCRIBE " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void initLayout() {
        regButton = (Button) findViewById(R.id.registerButton);
        unregButton = (Button) findViewById(R.id.unregisterButton);
        subscribeButton = (Button) findViewById(R.id.subscribeButton);
        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        getUsersButton = (Button) findViewById(R.id.getUsersButton);
        textMessage = (TextView) findViewById(R.id.textMessage);
        userName = (EditText) findViewById(R.id.userName);
        message = (EditText) findViewById(R.id.storeMessage);
        queryText = (EditText) findViewById(R.id.queryText);
        listView = (ListView) findViewById(R.id.numenaUserListView);
    }

    public class UserAdapter extends BaseAdapter {

        private List<NumenaObject> numenaUserList = new ArrayList<>();
        private Context context;
        private LayoutInflater layoutInflater;

        public UserAdapter(Context context) {
            this.context = context;
            layoutInflater = LayoutInflater.from(context);

        }

        public void refreshData(List<NumenaObject> numenaUsers) {
            numenaUserList.clear();
            numenaUserList.addAll(numenaUsers);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return numenaUserList.size();
        }

        @Override
        public NumenaObject getItem(int position) {
            return numenaUserList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final NumenaUser numenaUser = (NumenaUser) getItem(position);
            convertView = layoutInflater.inflate(R.layout.listitem_numenauser, null);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedUser = numenaUser;
                    Toast.makeText(activity, "SELECTED USER IS " + selectedUser.getUsername(), Toast.LENGTH_SHORT).show();
                }
            });
            TextView txt = (TextView) convertView.findViewById(R.id.numenaUserName);
            txt.setText(numenaUser.getUsername());
            return convertView;
        }
    }
}
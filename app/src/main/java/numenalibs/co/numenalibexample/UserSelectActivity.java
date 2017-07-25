package numenalibs.co.numenalibexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.Numena;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Utils;

import static numenalibs.co.numenalibexample.MainActivity.TESTORGANISATION;

public class UserSelectActivity extends AppCompatActivity {

    private Numena numena;
    private NumenaUser selectedUser;
    private Activity activity;
    private EditText queryText;
    private ListView listView;
    private Button getUsersButton;
    private UserAdapter adapter;
    private String queryString;
    private byte[] pbkey, skey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        numena = Numena.getInstance();
        activity = this;
        queryText = (EditText) findViewById(R.id.queryText);
        listView = (ListView) findViewById(R.id.numenaUserListView);
        getUsersButton = (Button) findViewById(R.id.getUsersButton);
        adapter = new UserAdapter(this);
        listView.setAdapter(adapter);

        queryText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryString = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numena.getMessageHandler().getUsers(queryString, TESTORGANISATION, new ResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {
                        Toast.makeText(activity, "QUERY " + numenaResponse.getStatus(), Toast.LENGTH_SHORT).show();
                        adapter.refreshData(numenaResponse.getNumenaObjects());
                    }
                });
            }
        });

        Bundle bundle = getIntent().getExtras();
        pbkey = bundle.getByteArray("PBKEY");
        skey = bundle.getByteArray("SKKEY");
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
                    byte[] publicKey;
                    try {
                        JSONObject jsonObject = new JSONObject(new String(selectedUser.getAppData()));
                        publicKey = jsonObject.getString("appPublicKey").getBytes("ISO-8859-1");
                        Intent intent = new Intent(UserSelectActivity.this, ChatActivity.class);
                        intent.putExtra("PBKEY", pbkey);
                        intent.putExtra("SKKEY", skey);
                        intent.putExtra("USERAPPKEY", publicKey);
                        intent.putExtra("USERKEY", numenaUser.getPublicKey());
                        intent.putExtra("USERAPPDATA", numenaUser.getAppData());
                        intent.putExtra("USERORG", numenaUser.getOrganisationId());
                        intent.putExtra("USERNAME", numenaUser.getUsername());
                        startActivity(intent);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            TextView txt = (TextView) convertView.findViewById(R.id.numenaUserName);
            txt.setText(numenaUser.getUsername());
            return convertView;
        }
    }

}

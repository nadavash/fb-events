package edu.washington.crew.fbevents;

import android.app.Fragment;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import android.content.pm.*;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.*;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends ActionBarActivity implements EventFragment.OnFragmentInteractionListener {

    public static final String TAG = "MainActivity";

    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;
    private AccessToken accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Test code to get all events from repo */
        // If repo has not been instantiated
        FbEventRepository repo = new FbEventRepository();
//        try {
//            repo.generateEventsFromJson(this.getResources().openRawResource(R.raw.data));
//        } catch (IOException e) {
//            Toast.makeText(MainActivity.this, "Error: IO Exception", Toast.LENGTH_SHORT).show();
//        }
//        Log.i(TAG, repo.getAllEvents().toString());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new EventFragment())
                    .commit();
        }

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldToken, AccessToken newToken) {
                updateWithToken(newToken);
            }
        };

        updateWithToken(AccessToken.getCurrentAccessToken());
    }
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_logout) {
            LoginManager.getInstance().logOut();
        } else if (id == R.id.action_refresh) {
            fetchEventData();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onFragmentInteraction(String string) {
        //you can leave it empty
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void updateWithToken(AccessToken token) {
        Log.i(TAG, "Updating with token " + token);

        accessToken = token;
        if (token == null || token.isExpired()) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } else {
            fetchEventData();
        }
    }

    private void fetchEventData() {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                if (jsonObject == null) {
                    Log.e(TAG, graphResponse.getError().getErrorMessage());
                    return;
                }

                FbEventRepository repo = new FbEventRepository();
                try {
                    repo.generateFromJsonArray(jsonObject.getJSONObject("events").getJSONArray("data"));
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, new EventFragment())
                            .commit();
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "events");
        request.setParameters(params);
        request.executeAsync();
    }
}

package com.example.bluetooth_le_demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class DeviceControlActivity extends AppCompatActivity {

    private TextView deviceNameTxt;
    private TextView deviceAddressTxt;
    private TextView connectionStatusTxt;

    private BluetoothDevice device;
    private BluetoothLeService bluetoothLeService;

    // MQTT Client
    private MqttConnectOptions mqttConnectOptions;
    private MqttAndroidClient mqttAndroidClient;
    private String mqttHost = "tcp://143.198.239.26:1883";
    private String clientID = "BLEDemo";
    final private String username = "zzxb";
    final private String password = "zzxb+_)(";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if(bluetoothLeService != null){
                // call functions on service to check connection and connect to devices
                if(!bluetoothLeService.initialize()){
                    Log.e(BluetoothLeService.TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                // perform device connection
                bluetoothLeService.connect(device.getAddress());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLeService = null;
        }
    };

    private void updateConnectionState(String state){
        System.out.println("=> connection state:"+state);
        runOnUiThread(() -> { connectionStatusTxt.setText(state);});
    }

    private boolean connected;
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            System.out.println("=> Action:"+action);
            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
                connected = true;
                updateConnectionState(getResources().getString(R.string.connected));
            }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                connected = false;
                updateConnectionState(getResources().getString(R.string.disconnected));
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                // Show all supported services
                displayGattServices(bluetoothLeService.getSupportedGattServices());
            }else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
                System.out.println("=> Remote data:"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private void displayGattServices(List<BluetoothGattService> gattServices){
        if(gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown Characteristic";

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gatCharacteristicData = new ArrayList<>();

        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for(BluetoothGattService gattService: gattServices){
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

            for(BluetoothGattCharacteristic gattCharacteristic: gattCharacteristics){
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }

            mGattCharacteristics.add(charas);
            gatCharacteristicData.add(gattCharacteristicGroupData);
        }

        System.out.println("Supported services");
        for(HashMap<String, String> service:gattServiceData){
            service.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + ":" + entry.getValue());
            });
        }
        System.out.println("Supported characteristics");
        for(ArrayList<HashMap<String, String>> charas:gatCharacteristicData){
            for (HashMap<String, String> chara:charas){
                chara.entrySet().forEach(stringStringEntry -> {
                    System.out.println(stringStringEntry.getKey()+":"+stringStringEntry.getValue());
                });
            }
            System.out.println("--");
        }

        // Just reading the data to display on console
        for(ArrayList<BluetoothGattCharacteristic> bluetoothGattCharacteristics:mGattCharacteristics){
            for(BluetoothGattCharacteristic gattCharacteristic: bluetoothGattCharacteristics){
                if(BluetoothLeService.UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())){
                    System.out.println(gattCharacteristic.getProperties());
                    bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    bluetoothLeService.readCharacteristic(gattCharacteristic);
                }
//                bluetoothLeService.readCharacteristic(gattCharacteristic);
            }
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deviceNameTxt = findViewById(R.id.device_name);
        deviceAddressTxt = findViewById(R.id.device_address);
        connectionStatusTxt = findViewById(R.id.connection_status);

        bindData();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // MQTT Client
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        clientID += System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), mqttHost, clientID);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("MQTT", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i("MQTT", "topic: " + topic + ", msg: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i("MQTT", "msg delivered");
            }
        });

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("MQTT", "connect succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "connect failed");
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connect_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_connect:
                // just testing
                sendDataToServer(99);
                System.out.println("Connecting");
                connectionStatusTxt.setText(R.string.connecting);
                if(bluetoothLeService != null){
                    boolean result = bluetoothLeService.connect(device.getAddress());
                    Log.d(BluetoothLeService.TAG, "connect request result="+result);
                }
                break;
            default:
                onBackPressed();
        }
        return true;
    }

    //TODO: get data and bind to view
    public void bindData(){
        if(getIntent().hasExtra("device")){
            device = getIntent().getParcelableExtra("device");
            deviceNameTxt.setText(device.getName());
            deviceAddressTxt.setText(device.getAddress());
        }else {
            Toast.makeText(this, "No data available!", Toast.LENGTH_SHORT).show();
        }
    }

    //TODO: send data to server
    public void sendDataToServer(int heartRate){
//        sendViaAPI(heartRate);

        // publish data through mqtt protocol
        JSONObject payload = new JSONObject();
        try {
            payload.put("heart_rate", heartRate);
            payload.put("device", "android");
            publishMessage(payload.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //TODO: send data to server via api
    public void sendViaAPI(int heartRate){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://143.198.239.26:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        APIService apiService = retrofit.create(APIService.class);
        HashMap<String, Object> requestData = new HashMap<>();
        requestData.put("heart_rate", heartRate);
        Call<APIResponse> call = apiService.sendHeartRate(requestData);
        call.enqueue(new Callback<APIResponse>() {
            @Override
            public void onResponse(Call<APIResponse> call, Response<APIResponse> response) {
                System.out.println("Response message= " + response.body().getMessage());
            }

            @Override
            public void onFailure(Call<APIResponse> call, Throwable t) {
                System.out.println(t.getLocalizedMessage());
            }
        });
    }

    //TODO: publish data to mqtt broker
    public void publishMessage(String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect(mqttConnectOptions);
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes("utf-8"));
            message.setQos(0);
            mqttAndroidClient.publish("test", message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("MQTT", "publish succeed!") ;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("MQTT", "publish failed!") ;
                }
            });
        } catch (MqttException | UnsupportedEncodingException e) {
            Log.e("MQTT", e.toString());
            e.printStackTrace();
        }
    }
}
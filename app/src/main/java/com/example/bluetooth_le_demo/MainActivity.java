package com.example.bluetooth_le_demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import pl.bclogic.pulsator4droid.library.PulsatorLayout;

public class MainActivity extends AppCompatActivity{

    private RecyclerView myRecyclerView;
    private PulsatorLayout pulsator;
    private ImageView myImageView;
    private MyRecyclerViewAdapter myRecyclerViewAdapter;
    private TextView placeholderText;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler mHandler;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    private static final int REQUEST_LOCATION_CODE = 1;
    private static final int SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle("BLE Scan Device");

        // ask location permission
        askForPermission();

        myRecyclerView = findViewById(R.id.myRecylerView);
        myImageView = findViewById(R.id.myimage);
        pulsator = (PulsatorLayout) findViewById(R.id.pulsator);
        placeholderText = findViewById(R.id.placeholder_text);
        hideElements(true, false, true, false);

        bluetoothDevices = new ArrayList<BluetoothDevice>();
        myRecyclerViewAdapter = new MyRecyclerViewAdapter(this, bluetoothDevices);
        myRecyclerView.setAdapter(myRecyclerViewAdapter);
        myRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mHandler = new Handler(Looper.getMainLooper());
        // Use this to determine whether BLE is supported on the device.
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(bluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    // TODO: ask for user permission to allow location
    public void askForPermission(){
        System.out.println("Device SDK Version: "+ Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        }else {
            checkLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    // TODO: check for for location permission to access bluetooth devices
    public void checkLocationPermission(String permission){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            System.out.println("=> FG Permission not yet granted");
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, REQUEST_LOCATION_CODE);
                            }
                        });
                builder.create().show();
            }else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, REQUEST_LOCATION_CODE);
            }
        }else {
            System.out.println("=> FG Permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_LOCATION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                System.out.println("=> [ok] Permission granted");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan:
                System.out.println("Scanning...");
                bluetoothDevices.clear();
                hideElements(true, false, false, true);
                bluetoothAdapter.startDiscovery();
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                scanLeDevice(true);
                break;
        }
        return true;
    }

    // TODO: Scan for bluetooth devices
    private void scanLeDevice(boolean enable){
        if(enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Time out.");
                    pulsator.stop();
                    hideElements(false, true, true, true);
                    if(bluetoothDevices.size() == 0){
                        Toast.makeText(MainActivity.this, "No devices found, try again! Please make sure your location service is turned on.", Toast.LENGTH_LONG).show();
                    }
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            pulsator.start();
            bluetoothLeScanner.startScan(leScanCallback);
        }else {
            pulsator.stop();
            hideElements(false, true, true, true);
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // TODO: Call after bluetooth finished scanning for devices
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(!bluetoothDevices.contains(result.getDevice())){
                if(result.getDevice().getName() != null){
                    bluetoothDevices.add(result.getDevice());
                    System.out.println("=> My Device: "+result.getDevice().getName());
                    System.out.println("=> Device type: "+result.getDevice().getType());
                    System.out.println("=> Device bond state: "+result.getDevice().getBondState());
                }
            }
            myRecyclerViewAdapter.notifyDataSetChanged();
        }
    };

    // TODO: Hide elements such as recyclerview, imageview, pulsator
    public void hideElements(Boolean e1, Boolean e2, Boolean e3, Boolean e4){
        myRecyclerView.setVisibility(e1 ? View.GONE:View.VISIBLE);
        myImageView.setVisibility(e2 ? View.GONE:View.VISIBLE);
        pulsator.setVisibility(e3 ? View.GONE:View.VISIBLE);
        placeholderText.setVisibility(e4 ? View.GONE:View.VISIBLE);
    }
}
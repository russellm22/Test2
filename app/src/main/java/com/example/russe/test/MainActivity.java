package com.example.russe.test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

import static com.example.russe.test.MarkerList.locData;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static MainActivity mainActivity = new MainActivity();
    //aws database
    DynamoDBMapper dynamoDBMapper;


    //enabling bluetooth
    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    Button enableDisableDiscoverableBTN;

    BluetoothConnectionService mBluetoothConnection;
    Button btnStartConnection;
    Button btnSend;
    EditText etSend;

    Runnable runnable = new Runnable() {
        public void run() {
            //DynamoDB calls go here
        }
    };

    TextView incomingMessages;
    StringBuilder messages;

    public static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDevicesListAdapter;
    ListView lvNewDevices;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "onReceive: Discoverability enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability disabled. Not able to receive connection.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability disabled. Able to receive connection.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: connected");
                        break;
                }
            }
        }
    };
    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDevicesListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDevicesListAdapter);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }


    static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;

    @Override
    //app is initiated
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AWSMobileClient enables AWS user credentials to access your table
        AWSMobileClient.getInstance().initialize(this).execute();

        final AWSCredentialsProvider[] credentialsProvider = {AWSMobileClient.getInstance().getCredentialsProvider()};
        final AWSConfiguration[] configuration = {AWSMobileClient.getInstance().getConfiguration()};


        // Add code to instantiate a AmazonDynamoDBClient
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider[0]);

        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration[0])
                .build();

        String l = null;

        for (Marker m : locData) {
            l = l + m.getLatti() + ", " + m.getLongi() + " ";
        }

        ((EditText) findViewById(R.id.rText)).setText(l);

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        //enabling bluetooth
        //initialize bluetooth on/off button
        Button btnOnOff = (Button) findViewById(R.id.btnONOFF);
        enableDisableDiscoverableBTN = (Button) findViewById(R.id.btnDiscoverable_on_off);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);


        //broadcasts when bond state changes ie pairing
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        lvNewDevices.setOnItemClickListener(MainActivity.this);

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBTN();
            }
        });



        /*//broadcast when bond state changes
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);*/


        //button to add marker
        final Button secondActivity = (Button) findViewById(R.id.locationBTN);

        //location list


        //on click will add new marker to arraylist
        secondActivity.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                //string to print arraylist of markers
                String locations = "";
                queryMarkers();

                lvNewDevices.bringToFront();

                //sets textbox to list of marker coordinates
                for (Marker innerList : locData) {
                    locations = locations + innerList.getLatti() + ", " + innerList.getLongi() + " ";
                }

                ((EditText) findViewById(R.id.rText)).setText(locations);



                /*Log.d(TAG, "We got to this location");

                DynamoDBQueryExpression scanExpression = new DynamoDBQueryExpression<>();

                PaginatedQueryList result = dynamoDBMapper.query(MarkerLocationDO.class, scanExpression);
                Log.d(TAG, "But did we get to this location");*/


            }
        });

        enableDisableDiscoverableBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "enableDisableDIscoverableBTN: Making device discoverable for 300 seconds");
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);

                startActivity(discoverableIntent);

                IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(mBroadcastReceiver2, intentFilter);
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });

        /*btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);

                etSend.setText("");
            }
        });
*/

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);

            Log.d(TAG, "this works boyyyy");
            getLocation();

        }
    };

    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }

    //enable or disable bluetooth on phone
    public void enableDisableBTN() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBTN: Does not have BT capabilities.");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBTN: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        } else if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBTN: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }


    //sets textboxs to lat and long and returns a marker with the location
    public Marker getLocation() {

        double latti = 0.0;
        double longi = 0.0;
        //String gridLoc = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                latti = location.getLatitude();
                longi = location.getLongitude();
            }
        }
        //gridLoc = getGridLocation();

        Marker newMarker = new Marker(latti, longi);
        locData.add(newMarker);

        //send to aws
        final MarkerLocationTestDO nnMarker = new MarkerLocationTestDO();


        nnMarker.setMarkerID(generateMarkerID());

        nnMarker.setLattitude(latti);

        nnMarker.setLongitude(longi);

        nnMarker.setGridLocation(getGridLocation());


        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(nnMarker);

                // Item saved

            }
        }).start();

        //queryMarkers();


        return newMarker;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_LOCATION:
                getLocation();
                break;
        }
    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }


    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }

        startConnection();


    }

    public double getSpeed() {

        double speed = 0;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                speed = location.getSpeed();
            }
        }
        return speed;
    }

    public String getGridLocation()
    {
        String curLat = null;
        String curLong = null;
        String curGridLocation = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                curLat = String.valueOf((int)location.getLatitude());
                curLong = String.valueOf((int)location.getLongitude());
            }

            curGridLocation = curLat + curLong;
            return curGridLocation;

        }



        return "hi";
    }

    public void queryMarkers()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {


                MarkerLocationTestDO mark = new MarkerLocationTestDO();
                //get current grid location here for setGridLocation
                mark.setGridLocation(getGridLocation());

                DynamoDBQueryExpression<MarkerLocationTestDO> queryExpression = new DynamoDBQueryExpression<MarkerLocationTestDO>()
                        .withHashKeyValues(mark);



                /*DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues("G5");*/

                PaginatedQueryList<MarkerLocationTestDO> result = dynamoDBMapper.query(MarkerLocationTestDO.class, queryExpression);


                //PaginatedScanList<MarkerLocationDO> result = dynamoDBMapper.scan(MarkerLocationDO.class, scanExpression,);
                for (MarkerLocationTestDO m : result) {
                    Log.d(TAG, "value: " + m.getLongitude());
                    Log.d(TAG, "value: " + m.getLattitude());
                    Log.d(TAG, "value: " + m.getGridLocation());

                }
            }
        }).start();
    }

    public String generateMarkerID()
    {
        String uniqueID = UUID.randomUUID().toString();
        return uniqueID;
    }

    public void saveMarker(double latti, double longi)
    {
        //send to aws
        final MarkerLocationTestDO nnMarker = new MarkerLocationTestDO();


        nnMarker.setMarkerID(generateMarkerID());

        nnMarker.setLattitude(latti);

        nnMarker.setLongitude(longi);

        nnMarker.setGridLocation(getGridLocation());


        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(nnMarker);

                // Item saved

            }
        }).start();
    }

}


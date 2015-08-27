package com.cerevo.blueninja.bletimesetter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    //BLEスキャンタイムアウト[ms]
    private static final int SCAN_TIMEOUT = 10000;
    //接続対象のデバイス名
    private static final String DEVICE_NAME = "CDP-TZ01B_CTS";
    /* UUIDs */
    //Current Time Service
    private static final String UUID_SERVICE_PCTS = "00001805-0000-1000-8000-00805f9b34fb";
    //Current Time(Pseudo) ※本来Current TimeにWriteするのは規格外ですぜ
    private static final String UUID_CHARACTERISTIC_PCT = "00002a2b-0000-1000-8000-00805f9b34fb";
    //キャラクタリスティック設定UUID
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    //ログのTAG
    private static final String LOG_TAG = "BNBLE_SETTIME";

    private BluetoothManager mBtManager;
    private BluetoothAdapter mBtAdapter;

    private Calendar mCalendarLocal;
    private Handler mHandler;
    private Timer mLocalTime;

    private AppState mAppStat = AppState.INIT;

    private TextView mtextViewLocalTimeValue;
    private TextView mtextViewBlueNinjaTimeValue;

    private Button mbuttonConnect;
    private Button mbuttonDisconnect;
    private Button mbuttonSetTime;

    private enum AppState {
        INIT,
        BLE_SCANNING,
        BLE_SCAN_FAILED,
        BLE_DEV_FOUND,
        BLE_SRV_FOUND,
        BLE_CHARACTERISTIC_NOT_FOUND,
        BLE_CONNECTED,
        BLE_DISCONNECTED,
        BLE_SRV_NOT_FOUND,
        BLE_READ_SUCCESS,
        BLE_CLOSED
    }

    private void setStatus(AppState stat)
    {
        Message msg = new Message();
        msg.what = stat.ordinal();
        msg.obj = stat.name();

        mAppStat = stat;
        mHandler.sendMessage(msg);
    }

    private AppState getStatus()
    {
        return mAppStat;
    }
    private void refreshLocalTime()
    {
        Message msg = new Message();
        msg.what = -1;
        msg.obj = null;

        mHandler.sendMessage(msg);
    }

    private void refreshBlueNinjaTime()
    {
        Message msg = new Message();
        msg.what = -2;
        msg.obj = null;

        mHandler.sendMessage(msg);
    }

    View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonConnect:
                    connectBLE();
                    break;
                case R.id.buttonDisconnect:
                    disconnectBLE();
                    break;
                case R.id.buttonSetTime:
                    /* BLE CurrentTimeの生成 */
                    mCalendarLocal.setTimeInMillis(System.currentTimeMillis());
                    int year = mCalendarLocal.get(Calendar.YEAR);
                    int mon = mCalendarLocal.get(Calendar.MONTH) + 1;
                    int mday = mCalendarLocal.get(Calendar.DAY_OF_MONTH);
                    int wday = mCalendarLocal.get(Calendar.DAY_OF_WEEK);
                    int hour = mCalendarLocal.get(Calendar.HOUR_OF_DAY);
                    int min = mCalendarLocal.get(Calendar.MINUTE);
                    int sec = mCalendarLocal.get(Calendar.SECOND);

                    byte ble_cts_dat[] = new byte[10];
                    ble_cts_dat[0] = (byte)(year & 0xff);
                    ble_cts_dat[1] = (byte)((year >> 8) & 0xff);
                    ble_cts_dat[2] = (byte)mon;
                    ble_cts_dat[3] = (byte)mday;
                    ble_cts_dat[4] = (byte)hour;
                    ble_cts_dat[5] = (byte)min;
                    ble_cts_dat[6] = (byte)sec;
                    ble_cts_dat[7] = (byte)wday;
                    ble_cts_dat[8] = 0;
                    ble_cts_dat[9] = 0x00;

                    mCharacteristec.setValue(ble_cts_dat);
                    mGatt.writeCharacteristic(mCharacteristec);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* BLEの初期化 */
        mBtManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
        if ((mBtAdapter == null) || (mBtAdapter.isEnabled() == false)) {
            Toast.makeText(getApplicationContext(), "Bluetoothが有効ではありません", Toast.LENGTH_SHORT).show();
            finish();
        }

        /* ウィジェットの初期化 */
        //TextViews
        mtextViewLocalTimeValue = (TextView)findViewById(R.id.textViewLocalTimeValue);
        mtextViewBlueNinjaTimeValue = (TextView)findViewById(R.id.textViewBlueNinjaTimeValue);
        //Buttons
        mbuttonConnect = (Button)findViewById(R.id.buttonConnect);
        mbuttonConnect.setOnClickListener(buttonClickListener);
        mbuttonConnect.setEnabled(true);

        mbuttonDisconnect = (Button)findViewById(R.id.buttonDisconnect);
        mbuttonDisconnect.setOnClickListener(buttonClickListener);
        mbuttonDisconnect.setEnabled(false);

        mbuttonSetTime = (Button)findViewById(R.id.buttonSetTime);
        mbuttonSetTime.setOnClickListener(buttonClickListener);
        mbuttonSetTime.setEnabled(false);

        /*  */
        mCalendarLocal = Calendar.getInstance();
        /* UI更新ハンドラ */
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == -1) {
                    /* LocalTimeの表示更新 */
                    mCalendarLocal.setTimeInMillis(System.currentTimeMillis());
                    int year = mCalendarLocal.get(Calendar.YEAR);
                    int mon = mCalendarLocal.get(Calendar.MONTH) + 1;
                    int mday = mCalendarLocal.get(Calendar.DAY_OF_MONTH);
                    int wday = mCalendarLocal.get(Calendar.DAY_OF_WEEK);
                    int hour = mCalendarLocal.get(Calendar.HOUR_OF_DAY);
                    int min = mCalendarLocal.get(Calendar.MINUTE);
                    int sec = mCalendarLocal.get(Calendar.SECOND);

                    mtextViewLocalTimeValue.setText(
                            String.format(
                                    "%04d/%02d/%02d %02d:%02d:%02d",
                                    year, mon, mday,
                                    hour, min, sec));

                    if (getStatus() == AppState.BLE_CONNECTED) {
                        if (mGatt != null) {
                            mGatt.readCharacteristic(mCharacteristec);
                        }
                    }

                    return;
                }

                if (msg.what == -2) {
                    mtextViewBlueNinjaTimeValue.setText(mBNTime);
                    return;
                }
                /*  */
                final TextView textStatus = (TextView)findViewById(R.id.textViewStatus);
                textStatus.setText((String)msg.obj);
                /*  */
                AppState state = AppState.values()[msg.what];
                switch (state) {
                    case BLE_CONNECTED:
                        mbuttonConnect.setEnabled(false);
                        mbuttonDisconnect.setEnabled(true);
                        mbuttonSetTime.setEnabled(true);
                        break;
                    case BLE_DISCONNECTED:
                    case BLE_CLOSED:
                        mbuttonConnect.setEnabled(true);
                        mbuttonDisconnect.setEnabled(false);
                        mbuttonSetTime.setEnabled(false);
                        mtextViewBlueNinjaTimeValue.setText("----/--/-- --:--:--");
                        break;
                }
            }
        };

        mLocalTime = new Timer();
        mLocalTime.schedule(new TimerTask() {
            @Override
            public void run() {
                //LocalTime(Andoriodの時刻)表示を更新
                refreshLocalTime();
            }
        }, 0, 1000);

    }

    private BluetoothGatt mBtGatt;
    /**
     * BLEスキャンコールバック
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_TAG, String.format("Device found: %s[%s]", device.getName(), device.getUuids()));
            if (DEVICE_NAME.equals(device.getName())) {
                //BlueNinjaを発見
                setStatus(AppState.BLE_DEV_FOUND);
                mBtAdapter.stopLeScan(this);
                mBtGatt = device.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);
            }
        }
    };

    private BluetoothGattCharacteristic mCharacteristec;
    private BluetoothGatt mGatt;
    private  String mBNTime;
    /**
     * GATTコールバック
     */
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        /**
         * ステータス変更
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    /* 接続 */
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    /* 切断 */
                    setStatus(AppState.BLE_DISCONNECTED);
                    mBtGatt = null;
                    break;
            }
        }

        /**
         * サービスを発見
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            BluetoothGattService service = gatt.getService(UUID.fromString(UUID_SERVICE_PCTS));
            if (service == null) {
                //サービスが見つからない
                setStatus(AppState.BLE_SRV_NOT_FOUND);
            } else {
                //サービスが見つかった
                setStatus(AppState.BLE_SRV_FOUND);
                mCharacteristec = service.getCharacteristic(UUID.fromString(UUID_CHARACTERISTIC_PCT));
                if (mCharacteristec == null) {
                    //Characteristicが見つからない
                    setStatus(AppState.BLE_CHARACTERISTIC_NOT_FOUND);
                    return;
                }
            }
            mGatt = gatt;
            setStatus(AppState.BLE_CONNECTED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(LOG_TAG, "onCharacteristicRead: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //READ成功
                if (UUID_CHARACTERISTIC_PCT.equals(characteristic.getUuid().toString())) {
                    byte[] ble_cts_data = characteristic.getValue();

                    int year = (ble_cts_data[0] & 0x000000ff) | ((ble_cts_data[1] << 8) & 0x0000ff00);
                    int mon = ble_cts_data[2];
                    int mday = ble_cts_data[3];
                    int hour = ble_cts_data[4];
                    int min = ble_cts_data[5];
                    int sec = ble_cts_data[6];
                    int wday = ble_cts_data[7];

                    mBNTime = String.format("%d/%02d/%02d %02d:%02d:%02d", year, mon, mday, hour, min, sec, wday);
                    refreshBlueNinjaTime();
                }
            }
        }
    };

    private void connectBLE()
    {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtAdapter.stopLeScan(mLeScanCallback);
                if (AppState.BLE_SCANNING.equals(getStatus())) {
                    setStatus(AppState.BLE_SCAN_FAILED);
                }
            }
        }, SCAN_TIMEOUT);

        mBtAdapter.stopLeScan(mLeScanCallback);
        mBtAdapter.startLeScan(mLeScanCallback);
        setStatus(AppState.BLE_SCANNING);
    }

    private void disconnectBLE()
    {
        if (mBtGatt != null) {
            mBtGatt.close();
            mBtGatt = null;
            mCharacteristec = null;
            setStatus(AppState.BLE_CLOSED);
        }
    }
}

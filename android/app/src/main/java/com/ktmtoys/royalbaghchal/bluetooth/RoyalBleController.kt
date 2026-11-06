package com.ktmtoys.royalbaghchal.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.ktmtoys.royalbaghchal.game.GameState
import com.ktmtoys.royalbaghchal.game.Move
import com.ktmtoys.royalbaghchal.game.Side
import java.util.UUID

/** BLE CENTRAL for a compatible external board, never a generic phone-to-phone peer. */
@SuppressLint("MissingPermission")
class RoyalBleController(
    private val context:Context,
    private val onStatus:(String)->Unit,
    private val onMove:(Int?,Int)->Unit
){
    companion object {
        private val SERVICE=UUID.fromString("b0a6c7a1-7f0b-4e3b-b416-14df3b0a0001")
        private val WRITE=UUID.fromString("b0a6c7a1-7f0b-4e3b-b416-14df3b0a0002")
        private val NOTIFY=UUID.fromString("b0a6c7a1-7f0b-4e3b-b416-14df3b0a0003")
        private val CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    private val handler=Handler(Looper.getMainLooper())
    private val adapter=(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanning=false
    private var gatt:BluetoothGatt?=null
    private var write:BluetoothGattCharacteristic?=null
    private var latest:GameState?=null
    private val stopScan=Runnable{if(scanning){stopScanning();status("No matching board found")}}
    private fun status(v:String){handler.post {onStatus(v)}}
    fun supported():Boolean=adapter!=null &&context.packageManager.hasSystemFeature("android.hardware.bluetooth_le")
    private val scanCallback=object:ScanCallback(){
        override fun onScanResult(callbackType:Int,result:ScanResult){
            stopScanning()
            status("Connecting to ${result.device.name?:"BLE board"}…")
            gatt=result.device.connectGatt(context,false,gattCallback,BluetoothDevice.TRANSPORT_LE)
        }
        override fun onScanFailed(errorCode:Int){
            scanning=false;status("Scan failed: $errorCode")
        }
    }
    private val gattCallback=object:BluetoothGattCallback(){
        override fun onConnectionStateChange(g:BluetoothGatt,status:Int,newState:Int){
            if(status==BluetoothGatt.GATT_SUCCESS &&newState==BluetoothProfile.STATE_CONNECTED){
                this@RoyalBleController.gatt=g
                this@RoyalBleController.status("Discovering royal board services…")
                g.discoverServices()
            }else {
                if(this@RoyalBleController.gatt===g){
                    write=null;this@RoyalBleController.gatt=null
                }
                g.close()
                this@RoyalBleController.status("Board disconnected")
            }
        }
        override fun onServicesDiscovered(g:BluetoothGatt,status:Int){
            if(status!=BluetoothGatt.GATT_SUCCESS){this@RoyalBleController.status("Service discovery failed");return}
            val service=g.getService(SERVICE)
            val wr=service?.getCharacteristic(WRITE)
            val no=service?.getCharacteristic(NOTIFY)
            if(wr==null||no==null){
                this@RoyalBleController.status("This device does not implement the Baghchal BLE protocol")
                g.disconnect()
                return
            }
            write=wr
            g.setCharacteristicNotification(no,true)
            val descriptor=no.getDescriptor(CCCD)
            if(descriptor!=null){
                if(Build.VERSION.SDK_INT>=33)
                    g.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                else {
                    @Suppress("DEPRECATION")
                    descriptor.value=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(descriptor)
                }
            }
            this@RoyalBleController.status("Royal board connected")
            latest?.let {sync(it)}
        }
        override fun onCharacteristicChanged(g:BluetoothGatt,
            characteristic:BluetoothGattCharacteristic,value:ByteArray){
            if(characteristic.uuid==NOTIFY)receive(value)
        }
        @Deprecated("Android 12 and lower callback")
        override fun onCharacteristicChanged(g:BluetoothGatt,
            characteristic:BluetoothGattCharacteristic){
            if(characteristic.uuid==NOTIFY){
                @Suppress("DEPRECATION")
                val data=characteristic.value
                receive(data)
            }
        }
    }
    private fun receive(data:ByteArray){
        val text=data.toString(Charsets.UTF_8).trim()
        if(text=="R"){latest?.let{sync(it)};return}
        val m=Regex("^M:(-1|\\d{1,2}):(\\d{1,2})$").matchEntire(text)?:return
        val from=m.groupValues[1].toIntOrNull()?:return
        val to=m.groupValues[2].toIntOrNull()?:return
        if(from !in -1..24 ||to !in 0..24)return
        handler.post {onMove(if(from==-1)null else from,to)}
    }
    fun pair(game:GameState){
        latest=game
        if(!supported()){status("Bluetooth Low Energy unavailable");return}
        if(adapter?.isEnabled!=true){status("Enable Bluetooth in Android settings");return}
        closeConnection()
        val scanner=adapter.bluetoothLeScanner
        if(scanner==null){status("Bluetooth scanner unavailable");return}
        val filters=listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE)).build())
        val settings=ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try{
            scanning=true
            status("Scanning for compatible Baghchal board…")
            scanner.startScan(filters,settings,scanCallback)
            handler.postDelayed(stopScan,12000L)
        }catch(e:Exception){
            scanning=false;status("Bluetooth scan failed: ${e.message}")
        }
    }
    private fun stopScanning(){
        handler.removeCallbacks(stopScan)
        if(scanning){
            scanning=false
            try{adapter?.bluetoothLeScanner?.stopScan(scanCallback)}catch(_:Exception){}
        }
    }
    private fun closeConnection(){
        stopScanning();write=null
        try{gatt?.disconnect();gatt?.close()}catch(_:Exception){}
        gatt=null
    }
    private fun base36(value:Long):String=value.toString(36)
    /** 25 ternary positions fit in a signed Long and the 20-byte BLE packet. */
    fun sync(game:GameState){
        latest=game
        var value=0L
        for(piece in game.board){
            value=value*3+(when(piece?.side){Side.GOAT->1L;Side.TIGER->2L;null->0L})
        }
        val packet="S:"+(if(game.turn==Side.GOAT)"g" else "t")+":"+
            base36(game.goatsPlaced.toLong())+":"+base36(game.goatsCaptured.toLong())+":"+
            base36(value)
        send(packet)
    }
    fun move(move:Move,game:GameState){
        latest=game
        send("M:${move.from?:-1}:${move.to}")
    }
    private fun send(payload:String){
        val bytes=payload.toByteArray(Charsets.UTF_8)
        if(bytes.size>20)return
        val g=gatt?:return
        val characteristic=write?:return
        try{
            if(Build.VERSION.SDK_INT>=33) {
                g.writeCharacteristic(characteristic,bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            }else {
                @Suppress("DEPRECATION")
                characteristic.value=bytes
                @Suppress("DEPRECATION")
                g.writeCharacteristic(characteristic)
            }
        }catch(e:Exception){status("Board write failed: ${e.message}")}
    }
    fun disconnect(){closeConnection();status("Board disconnected")}
    fun close(){closeConnection();handler.removeCallbacksAndMessages(null)}
}

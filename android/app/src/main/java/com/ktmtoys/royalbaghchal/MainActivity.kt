package com.ktmtoys.royalbaghchal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ktmtoys.royalbaghchal.bluetooth.RoyalBleController
import com.ktmtoys.royalbaghchal.ui.RoyalApp
import com.ktmtoys.royalbaghchal.ui.RoyalTheme

class MainActivity:ComponentActivity(){
    private val vm:RoyalGameViewModel by viewModels()
    private lateinit var ble:RoyalBleController
    private val blePermissions=registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if(granted.values.all{it})ble.pair(vm.game)
        else vm.updateBleStatus("Bluetooth permissions not granted")
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        ble=RoyalBleController(this,
            onStatus={vm.updateBleStatus(it)},
            onMove={from,to->vm.bleMove(from,to)})
        vm.bleEmitter={move,game->ble.move(move,game)}
        setContent {
            RoyalTheme {
                RoyalApp(vm,onPair={pairBoard()},onDisconnectBle={ble.disconnect()})
            }
        }
    }
    private fun pairBoard(){
        if(vm.mode==Mode.ONLINE){
            vm.notify(vm.label("bleMissing"));return
        }
        if(!ble.supported()){vm.updateBleStatus(vm.label("noHardware"));return}
        val required=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if(required.all {
            ContextCompat.checkSelfPermission(this,it)==PackageManager.PERMISSION_GRANTED
        })ble.pair(vm.game)
        else blePermissions.launch(required)
    }
    override fun onDestroy(){
        vm.bleEmitter=null
        ble.close()
        super.onDestroy()
    }
}

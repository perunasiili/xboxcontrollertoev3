package com.example.ev3bridge

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPair = findViewById<Button>(R.id.btnPair)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val editMac = findViewById<EditText>(R.id.editMac)
        val status = findViewById<TextView>(R.id.status)

        btnPair.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        btnConnect.setOnClickListener {
            val mac = editMac.text.toString().trim()
            if (mac.isNotEmpty()) connectToEv3(mac)
        }

        requestBluetoothPermissionsIfNeeded()
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1)
        }
    }

    private fun connectToEv3(mac: String) {
        val device: BluetoothDevice? = btAdapter?.getRemoteDevice(mac)
        if (device == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btAdapter?.cancelDiscovery()
                btSocket?.connect()
                outStream = btSocket?.outputStream
                runOnUiThread { findViewById<TextView>(R.id.status).text = "CONNECTED" }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { findViewById<TextView>(R.id.status).text = "FAILED: ${'$'}{e.message}" }
            }
        }
    }

    private fun sendJsonCommand(json: JSONObject) {
        val bytes = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outStream?.write(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val source = event.source
        if ((source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            val leftX = getCenteredAxis(event, MotionEvent.AXIS_X)
            val rt = getTrigger(event)

            val cSpeed = (leftX * 100).toInt()
            val jsonC = JSONObject().apply {
                put("cmd", "motor")
                put("port", "C")
                put("speed", cSpeed)
            }
            sendJsonCommand(jsonC)

            val abSpeed = (rt * 100).toInt()
            val jsonAB = JSONObject().apply {
                put("cmd", "motors")
                put("ports", "AB")
                put("speed", abSpeed)
            }
            sendJsonCommand(jsonAB)

            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun getCenteredAxis(event: MotionEvent, axis: Int): Float {
        val value = event.getAxisValue(axis)
        return if (kotlin.math.abs(value) < 0.10f) 0f else value
    }

    private fun getTrigger(event: MotionEvent): Float {
        val rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        if (rTrigger != 0f) return (rTrigger.coerceIn(0f,1f))
        val z = event.getAxisValue(MotionEvent.AXIS_Z)
        if (z != 0f) return ((z + 1f) / 2f)
        return 0f
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { btSocket?.close() } catch (ignored: Exception) {}
    }
}

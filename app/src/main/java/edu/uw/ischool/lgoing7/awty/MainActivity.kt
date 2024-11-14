package edu.uw.ischool.lgoing7.awty

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var messageInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var minutesInput: EditText
    private lateinit var timerText: TextView
    private lateinit var startStopButton: Button

    private var timer: CountDownTimer? = null
    private var isRunning = false

    companion object {
        private const val SMS_PERMISSION_REQUEST = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageInput = findViewById(R.id.messageInput)
        phoneInput = findViewById(R.id.phoneInput)
        minutesInput = findViewById(R.id.minutesInput)
        timerText = findViewById(R.id.timerText)
        startStopButton = findViewById(R.id.startStopButton)

        startStopButton.setOnClickListener {
            if (!isRunning) {
                val validationResult = validateInputs()
                if (validationResult.first) {
                    checkSmsPermissionAndStart()
                } else {
                    Toast.makeText(this,
                        validationResult.second,
                        Toast.LENGTH_LONG).show()
                }
            } else {
                stopService()
            }
        }
    }

    private fun checkSmsPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST)
        } else {
            Toast.makeText(this,
                "Beginning nagging service...",
                Toast.LENGTH_SHORT).show()
            startService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                        "Beginning nagging service...",
                        Toast.LENGTH_SHORT).show()
                    startService()
                } else {
                    Toast.makeText(this,
                        "SMS permission denied. Cannot start service.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(): Pair<Boolean, String> {
        val message = messageInput.text.toString()
        val phone = phoneInput.text.toString().replace(Regex("[^0-9]"), "")
        val minutes = minutesInput.text.toString()

        return when {
            message.isEmpty() -> Pair(false, "Message cannot be empty")
            phone.isEmpty() -> Pair(false, "Phone number cannot be empty")
            phone.length != 10 -> Pair(false, "Phone number must be exactly 10 digits")
            minutes.isEmpty() -> Pair(false, "Minutes cannot be empty")
            minutes.toIntOrNull()?.let { it <= 0 } ?: true ->
                Pair(false, "Minutes must be a positive number")
            else -> Pair(true, "")
        }
    }

    private fun startService() {
        isRunning = true
        startStopButton.text = getString(R.string.stop)
        val minutes = minutesInput.text.toString().toLong()
        startTimer(minutes * 60 * 1000) // min -> ms
    }

    private fun stopService() {
        isRunning = false
        startStopButton.text = getString(R.string.start)
        timer?.cancel()
        timerText.text = getString(R.string.next_message_in)
    }

    private fun startTimer(totalTimeInMillis: Long) {
        timer?.cancel()

        timer = object : CountDownTimer(totalTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText.text = getString(R.string.next_message_in_02d_02d).format(minutes, seconds)
            }

            override fun onFinish() {
                sendSmsMessage()
                if (isRunning) {
                    startTimer(totalTimeInMillis)
                }
            }
        }.start()
    }

    private fun sendSmsMessage() {
        try {
            val phone = phoneInput.text.toString()
                .replace(Regex("[^0-9]"), "")
            val message = messageInput.text.toString()

            @Suppress("DEPRECATION") val smsManager = SmsManager.getDefault()

            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    "+1$phone",  // +1 for US
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    "+1$phone",
                    null,
                    message,
                    null,
                    null
                )
            }

            Toast.makeText(this,
                "SMS sent to $phone",
                Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this,
                "Failed to send SMS: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
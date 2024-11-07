package edu.uw.ischool.lgoing7.awty

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var messageInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var minutesInput: EditText
    private lateinit var timerText: TextView
    private lateinit var startStopButton: Button

    private var timer: CountDownTimer? = null
    private var isRunning = false

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
                    Toast.makeText(this,
                        "Beginning nagging service...",
                        Toast.LENGTH_SHORT).show()
                    startService()
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
        "Stop".also { startStopButton.text = it }
        val minutes = minutesInput.text.toString().toLong()
        startTimer(minutes * 60 * 1000) // min -> ms
    }

    private fun stopService() {
        isRunning = false
        "Start".also { startStopButton.text = it }
        timer?.cancel()
        "Next message in: --:--".also { timerText.text = it }
    }

    private fun startTimer(totalTimeInMillis: Long) {
        timer?.cancel()

        timer = object : CountDownTimer(totalTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                "Next message in: %02d:%02d".format(minutes, seconds).also { timerText.text = it }
            }

            override fun onFinish() {
                showMessage()

                if (isRunning) {
                    startTimer(totalTimeInMillis)
                }
            }
        }.start()
    }

    private fun showMessage() {
        val phone = phoneInput.text.toString()
        val message = messageInput.text.toString()
        Toast.makeText(this,
            "Texting $phone\n$message",
            Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
package com.morninglock

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class EditAlarmActivity : AppCompatActivity() {

    private var alarmId: Int = -1
    private var selectedHour   = 8
    private var selectedMinute = 0
    private var selectedRingtoneUri = "default"
    private var lastSliderProgress = 0

    private lateinit var tvTime:         TextView
    private lateinit var etLabel:        EditText
    private lateinit var switchPrimary:  Switch
    private lateinit var switchVibrate:  Switch
    private lateinit var tvRingtone:     TextView
    private lateinit var lockSection:    LinearLayout
    private lateinit var seekLock:       SeekBar
    private lateinit var tvLockDuration: TextView
    private lateinit var btnSave:        Button
    private lateinit var tvPrimaryNote:  TextView
    private lateinit var tvTimeUntil:    TextView

    private var vibrator: Vibrator? = null
    private val RINGTONE_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        alarmId = intent.getIntExtra("alarm_id", -1)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        tvTime         = findViewById(R.id.tvTime)
        etLabel        = findViewById(R.id.etLabel)
        switchPrimary  = findViewById(R.id.switchPrimary)
        switchVibrate  = findViewById(R.id.switchVibrate)
        tvRingtone     = findViewById(R.id.tvRingtone)
        lockSection    = findViewById(R.id.lockSection)
        seekLock       = findViewById(R.id.seekLock)
        tvLockDuration = findViewById(R.id.tvLockDuration)
        btnSave        = findViewById(R.id.btnSave)
        tvPrimaryNote  = findViewById(R.id.tvPrimaryNote)
        tvTimeUntil    = findViewById(R.id.tvTimeUntil)

        // Slider: 0 = 30 min, 90 = 120 min (1 step = 1 minute, haptic each step)
        seekLock.max = 90
        seekLock.progress = 0
        lastSliderProgress = 0

        seekLock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = 30 + progress
                tvLockDuration.text = formatDuration(duration)
                if (fromUser && progress != lastSliderProgress) {
                    vibrateSliderTick()
                    lastSliderProgress = progress
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        })

        switchPrimary.setOnCheckedChangeListener { _, checked ->
            lockSection.visibility   = if (checked) View.VISIBLE else View.GONE
            tvPrimaryNote.visibility = if (checked) View.VISIBLE else View.GONE
        }

        tvTime.setOnClickListener { showTimePicker() }

        // Ringtone row click
        findViewById<View>(R.id.ringtoneRow).setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                if (selectedRingtoneUri != "default") {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri))
                }
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, RINGTONE_REQUEST)
        }

        btnSave.setOnClickListener { saveAlarm() }

        if (alarmId != -1) {
            lifecycleScope.launch {
                val alarm = AppDatabase.getInstance(this@EditAlarmActivity).alarmDao().getAlarm(alarmId)
                alarm?.let { populateFields(it) }
            }
        } else {
            updateTimeDisplay()
            updateTimeUntil()
        }

        supportActionBar?.title = if (alarmId == -1) "New Alarm" else "Edit Alarm"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun vibrateSliderTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(15, 80))
        }
    }

    private fun populateFields(alarm: Alarm) {
        selectedHour        = alarm.hour
        selectedMinute      = alarm.minute
        selectedRingtoneUri = alarm.ringtoneUri

        updateTimeDisplay()
        updateTimeUntil()
        etLabel.setText(alarm.label)
        switchPrimary.isChecked = alarm.isPrimary
        switchVibrate.isChecked = alarm.vibrate

        val sliderProgress = (alarm.lockDurationMinutes - 30).coerceIn(0, 90)
        seekLock.progress  = sliderProgress
        lastSliderProgress = sliderProgress
        tvLockDuration.text = formatDuration(alarm.lockDurationMinutes)

        lockSection.visibility   = if (alarm.isPrimary) View.VISIBLE else View.GONE
        tvPrimaryNote.visibility = if (alarm.isPrimary) View.VISIBLE else View.GONE
        tvRingtone.text = if (alarm.ringtoneUri == "default") "Default alarm" else "Custom"
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            selectedHour   = hour
            selectedMinute = minute
            updateTimeDisplay()
            updateTimeUntil()
            // Small haptic on time selection
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }, selectedHour, selectedMinute, false).show()
    }

    private fun updateTimeDisplay() {
        val amPm = if (selectedHour < 12) "AM" else "PM"
        val h = when {
            selectedHour == 0  -> 12
            selectedHour > 12  -> selectedHour - 12
            else               -> selectedHour
        }
        tvTime.text = "%d:%02d %s".format(h, selectedMinute, amPm)
    }

    private fun updateTimeUntil() {
        val now   = Calendar.getInstance()
        val alarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val diffMs    = alarm.timeInMillis - now.timeInMillis
        val totalMins = (diffMs / 1000 / 60).toInt()
        val hours     = totalMins / 60
        val mins      = totalMins % 60

        tvTimeUntil.text = when {
            hours > 0 && mins > 0 -> "Alarm in ${hours}h ${mins}m"
            hours > 0             -> "Alarm in ${hours}h"
            else                  -> "Alarm in ${mins}m"
        }
        tvTimeUntil.visibility = View.VISIBLE
    }

    private fun saveAlarm() {
        // Read slider value directly — this was the bug causing 30min always
        val finalLockDuration = 30 + seekLock.progress

        val alarm = Alarm(
            id                  = if (alarmId == -1) 0 else alarmId,
            hour                = selectedHour,
            minute              = selectedMinute,
            label               = etLabel.text.toString().trim(),
            isPrimary           = switchPrimary.isChecked,
            isEnabled           = true,
            snoozeMinutes       = 5,
            lockDurationMinutes = finalLockDuration,
            vibrate             = switchVibrate.isChecked,
            ringtoneUri         = selectedRingtoneUri,
            repeatDays          = 127
        )

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@EditAlarmActivity)
            if (alarmId == -1) {
                val newId = db.alarmDao().insert(alarm).toInt()
                AlarmScheduler.schedule(this@EditAlarmActivity, alarm.copy(id = newId))
            } else {
                db.alarmDao().update(alarm)
                AlarmScheduler.cancel(this@EditAlarmActivity, alarm)
                AlarmScheduler.schedule(this@EditAlarmActivity, alarm)
            }
            // Haptic on save
            vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            finish()
        }
    }

    private fun formatDuration(minutes: Int): String {
        return if (minutes < 60) "$minutes min"
        else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RINGTONE_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedRingtoneUri = uri.toString()
                tvRingtone.text = RingtoneManager.getRingtone(this, uri)?.getTitle(this) ?: "Custom"
            } else {
                selectedRingtoneUri = "default"
                tvRingtone.text = "Default alarm"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

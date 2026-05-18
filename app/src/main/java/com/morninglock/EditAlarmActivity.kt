package com.morninglock

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditAlarmActivity : AppCompatActivity() {

    private var alarmId: Int = -1
    private var existingAlarm: Alarm? = null
    private var selectedHour   = 8
    private var selectedMinute = 0
    private var isPrimary      = false
    private var selectedRingtoneUri = "default"
    private var lockDuration   = 30

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

    private val RINGTONE_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        alarmId = intent.getIntExtra("alarm_id", -1)

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

        // Slider: 0 = 30 min, 90 = 120 min (step = 1 min)
        seekLock.max = 90
        seekLock.progress = 0
        seekLock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                lockDuration = 30 + progress
                tvLockDuration.text = formatDuration(lockDuration)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        switchPrimary.setOnCheckedChangeListener { _, checked ->
            isPrimary = checked
            lockSection.visibility = if (checked) android.view.View.VISIBLE else android.view.View.GONE
            tvPrimaryNote.visibility = if (checked) android.view.View.VISIBLE else android.view.View.GONE
        }

        tvTime.setOnClickListener { showTimePicker() }

        tvRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                if (selectedRingtoneUri != "default") {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri))
                }
            }
            startActivityForResult(intent, RINGTONE_REQUEST)
        }

        btnSave.setOnClickListener { saveAlarm() }

        // Load existing alarm if editing
        if (alarmId != -1) {
            lifecycleScope.launch {
                existingAlarm = AppDatabase.getInstance(this@EditAlarmActivity).alarmDao().getAlarm(alarmId)
                existingAlarm?.let { populateFields(it) }
            }
        } else {
            updateTimeDisplay()
        }

        supportActionBar?.title = if (alarmId == -1) "New Alarm" else "Edit Alarm"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun populateFields(alarm: Alarm) {
        selectedHour   = alarm.hour
        selectedMinute = alarm.minute
        isPrimary      = alarm.isPrimary
        lockDuration   = alarm.lockDurationMinutes
        selectedRingtoneUri = alarm.ringtoneUri

        updateTimeDisplay()
        etLabel.setText(alarm.label)
        switchPrimary.isChecked = alarm.isPrimary
        switchVibrate.isChecked = alarm.vibrate
        seekLock.progress = (alarm.lockDurationMinutes - 30).coerceIn(0, 90)
        tvLockDuration.text = formatDuration(alarm.lockDurationMinutes)
        lockSection.visibility  = if (alarm.isPrimary) android.view.View.VISIBLE else android.view.View.GONE
        tvPrimaryNote.visibility = if (alarm.isPrimary) android.view.View.VISIBLE else android.view.View.GONE
        tvRingtone.text = if (alarm.ringtoneUri == "default") "Default alarm" else "Custom ringtone"
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            selectedHour   = hour
            selectedMinute = minute
            updateTimeDisplay()
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

    private fun saveAlarm() {
        val alarm = Alarm(
            id                  = if (alarmId == -1) 0 else alarmId,
            hour                = selectedHour,
            minute              = selectedMinute,
            label               = etLabel.text.toString().trim(),
            isPrimary           = switchPrimary.isChecked,
            isEnabled           = true,
            snoozeMinutes       = 5,
            lockDurationMinutes = lockDuration,
            vibrate             = switchVibrate.isChecked,
            ringtoneUri         = selectedRingtoneUri,
            repeatDays          = 127 // Every day by default
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

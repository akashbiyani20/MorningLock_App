package com.morninglock

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class EditAlarmActivity : AppCompatActivity() {

    private var alarmId: Int = -1
    private var selectedHour   = 8
    private var selectedMinute = 0
    private var selectedRingtoneUri = "default"

    private lateinit var npHour:        NumberPicker
    private lateinit var npMinute:      NumberPicker
    private lateinit var npAmPm:        NumberPicker
    private lateinit var etLabel:       EditText
    private lateinit var switchPrimary: SwitchCompat
    private lateinit var switchVibrate: SwitchCompat
    private lateinit var tvRingtone:    TextView
    private lateinit var lockSection:   LinearLayout
    private lateinit var rulerLock:     GrainRuler
    private lateinit var tvLockDuration:TextView
    private lateinit var btnSave:       Button
    private lateinit var btnDelete:     Button
    private lateinit var tvPrimaryNote: TextView
    private lateinit var tvTimeUntil:   TextView

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

        npHour         = findViewById(R.id.npHour)
        npMinute       = findViewById(R.id.npMinute)
        npAmPm         = findViewById(R.id.npAmPm)
        etLabel        = findViewById(R.id.etLabel)
        switchPrimary  = findViewById(R.id.switchPrimary)
        switchVibrate  = findViewById(R.id.switchVibrate)
        tvRingtone     = findViewById(R.id.tvRingtone)
        lockSection    = findViewById(R.id.lockSection)
        rulerLock      = findViewById(R.id.rulerLock)
        tvLockDuration = findViewById(R.id.tvLockDuration)
        btnSave        = findViewById(R.id.btnSave)
        btnDelete      = findViewById(R.id.btnDelete)
        tvPrimaryNote  = findViewById(R.id.tvPrimaryNote)
        tvTimeUntil    = findViewById(R.id.tvTimeUntil)

        setupTimeWheels()
        setupLockRuler()

        switchPrimary.setOnCheckedChangeListener { _, checked -> setLockEnabled(checked) }
        setLockEnabled(switchPrimary.isChecked)

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

        btnDelete.text = if (alarmId == -1) "CANCEL" else "DELETE"
        btnDelete.setOnClickListener {
            if (alarmId == -1) finish() else deleteAlarm()
        }

        if (alarmId != -1) {
            lifecycleScope.launch {
                val alarm = AppDatabase.getInstance(this@EditAlarmActivity).alarmDao().getAlarm(alarmId)
                alarm?.let { populateFields(it) }
            }
        } else {
            setWheelsFrom24(selectedHour, selectedMinute)
            refreshSelectedTime()
        }

        supportActionBar?.title = if (alarmId == -1) "New Alarm" else "Edit Alarm"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // ─── Time wheels ───────────────────────────────────────────────────────────

    private fun setupTimeWheels() {
        npHour.minValue = 1
        npHour.maxValue = 12
        npHour.wrapSelectorWheel = true

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.wrapSelectorWheel = true
        npMinute.setFormatter { String.format("%02d", it) }

        npAmPm.minValue = 0
        npAmPm.maxValue = 1
        npAmPm.displayedValues = arrayOf("AM", "PM")
        npAmPm.wrapSelectorWheel = false

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            vibrateTick()
            refreshSelectedTime()
        }
        npHour.setOnValueChangedListener(listener)
        npMinute.setOnValueChangedListener(listener)
        npAmPm.setOnValueChangedListener(listener)
    }

    private fun setWheelsFrom24(hour24: Int, minute: Int) {
        npHour.value   = if (hour24 % 12 == 0) 12 else hour24 % 12
        npMinute.value = minute
        npAmPm.value   = if (hour24 >= 12) 1 else 0
    }

    private fun readWheels24(): Pair<Int, Int> {
        val h12  = npHour.value
        val isPm = npAmPm.value == 1
        val hour24 = when {
            isPm && h12 == 12  -> 12
            isPm               -> h12 + 12
            !isPm && h12 == 12 -> 0
            else               -> h12
        }
        return hour24 to npMinute.value
    }

    private fun refreshSelectedTime() {
        val (h, m) = readWheels24()
        selectedHour = h
        selectedMinute = m
        updateTimeUntil()
    }

    // ─── Lock-duration ruler ─────────────────────────────────────────────────

    private fun setupLockRuler() {
        rulerLock.minValue = 1
        rulerLock.maxValue = 720
        rulerLock.value = 30
        tvLockDuration.text = formatDuration(rulerLock.value)
        rulerLock.onValueChanged = { tvLockDuration.text = formatDuration(it) }
    }

    private fun vibrateTick() {
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

        setWheelsFrom24(alarm.hour, alarm.minute)
        refreshSelectedTime()
        etLabel.setText(alarm.label)
        switchPrimary.isChecked = alarm.isPrimary
        switchVibrate.isChecked = alarm.vibrate

        rulerLock.value = alarm.lockDurationMinutes
        tvLockDuration.text = formatDuration(alarm.lockDurationMinutes)

        setLockEnabled(alarm.isPrimary)
        tvRingtone.text = if (alarm.ringtoneUri == "default") "Default alarm" else "Custom"
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
        refreshSelectedTime()
        val finalLockDuration = rulerLock.value

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
            vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            finish()
        }
    }

    /** Lock duration stays on screen always; orange + interactive when Primary, muted otherwise. */
    private fun setLockEnabled(on: Boolean) {
        lockSection.setBackgroundResource(if (on) R.drawable.bg_card_primary else R.drawable.bg_card_normal)
        tvLockDuration.setTextColor(getColor(if (on) R.color.orange_primary else R.color.text_hint))
        rulerLock.isEnabled = on
        rulerLock.alpha = if (on) 1f else 0.45f
    }

    private fun deleteAlarm() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@EditAlarmActivity)
            val a = db.alarmDao().getAlarm(alarmId)
            if (a != null) {
                AlarmScheduler.cancel(this@EditAlarmActivity, a)
                db.alarmDao().delete(a)
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

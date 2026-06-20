package com.morninglock

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: AlarmAdapter
    private lateinit var tvPermWarning: TextView

    private lateinit var alarmSection: View
    private lateinit var timerSection: View
    private lateinit var fab: FloatingActionButton
    private lateinit var btnTabAlarm: ImageButton
    private lateinit var btnTabTimer: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerAlarms)
        fab              = findViewById(R.id.fabAddAlarm)
        val btnSettings  = findViewById<ImageButton>(R.id.btnSettings)
        val layoutEmpty  = findViewById<View>(R.id.layoutEmpty)
        tvPermWarning    = findViewById(R.id.tvPermWarning)

        alarmSection = findViewById(R.id.alarmSection)
        timerSection = findViewById(R.id.timerSection)
        btnTabAlarm  = findViewById(R.id.btnTabAlarm)
        btnTabTimer  = findViewById(R.id.btnTabTimer)

        adapter = AlarmAdapter(
            onToggle = { alarm, enabled ->
                lifecycleScope.launch {
                    val updated = alarm.copy(isEnabled = enabled)
                    db.alarmDao().update(updated)
                    if (enabled) AlarmScheduler.schedule(this@MainActivity, updated)
                    else         AlarmScheduler.cancel(this@MainActivity, alarm)
                }
            },
            onEdit = { alarm ->
                startActivity(Intent(this, EditAlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarm.id)
                })
            },
            onDelete = { alarm ->
                lifecycleScope.launch {
                    AlarmScheduler.cancel(this@MainActivity, alarm)
                    db.alarmDao().delete(alarm)
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.alarmDao().getAllAlarms().collectLatest { alarms ->
                adapter.submitList(alarms)
                layoutEmpty.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fab.setOnClickListener {
            startActivity(Intent(this, EditAlarmActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupTimerSection()
        showTab(alarm = true)
        btnTabAlarm.setOnClickListener { showTab(alarm = true) }
        btnTabTimer.setOnClickListener { showTab(alarm = false) }

        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    // ─── Tabs ──────────────────────────────────────────────────────────────────

    private fun showTab(alarm: Boolean) {
        alarmSection.visibility = if (alarm) View.VISIBLE else View.GONE
        timerSection.visibility = if (alarm) View.GONE else View.VISIBLE
        fab.visibility          = if (alarm) View.VISIBLE else View.GONE

        val active   = ColorStateList.valueOf(getColor(R.color.orange_primary))
        val inactive = ColorStateList.valueOf(getColor(R.color.text_hint))
        btnTabAlarm.imageTintList = if (alarm) active else inactive
        btnTabTimer.imageTintList = if (alarm) inactive else active
    }

    // ─── Focus timer ───────────────────────────────────────────────────────────

    private fun setupTimerSection() {
        val ruler    = findViewById<GrainRuler>(R.id.grainSlider)
        val tvDur    = findViewById<TextView>(R.id.tvTimerDuration)
        val btnStart = findViewById<Button>(R.id.btnStartFocus)

        ruler.minValue = 1
        ruler.maxValue = 720
        ruler.value = 30
        tvDur.text = formatDuration(ruler.value)
        ruler.onValueChanged = { tvDur.text = formatDuration(it) }

        btnStart.setOnClickListener {
            if (!hasUsageStatsPermission() || !Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Grant Usage Access + Overlay permission first (see the warning on the Alarms tab)",
                    Toast.LENGTH_LONG
                ).show()
                showTab(alarm = true)
                return@setOnClickListener
            }
            val intent = Intent(this, LockService::class.java)
                .putExtra("lock_duration_minutes", ruler.value)
            startForegroundService(intent)
        }
    }

    private fun formatDuration(minutes: Int): String =
        if (minutes < 60) "$minutes min"
        else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val usageOk   = hasUsageStatsPermission()
        val overlayOk = Settings.canDrawOverlays(this)

        if (!usageOk || !overlayOk) {
            tvPermWarning.visibility = View.VISIBLE
            tvPermWarning.text = buildString {
                if (!usageOk)   append("⚠️ Usage Access needed   ")
                if (!overlayOk) append("⚠️ Overlay permission needed")
                append("\nTap here to fix →")
            }
            tvPermWarning.setOnClickListener {
                if (!usageOk) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        } else {
            tvPermWarning.visibility = View.GONE
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
        ) == AppOpsManager.MODE_ALLOWED
    }
}

// ─── Adapter ──────────────────────────────────────────────────────────────────

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onEdit:   (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private var alarms: List<Alarm> = emptyList()

    fun submitList(list: List<Alarm>) {
        alarms = list
        notifyDataSetChanged()
    }

    inner class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card:       View         = view.findViewById(R.id.alarmCard)
        val tvTime:     TextView     = view.findViewById(R.id.tvTime)
        val tvLabel:    TextView     = view.findViewById(R.id.tvLabel)
        val tvDays:     TextView     = view.findViewById(R.id.tvDays)
        val tvType:     TextView     = view.findViewById(R.id.tvType)
        val tvLockInfo: TextView     = view.findViewById(R.id.tvLockInfo)
        val toggle:     SwitchCompat = view.findViewById(R.id.switchEnabled)
        val btnDelete:  ImageButton  = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        val ctx   = holder.itemView.context

        holder.tvTime.text  = alarm.timeLabel()
        holder.tvLabel.text = alarm.label.ifEmpty { if (alarm.isPrimary) "Primary Alarm" else "Alarm" }
        holder.tvDays.text  = alarm.daysLabel()

        if (alarm.isPrimary) {
            holder.tvType.visibility     = View.VISIBLE
            holder.tvLockInfo.visibility = View.VISIBLE
            holder.tvType.text           = "PRIMARY"
            holder.tvLockInfo.text       = "Locks for ${alarm.lockDurationMinutes} min on stop"
            holder.card.setBackgroundResource(R.drawable.bg_card_primary)
            holder.tvTime.setTextColor(ctx.getColor(R.color.orange_primary))
        } else {
            holder.tvType.visibility     = View.GONE
            holder.tvLockInfo.visibility = View.GONE
            holder.card.setBackgroundResource(R.drawable.bg_card_normal)
            holder.tvTime.setTextColor(ctx.getColor(R.color.text_primary))
        }

        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = alarm.isEnabled
        holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(alarm, checked) }

        holder.itemView.setOnClickListener { onEdit(alarm) }
        holder.btnDelete.setOnClickListener { onDelete(alarm) }
    }

    override fun getItemCount() = alarms.size
}

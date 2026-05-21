package com.morninglock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: AlarmAdapter
    private lateinit var tvGreeting: TextView
    private lateinit var tvPermWarning: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerAlarms)
        val fab          = findViewById<FloatingActionButton>(R.id.fabAddAlarm)
        tvGreeting       = findViewById(R.id.tvGreeting)
        tvPermWarning    = findViewById(R.id.tvPermWarning)

        setGreeting()

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
                findViewById<TextView>(R.id.tvEmpty).visibility =
                    if (alarms.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fab.setOnClickListener {
            startActivity(Intent(this, EditAlarmActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        setGreeting()
        checkPermissions()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 4..11  -> "Good morning, Akash ☀️"
            in 12..16 -> "Coffee time, Akash ☕"
            in 17..20 -> "Good evening, Akash 🌆"
            else      -> "Night owl mode, Akash 🌙"
        }
    }

    private fun checkPermissions() {
        val usageOk  = hasUsageStatsPermission()
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

// ─── RecyclerView Adapter ─────────────────────────────────────────────────────

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
        val card:       View        = view.findViewById(R.id.alarmCard)
        val tvTime:     TextView    = view.findViewById(R.id.tvTime)
        val tvLabel:    TextView    = view.findViewById(R.id.tvLabel)
        val tvDays:     TextView    = view.findViewById(R.id.tvDays)
        val tvType:     TextView    = view.findViewById(R.id.tvType)
        val tvLockInfo: TextView    = view.findViewById(R.id.tvLockInfo)
        val toggle:     Switch      = view.findViewById(R.id.switchEnabled)
        val btnDelete:  ImageButton = view.findViewById(R.id.btnDelete)
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
            holder.tvLockInfo.text       = "🔒 Locks for ${alarm.lockDurationMinutes} min on stop"
            holder.card.setBackgroundResource(R.drawable.bg_card_primary)
            holder.tvTime.setTextColor(ctx.getColor(R.color.orange_primary))
        } else {
            holder.tvType.visibility     = View.GONE
            holder.tvLockInfo.visibility = View.GONE
            holder.card.setBackgroundResource(R.drawable.bg_card_normal)
            holder.tvTime.setTextColor(ctx.getColor(R.color.text_primary))
        }

        // Prevent toggle listener firing during bind
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = alarm.isEnabled
        holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(alarm, checked) }

        holder.itemView.setOnClickListener { onEdit(alarm) }
        holder.btnDelete.setOnClickListener { onDelete(alarm) }
    }

    override fun getItemCount() = alarms.size
}

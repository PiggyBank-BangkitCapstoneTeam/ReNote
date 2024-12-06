package com.piggybank.renote.ui.Akun

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.piggybank.renote.R
import com.piggybank.renote.util.NotificationReceiver
import java.util.Calendar
import java.util.Locale

class NotifikasiFragment : Fragment() {
    private lateinit var timeText: TextView
    private lateinit var messageInput: EditText
    private var selectedHour: Int = -1
    private var selectedMinute: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifikasi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.GONE

        timeText = view.findViewById(R.id.timeText)
        messageInput = view.findViewById(R.id.messageInput)
        val setAlarmButton = view.findViewById<Button>(R.id.setAlarmButton)
        val cancelAlarmButton = view.findViewById<Button>(R.id.cancelAlarmButton)

        val (savedHour, savedMinute, savedMessage) = loadNotificationData()
        if (savedHour != -1 && savedMinute != -1 && savedMessage != null) {
            selectedHour = savedHour
            selectedMinute = savedMinute
            timeText.text = String.format(Locale.getDefault(), "%02d:%02d", savedHour, savedMinute)
            messageInput.setText(savedMessage)
        }

        timeText.setOnClickListener {
            showTimePickerDialog()
        }

        setAlarmButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (selectedHour == -1 || selectedMinute == -1) {
                Toast.makeText(requireContext(), "Harap atur waktu terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi pesan notifikasi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scheduleNotification(message)
        }

        cancelAlarmButton.setOnClickListener {
            cancelNotification()
        }

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.VISIBLE
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, hourOfDay, minuteOfHour ->
            selectedHour = hourOfDay
            selectedMinute = minuteOfHour
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            timeText.text = formattedTime
            Toast.makeText(requireContext(), "Waktu dipilih: $formattedTime", Toast.LENGTH_SHORT).show()
        }, hour, minute, true).show()
    }

    private fun scheduleNotification(message: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        saveNotificationData(selectedHour, selectedMinute, message)

        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("TITLE", "ReNote")
            putExtra("MESSAGE", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Gunakan setRepeating untuk menjadwalkan alarm harian
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Toast.makeText(requireContext(), "Notifikasi telah dijadwalkan setiap hari", Toast.LENGTH_SHORT).show()
    }

    private fun cancelNotification() {
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Toast.makeText(requireContext(), "Notifikasi telah dibatalkan", Toast.LENGTH_SHORT).show()
    }

    private fun saveNotificationData(hour: Int, minute: Int, message: String) {
        val sharedPreferences = requireContext().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putInt("HOUR", hour)
            putInt("MINUTE", minute)
            putString("MESSAGE", message)
            apply()
        }
    }

    private fun loadNotificationData(): Triple<Int, Int, String?> {
        val sharedPreferences = requireContext().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val hour = sharedPreferences.getInt("HOUR", -1)
        val minute = sharedPreferences.getInt("MINUTE", -1)
        val message = sharedPreferences.getString("MESSAGE", null)
        return Triple(hour, minute, message)
    }
}

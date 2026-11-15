package org.fossify.calendar.dialogs

import android.app.DatePickerDialog
import androidx.appcompat.app.AlertDialog
import org.fossify.calendar.R
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.databinding.DialogDayLabelBinding
import org.fossify.calendar.extensions.config
import org.fossify.calendar.helpers.DayLabels
import org.fossify.calendar.models.DayLabel
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getDatePickerDialogTheme
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.getJavaDayOfWeekFromISO
import java.time.LocalDate

class EditDayLabelDialog(
    private val activity: SimpleActivity,
    label: DayLabel?,
    private val callback: (label: DayLabel) -> Unit
) {
    private val binding by activity.viewBinding(DialogDayLabelBinding::inflate)
    private var startDate = label?.let { DayLabels.parseDayCode(it.startDayCode) } ?: LocalDate.now()

    init {
        binding.dayLabelTitle.setText(label?.title.orEmpty())
        binding.dayLabelStart.setOnClickListener { pickStartDate() }
        updateStartDate()

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = if (label == null) R.string.add_day_label else R.string.edit_day_label
                ) { alertDialog ->
                    alertDialog.showKeyboard(binding.dayLabelTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = binding.dayLabelTitle.value.trim()
                        if (title.isEmpty()) {
                            activity.toast(R.string.title_empty)
                        } else {
                            callback(DayLabel(title, DayLabels.toDayCode(startDate)))
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun updateStartDate() {
        binding.dayLabelStart.text = DayLabels.formatDate(activity.resources, startDate)
    }

    private fun pickStartDate() {
        activity.hideKeyboard()
        DatePickerDialog(
            activity,
            activity.getDatePickerDialogTheme(),
            { _, year, month, dayOfMonth ->
                startDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateStartDate()
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        ).apply {
            datePicker.firstDayOfWeek = getJavaDayOfWeekFromISO(activity.config.firstDayOfWeek)
            show()
        }
    }
}

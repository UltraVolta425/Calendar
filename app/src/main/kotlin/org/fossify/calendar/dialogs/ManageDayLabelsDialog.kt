package org.fossify.calendar.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.calendar.R
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.databinding.DialogManageDayLabelsBinding
import org.fossify.calendar.databinding.ItemDayLabelBinding
import org.fossify.calendar.extensions.config
import org.fossify.calendar.helpers.DayLabels
import org.fossify.calendar.models.DayLabel
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.viewBinding

class ManageDayLabelsDialog(
    private val activity: SimpleActivity,
    private val callback: () -> Unit
) {
    private val binding by activity.viewBinding(DialogManageDayLabelsBinding::inflate)
    private val labels = activity.config.dayLabels.toMutableList()

    init {
        updateList()
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
                activity.config.dayLabels = labels
                callback()
            }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .setNeutralButton(R.string.add_day_label, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.day_labels) { alertDialog ->
                    // set here instead of on the builder, so adding a label does not close this dialog
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        EditDayLabelDialog(activity, null) {
                            labels.add(it)
                            updateList()
                        }
                    }
                }
            }
    }

    private fun updateList() {
        val textColor = activity.getProperTextColor()
        binding.dayLabelsPlaceholder.beVisibleIf(labels.isEmpty())
        binding.dayLabelsList.removeAllViews()
        labels.forEachIndexed { index, label ->
            addLabel(index, label, textColor)
        }
    }

    private fun addLabel(index: Int, label: DayLabel, textColor: Int) {
        val item = ItemDayLabelBinding.inflate(activity.layoutInflater, binding.dayLabelsList, false)
        item.dayLabelTitle.apply {
            text = label.title
            setTextColor(textColor)
        }

        item.dayLabelStart.apply {
            text = DayLabels.parseDayCode(label.startDayCode)?.let { DayLabels.formatDate(resources, it) }
            setTextColor(textColor)
        }

        item.dayLabelDelete.apply {
            applyColorFilter(textColor)
            setOnClickListener {
                labels.removeAt(index)
                updateList()
            }
        }

        item.dayLabelHolder.setOnClickListener {
            EditDayLabelDialog(activity, label) {
                labels[index] = it
                updateList()
            }
        }

        binding.dayLabelsList.addView(item.root)
    }
}

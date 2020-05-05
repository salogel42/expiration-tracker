package com.example.expirationtracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker
        val c = Calendar.getInstance()
        var year = c.get(Calendar.YEAR)
        var month = c.get(Calendar.MONTH)
        var day = c.get(Calendar.DAY_OF_MONTH)
        // but if bundle contains a date, use it instead
        arguments?.let {
            if (it.containsKey(DEFAULT_DATE)) {
                val date = Date(it.getString(DEFAULT_DATE))
                year = date.year
                month = date.month
                day = date.day
            }
        }

        // Create a new instance of DatePickerDialog and return it
        return activity?.let { DatePickerDialog(it, this, year, month, day) } as Dialog
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        // Do something with the date chosen by the user
    }

    val DEFAULT_DATE = "default date"
}
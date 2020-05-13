package com.example.expirationtracker.ui.items

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.expirationtracker.R
import com.example.expirationtracker.data.Items
import com.google.firebase.Timestamp
import kotlinx.android.synthetic.main.activity_expiration_setting.*
import java.io.File
import java.util.*


class ExpirationSettingActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_expiration_setting)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent.extras?.let {
            if (it.containsKey(ARG_IMAGE_FILENAME)) {
                val fileName = it.getString(ARG_IMAGE_FILENAME)
                val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val fullPath = "${storageDir}/${fileName}"
                val uri = Uri.parse(fullPath)
                expImage.setImageURI(uri)
            }
            if (it.containsKey(ARG_RESULT_TEXT)) {
                resultText.setText(it.getString(ARG_RESULT_TEXT))
            }
        }


        ItemDetailActivity.setupDatePicker(this, Timestamp(Calendar.getInstance().time), expDate)

        submitButton.setOnClickListener {
            val dataIntent = Intent()
            Log.d(TAG, "in submit listener text of expDate ${expDate.text.toString()}")
            dataIntent.putExtra(ARG_EXPIRATION_DATE, expDate.text.toString())
            setResult(Activity.RESULT_OK, dataIntent)
            finish()
        }
    }


    companion object {
        val ARG_RESULT_TEXT = "result_text"
        val ARG_IMAGE_FILENAME = "image_filename"
        val ARG_EXPIRATION_DATE = "expiration_date"
        val TAG = "ExpirationSettingActivity"
    }
}

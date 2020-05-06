package com.example.expirationtracker.ui.items

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.expirationtracker.DatePickerFragment
import com.example.expirationtracker.R
import com.example.expirationtracker.dummy.Items
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * An activity representing a single Item detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [ItemListFragment].
 */
class ItemDetailActivity : AppCompatActivity() {

    private val TAG = "ItemDetailActivity"

    val REQUEST_TAKE_PHOTO = 1
    lateinit var currentPhotoPath: String

    lateinit var edtTitle: EditText
    lateinit var edtDescription: EditText
    lateinit var btAdd: Button

    lateinit private var firestoreDB: FirebaseFirestore
    lateinit private var id: String
    lateinit private var item: Items.ExpirableItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_item_detail)
        setSupportActionBar(detail_toolbar)

        addItemButton.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own detail action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edtDescription = item_detail.edtDescription
        btAdd = item_detail.btAdd

        firestoreDB = FirebaseFirestore.getInstance()
        savedInstanceState?.let {
            Log.d(TAG, "got bundle: " + it)
            if (it.containsKey(ARG_ITEM_ID)) {
                item = Items.ITEM_MAP[it.getString(ARG_ITEM_ID)]!!
                // load the rest of the data
                toolbar_layout?.title = item?.name
            }
        }

        btAdd.setOnClickListener {
            Log.d(TAG, "should save it off now")
//            val title = edtTitle.text.toString()
//            val content: String = edtContent.getText().toString()
//            if (title.length > 0) {
//                if (id.length > 0) {
//                    updateItem(id, title, content)
//                } else {
//                    createItem(title, content)
//                }
//            }
//            finish()
        }
//        Bundle().apply {
//            id = getString("UpdateNoteId")!!
//            edtTitle.setText(getString("UpdateNoteTitle"))
//            edtDescription.setText(getString("UpdateNoteContent"))
//        }

    }

    //
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//
//    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                navigateUpTo(Intent(this, ItemListFragment::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    fun showDatePickerDialog(v: View) {
        val newFragment = DatePickerFragment()
        newFragment.show(supportFragmentManager, "datePicker")
    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    print("Error creating file")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.expirationtracker.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }



    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // TODO: get storage stuff working
    // "gs://expiration-images"
    fun uploadImageToCloudStorage(url: String) {
        // TODO(sdspikes): get this working -- Firebase Console won't let me enable Firebase Storage for some reason, try again tomorrow
        // TODO(sdspikes): don't hard-code this bucket
        val storage = Firebase.storage(url)
        val storageRef = storage.reference

        // Create a reference to our new image
        var file = Uri.fromFile(File(currentPhotoPath))
        val photoRef = storageRef.child("${file.lastPathSegment}")
        var uploadTask = photoRef.putFile(file)

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            print("no worky")
        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // TODO(maybe grab the corresponding text from expiration-text?)
            print("workeed")
        }
    }

    fun getExpirationImage(view: View) {
        dispatchTakePictureIntent()
//        val getContent = registerForActivityResult(GetContent()) { uri: Uri? ->
//            // Handle the returned Uri
//        }
        Log.d(TAG, "took pic, at ${currentPhotoPath}")
        // TODO: kick off OCR
    }
    fun getItemImage(view: View) {
        dispatchTakePictureIntent()
        Log.d(TAG, "took pic, at ${currentPhotoPath}")
        // TODO: store it somewhere, also show thumbnail/preview on the details page, click through for full sized?
    }
    fun scanBarcode(view: View) {
        dispatchTakePictureIntent()
        Log.d(TAG, "took pic, at ${currentPhotoPath}")
        // TODO: maybe the live barcode thingy instead of taking an image -- extract barcode to store
        // TODO: later, look up barcode in product api
    }
}

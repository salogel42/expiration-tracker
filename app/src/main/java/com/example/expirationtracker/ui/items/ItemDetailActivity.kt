package com.example.expirationtracker.ui.items

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.expirationtracker.R
import com.example.expirationtracker.data.Items
import com.example.expirationtracker.databinding.FragmentItemDetailBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_item_detail.*
import java.io.File
import java.io.IOException
import java.lang.Math.E
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

    val TAKE_EXPIRATION_PHOTO = 1
    val TAKE_ITEM_PHOTO = 2
    val TAKE_BARCODE_PHOTO = 3
    lateinit var currentPhotoPath: String


    lateinit private var firestoreDB: FirebaseFirestore
    private var currentImageFilename: String = ""
    private var itemImageFilename: String = ""
    private var id = ""
    private var item = Items.ExpirableItem()
    lateinit var binding:FragmentItemDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        TODO(maybe): set up actual 2-way databinding
        setContentView(R.layout.fragment_item_detail)
        setSupportActionBar(detail_toolbar)

        var bundle = intent.extras
        bundle?.let {
            Log.d(TAG, "got bundle: " + it)
            if (it.containsKey(ARG_ITEM_ID)) {

                // Grab the id and get the corresponding object to fill the fields
                id = it.getString(ARG_ITEM_ID).toString()
                item = Items.ITEM_MAP[id]!!

                edtName.setText(item.name)
                edtNotes.setText(item.notes)
                loadImage(item.imageFilename)

                toolbar_layout?.title = "Item Details"
            }
        }

        setupDatePicker(item.expirationDate, expDate)
        setupDatePicker(item.notificationDate, notifDate)

        firestoreDB = FirebaseFirestore.getInstance()

        addItemButton.setOnClickListener {
            item.name = edtName.text.toString()
            item.notes = edtNotes.text.toString()
            item.expirationDate = Timestamp(Date(expDate.text.toString()))
            item.notificationDate = Timestamp(Date(notifDate.text.toString()))
            item.imageFilename = itemImageFilename
            item.barcode = barcodeText.text.toString()

            if (id == "") {
                firestoreDB.collection("items")
                    .add(item)
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot successfully written!")
                        Toast.makeText(this,  "New item saved", Toast.LENGTH_LONG)
                    }
                    .addOnFailureListener {
                        e -> Log.w(TAG, "Error writing document", e)
                        Toast.makeText(this,  "New item wasn't saved", Toast.LENGTH_LONG)
                    }
            } else {
                firestoreDB.collection("items")
                    .document(id)
                    .set(item)
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot successfully updated!")
                        Toast.makeText(this,  "Item updated!", Toast.LENGTH_LONG)
                    }
                    .addOnFailureListener {
                            e -> Log.w(TAG, "Error writing document", e)
                        Toast.makeText(this,  "Updated item wasn't saved", Toast.LENGTH_LONG)
                    }
            }
            finish()
        }
    }

    private fun setupDatePicker(ts : Timestamp, text: EditText) {
        text.setText(Items.getShortDate(ts.toDate()))
        text.setOnClickListener {
            var c = Calendar.getInstance()
            c.time = Date(text.text.toString())
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, y, m, d ->
                c.set(y, m, d)
                text.setText(Items.getShortDate(Date(c.timeInMillis)))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

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

    private fun loadImage(fileName: String) {
        if (fileName == "") return

        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val fullPath = "${storageDir}/${fileName}"
        val uri = Uri.parse(fullPath)

        var file = File(fullPath);
        if(file.exists())
            itemImage.setImageURI(uri)
        else {
            // File doesn't exist locally, so we need to pull it from the Cloud Storage bucket
            // TODO: move storage url to a config/env file
            val storage = Firebase.storage("gs://expiration-item-images")
            val storageRef = storage.reference
            val photoRef = storageRef.child(fileName)
            val localFile = File(storageDir, fileName)
            photoRef.getFile(localFile).addOnSuccessListener {
                itemImage.setImageURI(uri)
            }
        }

    }

    private fun dispatchTakePictureIntent(action: Int) {
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
                    startActivityForResult(takePictureIntent, action)
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

    fun uploadImageToCloudStorage(url: String, fileName: String, uri: Uri) {
        val storage = Firebase.storage(url)
        val storageRef = storage.reference

        // Create a reference to our new image
        val photoRef = storageRef.child(fileName)
        var uploadTask = photoRef.putFile(uri)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Intent not ok")
            return
        }
        var uri = Uri.fromFile(File(currentPhotoPath))
        currentImageFilename = uri.lastPathSegment.toString()
        Log.d(TAG, "took image, Uri: ${uri}")
        if (requestCode == TAKE_EXPIRATION_PHOTO) {
            // TODO: Look up best practice for where to put the cloud storage strings
            //       - in config files? constants? strings.xml?
            uploadImageToCloudStorage("gs://expiration-ocr-images", currentImageFilename, uri)
            // TODO: kick off OCR
            return
        }
        if (requestCode == TAKE_ITEM_PHOTO) {
            uploadImageToCloudStorage("gs://expiration-item-images", currentImageFilename, uri)
            itemImageFilename = currentImageFilename
            itemImage.setImageURI(uri)
        }
        if (requestCode == TAKE_BARCODE_PHOTO) {
            val image: FirebaseVisionImage
            try {
                image = FirebaseVisionImage.fromFilePath(this, uri)
                val detector = FirebaseVision.getInstance()
                    .visionBarcodeDetector

                val result = detector.detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        // TODO: if multiple, ask user which to use?
                        for (barcode in barcodes) {
                            Log.d(TAG, "got barcode: $barcode.rawValue" )
                            barcodeText.text = barcode.rawValue
                            // TODO: look up barcode in barcodelookup api
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "barcode ml failed $it" )
                    }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    fun getExpirationImage(view: View) {
        dispatchTakePictureIntent(TAKE_EXPIRATION_PHOTO)
    }
    fun getItemImage(view: View) {
        dispatchTakePictureIntent(TAKE_ITEM_PHOTO)
    }
    fun scanBarcode(view: View) {
        // TODO: maybe the live barcode thingy instead of taking an image -- extract barcode to store
        dispatchTakePictureIntent(TAKE_BARCODE_PHOTO)
    }


}

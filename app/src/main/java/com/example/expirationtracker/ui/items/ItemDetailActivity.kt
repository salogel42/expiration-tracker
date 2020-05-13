package com.example.expirationtracker.ui.items

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.expirationtracker.R
import com.example.expirationtracker.data.Items
import com.example.expirationtracker.databinding.FragmentItemDetailBinding
import com.example.expirationtracker.ui.items.ExpirationSettingActivity.Companion.ARG_EXPIRATION_DATE
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.expDate
import kotlinx.android.synthetic.main.fragment_item_detail.itemImage
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.ParseException
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
    private val EXPIRATION_DATE_ACTIVITY = 4

    // TODO: find a way to convert date strings without manually specifying formats?
    val FORMAT_STRINGS = Arrays.asList("ddMMMy", "MMMddy", "M/d/y", "M-d-y", "M/y", "M-y", "yyyyMMdd");


    lateinit private var firestoreDB: FirebaseFirestore
    lateinit var currentPhotoPath: String
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

        intent.extras?.let {
            Log.d(TAG, "got bundle: " + it)
            if (it.containsKey(ARG_ITEM_ID)) {

                // Grab the id and get the corresponding object to fill the fields
                id = it.getString(ARG_ITEM_ID).toString()
                item = Items.ITEM_MAP[id]!!

                edtName.setText(item.name)
                edtNotes.setText(item.notes)
                loadImage(item.imageFilename)
                barcodeText.setText(item.barcode)
                if (item.productLink != "") {
                    productLink.setText(item.productLink)
                    productLink.visibility = VISIBLE
                }

                toolbar_layout?.title = "Item Details"
            }
        }

        setupDatePicker(this, item.expirationDate, expDate)
        setupDatePicker(this, item.notificationDate, notifDate)

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



    companion object {
        fun setupDatePicker(context: Context, ts : Timestamp, text: EditText) {
            text.setText(Items.getShortDate(ts.toDate()))
            text.setOnClickListener {
                var c = Calendar.getInstance()
                c.time = Date(text.text.toString())
                DatePickerDialog(context, DatePickerDialog.OnDateSetListener { _, y, m, d ->
                    c.set(y, m, d)
                    text.setText(Items.getShortDate(Date(c.timeInMillis)))
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

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

    fun uploadImageToCloudStorage(url: String, fileName: String, uri: Uri): UploadTask {
        val storage = Firebase.storage(url)
        val storageRef = storage.reference

        // Create a reference to our new image
        val photoRef = storageRef.child(fileName)
        return photoRef.putFile(uri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "in onActivityResult, with extras: ${data?.extras}")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Intent not ok")
            return
        }
        if (requestCode == EXPIRATION_DATE_ACTIVITY) {
            Log.d(TAG, "got extras: ${data?.extras}")
            data?.extras?.let {
                if (it.containsKey(ARG_EXPIRATION_DATE)) {
                    val date = it.get(ARG_EXPIRATION_DATE).toString()
                    expDate.setText(date)
                }
            }
            // todo: how get info out?
            return
        }
        if (requestCode != TAKE_EXPIRATION_PHOTO && requestCode != TAKE_ITEM_PHOTO && requestCode != TAKE_BARCODE_PHOTO) {
            // for any other intents, don't do stuff with the image file
            return
        }
        // TODO: save image to gallery?
        var uri = Uri.fromFile(File(currentPhotoPath))
        val currentImageFilename = uri.lastPathSegment.toString()
        Log.d(TAG, "took image, Uri: ${uri}")
        if (requestCode == TAKE_ITEM_PHOTO) {
            val uploadTask = uploadImageToCloudStorage("gs://expiration-item-images", currentImageFilename, uri)
            uploadTask.addOnSuccessListener {
                itemImageFilename = currentImageFilename
                itemImage.setImageURI(uri)
            }
            return
        }
        // needed by both expiration and barcode
        val image: FirebaseVisionImage
        try {
            image = FirebaseVisionImage.fromFilePath(this, uri)

            if (requestCode == TAKE_EXPIRATION_PHOTO) {
                // TODO: Look up best practice for where to put the cloud storage strings
                //       - in config files? constants? strings.xml?
                val uploadTask = uploadImageToCloudStorage("gs://expiration-ocr-images", currentImageFilename, uri)
                // Register observers to listen for when the download is done or if it fails
                uploadTask.addOnFailureListener {
                    Log.e(TAG, "$it")
                }.addOnSuccessListener {
                    // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                    // TODO: kick off OCR
                    Log.e(TAG, "upload succeeded")
                }

//                val detector = FirebaseVision.getInstance()
//                    .onDeviceTextRecognizer
                Log.d(TAG, "about to start OCR")
                val detector = FirebaseVision.getInstance().cloudTextRecognizer
                val result = detector.processImage(image)
                    .addOnSuccessListener { result ->
                        Log.d(TAG, "did OCR, got ${result}")
                        val d = getDateFromResult(result)
                        if (d == null) {
                            val intent = Intent(this, ExpirationSettingActivity::class.java).apply {
                                Log.d(ContentValues.TAG, "sending extras to intent (image filename ${currentImageFilename}, result text: ${result.text})")
                                putExtra(ExpirationSettingActivity.ARG_IMAGE_FILENAME, currentImageFilename)
                                putExtra(ExpirationSettingActivity.ARG_RESULT_TEXT, result.text)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivityForResult(intent, EXPIRATION_DATE_ACTIVITY)

                        } else {
                            expDate.setText(Items.getShortDate(d))
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "$e")
                        // Task failed with an exception
                        // ...
                    }
            }
            if (requestCode == TAKE_BARCODE_PHOTO) {
                val detector = FirebaseVision.getInstance()
                    .visionBarcodeDetector

                val result = detector.detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        // TODO: if multiple, ask user which to use?
                        for (barcode in barcodes) {
                            Log.d(TAG, "got barcode: ${barcode.rawValue}" )
//                            item.barcode = barcode.rawValue.toString()
                            barcodeText.setText(barcode.rawValue.toString())

                            val queue = Volley.newRequestQueue(this)
                            val url = "https://api.barcodelookup.com/v2/products?barcode=${barcode.rawValue}&key=4c4h7t9vq9cx6znp12q980h55mkpr7"

                            item.productLink = "https://www.barcodelookup.com/${barcode.rawValue}"
                            productLink.text = item.productLink
                            productLink.visibility = VISIBLE

                            // Request a string response from the provided URL.
                            val stringRequest = StringRequest(
                                Request.Method.GET, url,
                                com.android.volley.Response.Listener<String> { response ->
                                    // Display the first 500 characters of the response string.
                                    Log.d(TAG, "from barcodelookup: $response")
                                    var json = JSONObject(response)
                                    var product = json.getJSONArray("products").getJSONObject(0)
                                    item.productName = product.get("product_name").toString()

                                    if (edtNotes.text.toString() == "")
                                        edtNotes.setText(product.get("description").toString())

                                    if (edtName.text.toString() == "")
                                        edtName.setText(item.productName)
                                },
                                com.android.volley.Response.ErrorListener { })

                            // Add the request to the RequestQueue.
                            queue.add(stringRequest)
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "barcode ml failed $it" )
                    }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun getDateFromResult(result: FirebaseVisionText) : Date? {
        val resultText = result.text
        Log.d(TAG, "did OCR, got result ${resultText}")
        var date = tryParse(resultText)

        if (date != null) return date
        for (block in result.textBlocks) {
            date = tryParse(block.text)
            if (date != null) return date
            for (line in block.lines) {
                val lineText = line.text
                date = tryParse(lineText)
                if (date != null) return date
                Log.d(TAG, "did OCR, got lineText ${lineText}")
            }
        }
        return tryParse(resultText.replace("(.*)BY ?".toRegex(), ""))
    }

    private fun tryParse(dateString: String) : Date?
    {
        for (formatString in FORMAT_STRINGS)
        {
            try
            {
                var date = SimpleDateFormat(formatString).parse(dateString);
                Log.d(TAG, "tryParse succeeded at to parsing ${dateString} with format ${formatString}: $date")
                return date
            }
            catch (e: ParseException) {
                Log.d(TAG, "tryParse failed to parse ${dateString} with format ${formatString}")
            }
        }

        return null;
    }

    // TODO: allow to choose image from gallery?
    fun getExpirationImage(view: View) {
        dispatchTakePictureIntent(TAKE_EXPIRATION_PHOTO)
    }
    fun getItemImage(view: View) {
        dispatchTakePictureIntent(TAKE_ITEM_PHOTO)
    }
    fun scanBarcode(view: View) {
        // TODO: maybe the live barcode thingy instead of taking an image
        dispatchTakePictureIntent(TAKE_BARCODE_PHOTO)
    }


}

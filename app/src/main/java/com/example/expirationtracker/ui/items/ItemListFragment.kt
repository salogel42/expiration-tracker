package com.example.expirationtracker.ui.items

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expirationtracker.R
import com.example.expirationtracker.dummy.ItemContent
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_list_content.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListFragment : Fragment() {

    val REQUEST_TAKE_PHOTO = 1
    lateinit var currentPhotoPath: String
    lateinit var mAdapter: SimpleItemRecyclerViewAdapter
    lateinit var firestoreDB: FirebaseFirestore

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(activity?.packageManager!!)?.also {
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
                        requireActivity(),
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
        val storageDir: File = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun setupAddItemButton(root: View) {
        val fab: FloatingActionButton = root.findViewById(R.id.addItemButton)
        fab.setOnClickListener { view ->
            dispatchTakePictureIntent()

            // Create a storage reference from our app
            // TODO(sdspikes): get this working -- Firebase Console won't let me enable Firebase Storage for some reason, try again tomorrow
            // TODO(sdspikes): don't hard-code this bucket
            val storage = Firebase.storage("gs://expiration-images")
            val storageRef = storage.reference

            // Create a reference to "mountains.jpg"
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

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_item_list, container, false)
        setupAddItemButton(root)
        setupRecyclerView(root)
        return root
    }
    lateinit var firestoreListener: ListenerRegistration
    lateinit var recyclerView: RecyclerView

    private fun timestampToDate(timestamp: Any?) : Date{
        val firebaseTimestamp = timestamp as com.google.firebase.Timestamp
        return firebaseTimestamp.toDate()
    }
    private fun setupRecyclerView(v: View) {
        recyclerView = v.findViewById(R.id.item_list)

        firestoreDB = FirebaseFirestore.getInstance()

        getExistingItems(v)

        firestoreListener = firestoreDB.collection("notes")
            .addSnapshotListener { documentSnapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed!", e)
                    return@addSnapshotListener
                }
                val items: MutableList<ItemContent.ExpirableItem> = ArrayList()
                for (doc in documentSnapshots!!) {
                    items.add(ItemContent.ExpirableItem(doc.id, doc.data, timestampToDate(doc.data["expirationDate"])))
                }
                mAdapter = SimpleItemRecyclerViewAdapter(items, firestoreDB)
                recyclerView.setAdapter(mAdapter)
            }

    }

    private fun getExistingItems(v:View) {
        firestoreDB.collection("items")
            .get()
            .addOnSuccessListener { result ->
                Log.d(ContentValues.TAG, "got Items from firestore ${result}")
                var items = ArrayList<ItemContent.ExpirableItem>()
                for (document in result) {
                    Log.d(ContentValues.TAG, "got item ${document} => ${document.data}")
                    items.add(ItemContent.ExpirableItem(document.id, document.data, timestampToDate(document.data["expirationDate"])))
                }

                mAdapter = SimpleItemRecyclerViewAdapter(items, firestoreDB)
                val mLayoutManager: RecyclerView.LayoutManager =
                    LinearLayoutManager(v.context)
                recyclerView.layoutManager = mLayoutManager
                recyclerView.itemAnimator = DefaultItemAnimator()
                recyclerView.adapter = mAdapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents.", exception)
            }

    }

    class SimpleItemRecyclerViewAdapter(
        private val values: MutableList<ItemContent.ExpirableItem>,
        private val firestoreDB: FirebaseFirestore
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

         override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.expirationView.text = ItemContent.getShortDate(item.expirationDate)
            holder.contentView.text = item.name

            with(holder.itemView) {
                setOnClickListener(View.OnClickListener { v ->
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                })
            }

            holder.editView.setOnClickListener(View.OnClickListener {
                updateItem(it, item) }
            )

            holder.deleteView.setOnClickListener(View.OnClickListener {
                deleteItem(
                    it,
                    item.id,
                    position
                )
            })
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val expirationView: TextView = view.expirationDate
            val contentView: TextView = view.content
            val editView: TextView = view.edit
            val deleteView: TextView = view.delete
        }

        private fun updateItem(v: View, item: ItemContent.ExpirableItem) {
            val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // TODO: add other fields?
                putExtra("UpdateId", item.id)
                putExtra("UpdateName", item.name)
                putExtra("UpdateDescription", item.description)
            }
            v.context.startActivity(intent)
        }
        private fun deleteItem(v: View, id: String, position: Int) {
            firestoreDB.collection("items")
                .document(id)
                .delete()
                .addOnCompleteListener {
                    values.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, values.size)
                    Toast.makeText(v.context, "Item has been deleted!", Toast.LENGTH_SHORT).show()
                }
        }

    }
}

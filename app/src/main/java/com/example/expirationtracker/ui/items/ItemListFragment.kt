package com.example.expirationtracker.ui.items

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
import com.example.expirationtracker.dummy.Items
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


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListFragment : Fragment() {

    lateinit var mAdapter: SimpleItemRecyclerViewAdapter
    lateinit var firestoreDB: FirebaseFirestore



    fun setupAddItemButton(root: View) {
        val fab: FloatingActionButton = root.findViewById(R.id.addItemButton)
        fab.setOnClickListener { v ->
            val intent = Intent(v.context, ItemDetailActivity::class.java)
            v.context.startActivity(intent)
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

        Log.d(TAG, "about to call getExistingItems")
        Items.getExistingItems()

        mAdapter = SimpleItemRecyclerViewAdapter(firestoreDB)
        val mLayoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(v.context)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = mAdapter


        firestoreListener = firestoreDB.collection("items")
            .addSnapshotListener { documentSnapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed!", e)
                    return@addSnapshotListener
                }
                Items.clearAll()
                for (doc in documentSnapshots!!) {
                    Items.addItem(Items.ExpirableItem(doc.id, doc.data, timestampToDate(doc.data["expirationDate"])))
                }
                mAdapter = SimpleItemRecyclerViewAdapter(firestoreDB)
                recyclerView.setAdapter(mAdapter)
            }

    }

    class SimpleItemRecyclerViewAdapter(
        private val firestoreDB: FirebaseFirestore
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

         override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = Items.ITEMS[position]
            holder.expirationView.text = Items.getShortDate(item.expirationDate)
            holder.contentView.text = item.name

            with(holder.itemView) {
                setOnClickListener(View.OnClickListener { v ->
                    updateItem(v, item)
//                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
//                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
//                    }
//                    v.context.startActivity(intent)
                })
            }

//            holder.editView.setOnClickListener(View.OnClickListener {
//                updateItem(it, item) }
//            )

            holder.deleteView.setOnClickListener(View.OnClickListener {
                deleteItem(
                    it,
                    item.id,
                    position
                )
            })
        }

        override fun getItemCount() = Items.size()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val expirationView: TextView = view.expirationDate
            val contentView: TextView = view.content
            val deleteView: TextView = view.delete
        }

        private fun updateItem(v: View, item: Items.ExpirableItem) {
            val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                putExtra(ItemDetailActivity.ARG_ITEM_ID, item.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            v.context.startActivity(intent)
        }
        private fun deleteItem(v: View, id: String, position: Int) {
            firestoreDB.collection("items")
                .document(id)
                .delete()
                .addOnCompleteListener {
                    Items.removeItem(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, Items.size())
                    Toast.makeText(v.context, "Item has been deleted!", Toast.LENGTH_SHORT).show()
                }
        }

    }
}

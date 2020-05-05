package com.example.expirationtracker.ui.items

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.expirationtracker.DatePickerFragment
import com.example.expirationtracker.R
import com.example.expirationtracker.dummy.Items
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*


/**
 * An activity representing a single Item detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [ItemListFragment].
 */
class ItemDetailActivity : AppCompatActivity() {

    private val TAG = "ItemDetailActivity"

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

    fun getExpirationImage(view: View) {}
    fun getItemImage(view: View) {}
    fun scanBarcode(view: View) {}
}

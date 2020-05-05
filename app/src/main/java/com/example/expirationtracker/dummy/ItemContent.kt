package com.example.expirationtracker.dummy

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.sql.Types.TIMESTAMP
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 */
object ItemContent {

    /**
     * An array of items.
     */
    val ITEMS: MutableList<ExpirableItem> = ArrayList()

    /**
     * A map of items, by ID.
     */
    val ITEM_MAP: MutableMap<String, ExpirableItem> = HashMap()

    private val COUNT = 25

    //pull items from cloud firestore
    fun getItemsFromFirestore() {
        val db = Firebase.firestore
        Log.d(TAG, "getting Items from firestore")
        db.collection("items")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "got Items from firestore ${result}")
                for (document in result) {
                    Log.d(TAG, "got item ${document} => ${document.data}")
                    val timestamp = document.data["expirationDate"] as com.google.firebase.Timestamp
                    val date = timestamp.toDate()
                    addItem(ExpirableItem(document.id, document.data, date))
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }
    init {
        getItemsFromFirestore()
    }

    private fun addItem(item: ExpirableItem) {
        ITEMS.add(item)
        Log.d(TAG, "item created ${item.name} => ${item.id}")
        ITEM_MAP.put(item.id, item)
    }


    fun getShortDate(date: Date) : String {
        val sdf = SimpleDateFormat("MMM dd, yyyy")
        return sdf.format(date)
    }
    /**
     * An object representing an item that will expire.
     */
    data class ExpirableItem(val id: String, val name: String, val description: String, val imageFilename:String, val textFilename:String, val expirationDate:Date) {

        constructor(id: String, data: Map<String, Any>, expDate: Date) : this(
            id,
            data["name"].toString(),
            data["description"].toString(),
            data["imageFilename"].toString(),
            data["textFilename"].toString(),
            expDate
        )

        override fun toString(): String = name
    }
}

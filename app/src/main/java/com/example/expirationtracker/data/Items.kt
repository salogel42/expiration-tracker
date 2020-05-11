package com.example.expirationtracker.data

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 */
object Items {

    /**
     * An array of items.
     */
    val ITEMS: MutableList<ExpirableItem> = ArrayList()

    /**
     * A map of items, by ID.
     */
    val ITEM_MAP: MutableMap<String, ExpirableItem> = HashMap()

    private val COUNT = 25

    fun timestampToDate(timestamp: Any?) : Date{
        if (timestamp == null) return Calendar.getInstance().time
        val firebaseTimestamp = timestamp as com.google.firebase.Timestamp
        return firebaseTimestamp.toDate()
    }

    fun dateToTimestampString(date: Date) : String {
        return com.google.firebase.Timestamp(date).toString()
    }

    //pull items from cloud firestore
    fun getExistingItems() {
        Log.d(TAG, "inside getExistingItems")

        val db = Firebase.firestore
        Log.d(TAG, "getting Items from firestore")
        db.collection("items")
            .get()
            .addOnSuccessListener {
                it.documents.forEach {
                    var item = it.toObject<ExpirableItem>()!!
                    item.id = it.id
                    addItem(item)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    fun clearAll() {
        ITEMS.clear()
        ITEM_MAP.clear()
    }

    fun addItem(item: ExpirableItem) {
        ITEMS.add(item)
        Log.d(TAG, "item created ${item.name} => ${item.id}")
        ITEM_MAP.put(item.id, item)
    }

    fun removeItem(item: ExpirableItem) {
        ITEMS.remove(item)
        ITEM_MAP.remove(item.id)
    }

    fun size() : Int {
        return ITEMS.size
    }

    fun removeItem(position: Int) {
        ITEMS.removeAt(position)
        ITEM_MAP.remove(
            ITEMS[position].id)
    }

    fun getShortDate(date: Date) : String {
        val sdf = SimpleDateFormat("MMM dd, yyyy")
        return sdf.format(date)
    }


    /**
     * An object representing an item that will expire.
     */
    data class ExpirableItem(
        var id: String = "",
        var name: String = "",
        var notes: String = "",
        var imageFilename: String = "",
        var textFilename: String = "",
        var notificationDate: Timestamp = Timestamp.now(),
        var expirationDate: Timestamp = Timestamp.now(),
        var barcode: String = "",
        var productName: String = "",
        var productLink: String = "",
        var userId: String? = Firebase.auth.currentUser?.uid
    ) {
        override fun toString(): String = name
    }
}

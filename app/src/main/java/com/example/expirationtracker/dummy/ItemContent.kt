package com.example.expirationtracker.dummy

import java.util.ArrayList
import java.util.HashMap

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

    init {
        //pull items from cloud firestore
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }

    private fun addItem(item: ExpirableItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    private fun createDummyItem(position: Int): ExpirableItem {
        return ExpirableItem(position.toString(), "Item " + position, makeDetails(position), expirationImageId = "e" + position, imageId = "1" + position)
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class ExpirableItem(val id: String, val name: String, val details: String, val expirationImageId:String, val imageId:String) {
        override fun toString(): String = name
    }
}

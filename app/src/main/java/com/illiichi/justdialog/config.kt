package com.illiichi.justdialog

import com.typesafe.config.ConfigFactory
import java.io.InputStream

data class Item(val title: String, val pages: List<String>){
    override fun toString(): String {
        return title; // cheat for ListView to display title
    }
}
data class Category(val name: String, val description: String, val items: List<Item>)


fun parseConfig(input: InputStream): List<Category> {
    val config = ConfigFactory.parseReader(input.bufferedReader())

    return config.getObjectList("menu").map { it.toConfig() }.map { categoryConfig ->
        Category(
                categoryConfig.getString("name"),
                categoryConfig.getString("description"),
                categoryConfig.getObjectList("items")
                        .map { it.toConfig() }.map {
                            Item(it.getString("title"),
                                    it.getStringList("pages"))
                        }
        )
    }
}
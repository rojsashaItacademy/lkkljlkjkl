package kg.nurik.poligonapp.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

fun Int.createMarkerIndex(): JsonElement {
    return JsonParser.parseString("{\"index\":$this}")
}

fun JsonElement.getMarkerIndex(): Int {
    return (this as JsonObject).getAsJsonPrimitive("index").toString().toInt()
}
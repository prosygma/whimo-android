/*
 * Copyright (c) 2025 EFI (https://efi.int/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.whimo.utils

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(Uri::class.java, UriAdapter())
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
    .create()

/** Stores a URI as a string while accepting records written before this adapter existed. */
class UriAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        if (value == null) out.nullValue() else out.value(value.toString())
    }

    override fun read(`in`: JsonReader): Uri? {
        val value = JsonParser.parseReader(`in`)
        if (value.isJsonNull) return null
        if (value.isJsonPrimitive) return Uri.parse(value.asString)

        // Old Gson versions reflected Uri.StringUri and Uri.HierarchicalUri fields.
        val obj = value.asJsonObject
        obj.get("uriString")?.takeIf { it.isJsonPrimitive }?.let { return Uri.parse(it.asString) }
        val scheme = component(obj.get("scheme"))
            ?: throw JsonParseException("Stored URI has no scheme")
        val authority = component(obj.get("authority"))
            ?: throw JsonParseException("Stored URI has no authority")
        val builder = Uri.Builder().scheme(scheme).encodedAuthority(authority)
        component(obj.get("path"))?.let(builder::encodedPath)
        component(obj.get("query"))?.let(builder::encodedQuery)
        component(obj.get("fragment"))?.let(builder::encodedFragment)
        return builder.build()
    }

    private fun component(value: JsonElement?): String? {
        if (value == null || value.isJsonNull) return null
        if (value.isJsonPrimitive) return value.asString
        if (!value.isJsonObject) return null
        val obj = value.asJsonObject
        return component(obj.get("encoded")) ?: component(obj.get("decoded"))
    }
}

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.format(formatter))
        }
    }

    override fun read(`in`: JsonReader): LocalDate? {
        return if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            val value = `in`.nextString()
            LocalDate.parse(value, formatter)
        }
    }
}

class LocalDateTimeAdapter : TypeAdapter<LocalDateTime>() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun write(out: JsonWriter, value: LocalDateTime?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.format(formatter))
        }
    }

    override fun read(`in`: JsonReader): LocalDateTime? {
        return if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            val value = `in`.nextString()
            LocalDateTime.parse(value, formatter)
        }
    }
}

inline fun <reified T> T.toJsonArgs(): String {
    return gson.toJson(this)
}

inline fun <reified T> String.fromJsonArgs(): T {
    return gson.fromJson(this, T::class.java)
}

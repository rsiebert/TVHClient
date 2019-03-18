@file:JvmName("SearchMenuUtils")

package org.tvheadend.tvhclient.ui.common

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.search.SearchActivity

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

@JvmOverloads
fun onMenuSelected(context: Context, menuItemId: Int, title: String?, channelId: Int = 0): Boolean {
    if (title == null || title.isEmpty()) {
        return false
    }
    when (menuItemId) {
        R.id.menu_search_imdb -> return searchImdbWebsite(context, title)
        R.id.menu_search_fileaffinity -> return searchFileAffinityWebsite(context, title)
        R.id.menu_search_youtube -> return searchYoutube(context, title)
        R.id.menu_search_google -> return searchGoogle(context, title)
        R.id.menu_search_epg -> return searchEpgSelection(context, title, channelId)
    }
    return false
}

private fun searchYoutube(context: Context, title: String): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        // Search for the given title using the installed youtube application
        var intent = Intent(Intent.ACTION_SEARCH, Uri.parse("vnd.youtube:"))
        intent.putExtra("query", url)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val packageManager = context.packageManager
        if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            // No app is installed, fall back to the website version
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.youtube.com/results?search_query=$url")
        }
        context.startActivity(intent)
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

private fun searchGoogle(context: Context, title: String): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.google.com/search?q=$url")
        context.startActivity(intent)
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

private fun searchImdbWebsite(context: Context, title: String): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///find?s=tt&q=$url"))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            intent.data = Uri.parse("http://www.imdb.com/find?s=tt&q=$url")
            context.startActivity(intent)
        }
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

private fun searchFileAffinityWebsite(context: Context, title: String): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.filmaffinity.com/es/search.php?stext=$url")
        context.startActivity(intent)
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

private fun searchEpgSelection(context: Context, title: String, channelId: Int): Boolean {
    val intent = Intent(context, SearchActivity::class.java)
    intent.action = Intent.ACTION_SEARCH
    intent.putExtra(SearchManager.QUERY, title)
    intent.putExtra("type", "program_guide")
    if (channelId > 0) {
        intent.putExtra("channelId", channelId)
    }
    context.startActivity(intent)
    return true
}


package com.example.newsapp

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

var titles:ArrayList<String> = ArrayList()
var content:ArrayList<String> = ArrayList()
lateinit var adapter: ArrayAdapter<String>
lateinit var articlesDB: SQLiteDatabase
open class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        articlesDB = this.openOrCreateDatabase("Articles", Context.MODE_PRIVATE,null)
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId ,INTEGER,title VARCHAR,content VARCHAR)")

        val task = DownloadTask()
        try {
           task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty")
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        val listView = findViewById<ListView>(R.id.listview)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,titles)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(applicationContext, ArticleActivity::class.java)
            intent.putExtra("content", content[position])
            startActivity(intent)
        }
        updateListView()
    }

    fun updateListView(){
        val c: Cursor = articlesDB.rawQuery("SELECT * FROM articles",null)
        val contextIndex = c.getColumnIndex("content")
        val titleIndex = c.getColumnIndex("title")
        if(c.moveToFirst()){
            titles.clear()
            content.clear()

            do{
                titles.add(c.getString(titleIndex))
                content.add(c.getString(contextIndex))

            }while (c.moveToNext())
            adapter.notifyDataSetChanged()
        }
    }

    class DownloadTask : AsyncTask<String, Unit, String>(){
        override fun doInBackground(vararg urls: String?): String? {
            var result: String = ""
            var urlConnection: HttpURLConnection? = null

            try{
                var url = URL(urls[0])
                urlConnection = url.openConnection() as HttpURLConnection
                var inputStream = urlConnection.inputStream
                var inputStreamReader = InputStreamReader(inputStream)
                var data = inputStreamReader.read()
                var current: Char
                while(data != -1){
                    current = data.toChar()
                    result += current
                    data = inputStreamReader.read()
                }
                Log.e("RESULT: ",result.toString())
                val jsonArray = JSONArray(result)
                var numberOfItems =20
                if(jsonArray.length() < 20){
                    numberOfItems = jsonArray.length()
                }
                articlesDB.execSQL("DELETE FROM articles")
                var i=0
                while(i < numberOfItems){
                    val articleId = jsonArray.getString(i)
                    url = URL("https://hacker-news.firebaseio.com/v0/item/$articleId.json?print=pretty")
                    urlConnection = url.openConnection() as HttpURLConnection
                    inputStream = urlConnection.inputStream
                    inputStreamReader = InputStreamReader(inputStream)
                    data = inputStreamReader.read()
                    var articleInfo: String = ""
                    while(data != -1){
                        current = data.toChar()
                        articleInfo += current
                        data = inputStreamReader.read()
                    }
                    Log.e("ARTICLE INFO: ",articleInfo)
                    val jsonObject = JSONObject(articleInfo)
                    Log.e("NOT ","ENTERED")
                    if(jsonObject.getString("title").isNotEmpty() && jsonObject.getString("url").isNotEmpty()){
                        val articleTitle = jsonObject.getString("title")
                        val articleUrl = jsonObject.getString("url")
                        val sql = "INSERT INTO articles (articleId, title, content) VALUES (?,?,?)"
                        val statement = articlesDB.compileStatement(sql)
                        statement.bindString(1,articleId)
                        statement.bindString(2,articleTitle)
                        statement.bindString(3,articleUrl)
                        statement.execute()
                    }
                    i++
                }
                return result
            }
            catch (e: Exception){
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val c: Cursor = articlesDB.rawQuery("SELECT * FROM articles",null)
            val contextIndex = c.getColumnIndex("content")
            val titleIndex = c.getColumnIndex("title")
            if(c.moveToFirst()){
                titles.clear()
                content.clear()

                do{
                    titles.add(c.getString(titleIndex))
                    content.add(c.getString(contextIndex))

                }while (c.moveToNext())
                adapter.notifyDataSetChanged()
            }

        }
    }
}

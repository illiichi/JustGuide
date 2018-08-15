package com.illiichi.justdialog

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.app.Activity
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.widget.*

class MainActivity : AppCompatActivity() {
    val categoryListAdapter: ArrayAdapter<String> by lazy { ArrayAdapter<String>(this, R.layout.text_view_only) }
    var currentCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentCategory = this.intent.getStringExtra("category")
        if(checkPermission()){
            start(currentCategory)
        }else{
            requestStorageReadPermission()
        }
    }

    private fun start(category: String?) {
        val preferenceService = PreferenceManager.getDefaultSharedPreferences(this)
        val configPath = preferenceService.getString("config-path", "")

        // mixin でSettingPageとGuidePageを分割したかったが、自分型の指定方法ができないっぽい？
        if (category == null) {
            startSettingPage(configPath)

        } else {
            startGuidePage(configPath, category)
        }
    }

    fun startGuidePage(configPath: String?, category: String?) {
        this.setContentView(R.layout.activity_page_list)

        val guideListAdapter = ArrayAdapter<Item>(this, R.layout.text_view_only)
        val textCategory = this.findViewById<TextView>(R.id.textCategory)
        val guideList = this.findViewById<ListView>(R.id.guideList)
        val buttonDone = this.findViewById<Button>(R.id.buttonDone)
        val descriptionPage = this.findViewById<DescriptionPageView>(R.id.descriptionPage)

        val config = tryParseConfig(configPath).find { it.name == category }
        if (config == null) {
            throw RuntimeException("category ($category) not found from: $configPath")
        } else {
            textCategory.text = config.description

            guideList.adapter = guideListAdapter
            guideListAdapter.clear()
            guideListAdapter.addAll(config.items)

            guideList.setOnItemClickListener { _, _, position, _ ->
                val item = guideList.getItemAtPosition(position) as Item
                descriptionPage.show(item.title, item.pages.toTypedArray())
            }
        }
        buttonDone.setOnClickListener { _ -> finish() }
    }

    fun startSettingPage(configPath: String?) {
        this.setContentView(R.layout.activity_setting)

        val categoryListView = this.findViewById<ListView>(R.id.categoryListView)
        val textConfigPath = this.findViewById<TextView>(R.id.textConfigPath)

        categoryListView.adapter = categoryListAdapter
        categoryListView.setOnItemClickListener { _, _, position, _ ->
            val category = categoryListView.getItemAtPosition(position) as String

            val shortcutIntent = Intent(this, MainActivity::class.java)
            shortcutIntent.putExtra("category", category)
            shortcutIntent.flags = FLAG_ACTIVITY_SINGLE_TOP

            val intent = Intent()
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, /*"Just:$category"*/ "ヘルプ")
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_help_24dp  /*R.mipmap.ic_recovery */))
            intent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            sendBroadcast(intent)
        }

        textConfigPath.text = configPath

        if (configPath != null && configPath.isNotEmpty()) {
            updateConfig(configPath)
        }

        val buttonChooseFile = this.findViewById<Button>(R.id.buttonChooseFile)
        buttonChooseFile.setOnClickListener { _ ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, REQUEST_CODE_FILE_SELECTED)
        }
    }

    private val REQUEST_CODE_SDCARD_PERMISSION = 1000
    private val REQUEST_CODE_FILE_SELECTED = 1001


    @Synchronized
    public override fun onActivityResult(requestCode: Int,
                                         resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_CODE_FILE_SELECTED ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePath = data.data?.toString()
                    println(filePath)

                    if(filePath != null && filePath.isNotEmpty()) {
                        updateConfig(filePath)
                        val preferenceService = PreferenceManager.getDefaultSharedPreferences(this)
                        preferenceService.edit().putString("config-path", filePath).apply()
                        val textConfigPath = this.findViewById<TextView>(R.id.textConfigPath)
                        textConfigPath.text = filePath
                    }

                }
        }
    }

    private fun updateConfig(filePath: String) {
        val config = tryParseConfig(filePath)
        categoryListAdapter.clear()
        categoryListAdapter.addAll(config.map { it.name })
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_SDCARD_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start(currentCategory)
            } else {
                finish()
            }
        }
    }

    fun tryParseConfig(filePath: String?): List<Category> {
        try {
            val uri = Uri.parse(filePath)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            val input = this.contentResolver.openInputStream(uri)
            if (input == null) {
                throw RuntimeException("not found? ($filePath)")
            }else{
                return com.illiichi.justdialog.parseConfig(input)
            }
        }catch(ex: Throwable){
            ex.printStackTrace();
            Toast.makeText(this, "exception occured: $ex", Toast.LENGTH_LONG).show()
            return listOf()
        }
    }

    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageReadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_SDCARD_PERMISSION)

        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_SDCARD_PERMISSION)

        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)

        val newCategory = newIntent?.getStringExtra("category")

        if(currentCategory != newCategory) {
            currentCategory = newCategory
            start(currentCategory)
        }
    }
}

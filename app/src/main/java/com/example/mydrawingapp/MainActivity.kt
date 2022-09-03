package com.example.mydrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var currentOne: ImageButton? = null
    private var myDialog: Dialog? = null

    private val openGallery: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result -> if (result.resultCode == RESULT_OK && result.data != null){
                val newImage: ImageView = findViewById(R.id.background)
                newImage.setImageURI(result.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions -> permissions.entries.forEach {
                    val permissionName = it.key
                    val isGranted = it.value
                    if (isGranted) {
                        Toast.makeText(this, "Permission Granted.", Toast.LENGTH_LONG).show()
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGallery.launch(pickIntent)
                    } else {
                        if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                            Toast.makeText(this, "Permission Denied.", Toast.LENGTH_LONG).show()
                        }
                    }
                 }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setBrushSize(20.toFloat())

        val linearPalette = findViewById<LinearLayout>(R.id.palette)

        currentOne = linearPalette[1] as ImageButton
        currentOne?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selected_color))

        val brush: ImageButton = findViewById(R.id.brush)
        brush.setOnClickListener{
            brushDialogChooser()
        }

        val gallery: ImageButton = findViewById(R.id.gallery)
        gallery.setOnClickListener {
            storagePermission()
        }

        val undo: ImageButton = findViewById(R.id.undo)
        undo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val redo: ImageButton = findViewById(R.id.redo)
        redo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val save: ImageButton = findViewById(R.id.save)
        save.setOnClickListener{
            if(readStorage()){
                progressDialog()
                lifecycleScope.launch {
                    val frameLayout: FrameLayout = findViewById(R.id.drawing_view_container)
                    saveFile(getBitmap(frameLayout))
                }
            }
        }
    }

    private fun brushDialogChooser(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size_dialog)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        val bigBtn: ImageButton = brushDialog.findViewById(R.id.big_brush)
        bigBtn.setOnClickListener{
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view !== currentOne){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selected_color))
            currentOne?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.normal_color))
            currentOne = view
        }
    }

    private fun readStorage(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun storagePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("Drawing App", "Drawing App " + "needs access to your external storage.")
        }else{
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationalDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel"){
                dialog, _ -> dialog.dismiss()
        }
        builder.create().show()
    }

    private fun getBitmap(view: View): Bitmap{
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = view.background
        if(drawable != null){
            drawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveFile(bitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(bitmap !=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val fileLocation = File(externalCacheDir?.absoluteFile.toString() +
                            File.separator + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")
                    val fileOutput = FileOutputStream(fileLocation)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()
                    result = fileLocation.absolutePath
                    runOnUiThread {
                        cancelProgress()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_LONG).show()
                            shareFile(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                catch (e: Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun progressDialog(){
        myDialog = Dialog(this)
        myDialog?.setContentView(R.layout.customdialog)
        myDialog?.show()
    }

    private fun cancelProgress(){
        if(myDialog!=null){
            myDialog?.dismiss()
            myDialog = null
        }
    }

    private fun shareFile(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri -> val shareItem = Intent()
            shareItem.action = Intent.ACTION_SEND
            shareItem.putExtra(Intent.EXTRA_STREAM, uri)
            shareItem.type = "image/png"
            startActivity(Intent.createChooser(shareItem, "Share"))
        }
    }
}
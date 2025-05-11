package uk.ac.soton.personal_tutor_app.ui.lesson.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

class FilePicker(private val activity: Activity) {
    private var onFileSelected: ((Uri) -> Unit)? = null

    fun registerFilePicker(launcher: ActivityResultLauncher<Intent>, callback: (Uri) -> Unit) {
        onFileSelected = callback
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(intent)
    }

    fun handleResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                onFileSelected?.invoke(uri)
            }
        }
    }
}
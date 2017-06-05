package me.bramhaag.owoandroid.listeners

import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.Uri
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat
import android.text.Html
import android.util.Log
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import me.bramhaag.owoandroid.R
import me.bramhaag.owoandroid.activities.MainActivity
import me.bramhaag.owoandroid.api.ProgressRequestBody
import me.bramhaag.owoandroid.util.Consumer
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import java.util.*

class UploadButtonListener(val activity: MainActivity): View.OnClickListener {

    private val GALLERY_REQUEST = 0
    private val mUploadQueue = LinkedList<Uri>()
    lateinit var dialog: ProgressDialog

    init {
        activity.resultConsumerMap.put(GALLERY_REQUEST, object : Consumer<Intent?> {
            override fun accept(t: Intent?) {
                dialog = ProgressDialog(activity).apply {
                    setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                }

                if(t?.data != null) mUploadQueue.add(t.data)
                else if(t?.clipData != null) (0..t.clipData.itemCount - 1).mapTo(mUploadQueue) { t.clipData.getItemAt(it).uri }

                upload(mUploadQueue.first, 0, mUploadQueue.count())
            }
        })
    }

    override fun onClick(v: View) {
        requestPermissions(activity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        if(ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_DENIED) return

        val intent = Intent(Intent.ACTION_GET_CONTENT)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    .setType("*/*")
        startActivityForResult(activity, intent, GALLERY_REQUEST, null)
    }

    fun upload(uri: Uri, index: Int, total: Int) {
        dialog.setTitle(activity.getString(R.string.dialog_upload_title, index + 1, total))

        val requestFile = ProgressRequestBody(uri, dialog)
        val requestPart = MultipartBody.Part.createFormData("files[]", requestFile.name, requestFile)
        val call = activity.owo.service.upload(requestPart)

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(android.R.string.cancel), { _, _ ->
            call.cancel()
            mUploadQueue.clear()
            dialog.dismiss()
        })

        dialog.show()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(call.isCanceled || !response.isSuccessful) {
                    dialog.dismiss()

                    @Suppress("DEPRECATION")
                    MaterialDialog.Builder(activity)
                            .title(R.string.error_title)
                            .content(
                                    if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                                        Html.fromHtml(activity.getString(R.string.upload_error_message, 200, response.code(), response.errorBody()?.string()), Html.FROM_HTML_MODE_LEGACY)
                                    else
                                        Html.fromHtml(activity.getString(R.string.upload_error_message, 200, response.code(), response.errorBody()?.string()))
                            )
                            .positiveText(R.string.ok)
                            .show()
                    return
                }

                mUploadQueue.removeFirst()
                val obj = JSONObject(response.body()?.string()).getJSONArray("files").getJSONObject(0)
                activity.mRecycleViewManager.addFile(obj.getString("name"), URL("${PreferenceManager.getDefaultSharedPreferences(activity).getString("pref_destination", "https://owo.whats-th.is")}/${obj.getString("url")}"), Date())

                if(mUploadQueue.isNotEmpty()) {
                    upload(mUploadQueue.first, index + 1, total)
                    return
                }

                dialog.dismiss()
                if(total > 1) {
                    Snackbar.make(activity.findViewById(R.id.main_scroll_view), activity.getString(R.string.snackbar_upload_multi_content, total), Snackbar.LENGTH_LONG).show()
                } else {
                    val url = Uri.parse("https://owo.whats-th.is/" + obj.getString("url"))

                    Snackbar.make(activity.findViewById(R.id.main_scroll_view), activity.getString(R.string.snackbar_upload_single_content), Snackbar.LENGTH_LONG)
                            .setAction(R.string.dialog_open_file, { ContextCompat.startActivity(activity, Intent(Intent.ACTION_VIEW, url), null) })
                            .show()

                    (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText(url.toString(), url.toString())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("Upload error:", t.message)
                t.printStackTrace()
            }
        })
    }

    /*
    java.lang.NoClassDefFoundError: me.bramhaag.owoandroid.listeners.UploadButtonListener$1
          at me.bramhaag.owoandroid.listeners.UploadButtonListener.<init>(UploadButtonListener.kt:37)
          at me.bramhaag.owoandroid.activities.MainActivity.onCreate(MainActivity.kt:61)
          at android.app.Activity.performCreate(Activity.java:6237)
          at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1107)
          at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2369)
          at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2476)
          at android.app.ActivityThread.-wrap11(ActivityThread.java)
          at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1344)
          at android.os.Handler.dispatchMessage(Handler.java:102)
          at android.os.Looper.loop(Looper.java:148)
          at android.app.ActivityThread.main(ActivityThread.java:5417)
          at java.lang.reflect.Method.invoke(Native Method)
          at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
          at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
     */
}
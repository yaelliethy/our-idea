@file:Suppress("DEPRECATION")
package com.google.firebase.quickstart.fcm.kotlin
import android.support.design.widget.BottomNavigationView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.quickstart.fcm.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*

/**
 * Created by roberto on 9/29/16.
 */

class MainActivity: AppCompatActivity() {
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                Intent(this, MainActivity::class.java)
            }
            R.id.navigation_dashboard -> {
                message.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
    var LOCATION_REFRESH_TIME=1
    var LOCATION_REFRESH_DISTANCE=1
    var show_next_message = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContentView(R.layout.activity_main)
        FirebaseMessaging.getInstance().subscribeToTopic("fire_news")
                .addOnCompleteListener { task ->
                    var msg = getString(R.string.msg_subscribed)
                    if (!task.isSuccessful) {
                        msg = getString(R.string.msg_subscribe_failed)
                    }
                    Log.d(TAG, msg)
                }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW))
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras.get(key)
                Log.d(TAG, "Key: $key Value: $value")
            }
        }
        // [END handle_data_extras]

        Takepic.setOnClickListener{
            dispatchTakePictureIntent()

        }
}


    val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            val extras = data!!.extras
            val imageBitmap = extras!!.get("data") as Bitmap
            iv.setImageBitmap(imageBitmap)
            val image = FirebaseVisionImage.fromBitmap(imageBitmap)
            val metadata = FirebaseVisionImageMetadata.Builder()
                    .setWidth(480)   // 480x360 is typically sufficient for
                    .setHeight(360)  // image recognition
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .build()
// Or, to set the minimum confidence required:
            var hj = false
            val detector = FirebaseVision.getInstance()
                    .getVisionLabelDetector()
            val result: Task<MutableList<FirebaseVisionLabel>>?
            result = detector.detectInImage(image)
            result.addOnCompleteListener {
                for (label in result.getResult()!!) {
                val text = label.label
                informationTextView.text = text.toString()
                    if(text=="Fire") {
                        fusedLocationClient.lastLocation
                                .addOnSuccessListener { location : Location? ->
                                    val x=location?.longitude
                                    val y=location?.latitude
                                    val from=FirebaseInstanceId.getInstance().getInstanceId();
                                    from.addOnCompleteListener {
                                        val queue = Volley.newRequestQueue(this)
                                        val url = "http://ec2-18-224-40-108.us-east-2.compute.amazonaws.com/spot_that_fire/index.php/Firebase/send?location=$y,$x"
                                        com.google.firebase.quickstart.fcm.kotlin.MainActivity.show_next_message=0
// Request a string response from the provided URL.
                                        val stringRequest = StringRequest(Request.Method.GET, url,
                                                Response.Listener<String> { response ->
                                                    // Display the first 500 characters of the response string.
                                                    Log.d(TAG, "Response is: ${response.substring(0, 0)}")
                                                },
                                                Response.ErrorListener { Log.d(TAG, "Response is:NULL") })


// Add the request to the RequestQueue.
                                        queue.add(stringRequest)
                                    }

                                }

                    }
                val entityId = label.entityId
                val confidence = label.confidence
                    break
            }
            }
        }


        }


    companion object{
        private val REQUEST_TAKE_PHOTO = 0
        private val REQUEST_SELECT_IMAGE_IN_ALBUM = 1
        private val TAG = "MainActivity"
        var show_next_message: Int =1;
    }



}
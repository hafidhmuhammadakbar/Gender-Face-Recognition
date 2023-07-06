package com.pab.genderfacerecognition;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.net.Uri;

import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ktx.Firebase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    // API
    private final String API_ENDPOINT = "https://www.picpurify.com/analyse/1.1?task=face_gender_age_detection&API_KEY=fFj7Ahl67uQaHRRLhyj5aVJcxQ5NEw5S&url_image=";

    // Permission constant
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    // storage
    StorageReference storageReference;
    FirebaseStorage firebaseStorage;

    // Array of permission to be requested
    String[] cameraPermissions;
    String[] storagePermissions;

    Uri imageUri;
    String url;

    //views
    FrameLayout frameLayout;
    TextView genderTextView;
    ImageView avathar;

    String gender;

    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init array permissions
        cameraPermissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // init views
        frameLayout = findViewById(R.id.framelayout);
        genderTextView = findViewById(R.id.gender);
        avathar = findViewById(R.id.avathar);

        // init progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Verifying Image");
        progressDialog.setMessage("Please wait...");


        genderTextView.setVisibility(View.GONE);

        // init firebase storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

//        String imageUrl = "https://www.houseofwellness.com.au/wp-content/uploads/2023/01/vanilla-girl-make-up.jpg";
//        String finalUrl = API_ENDPOINT + imageUrl;
//        networking(finalUrl);
    }

    private void showImagePickDialog() {
        // show dialog  that contain camera and gallery for selecting image
        String options[] = {"Camera", "Gallery"};

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Pick Image From");
        alert.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                    // camera handling and check permission
                    if(!checkCameraPermission()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestCameraPermission();
                        }
                    }else {
                        pickFromCamera();
                    }
                }else if(which == 1){
                    // gallery handling and check permission
                    if(!checkStoragePermission()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestStoragePermission();
                        }
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });
        alert.create().show();
    }

    // To check and ask permission when open the app
    @RequiresApi (api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();
        // Check camera permission
        if (!checkCameraPermission()) {
            requestCameraPermission();
        }
        // Check storage permission
        if (!checkStoragePermission()) {
            requestStoragePermission();
        }
    }

    // Permission functions
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi (api = Build.VERSION_CODES.M)
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    @RequiresApi (api = Build.VERSION_CODES.M)
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    // Check camera permissions
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    // Check storage permissions
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Image picked from camera or gallery will be received here
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // Image is picked from gallery, get uri of image
//                assert data != null;
                imageUri = data.getData();
                uploadPhoto();
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                uploadPhoto();
            }
        }
    }

    private void uploadPhoto() {
        if(imageUri==null){
            Toast.makeText(getApplicationContext(), "Image URI is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // show progress dialog
        progressDialog.show();

        StorageReference storageRef = storageReference.child("image");

        storageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful());
                Uri downloadUri = uriTask.getResult();

                // getting url to pass in api
                url = downloadUri.toString();

                String finalUrl = API_ENDPOINT + url;

                // networking
                networking(finalUrl);

                // load image
                try{
                    Picasso.get().load(url).placeholder(R.drawable.ic_face).into(avathar);
                } catch (Exception e){

                }

                // use Handler show progress dialog until api call finishes and get data
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // set gender text view
                        genderTextView.setText(gender.toUpperCase());
                        // make gender text view visible
                        genderTextView.setVisibility(View.VISIBLE);

                        Toast.makeText(getApplicationContext(), "Verification Completed", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }, 6000);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    // API Networking
    private void networking(String url) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d("GenderFaceRecognition", "onSuccess: " + response.toString());

                try{
                    gender = response.getJSONObject("face_detection").getJSONArray("results").getJSONObject(0).getJSONObject("gender").getString("decision");
                } catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }

//                try {
//                    // Memeriksa status permintaan API
//                    String status = response.optString("status");
//                    if (status.equals("success")) {
//                        // Mendapatkan array hasil deteksi wajah
//                        JSONArray resultsArray = response.optJSONArray("results");
//                        if (resultsArray != null && resultsArray.length() > 0) {
//                            // Mengambil informasi usia dan gender untuk wajah pertama (indeks 0)
//                            JSONObject result = resultsArray.getJSONObject(0);
//
//                            // Mengambil informasi gender
//                            gender = response.getJSONObject("face_detection").getJSONArray("results").getJSONObject(0).getJSONObject("gender").getString("decision");
//
//                        } else {
//                            Toast.makeText(getApplicationContext(), "No face detected", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        Toast.makeText(getApplicationContext(), "API request failed", Toast.LENGTH_SHORT).show();
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d("GenderFaceRecognition", "onFailure: " + throwable.toString());
                Log.d("GenderFaceRecognition", "onFailure: " + errorResponse.toString());
                Log.d("GenderFaceRecognition", "onFailure: " + statusCode);
            }
        });
    }
}

package com.pab.genderfacerecognition;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    // API
    private final String API_ENDPOINT = "https://www.picpurify.com/analyse/1.1?task=face_gender_age_detection&API_KEY=fFj7Ahl67uQaHRRLhyj5aVJcxQ5NEw5S&url_image=";

    // Permission constant
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    private static final int IMAGE_PICPURIFY_MAX_WIDTH = 1200;
    private static final int IMAGE_PICPURIFY_MAX_HEIGHT = 1200;

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

    Handler handler = new Handler();


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
        genderTextView.setVisibility(View.GONE);
        gender = null;

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
                // Image is picked from gallery, get Uri of the image
                imageUri = data.getData();
                try {
                    // Resize the image
                    Bitmap resizedBitmap = resizeImage(imageUri, IMAGE_PICPURIFY_MAX_WIDTH, IMAGE_PICPURIFY_MAX_HEIGHT);

                    // Update the imageUri with the resized image
                    imageUri = getImageUri(getApplicationContext(), resizedBitmap);

                    // Upload the resized image
                    uploadPhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Failed to resize image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                try {
                    // Resize the captured image
                    Bitmap resizedBitmap = resizeImage(imageUri, IMAGE_PICPURIFY_MAX_WIDTH, IMAGE_PICPURIFY_MAX_HEIGHT);
                    Bitmap rotatedBitmap = rotateImage(resizedBitmap, 270); // Specify the rotation angle here (90 for left, -90 for right)

                    // Save the rotated bitmap to the original imageUri
                    OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    // Update the imageUri with the resized image
                    imageUri = getImageUri(getApplicationContext(), resizedBitmap);

                    // Upload the resized image
                    uploadPhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Failed to resize image", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    private Bitmap rotateImage(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        // Correct the orientation of the image based on Exif data (if available)
        try {
            ExifInterface exifInterface = new ExifInterface(imageUri.getPath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees += 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees += 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees += 90;
                    break;
            }
            matrix.postRotate(degrees);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
                    Toast.makeText(getApplicationContext(), "Failed to add Image to Screen", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFromCamera() {
        imageUri = null;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

//        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        imageUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        imageUri = null;
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    // API Networking
    private void networking(String url) {
        Log.d("GenderFaceRecognition", "onSuccess: Access Networking");
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d("GenderFaceRecognition", "onSuccess: " + response.toString());
                gender = null;

                try{
                    String status = response.optString("status");
                    if ("failure".equals(status)) {
                        gender = "failed to detect";
                        Toast.makeText(getApplicationContext(), "API request failed", Toast.LENGTH_SHORT).show();
                    } else {
                        gender = response.getJSONObject("face_detection").getJSONArray("results").getJSONObject(0).getJSONObject("gender").getString("decision");
                    }
                } catch (Exception e){
                    gender = "failed to detect";
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                // UI update and progress dialog dismissal
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(gender != null){
                            // set gender text view
                            genderTextView.setText(gender.toUpperCase());

                            // make gender text view visible
                            genderTextView.setVisibility(View.VISIBLE);

                            Toast.makeText(getApplicationContext(), "Verification Completed", Toast.LENGTH_SHORT).show();
                        } else {
                            // set gender text view
                            genderTextView.setText("UNKNOWN");

                            // make gender text view visible
                            genderTextView.setVisibility(View.VISIBLE);
                        }
                        progressDialog.dismiss();
                    }
                });
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
    private Bitmap resizeImage(Uri uri, int maxWidth, int maxHeight) throws IOException {
        // Load the image from the given Uri
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

        // Calculate the aspect ratio to maintain the image's original proportions
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float scaleFactor = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);

        // Calculate the new dimensions for the resized image
        int resizedWidth = Math.round(originalWidth * scaleFactor);
        int resizedHeight = Math.round(originalHeight * scaleFactor);

        // Create the resized bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true);

        // Recycle the original bitmap to free up memory
        bitmap.recycle();

        return resizedBitmap;
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";

        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imageFileName, null);
        return Uri.parse(path);
    }
}

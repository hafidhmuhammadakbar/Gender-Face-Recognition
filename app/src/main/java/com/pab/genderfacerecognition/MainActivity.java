package com.pab.genderfacerecognition;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;

import android.os.Bundle;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        // Create a reference to the image file you want to retrieve
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("4fe6097f646b518a543b9f99ff422147.jpg");
        System.out.println(imageRef.getDownloadUrl());

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String imageUrl = uri.toString();
            Picasso.get()
                    .load(imageUrl)
                    .into(imageView);
        }).addOnFailureListener(e -> {
            // Handle any errors that occur while fetching the download URL
        });
    }
}
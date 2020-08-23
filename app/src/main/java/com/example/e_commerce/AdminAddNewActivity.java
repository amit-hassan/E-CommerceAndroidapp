package com.example.e_commerce;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AdminAddNewActivity extends AppCompatActivity {

    private String CategoryName, Description, Price, Pname, saveCurrentDate, saveCurrentTime;
    private Button AddNewProductButton;
    private ImageView InputProductImage;
    private EditText InputProductName, InputProductDescription, InputProductPrice;
    private static final int GalleryPick = 1;
    private Uri ImageUri;
    private String productRandomKey, downloadImageUrl;
    private StorageReference ProductImagesRef;
    private DatabaseReference ProductRef;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_new);

      CategoryName = getIntent().getExtras().get("category").toString();
      ProductImagesRef = FirebaseStorage.getInstance().getReference().child("Product Images");
      ProductRef = FirebaseDatabase.getInstance().getReference().child("Products");

        AddNewProductButton = findViewById(R.id.add_new_product);
        InputProductImage = findViewById(R.id.select_product_image);
        InputProductName = findViewById(R.id.product_name);
        InputProductDescription = findViewById(R.id.product_description);
        InputProductPrice = findViewById(R.id.product_price);
        loadingBar = new ProgressDialog(this);
        
        InputProductImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenGallery();
            }
        });

        AddNewProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidateProductData();
            }
        });
        
    }

    /*private void OpenGallery() {

        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent,GalleryPick);

    }
    //and onActivityResult as
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
            switch (requestCode){
                case GalleryPick:
                    //data.getData returns the content URI for the selected Image
                    ImageUri = data.getData();
                    InputProductImage.setImageURI(ImageUri);
                    break;
            }
    }
*/
    private void OpenGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GalleryPick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GalleryPick && resultCode == RESULT_OK && data !=null)
        {
            ImageUri = data.getData();
            InputProductImage.setImageURI(ImageUri);
        }
    }

    private void ValidateProductData(){
        Description = InputProductDescription.getText().toString();
        Price = InputProductPrice.getText().toString();
        Pname = InputProductName.getText().toString();

        //Verify Here
        if(ImageUri == null){
            Toast.makeText(this, "Product image is mandatory...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "Please write product description...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(Price))
        {
            Toast.makeText(this, "Please write product price...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(Pname))
        {
            Toast.makeText(this, "Please write product Product Name...", Toast.LENGTH_SHORT).show();
        }

        else
        {
            //For Displaying Time
            StoreProductInformation();
        }
    }

    private void StoreProductInformation() {

        loadingBar.setTitle("Add New Product");
        loadingBar.setMessage("Dear Admin, Please wait while we are adding the new Product....");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        //Unique Product Random Key without using Firebase Push()
        productRandomKey = saveCurrentDate + saveCurrentTime;

        //Link of the Product Image
        final StorageReference filePath = ProductImagesRef.child(ImageUri.getLastPathSegment() + productRandomKey + ".jpg");


        final UploadTask uploadTask = filePath.putFile(ImageUri);


        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Getting Error Message
                String message = e.toString();
                Toast.makeText(AdminAddNewActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(AdminAddNewActivity.this, "Product Image Uploaded Successfully...", Toast.LENGTH_SHORT).show();

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if(!task.isSuccessful())
                        {
                            throw  task.getException();
                        }

                        //It's just getting the uri not the link
                        downloadImageUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful())
                        {
                            //To get the link
                            downloadImageUrl = task.getResult().toString();

                            Toast.makeText(AdminAddNewActivity.this, "get the Product image Url successfully...", Toast.LENGTH_SHORT).show();

                            //Now Store all information to the database
                            SaveProductInfoToDatabase();
                        }
                    }
                });
                
            }
        });

    }

    private void SaveProductInfoToDatabase() {
        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid", productRandomKey);
        productMap.put("date", saveCurrentDate);
        productMap.put("time", saveCurrentTime);
        productMap.put("description", Description);
        productMap.put("image", downloadImageUrl);
        productMap.put("category", CategoryName);
        productMap.put("price", Price);
        productMap.put("Pname", Pname);
        
        ProductRef.child(productRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {

                            Intent intent = new Intent(AdminAddNewActivity.this,AdminCategoryActivity.class);
                            startActivity(intent);

                            loadingBar.dismiss();
                            Toast.makeText(AdminAddNewActivity.this, "Product is added successfully...", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(AdminAddNewActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }


}
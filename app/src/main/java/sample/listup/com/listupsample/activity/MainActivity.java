package sample.listup.com.listupsample.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import sample.listup.com.listupsample.R;
import sample.listup.com.listupsample.models.Book;
import sample.listup.com.listupsample.utils.AppController;
import sample.listup.com.listupsample.utils.Helper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    //Buttons
    private Button barcodeBtn;
    private Button ISBNBtn;
    private Button allBooksBtn;
    private Button testCaseBtn;
    private Button addBookBtn;

    //EditText fields
    private EditText ISBNEditText;
    private EditText priceEditText;

    // Layouts
    private LinearLayout priceLayout;
    private TextView bookNameTextview;

    //Variables
    private String bookISBN;
    private Book insertingBook;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setting Fields and listeners on buttons
        barcodeBtn = (Button) findViewById(R.id.barcode_scan);
        ISBNBtn = (Button) findViewById(R.id.isbn_code);
        allBooksBtn = (Button) findViewById(R.id.all_books);
        addBookBtn = (Button) findViewById(R.id.add_book);
        testCaseBtn = (Button) findViewById(R.id.test_case);
        addBookBtn.setOnClickListener(this);
        barcodeBtn.setOnClickListener(this);
        ISBNBtn.setOnClickListener(this);
        allBooksBtn.setOnClickListener(this);
        testCaseBtn.setOnClickListener(this);

        // setting Layouts
        priceLayout = (LinearLayout) findViewById(R.id.price_layout);

        //Setting Text fields
        ISBNEditText = (EditText) findViewById(R.id.isbn_text);
        priceEditText = (EditText) findViewById(R.id.price);
        bookNameTextview = (TextView) findViewById(R.id.bookname_detected);


    }


    @Override
    public void onClick(View view) {

        // Onclick on buttons. opens the scan activity
        switch (view.getId()){
            case R.id.barcode_scan :

                // It uses Already pre installed app to scan barcode . it asks to install one app from playstore
                scanBarWithPreinstalledApp();

                 // It uses the barcode scanner library to get results. This library included in build.gradle.
                // scanBarcodeUsingLibrary();

                break;

            // To get All the books
            case R.id.all_books :
                Intent intent = new Intent(this,ListAllBookActivity.class);
                startActivity(intent);

            // To get book details from ISBN code
            case R.id.isbn_code :
                    if(ISBNEditText.getText().toString().length() > 0) {
                        Toast.makeText(this, ISBNEditText.getText().toString(), Toast.LENGTH_SHORT).show();
                        getBookDetails(ISBNEditText.getText().toString());
                    } else {
                        Toast.makeText(this, "Please Enter code.", Toast.LENGTH_SHORT).show();
                    }
                break;

            // It  asks for price and inserts book object into database.
            case R.id.add_book :
                String priceText = priceEditText.getText().toString();
                if(priceText.length() > 0 ){
                    insertingBook.setBookPrice(Integer.parseInt(priceText));
                    insertBookInDB(insertingBook);
                } else {
                    Toast.makeText(this, "Please Enter priceEditText..", Toast.LENGTH_SHORT).show();
                }
                break;

            // General TestCase
            case R.id.test_case :
                bookISBN = "9780759521438";
                getBookDetails("9780759521438");
                break;

            default:
                break;
        }
    }

    // product barcode mode. It scans barcode on Product mode
    // If no scanner available It downloads one from Google play store.. and We can use thirdParty library also
    public void scanBarWithPreinstalledApp() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(MainActivity.this, "No Scanner Found",
                    "Download a scanner code App? or use inbuilt app ?", "APP", "INBUILT").show();
        }
    }

    //alert dialog for downloadDialog, It will execute if No Scanner found, It installs one.
    private  AlertDialog showDialog(final Activity act, CharSequence title,
                                          CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        final AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {

                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(act, "Use Scanbar library", Toast.LENGTH_SHORT).show();
                scanBarcodeUsingLibrary();
            }
        });
        return downloadDialog.show();
    }

    //on ActivityResult method. We got a product after scanned

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Scan Completed..", Toast.LENGTH_SHORT).show();
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Log.d("ScanResult",contents);

                // We got product ISBN number and so get googlebook details
                getBookDetails(format);
            }
        } else {

            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if(result != null) {
                if(result.getContents() == null) {
                    Log.d("MainActivity", "Cancelled scan");
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("MainActivity", "Scanned");
                    String scanContent = result.getContents();
                    String scanFormat = result.getFormatName();

                    if(scanContent != null && scanFormat != null && scanFormat.equalsIgnoreCase("EAN_13")){
                        Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                        getBookDetails(result.getContents());
                    } else{
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Not a valid scan!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, intent);
            }
        }
    }

    // This uses google API to fetch bookdata from ISBN numbers.
    private void getBookDetails(String bookISBN) {

        String url = Helper.GOOGLE_BOOKS_API + bookISBN;

        // Volley request to get bookdata json.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    //On successful response We need to ask the user to enter price.
                    int totalItems = response.getInt("totalItems");
                    if(totalItems > 0) {
                        JSONObject item = response.getJSONArray("items").getJSONObject(0);
                        String title = item.getJSONObject("volumeInfo").getString("title");
                        String imageUrl = item.getJSONObject("volumeInfo").getJSONObject("imageLinks").
                                getString("smallThumbnail");
                        // Its Enter price and add book button will be activated.
                        priceLayout.setVisibility(View.VISIBLE);
                        addBookBtn.setVisibility(View.VISIBLE);
                        bookNameTextview.setText(title);
                        insertingBook  = new Book(title,0,imageUrl);
                    }else {
                        Toast.makeText(MainActivity.this, "No book found with this ISBN", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });

        AppController.getInstance().addToRequestQueue(request);
    }

    // Insert the book into database with created JSON object.
    private void insertBookInDB(Book book){

        Toast.makeText(this, "inserting book", Toast.LENGTH_SHORT).show();

        //Making Json Object from book data
        JSONObject object = new JSONObject();
        try {
            object.put("bookImage",book.getBookImage());
            object.put("bookName",book.getBookTitle());
            object.put("bookPrice",book.getBookPrice());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Posting bookdata object
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Helper.INSERT_BOOK_URL,
                object,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Toast.makeText(MainActivity.this, response.getString("result"), Toast.LENGTH_SHORT).show();
                    ISBNEditText.setText("");
                    priceLayout.setVisibility(View.GONE);
                    addBookBtn.setVisibility(View.GONE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error",error.toString());
                Toast.makeText(MainActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });
        AppController.getInstance().addToRequestQueue(request);
    }

// Code using Library

    public void scanBarcodeUsingLibrary() {

        new IntentIntegrator(this).initiateScan();

    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if(result != null) {
//            if(result.getContents() == null) {
//                Log.d("MainActivity", "Cancelled scan");
//                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
//            } else {
//                Log.d("MainActivity", "Scanned");
//                bookISBN = result.getContents();
//                getBookDetails(bookISBN);
//                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
//            }
//        } else {
//            // This is important, otherwise the result will not be passed to the fragment
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }

}

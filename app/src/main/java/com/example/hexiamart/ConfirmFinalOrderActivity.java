package com.example.hexiamart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hexiamart.Model.Products;
import com.example.hexiamart.Prevalent.Prevalent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class ConfirmFinalOrderActivity extends AppCompatActivity {

    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
            .clientId("Ab6ZKRCQaK8J534_FDpp_4BCnggYt_ugDau-pwNNCcKdc2MYCYaXnMknr--0C5zhlp9T0T_XBB3CvTn8");

    private EditText nameEditText, phoneEditText, addressEditText, cityEditText;
    private Button confirmOrderBtn;
    Products products = new Products();
    private String productName = products.getPname();
    private String totalAmount = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_final_order);

        totalAmount =  getIntent().getStringExtra("Total Price");
        Toast.makeText(this, "Total Price = $" + totalAmount, Toast.LENGTH_SHORT).show();

        confirmOrderBtn = findViewById(R.id.confirm_final_order_btn);
        nameEditText = findViewById(R.id.shipment_name);
        phoneEditText = findViewById(R.id.shipment_phone_number);
        addressEditText = findViewById(R.id.shipment_address);
        cityEditText = findViewById(R.id.shipment_city);

        confirmOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                onBuyPressed();
                Check();
            }
        });
    }
    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void Check()
    {
        if(TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this, "Please Provide Your Full Name", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneEditText.getText().toString())){
            Toast.makeText(this, "Please Provide Your Phone Number", Toast.LENGTH_SHORT).show();
        } else if(TextUtils.isEmpty(addressEditText.getText().toString())){
            Toast.makeText(this, "Please Provide Your Home Address", Toast.LENGTH_SHORT).show();
        } else if(TextUtils.isEmpty(cityEditText.getText().toString())){
            Toast.makeText(this, "Please Provide YourCity Name", Toast.LENGTH_SHORT).show();
        } else {
            ConfirmOrder();
        }
    }

    private void ConfirmOrder()
    {

        final String saveCurrentTime, saveCurrentDate;
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentDate.format(calForDate.getTime());

        final DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
            .child("Orders")
            .child(Prevalent.currentOnlineUser.getPhone());
        HashMap<String, Object> ordersMap = new HashMap<>();
        ordersMap.put("totalAmount", totalAmount);
        ordersMap.put("name", nameEditText.getText().toString());
        ordersMap.put("phone", phoneEditText.getText().toString());
        ordersMap.put("address", addressEditText.getText().toString());
        ordersMap.put("city", cityEditText.getText().toString());
        ordersMap.put("date", saveCurrentDate);
        ordersMap.put("time", saveCurrentTime);
        ordersMap.put("state", "not shipped");

        ordersRef.updateChildren(ordersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    FirebaseDatabase.getInstance().getReference().child("Cart List")
                            .child("Useer View")
                            .child(Prevalent.currentOnlineUser.getPhone())
                            .removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(ConfirmFinalOrderActivity.this, "Your final order has been placed successfully.", Toast.LENGTH_SHORT).show();
//                                        Intent intent = new Intent(ConfirmFinalOrderActivity.this, HomeActivity.class);
//                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                        startActivity(intent);

                                        Intent intent = new Intent(ConfirmFinalOrderActivity.this, PayPalService.class);
                                        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
                                        startService(intent);
                                    }
                                }
                            });
                }
            }
        });



    }
    public void onBuyPressed() {

        // PAYMENT_INTENT_SALE will cause the payment to complete immediately.
        // Change PAYMENT_INTENT_SALE to
        //   - PAYMENT_INTENT_AUTHORIZE to only authorize payment and capture funds later.
        //   - PAYMENT_INTENT_ORDER to create a payment for authorization and capture
        //     later via calls from your server.

//        PayPalPayment payment = new PayPalPayment(new BigDecimal("100.75"), "USD", "Frankenstein",
//                PayPalPayment.PAYMENT_INTENT_SALE);
        PayPalPayment payment = new PayPalPayment(new BigDecimal(String.valueOf(totalAmount)), "USD", "Total Price",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(this, PaymentActivity.class);

        // send the same configuration for restart resiliency
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    Log.i("paymentExample", confirm.toJSONObject().toString(4));

                    // TODO: send 'confirm' to your server for verification.
                    // see https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
                    // for more details.

                } catch (JSONException e) {
                    Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.i("paymentExample", "The user canceled.");
        } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Log.i("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
        }
    }
}
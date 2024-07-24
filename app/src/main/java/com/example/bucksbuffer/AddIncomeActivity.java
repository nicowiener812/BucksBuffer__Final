package com.example.bucksbuffer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddIncomeActivity extends BaseActivity {

    private TextInputEditText editTextIncomeTitle, editTextAmount;
    private AutoCompleteTextView spinnerSource;
    private MaterialButton buttonDatePicker, buttonAddIncome;
    private Calendar calendar;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_add_income);

        // Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Firestore
        db = FirebaseFirestore.getInstance();

        editTextIncomeTitle = findViewById(R.id.editTextIncomeTitle);
        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerSource = findViewById(R.id.spinnerSource);
        buttonDatePicker = findViewById(R.id.buttonDatePicker);
        buttonAddIncome = findViewById(R.id.buttonAddIncome);

        calendar = Calendar.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupSourceSpinner();

        setupDatePicker();

        buttonAddIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addIncome();
            }
        });
    }

    private void setupSourceSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.income_sources, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(adapter);
    }

    private void setupDatePicker() {
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateButtonText();
            }
        };

        buttonDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(AddIncomeActivity.this, date, calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        updateDateButtonText();
    }

    private void updateDateButtonText() {
        String dateFormat = "MMM d, yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        buttonDatePicker.setText(sdf.format(calendar.getTime()));
    }

    private void addIncome() {
        String title = editTextIncomeTitle.getText().toString().trim();
        String source = spinnerSource.getText().toString();
        String amountStr = editTextAmount.getText().toString().trim();
        long timestamp = calendar.getTimeInMillis();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> income = new HashMap<>();
        income.put("userId", userId);
        income.put("title", title);
        income.put("source", source);
        income.put("amount", amount);
        income.put("timestamp", timestamp);

        db.collection("incomes")
                .add(income)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddIncomeActivity.this, "Income added successfully", Toast.LENGTH_SHORT).show();
                    logIncomeAddedEvent(amount, source);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddIncomeActivity.this, "Error adding income", Toast.LENGTH_SHORT).show();
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
    }


    private void logIncomeAddedEvent(double amount, String source) {
        Bundle bundle = new Bundle();
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, amount);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, source);
        mFirebaseAnalytics.logEvent("income_added", bundle);
    }
}
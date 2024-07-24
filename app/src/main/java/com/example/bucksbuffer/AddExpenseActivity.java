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

public class AddExpenseActivity extends BaseActivity {

    private TextInputEditText editTextExpenseTitle, editTextAmount;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton buttonDatePicker, buttonAddExpense;
    private Calendar calendar;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_add_expense);

        // Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Firestore
        db = FirebaseFirestore.getInstance();

        editTextExpenseTitle = findViewById(R.id.editTextExpenseTitle);
        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        buttonDatePicker = findViewById(R.id.buttonDatePicker);
        buttonAddExpense = findViewById(R.id.buttonAddExpense);

        // Initialize calendar
        calendar = Calendar.getInstance();

        setupCategorySpinner();

        setupDatePicker();
        mAuth = FirebaseAuth.getInstance();

        buttonAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addExpense();
            }
        });
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
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
                new DatePickerDialog(AddExpenseActivity.this, date, calendar.get(Calendar.YEAR),
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

    private void addExpense() {
        String title = editTextExpenseTitle.getText().toString().trim();
        String category = spinnerCategory.getText().toString();
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

        Map<String, Object> expense = new HashMap<>();
        expense.put("userId", userId);
        expense.put("title", title);
        expense.put("category", category);
        expense.put("amount", amount);
        expense.put("timestamp", timestamp);

        db.collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddExpenseActivity.this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                    logExpenseAddedEvent(amount, category);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddExpenseActivity.this, "Error adding expense", Toast.LENGTH_SHORT).show();
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
    }


    private void logExpenseAddedEvent(double amount, String category) {
        Bundle bundle = new Bundle();
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, amount);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category);
        mFirebaseAnalytics.logEvent("expense_added", bundle);
    }
}
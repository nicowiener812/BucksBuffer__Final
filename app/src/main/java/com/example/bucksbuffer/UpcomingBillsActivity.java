package com.example.bucksbuffer;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bucksbuffer.adapter.BillsAdapter;
import com.example.bucksbuffer.model.Bill;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpcomingBillsActivity extends BaseActivity {

    private RecyclerView recyclerViewBills;
    private FloatingActionButton fabAddBill;
    private FirebaseFirestore db;
    private BillsAdapter billsAdapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_upcoming_bills);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        recyclerViewBills = findViewById(R.id.recyclerViewBills);
        fabAddBill = findViewById(R.id.fabAddBill);

        setupRecyclerView();
        loadUpcomingBills();

        fabAddBill.setOnClickListener(v -> showAddBillDialog());
    }

    private void setupRecyclerView() {
        billsAdapter = new BillsAdapter(new ArrayList<>(), this::onBillPaid);
        recyclerViewBills.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBills.setAdapter(billsAdapter);
    }

    private void loadUpcomingBills() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("bills")
                .whereEqualTo("paid", false)
                .orderBy("dueDate")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No upcoming bills found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Bill> bills = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Bill bill = document.toObject(Bill.class);
                        bill.setId(document.getId());
                        bills.add(bill);
                    }
                    billsAdapter.updateBills(bills);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading bills: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onBillPaid(Bill bill) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("bills").document(bill.getId())
                .update("paid", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bill marked as paid", Toast.LENGTH_SHORT).show();
                    addBillAsExpense(bill);
                    loadUpcomingBills();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating bill", Toast.LENGTH_SHORT).show();
                });
    }

    private void addBillAsExpense(Bill bill) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        Map<String, Object> expense = new HashMap<>();
        expense.put("userId", userId);
        expense.put("title", bill.getName());
        expense.put("category", "Bills");
        expense.put("amount", bill.getAmount());
        expense.put("timestamp", bill.getDueDate().getTime());

        db.collection("users").document(userId).collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Bill added as expense successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding bill as expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddBillDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_bill);

        EditText editTextBillName = dialog.findViewById(R.id.editTextBillName);
        EditText editTextAmount = dialog.findViewById(R.id.editTextAmount);
        DatePicker datePicker = dialog.findViewById(R.id.datePicker);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(v -> {
            String billName = editTextBillName.getText().toString().trim();
            String amountStr = editTextAmount.getText().toString().trim();

            if (billName.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            Calendar calendar = Calendar.getInstance();
            calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            Date dueDate = calendar.getTime();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();
            Map<String, Object> bill = new HashMap<>();
            bill.put("userId", userId);
            bill.put("name", billName);
            bill.put("amount", amount);
            bill.put("dueDate", dueDate);
            bill.put("paid", false);

            db.collection("users").document(userId).collection("bills")
                    .add(bill)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Bill added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUpcomingBills();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error adding bill", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}

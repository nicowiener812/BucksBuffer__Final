package com.example.bucksbuffer;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bucksbuffer.adapter.BillsAdapter;
import com.example.bucksbuffer.model.Bill;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends BaseActivity {

    private PieChart pieChartEarnings;
    private BarChart barChartExpenses;
    private CalendarView calendarView;
    private MaterialButton buttonAddExpense, buttonAddIncome;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private Map<Long, String> scheduleNotes;
    private RecyclerView recyclerViewUpcomingBills;
    private BillsAdapter upcomingBillsAdapter;
    private FirebaseAuth mAuth;
    private TextView textViewTotalExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_home);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        scheduleNotes = new HashMap<>();

        pieChartEarnings = findViewById(R.id.pieChartEarnings);
        barChartExpenses = findViewById(R.id.barChartExpenses);
        calendarView = findViewById(R.id.calendarView);
        buttonAddExpense = findViewById(R.id.buttonAddExpense);
        buttonAddIncome = findViewById(R.id.buttonAddIncome);
        textViewTotalExpense = findViewById(R.id.textViewTotalExpense);

        mAuth = FirebaseAuth.getInstance();

        setupCharts();
        setupCalendar();
        setupButtons();
        recyclerViewUpcomingBills = findViewById(R.id.recyclerViewUpcomingBills);
        setupUpcomingBillsRecyclerView();
        loadUpcomingBills();
        loadData();
    }
    private void setupUpcomingBillsRecyclerView() {
        upcomingBillsAdapter = new BillsAdapter(new ArrayList<>(), this::onBillPaid);
        recyclerViewUpcomingBills.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewUpcomingBills.setAdapter(upcomingBillsAdapter);
    }

    private void loadUpcomingBills() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(userId).collection("bills")
                .whereEqualTo("paid", false)
                .orderBy("dueDate")
                .limit(15)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Bill> bills = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Bill bill = document.toObject(Bill.class);
                        bill.setId(document.getId());
                        bills.add(bill);
                    }
                    upcomingBillsAdapter.updateBills(bills);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading upcoming bills", Toast.LENGTH_SHORT).show();
                });
    }

    private void onBillPaid(Bill bill) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).collection("bills").document(bill.getId())
                .update("paid", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bill marked as paid", Toast.LENGTH_SHORT).show();
                    addBillToExpenses(bill, userId);
                    loadUpcomingBills();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating bill", Toast.LENGTH_SHORT).show();
                });
    }

    private void addBillToExpenses(Bill bill, String userId) {
        Map<String, Object> expense = new HashMap<>();
        expense.put("userId", userId);
        expense.put("title", bill.getName());
        expense.put("category", "Bills");
        expense.put("amount", bill.getAmount());
        expense.put("timestamp", new Date().getTime());

        db.collection("users").document(userId).collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Bill added to expenses", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add bill to expenses", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCharts() {
        pieChartEarnings.setUsePercentValues(true);
        pieChartEarnings.getDescription().setEnabled(false);
        pieChartEarnings.setExtraOffsets(5, 10, 5, 5);
        pieChartEarnings.setDragDecelerationFrictionCoef(0.95f);
        pieChartEarnings.setDrawHoleEnabled(true);
        pieChartEarnings.setHoleColor(Color.WHITE);
        pieChartEarnings.setTransparentCircleRadius(61f);

        barChartExpenses.getDescription().setEnabled(false);
        barChartExpenses.setMaxVisibleValueCount(60);
        barChartExpenses.setPinchZoom(false);
        barChartExpenses.setDrawBarShadow(false);
        barChartExpenses.setDrawGridBackground(false);
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            long dateInMillis = getDateInMillis(year, month, dayOfMonth);
            showNoteDialog(dateInMillis);
        });
    }

    private long getDateInMillis(int year, int month, int day) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(year, month, day);
        return calendar.getTimeInMillis();
    }

    private void setupButtons() {
        buttonAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        buttonAddIncome.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddIncomeActivity.class);
            startActivity(intent);
        });
    }

    private void loadData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        loadEarnings(userId);
        loadExpenses(userId);
    }

    private void loadEarnings(String userId) {
        db.collection("incomes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<PieEntry> entries = new ArrayList<>();
                        float totalEarnings = 0f;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String source = document.getString("source");
                            double amount = document.getDouble("amount");
                            entries.add(new PieEntry((float) amount, source));
                            totalEarnings += amount;
                        }


                        PieDataSet dataSet = new PieDataSet(entries, "Income Sources");
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                        PieData data = new PieData(dataSet);
                        data.setValueTextSize(11f);
                        data.setValueTextColor(Color.WHITE);

                        pieChartEarnings.setData(data);
                        pieChartEarnings.invalidate();

                        logAnalyticsEvent("earnings_loaded", totalEarnings);
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(task.getException());
                    }
                });
    }

    private void loadExpenses(String userId) {
        db.collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<BarEntry> entries = new ArrayList<>();
                        Map<String, Float> categoryTotals = new HashMap<>();
                        float totalExpense = 0f;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String category = document.getString("category");
                            double amount = document.getDouble("amount");
                            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0f) + (float) amount);
                            totalExpense += amount;
                        }

                        // Set total expense text
                        String totalExpenseText = String.format("Total Expense: $%.2f", totalExpense);
                        textViewTotalExpense.setText(totalExpenseText);

                        int index = 0;
                        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
                            entries.add(new BarEntry(index++, entry.getValue(), entry.getKey()));
                        }

                        BarDataSet dataSet = new BarDataSet(entries, "Expense Categories");
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                        BarData data = new BarData(dataSet);
                        data.setBarWidth(0.9f);

                        barChartExpenses.setData(data);
                        barChartExpenses.invalidate();

                        logAnalyticsEvent("expenses_loaded", entries.size());
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(task.getException());
                    }
                });
    }

    private void showNoteDialog(long dateInMillis) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_schedule_note);

        EditText editTextNote = dialog.findViewById(R.id.editTextNote);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);

        loadNoteFromFirestore(dateInMillis, editTextNote);

        buttonSave.setOnClickListener(v -> {
            String note = editTextNote.getText().toString().trim();
            if (!note.isEmpty()) {
                saveNoteToFirestore(dateInMillis, note);
                Toast.makeText(HomeActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                logAnalyticsEvent("schedule_note_added", 1);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadNoteFromFirestore(long dateInMillis, EditText editTextNote) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateKey = String.valueOf(dateInMillis);
        db.collection("users").document(userId).collection("schedule_notes")
                .document(dateKey)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String note = documentSnapshot.getString("note");
                        editTextNote.setText(note);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading note", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNoteToFirestore(long dateInMillis, String note) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateKey = String.valueOf(dateInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(new Date(dateInMillis));

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("userId", userId);
        noteData.put("dateInMillis", dateKey);
        noteData.put("note", note);
        noteData.put("date", formattedDate);

        db.collection("users").document(userId).collection("schedule_notes")
                .document(dateKey)
                .set(noteData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HomeActivity.this, "Note saved to database", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Failed to save note to database", Toast.LENGTH_SHORT).show();
                });
    }



    private void logAnalyticsEvent(String eventName, double value) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
        bundle.putString("user_id", userId);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }
}



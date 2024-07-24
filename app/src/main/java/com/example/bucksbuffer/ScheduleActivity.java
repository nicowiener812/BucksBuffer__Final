package com.example.bucksbuffer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bucksbuffer.adapter.NoteAdapter;
import com.example.bucksbuffer.model.Note;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleActivity extends BaseActivity {

    private CalendarView calendarView;
    private TextView textViewSelectedDate;
    private EditText editTextNote;
    private Button buttonSaveNote;
    private FirebaseFirestore db;
    private String selectedDate;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerViewNotes;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendarView = findViewById(R.id.calendarViewSchedule);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        editTextNote = findViewById(R.id.editTextNote);
        buttonSaveNote = findViewById(R.id.buttonSaveNote);
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);

        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(notesList);

        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(noteAdapter);

        setupCalendar();
        setupSaveButton();
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            textViewSelectedDate.setText(String.format("Selected Date: %s", selectedDate));
            loadNoteAndShowSaveSection(selectedDate);
        });

        long currentDate = System.currentTimeMillis();
        calendarView.setDate(currentDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(currentDate));
        textViewSelectedDate.setText(String.format("Selected Date: %s", selectedDate));
        loadNoteAndShowSaveSection(selectedDate);
    }

    private void setupSaveButton() {
        buttonSaveNote.setOnClickListener(v -> saveNote());
    }

    private void loadNoteAndShowSaveSection(String date) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("schedule_notes")
                .document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String note = documentSnapshot.getString("note");
                        editTextNote.setText(note);
                    } else {
                        editTextNote.setText("");
                    }
                    findViewById(R.id.cardViewNote).setVisibility(View.VISIBLE);
                    loadAllNotes(userId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading note", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllNotes(String userId) {
        db.collection("users").document(userId).collection("schedule_notes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Note note = document.toObject(Note.class);
                        notesList.add(note);
                    }
                    noteAdapter.updateNotes(notesList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading notes", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNote() {
        String noteText = editTextNote.getText().toString().trim();
        if (noteText.isEmpty()) {
            Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note", noteText);
        noteData.put("date", selectedDate);

        db.collection("users").document(userId).collection("schedule_notes")
                .document(selectedDate)
                .set(noteData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    loadAllNotes(userId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show();
                });
    }
}

package com.example.bucksbuffer.model;

import java.util.Date;

public class Bill {
    private String id;
    private String name;
    private double amount;
    private Date dueDate;
    private boolean paid;

    // No-arg constructor for Firestore
    public Bill() {}

    public Bill(String name, double amount, Date dueDate, boolean paid) {
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = paid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
    public void setDueDate(com.google.firebase.Timestamp timestamp) {
        this.dueDate = timestamp.toDate();
    }
}
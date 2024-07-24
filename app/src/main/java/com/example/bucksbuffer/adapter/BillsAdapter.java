package com.example.bucksbuffer.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bucksbuffer.R;
import com.example.bucksbuffer.model.Bill;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BillsAdapter extends RecyclerView.Adapter<BillsAdapter.BillViewHolder> {

    private List<Bill> bills;
    private OnBillPaidListener onBillPaidListener;

    public BillsAdapter(List<Bill> bills, OnBillPaidListener onBillPaidListener) {
        this.bills = bills;
        this.onBillPaidListener = onBillPaidListener;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = bills.get(position);
        holder.bind(bill);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    public void updateBills(List<Bill> newBills) {
        this.bills = newBills;
        notifyDataSetChanged();
    }

    class BillViewHolder extends RecyclerView.ViewHolder {
        TextView textViewBillName, textViewAmount, textViewDueDate;
        CheckBox checkBoxPaid;

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewBillName = itemView.findViewById(R.id.textViewBillName);
            textViewAmount = itemView.findViewById(R.id.textViewAmount);
            textViewDueDate = itemView.findViewById(R.id.textViewDueDate);
            checkBoxPaid = itemView.findViewById(R.id.checkBoxPaid);
        }

        void bind(Bill bill) {
            textViewBillName.setText(bill.getName());
            textViewAmount.setText(String.format(Locale.getDefault(), "$%.2f", bill.getAmount()));
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            textViewDueDate.setText(sdf.format(bill.getDueDate()));
            checkBoxPaid.setChecked(bill.isPaid());

            checkBoxPaid.setOnClickListener(v -> {
                if (checkBoxPaid.isChecked()) {
                    onBillPaidListener.onBillPaid(bill);
                }
            });
        }
    }

    public interface OnBillPaidListener {
        void onBillPaid(Bill bill);
    }
}
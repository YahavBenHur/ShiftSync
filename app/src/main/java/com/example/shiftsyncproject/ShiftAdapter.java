package com.example.shiftsyncproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {

    private final ArrayList<Shift> shiftList;

    public ShiftAdapter(ArrayList<Shift> shiftList) {
        this.shiftList = shiftList;
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shift_item, parent, false);

        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {

        Shift shift = shiftList.get(position);

        // שימוש ב-getters במקום גישה ישירה לשדות, כי Shift.java עודכן
        // לשדות private עם encapsulation
        // משמרת פתוחה (עדיין לא משובצת) - employeeName ריק, מציגים placeholder ברור
        String displayName = shift.isOpen() ? "פתוחה - לא משובצת" : shift.getEmployeeName();
        holder.employeeNameText.setText(displayName);

        holder.dateText.setText(shift.getDate());

        holder.timeText.setText(
                shift.getStartTime() + " - " + shift.getEndTime()
        );
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    public static class ShiftViewHolder extends RecyclerView.ViewHolder {

        TextView employeeNameText;
        TextView dateText;
        TextView timeText;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);

            employeeNameText =
                    itemView.findViewById(R.id.employeeNameText);

            dateText =
                    itemView.findViewById(R.id.dateText);

            timeText =
                    itemView.findViewById(R.id.timeText);
        }
    }
}
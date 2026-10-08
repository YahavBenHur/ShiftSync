package com.example.shiftsyncproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Locale;

/**
 * מסך יצירת משמרת (למנהל). המשמרת נוצרת "פתוחה" (employeeUid ריק) ומקבלת
 * את ה-groupId של המנהל היוצר - כך שרק עובדים מאותה קבוצה יראו וישבצו
 * את עצמם אליה.
 */
public class CreateShiftFragment extends Fragment {

    private TextView dateText;
    private TextView startTimeText;
    private TextView endTimeText;

    private String selectedDate = null;
    private String selectedStartTime = null;
    private String selectedEndTime = null;

    public CreateShiftFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_shift, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dateText = view.findViewById(R.id.dateText);
        startTimeText = view.findViewById(R.id.startTimeText);
        endTimeText = view.findViewById(R.id.endTimeText);
        Button saveButton = view.findViewById(R.id.saveShiftButton);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        dateText.setOnClickListener(v -> showDatePicker());
        startTimeText.setOnClickListener(v -> showTimePicker(startTimeText, true));
        endTimeText.setOnClickListener(v -> showTimePicker(endTimeText, false));

        saveButton.setOnClickListener(v -> saveShift(mainActivity));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            dateText.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker(TextView targetText, boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            targetText.setText(formatted);
            if (isStartTime) {
                selectedStartTime = formatted;
            } else {
                selectedEndTime = formatted;
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
                .show();
    }

    private void saveShift(MainActivity mainActivity) {
        if (selectedDate == null || selectedStartTime == null || selectedEndTime == null) {
            Toast.makeText(getContext(), "יש למלא תאריך ושעות", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "יש להתחבר מחדש", Toast.LENGTH_SHORT).show();
            return;
        }

        // צריך את ה-groupId של המנהל לפני שיוצרים את המשמרת, כדי שהיא
        // תשויך לקבוצה הנכונה.
        mainActivity.getData(currentUser.getUid(), new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }

                Shift shift = new Shift(null, "", "", selectedDate, selectedStartTime,
                        selectedEndTime, employee.getGroupId());

                mainActivity.createShift(shift, new MainActivity.OnShiftSavedListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "המשמרת נוצרה ופתוחה לשיבוץ", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(CreateShiftFragment.this).popBackStack();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), "שגיאה בשמירה: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת פרטי מנהל", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
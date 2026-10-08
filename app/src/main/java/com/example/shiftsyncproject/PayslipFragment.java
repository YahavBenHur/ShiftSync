package com.example.shiftsyncproject;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class PayslipFragment extends Fragment {

    private LinearLayout rateInputContainer;
    private EditText hourlyRateInput;
    private Button calculateButton;

    private View resultsHeader;
    private Spinner monthSpinner;
    private View resultsScroll;
    private LinearLayout shiftsContainer;
    private View summaryContainer;
    private TextView totalHoursText;
    private TextView totalPayText;

    private List<Shift> allShifts = new ArrayList<>();
    private double currentHourlyRate = 0;

    public PayslipFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payslip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(PayslipFragment.this).popBackStack());

        rateInputContainer = view.findViewById(R.id.rateInputContainer);
        hourlyRateInput = view.findViewById(R.id.hourlyRateInput);
        calculateButton = view.findViewById(R.id.calculateButton);

        resultsHeader = view.findViewById(R.id.resultsHeader);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        resultsScroll = view.findViewById(R.id.resultsScroll);
        shiftsContainer = view.findViewById(R.id.shiftsContainer);
        summaryContainer = view.findViewById(R.id.summaryContainer);
        totalHoursText = view.findViewById(R.id.totalHoursText);
        totalPayText = view.findViewById(R.id.totalPayText);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        calculateButton.setOnClickListener(v -> {
            // סוגרים את המקלדת המספרית מיד - אחרת היא נשארת פתוחה ומכסה
            // את התוצאות, ונראה כאילו "כלום לא קרה" בלחיצה.
            hideKeyboard();

            String rateStr = hourlyRateInput.getText().toString().trim();

            if (TextUtils.isEmpty(rateStr)) {
                Toast.makeText(getContext(), "יש להזין שכר לשעה", Toast.LENGTH_SHORT).show();
                return;
            }

            double hourlyRate;
            try {
                hourlyRate = Double.parseDouble(rateStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "שכר לא תקין", Toast.LENGTH_SHORT).show();
                return;
            }

            if (hourlyRate < 0) {
                Toast.makeText(getContext(), "שכר לא יכול להיות שלילי", Toast.LENGTH_SHORT).show();
                return;
            }

            currentHourlyRate = hourlyRate;
            loadShifts(mainActivity);
        });
    }

    private void hideKeyboard() {
        if (getContext() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(hourlyRateInput.getWindowToken(), 0);
        }
    }

    private void loadShifts(MainActivity mainActivity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "יש להתחבר מחדש", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        mainActivity.getData(uid, new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }

                mainActivity.getShiftsForEmployee(uid, employee.getGroupId(),
                        new MainActivity.OnShiftListLoadedListener() {
                            @Override
                            public void onLoaded(List<Shift> shifts) {
                                if (getContext() == null) {
                                    return;
                                }

                                allShifts = shifts;

                                if (shifts.isEmpty()) {
                                    Toast.makeText(getContext(), "אין לך עדיין משמרות", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                setupMonthSpinner();
                            }

                            @Override
                            public void onError(String message) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "שגיאה בטעינת משמרות", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת פרטי עובד", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupMonthSpinner() {
        LinkedHashSet<String> months = new LinkedHashSet<>();
        for (Shift shift : allShifts) {
            if (shift.getDate() != null && shift.getDate().length() >= 7) {
                months.add(shift.getDate().substring(0, 7));
            }
        }

        List<String> sortedMonths = new ArrayList<>(months);
        Collections.sort(sortedMonths, Collections.reverseOrder());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, sortedMonths);
        monthSpinner.setAdapter(adapter);

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sortedMonths.size()) {
                    calculateForMonth(sortedMonths.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        rateInputContainer.setVisibility(View.GONE);
        resultsHeader.setVisibility(View.VISIBLE);
        resultsScroll.setVisibility(View.VISIBLE);
        summaryContainer.setVisibility(View.VISIBLE);
    }

    private void calculateForMonth(String yearMonth) {
        shiftsContainer.removeAllViews();

        double totalHours = 0;
        for (Shift shift : allShifts) {
            if (shift.getDate() != null && shift.getDate().startsWith(yearMonth)) {
                double hours = shift.getDurationInHours();
                totalHours += hours;
                shiftsContainer.addView(buildShiftRow(shift, hours));
            }
        }

        double totalPay = totalHours * currentHourlyRate;

        totalHoursText.setText(String.format(Locale.getDefault(),
                "סה\"כ שעות (%s): %.2f", yearMonth, totalHours));
        totalPayText.setText(String.format(Locale.getDefault(),
                "סה\"כ לתשלום: %.2f ₪", totalPay));
    }

    private TextView buildShiftRow(Shift shift, double hours) {
        TextView row = new TextView(getContext());
        row.setText(String.format(Locale.getDefault(),
                "%s | %s - %s | %.2f שעות",
                shift.getDate(), shift.getStartTime(), shift.getEndTime(), hours));
        row.setTextSize(15);
        row.setPadding(0, 12, 0, 12);
        return row;
    }
}


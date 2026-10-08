package com.example.shiftsyncproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * מסך "משמרות פתוחות" - מציג רק משמרות פתוחות מתוך הקבוצה של המשתמש
 * המחובר. העובד משבץ את עצמו, עם בדיקת התנגשויות מול המשמרות הקיימות
 * שלו לפני שהשיבוץ מתבצע בפועל.
 */
public class OpenShiftsFragment extends Fragment {

    private LinearLayout openShiftsContainer;
    private TextView emptyStateText;
    private String currentGroupId;

    public OpenShiftsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_open_shifts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        openShiftsContainer = view.findViewById(R.id.openShiftsContainer);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        View backButton = view.findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(OpenShiftsFragment.this).popBackStack());

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "יש להתחבר מחדש", Toast.LENGTH_SHORT).show();
            return;
        }

        // קודם מביאים את ה-groupId של המשתמש, ורק אז את המשמרות הפתוחות
        // של אותה קבוצה בלבד.
        mainActivity.getData(currentUser.getUid(), new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }
                currentGroupId = employee.getGroupId();
                loadOpenShifts(mainActivity);
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת פרטי משתמש", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadOpenShifts(MainActivity mainActivity) {
        mainActivity.getOpenShifts(currentGroupId, new MainActivity.OnShiftListLoadedListener() {
            @Override
            public void onLoaded(List<Shift> shifts) {
                if (getContext() == null) {
                    return;
                }

                openShiftsContainer.removeAllViews();

                if (shifts.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    return;
                }
                emptyStateText.setVisibility(View.GONE);

                for (Shift shift : shifts) {
                    openShiftsContainer.addView(buildShiftRow(mainActivity, shift));
                }
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת משמרות פתוחות", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private View buildShiftRow(MainActivity mainActivity, Shift shift) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);

        TextView info = new TextView(getContext());
        info.setText(shift.getDate() + " | " + shift.getStartTime() + " - " + shift.getEndTime());
        info.setTextSize(15);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoParams);

        Button assignButton = new Button(getContext());
        assignButton.setText("שיבוץ");
        assignButton.setOnClickListener(v -> tryAssign(mainActivity, shift));

        row.addView(info);
        row.addView(assignButton);
        return row;
    }

    private void tryAssign(MainActivity mainActivity, Shift targetShift) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "יש להתחבר מחדש", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        mainActivity.getShiftsForEmployee(uid, currentGroupId, new MainActivity.OnShiftListLoadedListener() {
            @Override
            public void onLoaded(List<Shift> existingShifts) {
                if (getContext() == null) {
                    return;
                }

                for (Shift existing : existingShifts) {
                    if (targetShift.overlapsWith(existing)) {
                        Toast.makeText(getContext(),
                                "יש לך כבר משמרת חופפת בתאריך ובשעות האלה",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                assignNow(mainActivity, targetShift, uid);
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בבדיקת התנגשויות", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void assignNow(MainActivity mainActivity, Shift shift, String uid) {
        mainActivity.getData(uid, new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }

                mainActivity.assignShiftToEmployee(shift.getId(), uid, employee.getName(),
                        new MainActivity.OnShiftSavedListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "שובצת למשמרת", Toast.LENGTH_SHORT).show();
                                loadOpenShifts(mainActivity);
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(getContext(), "שגיאה בשיבוץ: " + message,
                                        Toast.LENGTH_LONG).show();
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
}
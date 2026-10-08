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
 * מסך "שיבוצים" (למנהל בלבד) - רשימת כל המשמרות שכבר משובצות לעובדים,
 * עם אפשרות לבטל שיבוץ (המשמרת חוזרת להיות "פתוחה" לשיבוץ מחדש).
 */
public class AssignmentsFragment extends Fragment {

    private LinearLayout assignmentsContainer;
    private TextView emptyStateText;
    private String currentGroupId;

    public AssignmentsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assignments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        assignmentsContainer = view.findViewById(R.id.assignmentsContainer);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        View backButton = view.findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(AssignmentsFragment.this).popBackStack());

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "יש להתחבר מחדש", Toast.LENGTH_SHORT).show();
            return;
        }

        mainActivity.getData(currentUser.getUid(), new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }
                currentGroupId = employee.getGroupId();
                loadAssignments(mainActivity);
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת פרטי משתמש", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadAssignments(MainActivity mainActivity) {
        mainActivity.getAssignedShifts(currentGroupId, new MainActivity.OnShiftListLoadedListener() {
            @Override
            public void onLoaded(List<Shift> shifts) {
                if (getContext() == null) {
                    return;
                }

                assignmentsContainer.removeAllViews();

                if (shifts.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    return;
                }
                emptyStateText.setVisibility(View.GONE);

                for (Shift shift : shifts) {
                    assignmentsContainer.addView(buildShiftRow(mainActivity, shift));
                }
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת שיבוצים", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private View buildShiftRow(MainActivity mainActivity, Shift shift) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);

        TextView info = new TextView(getContext());
        info.setText(shift.getDate() + " | " + shift.getStartTime() + " - " + shift.getEndTime()
                + " | " + shift.getEmployeeName());
        info.setTextSize(14);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoParams);

        Button cancelButton = new Button(getContext());
        cancelButton.setText("ביטול שיבוץ");
        cancelButton.setTextSize(11);
        cancelButton.setOnClickListener(v -> {
            mainActivity.unassignShift(shift.getId(), new MainActivity.OnShiftSavedListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "השיבוץ בוטל - המשמרת פתוחה שוב", Toast.LENGTH_SHORT).show();
                    loadAssignments(mainActivity);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "שגיאה בביטול: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        row.addView(info);
        row.addView(cancelButton);
        return row;
    }
}
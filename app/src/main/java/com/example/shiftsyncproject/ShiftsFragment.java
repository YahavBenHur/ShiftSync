package com.example.shiftsyncproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * מסך "המשמרות שלי" - ההתנהגות שונה לפי תפקיד:
 * - עובד: רואה רק את המשמרות העתידיות שמשויכות אליו אישית.
 * - מנהל: רואה סקירה כללית של כל המשמרות העתידיות של הקבוצה שלו
 *   (גם פתוחות וגם משובצות), ובנוסף רואה כפתור "שיבוצים" לניהול שיבוצים.
 */
public class ShiftsFragment extends Fragment {

    public ShiftsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shifts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.shiftsRecyclerView);
        View emptyStateText = view.findViewById(R.id.emptyStateText);
        View addShiftFab = view.findViewById(R.id.addShiftFab);
        View payslipButton = view.findViewById(R.id.payslipButton);
        View openShiftsButton = view.findViewById(R.id.openShiftsButton);
        View assignmentsButton = view.findViewById(R.id.assignmentsButton);
        View logoutButton = view.findViewById(R.id.logoutButton);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        addShiftFab.setVisibility(View.GONE);
        assignmentsButton.setVisibility(View.GONE);

        payslipButton.setOnClickListener(v ->
                NavHostFragment.findNavController(ShiftsFragment.this)
                        .navigate(R.id.action_shiftsFragment_to_payslipFragment));

        openShiftsButton.setOnClickListener(v ->
                NavHostFragment.findNavController(ShiftsFragment.this)
                        .navigate(R.id.action_shiftsFragment_to_openShiftsFragment));

        assignmentsButton.setOnClickListener(v ->
                NavHostFragment.findNavController(ShiftsFragment.this)
                        .navigate(R.id.action_shiftsFragment_to_assignmentsFragment));

        addShiftFab.setOnClickListener(v ->
                NavHostFragment.findNavController(ShiftsFragment.this)
                        .navigate(R.id.action_shiftsFragment_to_createShiftFragment));

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            NavHostFragment.findNavController(ShiftsFragment.this)
                    .navigate(R.id.action_shiftsFragment_to_loginFragment);
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String uid = currentUser.getUid();

        mainActivity.getData(uid, new MainActivity.OnEmployeeLoadedListener() {
            @Override
            public void onLoaded(Employee employee) {
                if (getContext() == null || employee == null) {
                    return;
                }

                boolean isManager = "MANAGER".equalsIgnoreCase(employee.getRole());
                addShiftFab.setVisibility(isManager ? View.VISIBLE : View.GONE);
                assignmentsButton.setVisibility(isManager ? View.VISIBLE : View.GONE);

                if (isManager) {
                    loadGroupFutureShifts(employee.getGroupId(), recyclerView, emptyStateText);
                } else {
                    loadMyFutureShifts(employee.getGroupId(), uid, recyclerView, emptyStateText);
                }
            }

            @Override
            public void onError(String message) {
                // לא מציגים FAB/שיבוצים, וגם לא טוענים משמרות אם לא ידוע ה-groupId
            }
        });
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void loadMyFutureShifts(String groupId, String uid,
                                    RecyclerView recyclerView, View emptyStateText) {
        String today = today();
        Query shiftsQuery = FirebaseDatabase.getInstance()
                .getReference("shifts").orderByChild("groupId").equalTo(groupId);

        bindShiftsQuery(shiftsQuery, recyclerView, emptyStateText, shift ->
                uid.equals(shift.getEmployeeUid())
                        && shift.getDate() != null
                        && shift.getDate().compareTo(today) >= 0);
    }

    private void loadGroupFutureShifts(String groupId,
                                       RecyclerView recyclerView, View emptyStateText) {
        String today = today();
        Query shiftsQuery = FirebaseDatabase.getInstance()
                .getReference("shifts").orderByChild("groupId").equalTo(groupId);

        bindShiftsQuery(shiftsQuery, recyclerView, emptyStateText, shift ->
                shift.getDate() != null && shift.getDate().compareTo(today) >= 0);
    }

    private interface ShiftFilter {
        boolean matches(Shift shift);
    }

    private void bindShiftsQuery(Query shiftsQuery, RecyclerView recyclerView,
                                 View emptyStateText, ShiftFilter filter) {
        ArrayList<Shift> shiftList = new ArrayList<>();
        ShiftAdapter adapter = new ShiftAdapter(shiftList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        shiftsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (getContext() == null) {
                    return;
                }

                shiftList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Shift shift = snapshot.getValue(Shift.class);
                    if (shift != null && filter.matches(shift)) {
                        shiftList.add(shift);
                    }
                }

                adapter.notifyDataSetChanged();

                boolean isEmpty = shiftList.isEmpty();
                emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load shifts", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
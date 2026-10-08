package com.example.shiftsyncproject;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
    }

    public interface OnAuthResultListener {
        void onSuccess();
        void onError(String message);
    }

    public void register(String name, String phone, String address, String email,
                         String password, double hourlyRate, String role, String groupId,
                         OnAuthResultListener listener) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            listener.onError("Please fill all fields");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        addData(uid, name, phone, address, email, hourlyRate, role, groupId);
                        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                        listener.onSuccess();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Register failed: " + message, Toast.LENGTH_LONG).show();
                        listener.onError(message);
                    }
                });
    }

    public void login(String email, String password, OnAuthResultListener listener) {
        if (email.isEmpty() || password.isEmpty()) {
            listener.onError("Please fill all fields");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        listener.onSuccess();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Login failed: " + message, Toast.LENGTH_LONG).show();
                        listener.onError(message);
                    }
                });
    }

    public void addData(String uid, String name, String phone, String address,
                        String email, double hourlyRate, String role, String groupId) {

        Employee person = new Employee(uid, name, phone, address, email, hourlyRate, role, groupId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users").child(uid);

        myRef.setValue(person)
                .addOnSuccessListener(unused ->
                        android.util.Log.d("ShiftSync", "Employee saved successfully: " + uid))
                .addOnFailureListener(e ->
                        android.util.Log.e("ShiftSync", "Failed to save employee: " + e.getMessage(), e));
    }

    public interface OnEmployeeLoadedListener {
        void onLoaded(Employee employee);
        void onError(String message);
    }

    public void getData(String uid, OnEmployeeLoadedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users").child(uid);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Employee value = dataSnapshot.getValue(Employee.class);
                listener.onLoaded(value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public interface OnEmployeeListLoadedListener {
        void onLoaded(java.util.List<Employee> employees);
        void onError(String message);
    }

    public void getAllEmployees(OnEmployeeListLoadedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                java.util.List<Employee> employees = new java.util.ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Employee employee = snapshot.getValue(Employee.class);
                    if (employee != null) {
                        employees.add(employee);
                    }
                }
                listener.onLoaded(employees);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public interface OnShiftSavedListener {
        void onSuccess();
        void onError(String message);
    }

    public void createShift(Shift shift, OnShiftSavedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference shiftsRef = database.getReference("shifts");

        String newShiftId = shiftsRef.push().getKey();
        if (newShiftId == null) {
            listener.onError("Could not generate shift id");
            return;
        }
        shift.setId(newShiftId);

        shiftsRef.child(newShiftId).setValue(shift)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnShiftListLoadedListener {
        void onLoaded(java.util.List<Shift> shifts);
        void onError(String message);
    }

    public void getShiftsForEmployee(String employeeUid, String groupId,
                                     OnShiftListLoadedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference shiftsRef = database.getReference("shifts");

        shiftsRef.orderByChild("groupId").equalTo(groupId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        java.util.List<Shift> shifts = new java.util.ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Shift shift = snapshot.getValue(Shift.class);
                            if (shift != null && employeeUid.equals(shift.getEmployeeUid())) {
                                shifts.add(shift);
                            }
                        }
                        listener.onLoaded(shifts);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    public void getOpenShifts(String groupId, OnShiftListLoadedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference shiftsRef = database.getReference("shifts");

        shiftsRef.orderByChild("groupId").equalTo(groupId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        java.util.List<Shift> shifts = new java.util.ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Shift shift = snapshot.getValue(Shift.class);
                            if (shift != null && shift.isOpen()) {
                                shifts.add(shift);
                            }
                        }
                        listener.onLoaded(shifts);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    /**
     * משמרות שכבר משובצות (לא פתוחות) - לתמונת המצב של המנהל במסך
     * "שיבוצים", שם הוא יכול גם לבטל שיבוץ קיים.
     */
    public void getAssignedShifts(String groupId, OnShiftListLoadedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference shiftsRef = database.getReference("shifts");

        shiftsRef.orderByChild("groupId").equalTo(groupId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        java.util.List<Shift> shifts = new java.util.ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Shift shift = snapshot.getValue(Shift.class);
                            if (shift != null && !shift.isOpen()) {
                                shifts.add(shift);
                            }
                        }
                        listener.onLoaded(shifts);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    public void assignShiftToEmployee(String shiftId, String employeeUid, String employeeName,
                                      OnShiftSavedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference shiftRef = database.getReference("shifts").child(shiftId);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("employeeUid", employeeUid);
        updates.put("employeeName", employeeName);

        shiftRef.updateChildren(updates)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * ביטול שיבוץ - קריאה חוזרת ל-assignShiftToEmployee עם ערכים ריקים,
     * שמחזירה את המשמרת למצב "פתוחה" (isOpen() חוזר להיות true).
     */
    public void unassignShift(String shiftId, OnShiftSavedListener listener) {
        assignShiftToEmployee(shiftId, "", "", listener);
    }
}
package com.example.shiftsyncproject;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.security.SecureRandom;

/**
 * מסך הרשמה. עכשיו כולל בחירת תפקיד (מנהל/עובד) וקוד קבוצה:
 * - מנהל: מקבל קוד קבוצה חדש שנוצר אוטומטית (מוצג לו בסוף כדי שיעביר לעובדים).
 * - עובד: מזין את הקוד שקיבל מהמנהל שלו, כדי להצטרף לאותה קבוצה.
 */
public class RegisterFragment extends Fragment {

    private static final double DEFAULT_HOURLY_RATE = 0.0;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // בלי 0/O ו-1/I - קל לבלבל
    private static final int CODE_LENGTH = 6;

    public RegisterFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText emailEt = view.findViewById(R.id.registerEmail);
        EditText passwordEt = view.findViewById(R.id.registerPassword);
        EditText nameEt = view.findViewById(R.id.registerName);
        EditText phoneEt = view.findViewById(R.id.registerPhone);
        EditText addressEt = view.findViewById(R.id.registerAddress);
        RadioGroup roleRadioGroup = view.findViewById(R.id.roleRadioGroup);
        EditText groupCodeInput = view.findViewById(R.id.groupCodeInput);
        View managerInfoText = view.findViewById(R.id.managerInfoText);
        Button registerBtn = view.findViewById(R.id.button3);

        // מחליפים בין "שדה קוד קבוצה" (לעובד) ל"הודעת מידע" (למנהל) לפי הבחירה
        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isManager = checkedId == R.id.roleManagerRadio;
            groupCodeInput.setVisibility(isManager ? View.GONE : View.VISIBLE);
            managerInfoText.setVisibility(isManager ? View.VISIBLE : View.GONE);
        });

        registerBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String name = nameEt.getText().toString().trim();
            String phone = phoneEt.getText().toString().trim();
            String address = addressEt.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()
                    || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity == null) {
                return;
            }

            int selectedId = roleRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedRadio = view.findViewById(selectedId);
            boolean isManager = selectedRadio != null
                    && selectedRadio.getId() == R.id.roleManagerRadio;
            String role = isManager ? "MANAGER" : "EMPLOYEE";

            String groupId;
            if (isManager) {
                // המנהל יוצר קבוצה חדשה - קוד אקראי שנציג לו בסוף
                groupId = generateGroupCode();
            } else {
                groupId = groupCodeInput.getText().toString().trim().toUpperCase();
                if (groupId.isEmpty()) {
                    Toast.makeText(getContext(), "יש להזין קוד קבוצה מהמנהל שלך",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String finalGroupId = groupId;
            mainActivity.register(name, phone, address, email, password,
                    DEFAULT_HOURLY_RATE, role, finalGroupId, new MainActivity.OnAuthResultListener() {
                        @Override
                        public void onSuccess() {
                            if (isManager) {
                                showGroupCodeDialog(finalGroupId);
                            } else {
                                NavHostFragment.findNavController(RegisterFragment.this)
                                        .navigate(R.id.action_registerFragment_to_loginFragment);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            // ה-Toast כבר מוצג בתוך MainActivity.register, אין צורך לכפול כאן
                        }
                    });
        });
    }

    /**
     * מציגה למנהל את קוד הקבוצה שלו ב-dialog (לא Toast, כי Toast נעלם מהר
     * מדי ואי אפשר להעתיק ממנו) - חשוב שהמנהל יספיק להעתיק/לרשום את זה
     * לפני שהוא ממשיך הלאה, כי זה מה שהעובדים יזינו כדי להצטרף אליו.
     */
    private void showGroupCodeDialog(String groupCode) {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("נרשמת בהצלחה!")
                .setMessage("קוד הקבוצה שלך הוא:\n\n" + groupCode
                        + "\n\nתעבירי את הקוד הזה לעובדים שלך - הם יזינו אותו בהרשמה כדי להצטרף אליך.")
                .setCancelable(false)
                .setPositiveButton("הבנתי", (dialog, which) ->
                        NavHostFragment.findNavController(RegisterFragment.this)
                                .navigate(R.id.action_registerFragment_to_loginFragment))
                .show();
    }

    private String generateGroupCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }
}
package com.example.shiftsyncproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

/**
 * הערה: שיניתי מ-onCreateView לגישה עם onViewCreated - זו הפרקטיקה המומלצת
 * כשעובדים עם View Binding או findViewById ישירות, כי היא מפרידה בין
 * יצירת ה-View לבין האתחול שלו. אם אתה עדיין ב-onCreateView, זה עדיין תקין,
 * רק פחות נקי.
 *
 * הוספתי כאן ולידציה בסיסית לפני הקריאה ל-login(). אם ה-XML שלך משתמש
 * בשמות id שונים מ-R.id.Email / R.id.LoginPass / R.id.button2, תצטרך
 * להתאים את זה, ומומלץ מאוד לשנות את השמות ב-XML לשמות ברורים יותר
 * (למשל emailInput, passwordInput, createAccountButton).
 */
public class LoginFragment extends Fragment {

    public LoginFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText emailEt = view.findViewById(R.id.Email);
        EditText passwordEt = view.findViewById(R.id.LoginPass);
        Button loginBtn = view.findViewById(R.id.loginButton);
        Button createAccountBtn = view.findViewById(R.id.button2);

        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (!isInputValid(email, password, emailEt, passwordEt)) {
                return;
            }

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.login(email, password, new MainActivity.OnAuthResultListener() {
                    @Override
                    public void onSuccess() {
                        NavHostFragment.findNavController(LoginFragment.this)
                                .navigate(R.id.action_loginFragment_to_shiftsFragment);
                    }

                    @Override
                    public void onError(String message) {
                        // ה-Toast כבר מוצג בתוך MainActivity.login, אין צורך לכפול כאן
                    }
                });
            }
        });

        createAccountBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_loginFragment_to_registerFragment)
        );
    }

    /**
     * ולידציה בסיסית לפני שליחה ל-Firebase/שרת.
     * שים לב: זו רק בדיקת פורמט - היא לא מחליפה בדיקת אבטחה בצד השרת.
     */
    private boolean isInputValid(String email, String password,
                                 EditText emailEt, EditText passwordEt) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailEt.setError("יש להזין אימייל");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.setError("כתובת אימייל לא תקינה");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEt.setError("יש להזין סיסמה");
            valid = false;
        } else if (password.length() < 6) {
            // 6 תווים הוא המינימום שדורש Firebase Authentication
            passwordEt.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
            valid = false;
        }

        return valid;
    }
}
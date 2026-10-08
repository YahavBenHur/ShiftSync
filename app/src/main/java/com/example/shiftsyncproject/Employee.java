package com.example.shiftsyncproject;

/**
 * מודל עובד - מתאים לעבודה עם Firebase Realtime Database.
 * האימות (login/password) מנוהל ע"י Firebase Authentication, ולכן אין
 * כאן שדה password - רק uid שמקשר לחשבון ה-Auth.
 * groupId מקשר עובד לקבוצה (ארגון) של מנהל ספציפי - מנהל מייצר קוד קבוצה
 * בהרשמה, ועובדים מזינים את אותו קוד כדי להצטרף לאותה קבוצה.
 */
public class Employee {

    private String uid;
    private String name;
    private String phone;
    private String address;
    private String email;
    private double hourlyRate;
    private String role;         // "MANAGER" או "EMPLOYEE"
    private String groupId;      // קוד הקבוצה - מקשר עובד למנהל הספציפי שלו

    // קונסטרקטור ריק - חובה עבור Firebase לצורך דה-סריאליזציה אוטומטית
    public Employee() {
    }

    public Employee(String uid, String name, String phone, String address,
                    String email, double hourlyRate, String role, String groupId) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.email = email;
        this.hourlyRate = hourlyRate;
        this.role = role;
        this.groupId = groupId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }
}
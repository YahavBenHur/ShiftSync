package com.example.shiftsyncproject;

/**
 * מודל משמרת.
 * date בפורמט "yyyy-MM-dd", startTime/endTime בפורמט "HH:mm" (למשל "14:00").
 * groupId - אותו קוד קבוצה כמו של המנהל שיצר את המשמרת, כדי שרק עובדים
 * מאותה קבוצה יראו וישבצו את עצמם למשמרת הזו.
 */
public class Shift {

    private String id;
    private String employeeUid;   // ריק ("") = משמרת פתוחה, עדיין לא משובצת
    private String employeeName;
    private String date;
    private String startTime;
    private String endTime;
    private String groupId;

    public Shift() {
    }

    public Shift(String id, String employeeUid, String employeeName,
                 String date, String startTime, String endTime, String groupId) {
        this.id = id;
        this.employeeUid = employeeUid;
        this.employeeName = employeeName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeUid() {
        return employeeUid;
    }

    public void setEmployeeUid(String employeeUid) {
        this.employeeUid = employeeUid;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isOpen() {
        return employeeUid == null || employeeUid.isEmpty();
    }

    public boolean overlapsWith(Shift other) {
        if (other == null || date == null || !date.equals(other.date)) {
            return false;
        }

        Integer thisStart = toMinutesSinceMidnight(startTime);
        Integer thisEnd = toMinutesSinceMidnight(endTime);
        Integer otherStart = toMinutesSinceMidnight(other.startTime);
        Integer otherEnd = toMinutesSinceMidnight(other.endTime);

        if (thisStart == null || thisEnd == null || otherStart == null || otherEnd == null) {
            return false;
        }

        return thisStart < otherEnd && otherStart < thisEnd;
    }

    public double getDurationInHours() {
        Integer startMinutes = toMinutesSinceMidnight(startTime);
        Integer endMinutes = toMinutesSinceMidnight(endTime);

        if (startMinutes == null || endMinutes == null || endMinutes <= startMinutes) {
            return 0.0;
        }

        return (endMinutes - startMinutes) / 60.0;
    }

    private Integer toMinutesSinceMidnight(String time) {
        if (time == null) {
            return null;
        }
        String[] parts = time.trim().split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                return null;
            }
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
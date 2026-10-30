package com.app.projectcecily;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class RelationshipManager {

    private static final String PREFS_NAME = "prefs";
    private static final String FIREBASE_URL = "https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public static class Details {
        public String yourBirthday = "";
        public String partnerBirthday = "";
        public String yourGender = "Male (He/Him)";
        public String partnerGender = "Female (She/Her)";
        public String anniversaryDate = "";
        public long anniversaryTimestamp = 0;
        public String milestones = "";
    }

    public interface DetailsCallback {
        void onDetailsLoaded(Details details);
    }

    public static Details getLocalDetails(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Details details = new Details();
        details.yourBirthday = prefs.getString("rel_your_bday", "");
        details.partnerBirthday = prefs.getString("rel_partner_bday", "");
        details.yourGender = prefs.getString("rel_your_gender", "Male (He/Him)");
        details.partnerGender = prefs.getString("rel_partner_gender", "Female (She/Her)");
        details.anniversaryDate = prefs.getString("rel_anniversary_date", "");
        details.anniversaryTimestamp = prefs.getLong("rel_anniversary_ts", 0);
        details.milestones = prefs.getString("rel_milestones", "");
        return details;
    }

    public static void saveLocalDetails(Context context, Details details) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString("rel_your_bday", details.yourBirthday)
                .putString("rel_partner_bday", details.partnerBirthday)
                .putString("rel_your_gender", details.yourGender)
                .putString("rel_partner_gender", details.partnerGender)
                .putString("rel_anniversary_date", details.anniversaryDate)
                .putLong("rel_anniversary_ts", details.anniversaryTimestamp)
                .putString("rel_milestones", details.milestones)
                .apply();
    }

    public static void syncWithFirebase(Context context, Details details) {
        saveLocalDetails(context, details);
        String userId = UserManager.getUserId(context);
        String partnerId = UserManager.getPartnerId(context);

        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        Map<String, Object> map = new HashMap<>();
        map.put("yourBirthday", details.yourBirthday);
        map.put("partnerBirthday", details.partnerBirthday);
        map.put("yourGender", details.yourGender);
        map.put("partnerGender", details.partnerGender);
        map.put("anniversaryDate", details.anniversaryDate);
        map.put("anniversaryTimestamp", details.anniversaryTimestamp);
        map.put("milestones", details.milestones);

        db.child("relationships").child(userId).setValue(map);
        if (!partnerId.isEmpty()) {
            Map<String, Object> partnerMap = new HashMap<>();
            partnerMap.put("yourBirthday", details.partnerBirthday);
            partnerMap.put("partnerBirthday", details.yourBirthday);
            partnerMap.put("yourGender", details.partnerGender);
            partnerMap.put("partnerGender", details.yourGender);
            partnerMap.put("anniversaryDate", details.anniversaryDate);
            partnerMap.put("anniversaryTimestamp", details.anniversaryTimestamp);
            partnerMap.put("milestones", details.milestones);
            db.child("relationships").child(partnerId).setValue(partnerMap);
        }
    }

    public static void listenFromFirebase(Context context, DetailsCallback callback) {
        String userId = UserManager.getUserId(context);
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        db.child("relationships").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Details details = new Details();
                    details.yourBirthday = snapshot.child("yourBirthday").getValue(String.class) != null ? snapshot.child("yourBirthday").getValue(String.class) : "";
                    details.partnerBirthday = snapshot.child("partnerBirthday").getValue(String.class) != null ? snapshot.child("partnerBirthday").getValue(String.class) : "";
                    details.yourGender = snapshot.child("yourGender").getValue(String.class) != null ? snapshot.child("yourGender").getValue(String.class) : "Male (He/Him)";
                    details.partnerGender = snapshot.child("partnerGender").getValue(String.class) != null ? snapshot.child("partnerGender").getValue(String.class) : "Female (She/Her)";
                    details.anniversaryDate = snapshot.child("anniversaryDate").getValue(String.class) != null ? snapshot.child("anniversaryDate").getValue(String.class) : "";
                    Long ts = snapshot.child("anniversaryTimestamp").getValue(Long.class);
                    details.anniversaryTimestamp = ts != null ? ts : 0;
                    details.milestones = snapshot.child("milestones").getValue(String.class) != null ? snapshot.child("milestones").getValue(String.class) : "";

                    saveLocalDetails(context, details);
                    if (callback != null) callback.onDetailsLoaded(details);
                } else {
                    if (callback != null) callback.onDetailsLoaded(getLocalDetails(context));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static String formatDaysTogether(long anniversaryTimestamp) {
        if (anniversaryTimestamp <= 0) return "Set anniversary in settings";
        long diffMs = System.currentTimeMillis() - anniversaryTimestamp;
        if (diffMs < 0) return "Anniversary coming up!";
        long days = diffMs / (1000L * 60 * 60 * 24);
        return "Together for " + days + " days";
    }

    public static CharSequence formatPartnerBirthdayCountdown(String partnerName, String partnerBirthdayStr, String partnerHexColor) {
        String name = (partnerName != null && !partnerName.trim().isEmpty()) ? partnerName.trim() : "Partner";
        String hex = (partnerHexColor != null && !partnerHexColor.trim().isEmpty()) ? partnerHexColor.trim() : "#FF4081";

        if (partnerBirthdayStr == null || partnerBirthdayStr.trim().isEmpty()) {
            String html = "<font color='" + hex + "'><b>" + name + "</b></font>'s Birthday: Not set";
            return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
        }

        try {
            String[] parts = partnerBirthdayStr.trim().split("-");
            int month;
            int day;
            if (parts.length >= 3) {
                month = Integer.parseInt(parts[1]);
                day = Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                month = Integer.parseInt(parts[0]);
                day = Integer.parseInt(parts[1]);
            } else {
                String html = "<font color='" + hex + "'><b>" + name + "</b></font>'s Birthday: Not set";
                return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
            }

            java.util.Calendar today = java.util.Calendar.getInstance();
            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
            today.set(java.util.Calendar.MINUTE, 0);
            today.set(java.util.Calendar.SECOND, 0);
            today.set(java.util.Calendar.MILLISECOND, 0);

            java.util.Calendar nextBday = java.util.Calendar.getInstance();
            nextBday.set(java.util.Calendar.YEAR, today.get(java.util.Calendar.YEAR));
            nextBday.set(java.util.Calendar.MONTH, month - 1);
            nextBday.set(java.util.Calendar.DAY_OF_MONTH, day);
            nextBday.set(java.util.Calendar.HOUR_OF_DAY, 0);
            nextBday.set(java.util.Calendar.MINUTE, 0);
            nextBday.set(java.util.Calendar.SECOND, 0);
            nextBday.set(java.util.Calendar.MILLISECOND, 0);

            if (nextBday.before(today)) {
                nextBday.add(java.util.Calendar.YEAR, 1);
            }

            long diffMillis = nextBday.getTimeInMillis() - today.getTimeInMillis();
            long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis);

            String daysText;
            if (daysLeft == 0) daysText = "Today!";
            else if (daysLeft == 1) daysText = "1 day left";
            else daysText = daysLeft + " days left";

            String html = "<font color='" + hex + "'><b>" + name + "</b></font>'s Birthday: <font color='" + hex + "'><b>" + daysText + "</b></font>";
            return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
        } catch (Exception e) {
            String html = "<font color='" + hex + "'><b>" + name + "</b></font>'s Birthday: Not set";
            return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
        }
    }

    public static CharSequence formatPartnerCurrentTime(Context context, String partnerName, String partnerTimeZoneId, String partnerHexColor) {
        String name = (partnerName != null && !partnerName.trim().isEmpty()) ? partnerName.trim() : "Partner";
        String hex = (partnerHexColor != null && !partnerHexColor.trim().isEmpty()) ? partnerHexColor.trim() : "#FF4081";
        if (partnerTimeZoneId == null || partnerTimeZoneId.trim().isEmpty()) {
            partnerTimeZoneId = java.util.TimeZone.getDefault().getID();
        }

        try {
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(partnerTimeZoneId);
            java.text.SimpleDateFormat timeSdf;
            if (android.text.format.DateFormat.is24HourFormat(context)) {
                timeSdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
            } else {
                timeSdf = new java.text.SimpleDateFormat("h:mma", java.util.Locale.US);
            }
            timeSdf.setTimeZone(tz);
            String timeStr = timeSdf.format(new java.util.Date()).toLowerCase();

            java.text.SimpleDateFormat codeSdf = new java.text.SimpleDateFormat("z", java.util.Locale.US);
            codeSdf.setTimeZone(tz);
            String codeStr = codeSdf.format(new java.util.Date());

            String html = "<font color='" + hex + "'><b>" + name + "</b></font>'s Time: <font color='" + hex + "'><b>" + timeStr + "</b></font> (" + codeStr + ")";
            return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
        } catch (Exception e) {
            return "";
        }
    }
}

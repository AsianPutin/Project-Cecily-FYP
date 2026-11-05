package com.app.projectcecily;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class CountryCodePickerHelper {

    public interface OnCountrySelectedListener {
        void onCountrySelected(CountryCodeItem country);
    }

    public static List<CountryCodeItem> getGlobalCountryList() {
        List<CountryCodeItem> list = new ArrayList<>();
        list.add(new CountryCodeItem("🇲🇾", "+60", "Malaysia", "MY", "MYS"));
        list.add(new CountryCodeItem("🇸🇬", "+65", "Singapore", "SG", "SGP"));
        list.add(new CountryCodeItem("🇮🇩", "+62", "Indonesia", "ID", "IDN"));
        list.add(new CountryCodeItem("🇹🇭", "+66", "Thailand", "TH", "THA"));
        list.add(new CountryCodeItem("🇻🇳", "+84", "Vietnam", "VN", "VNM"));
        list.add(new CountryCodeItem("🇵🇭", "+63", "Philippines", "PH", "PHL"));
        list.add(new CountryCodeItem("🇰🇭", "+855", "Cambodia", "KH", "KHM"));
        list.add(new CountryCodeItem("🇱🇦", "+856", "Laos", "LA", "LAO"));
        list.add(new CountryCodeItem("🇲🇲", "+95", "Myanmar", "MM", "MMR"));
        list.add(new CountryCodeItem("🇧🇳", "+673", "Brunei", "BN", "BRN"));
        list.add(new CountryCodeItem("🇹🇱", "+670", "Timor-Leste", "TL", "TLS"));
        list.add(new CountryCodeItem("🇨🇳", "+86", "China", "CN", "CHN"));
        list.add(new CountryCodeItem("🇭🇰", "+852", "Hong Kong", "HK", "HKG"));
        list.add(new CountryCodeItem("🇲🇴", "+853", "Macau", "MO", "MAC"));
        list.add(new CountryCodeItem("🇹🇼", "+886", "Taiwan", "TW", "TWN"));
        list.add(new CountryCodeItem("🇯🇵", "+81", "Japan", "JP", "JPN"));
        list.add(new CountryCodeItem("🇰🇷", "+82", "South Korea", "KR", "KOR"));
        list.add(new CountryCodeItem("🇮🇳", "+91", "India", "IN", "IND"));
        list.add(new CountryCodeItem("🇵🇰", "+92", "Pakistan", "PK", "PAK"));
        list.add(new CountryCodeItem("🇧🇩", "+880", "Bangladesh", "BD", "BGD"));
        list.add(new CountryCodeItem("🇱🇰", "+94", "Sri Lanka", "LK", "LKA"));
        list.add(new CountryCodeItem("🇳🇵", "+977", "Nepal", "NP", "NPL"));
        list.add(new CountryCodeItem("🇸🇦", "+966", "Saudi Arabia", "SA", "SAU"));
        list.add(new CountryCodeItem("🇦🇪", "+971", "United Arab Emirates", "AE", "ARE"));
        list.add(new CountryCodeItem("🇶🇦", "+974", "Qatar", "QA", "QAT"));
        list.add(new CountryCodeItem("🇰🇼", "+965", "Kuwait", "KW", "KWT"));
        list.add(new CountryCodeItem("🇧🇭", "+973", "Bahrain", "BH", "BHR"));
        list.add(new CountryCodeItem("🇴🇲", "+968", "Oman", "OM", "OMN"));
        list.add(new CountryCodeItem("🇯🇴", "+962", "Jordan", "JO", "JOR"));
        list.add(new CountryCodeItem("🇱🇧", "+961", "Lebanon", "LB", "LBN"));
        list.add(new CountryCodeItem("🇮🇷", "+98", "Iran", "IR", "IRN"));
        list.add(new CountryCodeItem("🇮🇶", "+964", "Iraq", "IQ", "IRQ"));
        list.add(new CountryCodeItem("🇹🇷", "+90", "Turkey", "TR", "TUR"));
        list.add(new CountryCodeItem("🇺🇸", "+1", "United States", "US", "USA"));
        list.add(new CountryCodeItem("🇨🇦", "+1", "Canada", "CA", "CAN"));
        list.add(new CountryCodeItem("🇬🇧", "+44", "United Kingdom", "GB", "GBR"));
        list.add(new CountryCodeItem("🇦🇺", "+61", "Australia", "AU", "AUS"));
        list.add(new CountryCodeItem("🇳🇿", "+64", "New Zealand", "NZ", "NZL"));
        list.add(new CountryCodeItem("🇩🇪", "+49", "Germany", "DE", "DEU"));
        list.add(new CountryCodeItem("🇫🇷", "+33", "France", "FR", "FRA"));
        list.add(new CountryCodeItem("🇮🇹", "+39", "Italy", "IT", "ITA"));
        list.add(new CountryCodeItem("🇪🇸", "+34", "Spain", "ES", "ESP"));
        list.add(new CountryCodeItem("🇳🇱", "+31", "Netherlands", "NL", "NLD"));
        list.add(new CountryCodeItem("🇨🇭", "+41", "Switzerland", "CH", "CHE"));
        list.add(new CountryCodeItem("🇸🇪", "+46", "Sweden", "SE", "SWE"));
        list.add(new CountryCodeItem("🇳🇴", "+47", "Norway", "NO", "NOR"));
        list.add(new CountryCodeItem("🇩🇰", "+45", "Denmark", "DK", "DNK"));
        list.add(new CountryCodeItem("🇫🇮", "+358", "Finland", "FI", "FIN"));
        list.add(new CountryCodeItem("🇷🇺", "+7", "Russia", "RU", "RUS"));
        list.add(new CountryCodeItem("🇺🇦", "+380", "Ukraine", "UA", "UKR"));
        list.add(new CountryCodeItem("🇵🇱", "+48", "Poland", "PL", "POL"));
        list.add(new CountryCodeItem("🇬🇷", "+30", "Greece", "GR", "GRC"));
        list.add(new CountryCodeItem("🇧🇷", "+55", "Brazil", "BR", "BRA"));
        list.add(new CountryCodeItem("🇦🇷", "+54", "Argentina", "AR", "ARG"));
        list.add(new CountryCodeItem("🇲🇽", "+52", "Mexico", "MX", "MEX"));
        list.add(new CountryCodeItem("🇨🇱", "+56", "Chile", "CL", "CHL"));
        list.add(new CountryCodeItem("🇿🇦", "+27", "South Africa", "ZA", "ZAF"));
        list.add(new CountryCodeItem("🇪🇬", "+20", "Egypt", "EG", "EGY"));
        list.add(new CountryCodeItem("🇳🇬", "+234", "Nigeria", "NG", "NGA"));
        list.add(new CountryCodeItem("🇰🇪", "+254", "Kenya", "KE", "KEN"));
        return list;
    }

    public static CountryCodeItem findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String clean = code.trim();
        for (CountryCodeItem item : getGlobalCountryList()) {
            if (item.code.equalsIgnoreCase(clean)) {
                return item;
            }
        }
        return null;
    }

    public static CountryCodeItem detectUserCountry(Context context) {
        String countryIso = "";
        try {
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                countryIso = tm.getSimCountryIso();
                if (countryIso == null || countryIso.trim().isEmpty()) {
                    countryIso = tm.getNetworkCountryIso();
                }
            }
        } catch (Exception ignored) {}

        if (countryIso == null || countryIso.trim().isEmpty()) {
            try {
                countryIso = java.util.Locale.getDefault().getCountry();
            } catch (Exception ignored) {}
        }

        if (countryIso != null && !countryIso.trim().isEmpty()) {
            String iso2Upper = countryIso.trim().toUpperCase();
            for (CountryCodeItem item : getGlobalCountryList()) {
                if (item.iso2.equalsIgnoreCase(iso2Upper)) {
                    return item;
                }
            }
        }

        // Default fallback to Malaysia (🇲🇾 +60)
        return new CountryCodeItem("🇲🇾", "+60", "Malaysia", "MY", "MYS");
    }

    public static void showPicker(Context context, OnCountrySelectedListener listener) {
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_country_code_picker, null);
        sheet.setContentView(view);

        EditText etSearch = view.findViewById(R.id.etSearchCountry);
        RecyclerView rvCountries = view.findViewById(R.id.rvCountryCodes);

        List<CountryCodeItem> allCountries = getGlobalCountryList();
        CountryAdapter adapter = new CountryAdapter(allCountries, country -> {
            sheet.dismiss();
            if (listener != null) {
                listener.onCountrySelected(country);
            }
        });

        rvCountries.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                List<CountryCodeItem> filtered = new ArrayList<>();
                for (CountryCodeItem c : allCountries) {
                    if (c.matchesQuery(query)) {
                        filtered.add(c);
                    }
                }
                adapter.setList(filtered);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sheet.show();
    }

    private static class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.ViewHolder> {

        private List<CountryCodeItem> items;
        private final OnCountrySelectedListener listener;

        public CountryAdapter(List<CountryCodeItem> items, OnCountrySelectedListener listener) {
            this.items = items;
            this.listener = listener;
        }

        public void setList(List<CountryCodeItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_country_code, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CountryCodeItem item = items.get(position);
            holder.tvFlagEmoji.setText(item.flagEmoji);
            holder.tvCountryName.setText(item.name);
            holder.tvCountryIso.setText(item.iso2 + " / " + item.iso3);
            holder.tvCallingCode.setText(item.code);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCountrySelected(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFlagEmoji, tvCountryName, tvCountryIso, tvCallingCode;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFlagEmoji = itemView.findViewById(R.id.tvFlagEmoji);
                tvCountryName = itemView.findViewById(R.id.tvCountryName);
                tvCountryIso = itemView.findViewById(R.id.tvCountryIso);
                tvCallingCode = itemView.findViewById(R.id.tvCallingCode);
            }
        }
    }
}

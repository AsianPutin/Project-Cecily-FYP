package com.app.projectcecily;

public class CountryCodeItem {
    public String flagEmoji;
    public String code;
    public String name;
    public String iso2;
    public String iso3;

    public CountryCodeItem(String flagEmoji, String code, String name, String iso2, String iso3) {
        this.flagEmoji = flagEmoji;
        this.code = code;
        this.name = name;
        this.iso2 = iso2;
        this.iso3 = iso3;
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        String cleanCode = code.replace("+", "").toLowerCase();

        return name.toLowerCase().contains(q) ||
               code.toLowerCase().contains(q) ||
               cleanCode.contains(q) ||
               iso2.toLowerCase().equalsIgnoreCase(q) ||
               iso3.toLowerCase().equalsIgnoreCase(q) ||
               iso2.toLowerCase().contains(q) ||
               iso3.toLowerCase().contains(q);
    }
}

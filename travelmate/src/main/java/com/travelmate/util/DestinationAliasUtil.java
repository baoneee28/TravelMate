package com.travelmate.util;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DestinationAliasUtil {

    private static final Map<String, String> COMPACT_ALIAS_TO_SLUG = Map.ofEntries(
            Map.entry("dalat", "da-lat"),
            Map.entry("dlat", "da-lat"),
            Map.entry("nhatrang", "nha-trang"),
            Map.entry("danang", "da-nang"),
            Map.entry("dnang", "da-nang"),
            Map.entry("hoian", "hoi-an"),
            Map.entry("sapa", "sa-pa"),
            Map.entry("laocai", "lao-cai"),
            Map.entry("hagiang", "ha-giang"),
            Map.entry("ninhbinh", "ninh-binh"),
            Map.entry("hanoi", "ha-noi"),
            Map.entry("phuquoc", "phu-quoc"),
            Map.entry("tphcm", "ho-chi-minh"),
            Map.entry("hcm", "ho-chi-minh"),
            Map.entry("hcmc", "ho-chi-minh"),
            Map.entry("hochiminh", "ho-chi-minh"),
            Map.entry("hochiminhcity", "ho-chi-minh"),
            Map.entry("tphochiminh", "ho-chi-minh"),
            Map.entry("thanhphohochiminh", "ho-chi-minh"),
            Map.entry("saigon", "ho-chi-minh"),
            Map.entry("quangninh", "quang-ninh"),
            Map.entry("halong", "quang-ninh"),
            Map.entry("yenbai", "yen-bai"),
            Map.entry("sonla", "son-la"),
            Map.entry("mocchau", "son-la"),
            Map.entry("cantho", "can-tho"),
            Map.entry("vungtau", "vung-tau"),
            Map.entry("muine", "mui-ne"),
            Map.entry("phanrang", "ninh-thuan"),
            Map.entry("ninhthuan", "ninh-thuan")
    );

    private static final Map<String, List<String>> RELATED_SLUGS = Map.ofEntries(
            Map.entry("lao-cai", List.of("lao-cai", "sa-pa")),
            Map.entry("quang-ninh", List.of("quang-ninh", "ha-long"))
    );

    private static final Map<String, String> SLUG_TO_DISPLAY_NAME = Map.ofEntries(
            Map.entry("da-lat", "Đà Lạt"),
            Map.entry("nha-trang", "Nha Trang"),
            Map.entry("da-nang", "Đà Nẵng"),
            Map.entry("hoi-an", "Hội An"),
            Map.entry("sa-pa", "Sa Pa"),
            Map.entry("lao-cai", "Lào Cai"),
            Map.entry("ha-giang", "Hà Giang"),
            Map.entry("ninh-binh", "Ninh Bình"),
            Map.entry("ha-noi", "Hà Nội"),
            Map.entry("phu-quoc", "Phú Quốc"),
            Map.entry("ho-chi-minh", "TP. Hồ Chí Minh"),
            Map.entry("quang-ninh", "Quảng Ninh"),
            Map.entry("ha-long", "Hạ Long"),
            Map.entry("yen-bai", "Yên Bái"),
            Map.entry("son-la", "Sơn La"),
            Map.entry("can-tho", "Cần Thơ"),
            Map.entry("vung-tau", "Vũng Tàu"),
            Map.entry("mui-ne", "Mũi Né"),
            Map.entry("ninh-thuan", "Ninh Thuận")
    );

    private DestinationAliasUtil() {
    }

    public static String normalizeText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return Normalizer.normalize(
                        input.trim()
                                .replace("Đ", "D")
                                .replace("đ", "d"),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String compact(String normalizedText) {
        return normalizedText == null ? "" : normalizedText.replace(" ", "");
    }

    public static String normalizeSlug(String input) {
        String normalized = normalizeText(input);
        if (normalized.isBlank()) {
            return "";
        }

        String compact = compact(normalized);
        String aliasSlug = COMPACT_ALIAS_TO_SLUG.get(compact);
        if (aliasSlug != null) {
            return aliasSlug;
        }
        return normalized.replace(" ", "-");
    }

    public static Set<String> searchSlugs(String input) {
        String slug = normalizeSlug(input);
        LinkedHashSet<String> slugs = new LinkedHashSet<>();
        if (slug.isBlank()) {
            return slugs;
        }

        List<String> related = RELATED_SLUGS.get(slug);
        if (related != null) {
            slugs.addAll(related);
        } else {
            slugs.add(slug);
        }
        return slugs;
    }

    public static boolean matchesTextOrDestination(String value, String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }

        String normalizedValue = normalizeText(value);
        if (normalizedValue.isBlank()) {
            return false;
        }

        String compactKeyword = compact(normalizedKeyword);
        String compactValue = compact(normalizedValue);
        if (normalizedValue.contains(normalizedKeyword) || compactValue.contains(compactKeyword)) {
            return true;
        }

        Set<String> keywordSlugs = searchSlugs(keyword);
        Set<String> valueSlugs = searchSlugs(value);
        return keywordSlugs.stream().anyMatch(valueSlugs::contains);
    }

    public static String displayName(String input) {
        String slug = normalizeSlug(input);
        if (slug.isBlank()) {
            return "";
        }
        return SLUG_TO_DISPLAY_NAME.getOrDefault(slug, input != null ? input.trim() : "");
    }

}

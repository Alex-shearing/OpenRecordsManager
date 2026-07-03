package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.list.ListDefinition;

import java.util.*;

public class Lists {
    public static final ListDefinition SECURITY_CLASSIFICATION = ListDefinition.builder("Security Classification")
            .entry("unofficial", "UNOFFICIAL")
            .description("No damage. This information does not form part of official duty.")
            .alias("UO")
            .index(0)
            .endEntry()
            .entry("official", "OFFICIAL")
            .description("No or insignificant damage. This is the majority of routine information.")
            .alias("O")
            .index(10)
            .endEntry()
            .entry("official_sensitive", "OFFICIAL: Sensitive")
            .description("Limited damage to an individual, organisation or government generally if compromised.")
            .alias("OFFICIAL:Sensitive")
            .alias("OFFICIAL:SENSITIVE")
            .alias("O:S")
            .alias("OS")
            .index(20)
            .endEntry()
            .entry("protected", "PROTECTED")
            .description("Damage to the national interest, organisations or individuals.")
            .alias("P")
            .index(30)
            .endEntry()
            .entry("secret", "SECRET")
            .description("Serious damage to the national interest, organisations or individuals.")
            .alias("S")
            .index(40)
            .endEntry()
            .entry("top_secret", "TOP SECRET")
            .description("Exceptionally grave damage to the national interest, organisations or individuals.")
            .alias("TOP-SECRET")
            .alias("TS")
            .index(50)
            .endEntry()
            .entry("for_official_use_only", "For Official Use Only")
            .description("Discontinued in 2018 classification reforms.")
            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
            .alias("FOUO")
            .index(19)
            .endEntry()
            .entry("confidential", "CONFIDENTIAL")
            .description("Discontinued in 2018 classification reforms.")
            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
            .index(25)
            .endEntry()
            .entry("highly_protected", "HIGHLY PROTECTED")
            .description("Discontinued in 2012 classification reforms.")
            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
            .index(39)
            .endEntry()
            .entry("restricted", "RESTRICTED")
            .description("Discontinued in 2012 classification reforms.")
            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
            .index(25)
            .endEntry()
            .entry("x_in_confidence", "X-IN-CONFIDENCE")
            .description("Discontinued in 2012 classification reforms.")
            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
            .index(22)
            .endEntry()
            .build();

    public static final ListDefinition SECURITY_CAVEAT = ListDefinition.builder("Security Caveat")
            .entry("delicate_source", "DELICATE SOURCE").endEntry()
            .entry("orcon", "ORCON").endEntry()
            .entry("exclusive_for", "EXCLUSIVE FOR").endEntry()
            .entry("cabinet", "CABINET").endEntry()
            .entry("national_cabinet", "NATIONAL CABINET")
            .activeTo(new GregorianCalendar(2025, Calendar.JULY, 1))
            .endEntry()
            .build();
    public static final ListDefinition RELEASABILITY_CAVEAT = addCountyCodes(ListDefinition.builder("Releasability Caveat")
            .entry("austeo", "AUSTEO").description("Australian eyes only").index(0).endEntry())
            .entry("agao", "AGAO").description("Australian government agencies only").index(5).endEntry()
            .build();
    public static final ListDefinition INFORMATION_MANAGEMENT_MARKER = ListDefinition.builder("Information Management Marker")
            .entry("personal_privacy", "Personal-Privacy").endEntry()
            .entry("legal_privilege", "Legal-Privilege").endEntry()
            .entry("legislative_secrecy", "Legislative-Secrecy").endEntry()
            .build();
    public static final ListDefinition RECORD_CATEGORY = ListDefinition.builder("Record Category")
            .entry("archives", "Archives")
            .description(
                    "All of the records within a specified society, jurisdiction or sector brought into an encompassing framework."
            )
            .endEntry()
            .entry("archive", "Archive")
            .description("The whole body of records of an organisation or individual.")
            .endEntry()
            .entry("series", "Series")
            .description(
                    "A group of records created or maintained by an organisation or person that, regardless of currency, value or present " +
                            "custody, are in the same identifiable sequence, or result from the same accumulation or filing process, and are of " +
                            "similar function, format or information content.")
            .endEntry()
            .entry("file", "File")
            .description(
                    "A sequence of items, physically or virtually linked, that provides evidence of organisational or business activity. A file " +
                            "can be physical or electronic.")
            .endEntry()
            .entry("transaction_sequence", "Transaction Sequence")
            .description(
                    "A sequence of items, physically or virtually linked, that shows one coherent transaction leading to a specific outcome.")
            .endEntry()
            .entry("item", "Item")
            .description("The smallest discrete unit of records managed as an entity.")
            .endEntry()
            .build();

    private static ListDefinition.Builder addCountyCodes(ListDefinition.Builder builder) {
        Map<String, Integer> indexOverrides = Map.of(
                "AUS", 10,
                "CAN", 15,
                "GBR", 20,
                "NZL", 25,
                "USA", 30
        );

        List<String> twoLetterCodes = Arrays.stream(Locale.getISOCountries()).sorted().toList();

        for (int i = 0; i < twoLetterCodes.size(); i++) {
            String twoLetterCode = twoLetterCodes.get(i);
            Locale countryLocale = Locale.of("", twoLetterCode);

            String code = countryLocale.getISO3Country();

            builder.entry(code.toLowerCase(Locale.ROOT), code)
                    .description("Releasable to " + countryLocale.getDisplayCountry())
                    .index(indexOverrides.getOrDefault(code, 50 + i))
                    .endEntry();
        }

        return builder;
    }
}

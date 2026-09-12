package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.template.list.ListTemplate;

import java.util.*;

public class Lists {
    public static final ListTemplate SECURITY_CLASSIFICATION = ListTemplate.builder("Security Classification")
            .entry("unofficial", "UNOFFICIAL", e ->
                    e.description("No damage. This information does not form part of official duty.")
                            .alias("UO")
                            .index(0)
            )
            .entry("official", "OFFICIAL", e ->
                    e.description("No or insignificant damage. This is the majority of routine information.")
                            .alias("O")
                            .index(10)
            )
            .entry("official_sensitive", "OFFICIAL: Sensitive", e -> e
                    .description("Limited damage to an individual, organisation or government generally if compromised.")
                    .alias("OFFICIAL:Sensitive")
                    .alias("OFFICIAL:SENSITIVE")
                    .alias("O:S")
                    .alias("OS")
                    .index(20)
            )
            .entry("protected", "PROTECTED", e -> e
                    .description("Damage to the national interest, organisations or individuals.")
                    .alias("P")
                    .index(30)
            )
            .entry("secret", "SECRET", e -> e
                    .description("Serious damage to the national interest, organisations or individuals.")
                    .alias("S")
                    .index(40)
            )
            .entry("top_secret", "TOP SECRET", e -> e
                    .description("Exceptionally grave damage to the national interest, organisations or individuals.")
                    .alias("TOP-SECRET")
                    .alias("TS")
                    .index(50)
            )
            .entry("for_official_use_only", "For Official Use Only", e -> e
                    .description("Discontinued in 2018 classification reforms.")
                    .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                    .alias("FOUO")
                    .index(19)
            )
            .entry("confidential", "CONFIDENTIAL", e -> e
                    .description("Discontinued in 2018 classification reforms.")
                    .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                    .index(25)
            )
            .entry("highly_protected", "HIGHLY PROTECTED", e -> e
                    .description("Discontinued in 2012 classification reforms.")
                    .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                    .index(39)
            )
            .entry("restricted", "RESTRICTED", e -> e
                    .description("Discontinued in 2012 classification reforms.")
                    .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                    .index(25)
            )
            .entry("x_in_confidence", "X-IN-CONFIDENCE", e -> e
                    .description("Discontinued in 2012 classification reforms.")
                    .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                    .index(22)
            )
            .build();

    public static final ListTemplate SECURITY_CAVEAT = ListTemplate.builder("Security Caveat")
            .entry("delicate_source", "DELICATE SOURCE")
            .entry("orcon", "ORCON")
            .entry("exclusive_for", "EXCLUSIVE FOR")
            .entry("cabinet", "CABINET")
            .entry("national_cabinet", "NATIONAL CABINET", e -> e
                    .activeTo(new GregorianCalendar(2025, Calendar.JULY, 1))
            )
            .build();

    public static final ListTemplate RELEASABILITY_CAVEAT = addCountyCodes(ListTemplate.builder("Releasability Caveat")
            .entry("austeo", "AUSTEO", e -> e
                    .description("Australian eyes only")
                    .index(0)
            ))
            .entry("agao", "AGAO", e -> e
                    .description("Australian government agencies only")
                    .index(5)
            )
            .build();

    public static final ListTemplate INFORMATION_MANAGEMENT_MARKER = ListTemplate.builder("Information Management Marker")
            .entry("personal_privacy", "Personal-Privacy")
            .entry("legal_privilege", "Legal-Privilege")
            .entry("legislative_secrecy", "Legislative-Secrecy")
            .build();

    public static final ListTemplate RECORD_CATEGORY = ListTemplate.builder("Record Category")
            .entry("archives", "Archives", e -> e
                    .description(
                            "All of the records within a specified society, jurisdiction or sector brought into " +
                                    "an encompassing framework."
                    )
            )
            .entry("archive", "Archive", e -> e
                    .description("The whole body of records of an organisation or individual.")
            )
            .entry("series", "Series", e -> e
                    .description(
                            "A group of records created or maintained by an organisation or person that, regardless " +
                                    "of currency, value or present custody, are in the same identifiable sequence, " +
                                    "or result from the same accumulation or filing process, and are of similar" +
                                    "function, format or information content."
                    )
            )
            .entry("file", "File", e -> e
                    .description(
                            "A sequence of items, physically or virtually linked, that provides evidence of " +
                                    "organisational or business activity. A file can be physical or electronic."
                    )
            )
            .entry("transaction_sequence", "Transaction Sequence", e -> e
                    .description(
                            "A sequence of items, physically or virtually linked, that shows one coherent " +
                                    "transaction leading to a specific outcome."
                    )
            )
            .entry("item", "Item", e -> e
                    .description("The smallest discrete unit of records managed as an entity.")
            )
            .build();

    private static ListTemplate.Builder addCountyCodes(ListTemplate.Builder builder) {
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
            int index = indexOverrides.getOrDefault(code, 50 + i);

            builder.entry(code.toLowerCase(Locale.ROOT), code, e -> e
                    .description("Releasable to " + countryLocale.getDisplayCountry())
                    .index(index)
            );
        }

        return builder;
    }
}

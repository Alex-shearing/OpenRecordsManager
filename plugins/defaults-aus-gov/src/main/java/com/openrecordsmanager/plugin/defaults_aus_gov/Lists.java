package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.list.ListItem;

import java.util.*;

public class Lists {
    public static final ListDefinition SECURITY_CLASSIFICATION = ListDefinition.builder("security_classification")
            .entry("unofficial",
                    ListItem.builder("UNOFFICIAL")
                            .description("No damage. This information does not form part of official duty.")
                            .alias("UO")
                            .index(0)
                            .build()
            )
            .entry("official",
                    ListItem.builder("OFFICIAL")
                            .description("No or insignificant damage. This is the majority of routine information.")
                            .alias("O")
                            .index(10)
                            .build()
            )
            .entry("official_sensitive",
                    ListItem.builder("OFFICIAL: Sensitive")
                            .description("Limited damage to an individual, organisation or government generally if compromised.")
                            .alias("OFFICIAL:Sensitive")
                            .alias("OFFICIAL:SENSITIVE")
                            .alias("O:S")
                            .alias("OS")
                            .index(20)
                            .build()
            )
            .entry("protected",
                    ListItem.builder("PROTECTED")
                            .description("Damage to the national interest, organisations or individuals.")
                            .alias("P")
                            .index(30)
                            .build()
            )
            .entry("secret",
                    ListItem.builder("SECRET")
                            .description("Serious damage to the national interest, organisations or individuals.")
                            .alias("S")
                            .index(40)
                            .build()
            )
            .entry("top_secret",
                    ListItem.builder("TOP SECRET")
                            .description("Exceptionally grave damage to the national interest, organisations or individuals.")
                            .alias("TOP-SECRET")
                            .alias("TS")
                            .index(50)
                            .build()
            )
            .entry("for_official_use_only",
                    ListItem.builder("For Official Use Only")
                            .description("Discontinued in 2018 classification reforms.")
                            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                            .alias("FOUO")
                            .index(19)
                            .build()
            )
            .entry("confidential",
                    ListItem.builder("CONFIDENTIAL")
                            .description("Discontinued in 2018 classification reforms.")
                            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                            .index(25)
                            .build()
            )
            .entry("highly_protected",
                    ListItem.builder("HIGHLY PROTECTED")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(39)
                            .build()
            )
            .entry("restricted",
                    ListItem.builder("RESTRICTED")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(25)
                            .build()
            )
            .entry("x_in_confidence",
                    ListItem.builder("X-IN-CONFIDENCE")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(22)
                            .build()
            )
            .build();
    public static final ListDefinition SECURITY_CAVEAT = ListDefinition.builder("security_caveat")
            .entry("delicate_source", ListItem.builder("DELICATE SOURCE").build())
            .entry("orcon", ListItem.builder("ORCON").build())
            .entry("exclusive_for", ListItem.builder("EXCLUSIVE FOR").build())
            .entry("cabinet", ListItem.builder("CABINET").build())
            .entry("national_cabinet",
                    ListItem.builder("NATIONAL CABINET")
                            .activeTo(new GregorianCalendar(2025, Calendar.JULY, 1))
                            .build()
            )
            .build();
    public static final ListDefinition RELEASABILITY_CAVEAT = addCountyCodes(ListDefinition.builder("releasability_caveat")
            .entry("austeo", ListItem.builder("AUSTEO").description("Australian eyes only").index(0).build())
            .entry("agao", ListItem.builder("AGAO").description("Australian government agencies only").index(5).build()))
            .build();
    public static final ListDefinition INFORMATION_MANAGEMENT_MARKER = ListDefinition.builder("information_management_marker")
            .entry("personal_privacy", ListItem.builder("Personal-Privacy").build())
            .entry("legal_privilege", ListItem.builder("Legal-Privilege").build())
            .entry("legislative_secrecy", ListItem.builder("Legislative-Secrecy").build())
            .build();
    public static final ListDefinition RECORD_CATEGORY = ListDefinition.builder("record_category")
            .entry("archives",
                    ListItem.builder("Archives")
                            .description("All of the records within a specified society, jurisdiction or sector brought " +
                                    "into an encompassing framework.")
                            .build()
            )
            .entry("archive",
                    ListItem.builder("Archive")
                            .description("The whole body of records of an organisation or individual.")
                            .build()
            )
            .entry("series",
                    ListItem.builder("Series")
                            .description("A group of records created or maintained by an organisation or person that, " +
                                    "regardless of currency, value or present custody, are in the same identifiable " +
                                    "sequence, or result from the same accumulation or filing process, and are of " +
                                    "similar function, format or information content.")
                            .build()
            )
            .entry("file",
                    ListItem.builder("File")
                            .description("A sequence of items, physically or virtually linked, that provides " +
                                    "evidence of organisational or business activity. A file can be physical " +
                                    "or electronic.")
                            .build()
            )
            .entry("transaction_sequence",
                    ListItem.builder("Transaction Sequence")
                            .description("A sequence of items, physically or virtually linked, that shows one " +
                                    "coherent transaction leading to a specific outcome.")
                            .build()
            )
            .entry("item",
                    ListItem.builder("Item")
                            .description("The smallest discrete unit of records managed as an entity.")
                            .build()
            )
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

            builder.entry(code.toLowerCase(Locale.ROOT),
                    ListItem.builder(code)
                            .description("Releasable to " + countryLocale.getDisplayCountry())
                            .index(indexOverrides.getOrDefault(code, 50 + i))
                            .build()
            );
        }

        return builder;
    }
}

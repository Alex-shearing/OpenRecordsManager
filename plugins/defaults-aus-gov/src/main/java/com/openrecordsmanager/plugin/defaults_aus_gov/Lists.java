package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.list.ListItemDef;

import java.util.*;

public class Lists {
    public static final ListDefinition SECURITY_CLASSIFICATION = ListDefinition.builder("security_classification")
            .entry("unofficial",
                    ListItemDef.builder("UNOFFICIAL")
                            .description("No damage. This information does not form part of official duty.")
                            .alias("UO")
                            .index(0)
                            .build()
            )
            .entry("official",
                    ListItemDef.builder("OFFICIAL")
                            .description("No or insignificant damage. This is the majority of routine information.")
                            .alias("O")
                            .index(10)
                            .build()
            )
            .entry("official_sensitive",
                    ListItemDef.builder("OFFICIAL: Sensitive")
                            .description("Limited damage to an individual, organisation or government generally if compromised.")
                            .alias("OFFICIAL:Sensitive")
                            .alias("OFFICIAL:SENSITIVE")
                            .alias("O:S")
                            .alias("OS")
                            .index(20)
                            .build()
            )
            .entry("protected",
                    ListItemDef.builder("PROTECTED")
                            .description("Damage to the national interest, organisations or individuals.")
                            .alias("P")
                            .index(30)
                            .build()
            )
            .entry("secret",
                    ListItemDef.builder("SECRET")
                            .description("Serious damage to the national interest, organisations or individuals.")
                            .alias("S")
                            .index(40)
                            .build()
            )
            .entry("top_secret",
                    ListItemDef.builder("TOP SECRET")
                            .description("Exceptionally grave damage to the national interest, organisations or individuals.")
                            .alias("TOP-SECRET")
                            .alias("TS")
                            .index(50)
                            .build()
            )
            .entry("for_official_use_only",
                    ListItemDef.builder("For Official Use Only")
                            .description("Discontinued in 2018 classification reforms.")
                            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                            .alias("FOUO")
                            .index(19)
                            .build()
            )
            .entry("confidential",
                    ListItemDef.builder("CONFIDENTIAL")
                            .description("Discontinued in 2018 classification reforms.")
                            .activeTo(new GregorianCalendar(2018, Calendar.OCTOBER, 1))
                            .index(25)
                            .build()
            )
            .entry("highly_protected",
                    ListItemDef.builder("HIGHLY PROTECTED")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(39)
                            .build()
            )
            .entry("restricted",
                    ListItemDef.builder("RESTRICTED")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(25)
                            .build()
            )
            .entry("x_in_confidence",
                    ListItemDef.builder("X-IN-CONFIDENCE")
                            .description("Discontinued in 2012 classification reforms.")
                            .activeTo(new GregorianCalendar(2012, Calendar.AUGUST, 1))
                            .index(22)
                            .build()
            )
            .build();
    public static final ListDefinition SECURITY_CAVEAT = ListDefinition.builder("security_caveat")
            .entry("delicate_source", ListItemDef.builder("DELICATE SOURCE").build())
            .entry("orcon", ListItemDef.builder("ORCON").build())
            .entry("exclusive_for", ListItemDef.builder("EXCLUSIVE FOR").build())
            .entry("cabinet", ListItemDef.builder("CABINET").build())
            .entry("national_cabinet",
                    ListItemDef.builder("NATIONAL CABINET")
                            .activeTo(new GregorianCalendar(2025, Calendar.JULY, 1))
                            .build()
            )
            .build();
    public static final ListDefinition RELEASABILITY_CAVEAT = addCountyCodes(ListDefinition.builder("releasability_caveat")
            .entry("austeo", ListItemDef.builder("AUSTEO").description("Australian eyes only").index(0).build())
            .entry("agao", ListItemDef.builder("AGAO").description("Australian government agencies only").index(5).build()))
            .build();
    public static final ListDefinition INFORMATION_MANAGEMENT_MARKER = ListDefinition.builder("information_management_marker")
            .entry("personal_privacy", ListItemDef.builder("Personal-Privacy").build())
            .entry("legal_privilege", ListItemDef.builder("Legal-Privilege").build())
            .entry("legislative_secrecy", ListItemDef.builder("Legislative-Secrecy").build())
            .build();
    public static final ListDefinition RECORD_CATEGORY = ListDefinition.builder("record_category")
            .entry("archives",
                    ListItemDef.builder("Archives")
                            .description("All of the records within a specified society, jurisdiction or sector brought " +
                                    "into an encompassing framework.")
                            .build()
            )
            .entry("archive",
                    ListItemDef.builder("Archive")
                            .description("The whole body of records of an organisation or individual.")
                            .build()
            )
            .entry("series",
                    ListItemDef.builder("Series")
                            .description("A group of records created or maintained by an organisation or person that, " +
                                    "regardless of currency, value or present custody, are in the same identifiable " +
                                    "sequence, or result from the same accumulation or filing process, and are of " +
                                    "similar function, format or information content.")
                            .build()
            )
            .entry("file",
                    ListItemDef.builder("File")
                            .description("A sequence of items, physically or virtually linked, that provides " +
                                    "evidence of organisational or business activity. A file can be physical " +
                                    "or electronic.")
                            .build()
            )
            .entry("transaction_sequence",
                    ListItemDef.builder("Transaction Sequence")
                            .description("A sequence of items, physically or virtually linked, that shows one " +
                                    "coherent transaction leading to a specific outcome.")
                            .build()
            )
            .entry("item",
                    ListItemDef.builder("Item")
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
                    ListItemDef.builder(code)
                            .description("Releasable to " + countryLocale.getDisplayCountry())
                            .index(indexOverrides.getOrDefault(code, 50 + i))
                            .build()
            );
        }

        return builder;
    }
}

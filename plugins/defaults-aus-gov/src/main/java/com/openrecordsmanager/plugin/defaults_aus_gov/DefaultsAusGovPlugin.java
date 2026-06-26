package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.BuiltinComponents;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.PluginContext;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a set of defaults for the Australian Government.
 * The following sources were used:
 * - <a href="https://www.protectivesecurity.gov.au/system/files/2025-07/pspf-release-2025.pdf">Australian Government Protective Security Policy Framework</a>
 * - <a href="https://www.protectivesecurity.gov.au/system/files/2025-07/australian-government-email-protective-marking-standard-2025.PDF">Australian Government Protective Security Email Marking Standard</a>
 * - <a href="https://www.stylemanual.gov.au/writing-and-designing-content/security-classifications-and-protective-markings">Security classifications and protective markings</a>
 * - <a href="https://www.ombudsman.gov.au/__data/assets/pdf_file/0025/324169/20260206-FOI-2026-80019-Document-Bundle_Redacted.pdf">Guidance - Classifying and Distributing Office Information</a>
 * - <a href="https://www.security.tas.gov.au/protective-security/information-security/infosec-2-protecting-official-information">Tasmanian Government Protective Security Policy</a>
 */
public class DefaultsAusGovPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultsAusGovPlugin.class);

    public static final RecordTypeDefinition EMAIL_RECORD_TYPE = RecordTypeDefinition.builder("email")
            .name("Email Record")
            .description("Use this to record email records.")
            .supportsFile("text/plain", "text/html", "multipart/alternative", "multipart/mixed")
            .property(BuiltinComponents.DATE_CREATED)
            .property(BuiltinComponents.DATE_REGISTERED)
            .property(BuiltinComponents.NOTES, "This is an email")
            .property(RecordProperties.RECORD_SECURITY_CLASSIFICATION)
            .property(RecordProperties.RECORD_SECURITY_CAVEAT)
            .property(RecordProperties.RECORD_SECURITY_RELEASABILITY)
            .property(RecordProperties.RECORD_SECURITY_IMM)
            .property(RecordProperties.RECORD_CATEGORY)
            .property(RecordProperties.JURISDICTION)
            .build();

    @Override
    public String getName() {
        return "defaults_aus_gov";
    }

    @Override
    public void initialise(PluginContext registry) {
        LOGGER.info("Initializing plugin...");

        registry.registerComponents(
                Lists.SECURITY_CLASSIFICATION, Lists.SECURITY_CAVEAT, Lists.RELEASABILITY_CAVEAT, Lists.INFORMATION_MANAGEMENT_MARKER, Lists.RECORD_CATEGORY,

                RecordProperties.RECORD_SECURITY_CLASSIFICATION, RecordProperties.RECORD_SECURITY_CAVEAT, RecordProperties.RECORD_SECURITY_RELEASABILITY,
                RecordProperties.RECORD_SECURITY_IMM, RecordProperties.RECORD_CATEGORY, RecordProperties.JURISDICTION,

                UserProperties.USER_SECURITY_CLASSIFICATION, UserProperties.USER_SECURITY_CAVEAT,

                EMAIL_RECORD_TYPE
        );
    }
}

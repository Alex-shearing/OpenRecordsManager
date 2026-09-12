package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a set of defaults for the Australian Government.
 * Templates are auto-loaded from classpath JSON under {@code list/}, {@code object_property/},
 * and {@code record_type/} (see {@link com.openrecordsmanager.template.TemplateJsonLoader}).
 * <p>
 * Sources:
 * - <a href="https://www.protectivesecurity.gov.au/system/files/2025-07/pspf-release-2025.pdf">Australian Government Protective Security Policy Framework</a>
 * - <a href="https://www.protectivesecurity.gov.au/system/files/2025-07/australian-government-email-protective-marking-standard-2025.PDF">Australian Government Protective Security Email Marking Standard</a>
 * - <a href="https://www.stylemanual.gov.au/writing-and-designing-content/security-classifications-and-protective-markings">Security classifications and protective markings</a>
 * - <a href="https://www.ombudsman.gov.au/__data/assets/pdf_file/0025/324169/20260206-FOI-2026-80019-Document-Bundle_Redacted.pdf">Guidance - Classifying and Distributing Office Information</a>
 * - <a href="https://www.security.tas.gov.au/protective-security/information-security/infosec-2-protecting-official-information">Tasmanian Government Protective Security Policy</a>
 */
public class DefaultsAusGovPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultsAusGovPlugin.class);

    @Override
    public String getName() {
        return "defaults_aus_gov";
    }

    @Override
    public void initialise(RegistrationContext registry) {
        LOGGER.info("Initializing plugin (JSON templates auto-loaded from classpath)...");
    }
}

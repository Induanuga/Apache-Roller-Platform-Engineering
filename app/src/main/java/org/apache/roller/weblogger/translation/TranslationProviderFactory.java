/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.translation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.translation.impl.LingvaTranslationProvider;
import org.apache.roller.weblogger.translation.impl.SarvamTranslationProvider;

/**
 * Factory to obtain the configured TranslationProvider.
 *
 * Switch providers via roller-custom.properties (no recompile needed):
 *   translation.provider=sarvam   → Sarvam AI       (API key required)
 *   translation.provider=lingva   → Lingva Translate (NO key)
 *   translation.provider=gemini   → Google Gemini    (API key required)
 *   translation.provider=azure    → Microsoft Azure  (API key required)
 *
 * TWO ARCHITECTURALLY DIFFERENT APPROACHES:
 *   - Sarvam : Dedicated neural machine translation API (domain-specific MT model)
 *   - Lingva : LLM/Google-backed general translation (different engine entirely)
 *
 * Minimum config to run with zero API keys:
 *   translation.provider=sarvam   (has bundled demo key)
 *   — switch to lingva for the second provider with no key at all.
 */
public class TranslationProviderFactory {

    private static final Log log = LogFactory.getLog(TranslationProviderFactory.class);

    // -----------------------------------------------------------------------
    // Default provider — no API key needed for "lingva" or "sarvam" (demo key).
    // Valid values: "sarvam", "lingva", "gemini", "azure"
    // -----------------------------------------------------------------------
    public static final String DEFAULT_PROVIDER = "sarvam";

    private static TranslationProvider instance = null;

    /**
     * Returns a singleton TranslationProvider based on configuration.
     * Provider is selected by the system property "translation.provider",
     * falling back to DEFAULT_PROVIDER.
     */
    public static synchronized TranslationProvider getProvider() {
        if (instance != null) {
            return instance;
        }

        String providerId = System.getProperty("translation.provider", DEFAULT_PROVIDER).trim().toLowerCase();
        log.info("Translation provider selected: " + providerId);

        switch (providerId) {
            case "lingva":
                instance = new LingvaTranslationProvider();
                break;
            case "sarvam":
                instance = new SarvamTranslationProvider();
                break;
        }

        return instance;
    }

    /**
     * Force a specific provider (useful for testing or runtime switching).
     */
    public static synchronized void setProvider(TranslationProvider provider) {
        instance = provider;
    }

    /**
     * Reset the cached instance (forces re-read of config on next call).
     */
    public static synchronized void reset() {
        instance = null;
    }
}
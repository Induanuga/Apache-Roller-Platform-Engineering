/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.translation;

import java.util.List;
import java.util.Map;

/**
 * Interface for translation providers.
 * Implement this to add a new translation backend.
 * Supported providers: "sarvam", "gemini"
 */
public interface TranslationProvider {

    /**
     * Translate text from sourceLang to targetLang.
     * @param text       The text to translate
     * @param sourceLang BCP-47 language code (e.g. "en", "hi", "ta")
     * @param targetLang BCP-47 language code
     * @return Translated text
     * @throws TranslationException on failure
     */
    String translate(String text, String sourceLang, String targetLang) throws TranslationException;

    /**
     * Return the provider identifier, e.g. "sarvam", "gemini".
     */
    String getProviderId();

    /**
     * Return supported language codes.
     */
    List<Map<String, String>> getSupportedLanguages();
}
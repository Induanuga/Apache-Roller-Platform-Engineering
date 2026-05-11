/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.translation;

/**
 * Exception thrown when a translation operation fails.
 */
public class TranslationException extends Exception {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
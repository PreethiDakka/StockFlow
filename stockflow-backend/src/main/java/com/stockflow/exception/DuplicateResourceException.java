
package com.stockflow.exception;

public class DuplicateResourceException extends IllegalArgumentException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

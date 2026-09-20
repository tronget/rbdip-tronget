package com.rbdip.bookstore.customer;

public record CustomerName(String firstName, String lastName) {

    public static CustomerName from(String fullName) {
        String normalizedName = fullName.trim();
        int separator = normalizedName.indexOf(' ');
        if (separator < 0) {
            return new CustomerName(normalizedName, "");
        }
        return new CustomerName(
                normalizedName.substring(0, separator), normalizedName.substring(separator + 1).trim());
    }
}

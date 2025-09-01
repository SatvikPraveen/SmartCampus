// File: src/main/java/models/User.java
package models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a User in the Smart Campus system.
 * Demonstrates OOP principles: Abstraction, Encapsulation, and serves as base for inheritance.
 * 
 * Key Java concepts covered:
 * - Abstract classes and methods
 * - Encapsulation (private fields with getters/setters)
 * - Constructor chaining
 * - equals() and hashCode() implementation
 * - toString() override
 * - Java 8+ LocalDateTime usage
 */
public abstract class User {
    
    // Encapsulation: Private fields with controlled access
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isActive;
    
    // Protected constructor for inheritance
    protected User() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    // Parameterized constructor with validation
    protected User(String userId, String firstName, String lastName, String email, String phoneNumber) {
        this(); // Call default constructor
        setUserId(userId);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setPhoneNumber(phoneNumber);
    }
    
    // Abstract method - must be implemented by subclasses (Polymorphism)
    public abstract String getRole();
    
    // Abstract method for displaying user-specific information
    public abstract void displayInfo();
    
    // Encapsulation: Getters and Setters with validation
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.userId = userId.trim();
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        this.firstName = firstName.trim();
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        this.lastName = lastName.trim();
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email.trim().toLowerCase();
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber.trim();
        }
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    // Utility methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    // Object class overrides for proper comparison and string representation
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userId, user.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return String.format("%s{userId='%s', name='%s', email='%s', role='%s', active=%s}", 
            getClass().getSimpleName(), userId, getFullName(), email, getRole(), isActive);
    }
    
    // Method to validate user data (Template method pattern)
    public final boolean isValidUser() {
        return userId != null && !userId.trim().isEmpty() &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               email != null && email.contains("@") &&
               isActive;
    }
}
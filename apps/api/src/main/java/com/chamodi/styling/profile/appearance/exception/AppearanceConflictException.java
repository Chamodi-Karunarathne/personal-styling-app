package com.chamodi.styling.profile.appearance.exception;

public class AppearanceConflictException extends RuntimeException {
    public AppearanceConflictException() { super("Your personal details changed elsewhere. Reload before continuing"); }
}

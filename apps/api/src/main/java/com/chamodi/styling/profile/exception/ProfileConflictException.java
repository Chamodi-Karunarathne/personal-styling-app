package com.chamodi.styling.profile.exception;

public class ProfileConflictException extends RuntimeException {

	public ProfileConflictException() {
		super("Your preferences changed elsewhere. Reload before saving again");
	}
}

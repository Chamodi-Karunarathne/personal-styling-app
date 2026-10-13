package com.chamodi.styling.auth.dto;

public record CsrfResponse(String headerName, String token) {

	@Override
	public String toString() {
		return "CsrfResponse[token=[REDACTED]]";
	}

}

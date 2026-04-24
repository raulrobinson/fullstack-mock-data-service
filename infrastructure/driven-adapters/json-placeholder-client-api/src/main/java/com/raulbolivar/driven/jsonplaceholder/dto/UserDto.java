package com.raulbolivar.driven.jsonplaceholder.dto;

import lombok.Builder;

@Builder
public record UserDto(
	String website,
	Address address,
	String phone,
	String name,
	Company company,
	int id,
	String email,
	String username
) {
	@Builder
	public record Address(
			String zipcode,
			Geo geo,
			String suite,
			String city,
			String street
	) {
	}

	@Builder
	public record Company(
			String bs,
			String catchPhrase,
			String name
	) {
	}

	@Builder
	public record Geo(
			String lng,
			String lat
	) {
	}
}

package com.raulbolivar.model.user;

public record User(
	String website,
	Address address,
	String phone,
	String name,
	Company company,
	int id,
	String email,
	String username
) {
	public record Address(
			String zipcode,
			Geo geo,
			String suite,
			String city,
			String street
	) {
	}

	public record Company(
			String bs,
			String catchPhrase,
			String name
	) {
	}

	public record Geo(
			String lng,
			String lat
	) {
	}
}

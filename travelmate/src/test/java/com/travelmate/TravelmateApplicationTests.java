package com.travelmate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TravelmateApplicationTests {

	@Test
	void applicationEntryPointExists() {
		assertThat(TravelmateApplication.class).isNotNull();
	}

}

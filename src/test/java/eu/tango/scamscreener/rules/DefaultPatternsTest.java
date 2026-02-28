package eu.tango.scamscreener.rules;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPatternsTest {
	@Test
	void accountDataPatternRequiresContextForCode() {
		Pattern pattern = Pattern.compile(DefaultPatterns.ACCOUNT_DATA_PATTERN);

		assertTrue(pattern.matcher("please share your password").find());
		assertTrue(pattern.matcher("gimme the code").find());
		assertTrue(pattern.matcher("the code, give it").find());
		assertFalse(pattern.matcher("what is the code").find());
	}
}

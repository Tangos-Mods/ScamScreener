package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RareItemClassifierTest {
	@Test
	void classifiesExactItemKeyAsRare() {
		RareItemClassifier classifier = new RareItemClassifier(
			new RareItemClassifier.Rules(
				Set.of("crystal_helmet"),
				List.of(),
				List.of()
			)
		);

		RareItemClassifier.Match match = classifier.classify("crystal_helmet", "Crystal Helmet", List.of());
		assertTrue(match.rare());
	}

	@Test
	void classifiesLoreFragmentAsRare() {
		RareItemClassifier classifier = new RareItemClassifier(
			new RareItemClassifier.Rules(
				Set.of(),
				List.of(),
				List.of("dyed")
			)
		);

		RareItemClassifier.Match match = classifier.classify("some_item", "Some Item", List.of("This piece is dyed"));
		assertTrue(match.rare());
	}

	@Test
	void returnsNotRareWhenNoRuleMatches() {
		RareItemClassifier classifier = new RareItemClassifier(
			new RareItemClassifier.Rules(Set.of(), List.of("exotic"), List.of("dyed"))
		);

		RareItemClassifier.Match match = classifier.classify("aspect_of_the_end", "Aspect of the End", List.of("Teleport"));
		assertFalse(match.rare());
	}
}


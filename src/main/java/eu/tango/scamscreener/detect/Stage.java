package eu.tango.scamscreener.detect;

@FunctionalInterface
public interface Stage<I, O> {
	O apply(I input);
}

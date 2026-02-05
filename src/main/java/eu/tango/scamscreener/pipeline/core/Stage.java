package eu.tango.scamscreener.pipeline.core;

@FunctionalInterface
public interface Stage<I, O> {
	O apply(I input);
}

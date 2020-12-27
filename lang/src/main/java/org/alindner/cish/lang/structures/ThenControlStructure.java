package org.alindner.cish.lang.structures;

/**
 * 'if' structure
 */
public class ThenControlStructure {
	private final boolean condition;

	public ThenControlStructure(final boolean cond) {
		this.condition = cond;
	}

	/**
	 * if the given condition holds it executes the callback
	 *
	 * @param controlBodySimple callback
	 *
	 * @return this
	 */
	public ThenControlStructure then(final ControlBodySimple controlBodySimple) {
		if (this.condition) {
			controlBodySimple.doIt();
		}
		return this;
	}

	/**
	 * if the given condition doesn't holds it executes the callback
	 *
	 * @param controlBodySimple callback
	 *
	 * @return this
	 */
	public ThenControlStructure otherwise(final ControlBodySimple controlBodySimple) {
		if (!this.condition) {
			controlBodySimple.doIt();
		}
		return this;
	}
}

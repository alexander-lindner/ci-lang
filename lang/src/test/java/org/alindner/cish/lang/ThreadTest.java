package org.alindner.cish.lang;

import org.alindner.cish.lang.structures.ControlBodySimple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ThreadTest {

	@Test
	void async() {
		final java.lang.Thread thread = Thread.async(() -> {
			try {
				java.lang.Thread.sleep(3000);
			} catch (final InterruptedException e) {
				fail(e);
			}
		});
		assertTrue(thread.isAlive());
	}

	@Test
	void create() {
		final java.lang.Thread t = Thread.create(() -> fail("Thread is running"));
		assertFalse(t.isAlive());
	}

	@Test
	void start() {
		final Boolean[] run = new Boolean[]{false, false, false};
		final java.lang.Thread[] threads = new java.lang.Thread[]{
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[0] = true;
				}),
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[1] = true;
				}),
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[2] = true;
				})
		};
		Assertions.assertArrayEquals(run, new Boolean[]{false, false, false});
		Thread.start(threads);
		Arrays.stream(threads).forEach(thread -> {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				fail(e);
			}
		});
		Assertions.assertArrayEquals(run, new Boolean[]{true, true, true});
	}

	@Test
	void finished() {
		final Boolean[] run = new Boolean[3];
		final java.lang.Thread[] threads = new java.lang.Thread[]{
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[0] = true;
				}),
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[1] = true;
				}),
				Thread.create(() -> {
					try {
						java.lang.Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					run[2] = true;
				})
		};
		Thread.start(threads);
		final ControlBodySimple onOk    = () -> Assertions.assertArrayEquals(run, new Boolean[]{true, true, true});
		final int[]             counter = {0};
		final ControlBodySimple onOtherwise = new ControlBodySimple() {
			@Override
			public void doIt() {
				counter[0]++;
				try {
					java.lang.Thread.sleep(1000);
					if (counter[0] < 25) {
						Thread.finished(threads).then(onOk).otherwise(this);
					} else {
						fail("To much rounds");
					}
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Thread.finished(threads).then(onOk).otherwise(onOtherwise);
	}
}
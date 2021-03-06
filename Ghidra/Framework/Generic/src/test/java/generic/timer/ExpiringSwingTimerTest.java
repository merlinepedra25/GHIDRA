/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package generic.timer;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;

import generic.test.AbstractGenericTest;

public class ExpiringSwingTimerTest extends AbstractGenericTest {

	@Test
	public void testRunWhenReady() {

		int waitCount = 2;
		AtomicInteger counter = new AtomicInteger();
		BooleanSupplier isReady = () -> {
			return counter.incrementAndGet() > waitCount;
		};

		AtomicInteger runCount = new AtomicInteger();
		Runnable r = () -> {
			runCount.incrementAndGet();
		};
		ExpiringSwingTimer.runWhen(isReady, 10000, r);

		waitFor(() -> runCount.get() > 0);
		assertTrue("Timer did not wait for the condition to be true", counter.get() > waitCount);
		assertEquals("Client code was run more than once", 1, runCount.get());
	}

	@Test
	public void testGet() {

		String expectedResult = "Test result";
		int waitCount = 2;
		AtomicInteger counter = new AtomicInteger();
		Supplier<String> supplier = () -> {
			if (counter.incrementAndGet() > waitCount) {
				return expectedResult;
			}
			return null;
		};

		AtomicReference<String> result = new AtomicReference<>();
		AtomicInteger runCount = new AtomicInteger();
		Consumer<String> consumer = s -> {
			runCount.incrementAndGet();
			result.set(s);
		};
		ExpiringSwingTimer.get(supplier, 10000, consumer);

		waitFor(() -> runCount.get() > 0);
		assertEquals("", expectedResult, result.get());
		assertTrue("Timer did not wait for the condition to be true", counter.get() > waitCount);
		assertEquals("Client code was run more than once", 1, runCount.get());
	}

	@Test
	public void testRunWhenReady_Timeout() {

		BooleanSupplier isReady = () -> {
			return false;
		};

		AtomicBoolean didRun = new AtomicBoolean();
		Runnable r = () -> didRun.set(true);
		ExpiringSwingTimer timer = ExpiringSwingTimer.runWhen(isReady, 500, r);

		waitFor(() -> !timer.isRunning());

		assertFalse(didRun.get());
	}

	@Test
	public void testGet_Timeout() {

		Supplier<String> supplier = () -> {
			return null;
		};

		AtomicBoolean didRun = new AtomicBoolean();
		Consumer<String> consumer = s -> didRun.set(true);
		ExpiringSwingTimer timer = ExpiringSwingTimer.get(supplier, 500, consumer);

		waitFor(() -> !timer.isRunning());

		assertFalse(didRun.get());
	}

	@Test
	public void testWorkOnlyHappensOnce() {

		BooleanSupplier isReady = () -> {
			return true;
		};

		AtomicInteger runCount = new AtomicInteger();
		Runnable r = () -> {
			runCount.incrementAndGet();
		};

		ExpiringSwingTimer timer = ExpiringSwingTimer.runWhen(isReady, 10000, r);
		waitFor(() -> !timer.isRunning());
		assertEquals(1, runCount.get());

		timer.start();
		waitFor(() -> !timer.isRunning());
		assertEquals(1, runCount.get());
	}
}

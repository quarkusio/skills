/**
 * (C) Copyright IBM Corporation 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.websphere.samples.daytrader.util;

import java.util.concurrent.ArrayBlockingQueue;

public class Diagnostics {
	private static final int DRIVE_MEMORY = Integer.getInteger("DRIVE_MEMORY", 0);
	private static final int DRIVE_LATENCY = Integer.getInteger("DRIVE_LATENCY", 0);
	private static final int DRIVE_MEMACCUMULATION = Integer.getInteger("DRIVE_MEMACCUMULATION", 0);
	private static final ArrayBlockingQueue<byte[]> accumulation;

	static {
		if (DRIVE_MEMORY > 0) {
			Log.warning("DRIVE_MEMORY=" + DRIVE_MEMORY
					+ " has been specified which will allocate that many bytes on some app requests");
		}
		if (DRIVE_MEMACCUMULATION > 0) {
			Log.warning("DRIVE_MEMACCUMULATION=" + DRIVE_MEMACCUMULATION
					+ " has been specified which will accumulate up to " + (DRIVE_MEMORY * DRIVE_MEMACCUMULATION)
					+ " bytes");
			accumulation = new ArrayBlockingQueue<byte[]>(DRIVE_MEMACCUMULATION);
		} else {
			accumulation = null;
		}
		if (DRIVE_LATENCY > 0) {
			Log.warning("DRIVE_LATENCY=" + DRIVE_LATENCY
					+ " has been specified which will sleep that many milliseconds on some app requests");
		}
	}

	public static void checkDiagnostics() {
		if (DRIVE_MEMORY > 0) {
			byte[] memory = new byte[DRIVE_MEMORY];
			// Not sure if Java will optimize this away if we don't use it, so just
			// do something trivial
			int count = 0;
			for (byte b : memory) {
				if ((b & 0x01) > 0) {
					count++;
				}
			}
			if (count > 0) {
				Log.error("Something that shouldn't happen");
			}
			if (DRIVE_MEMACCUMULATION > 0) {
				synchronized (accumulation) {
					if (accumulation.size() >= DRIVE_MEMACCUMULATION) {
						accumulation.remove();
					}
					accumulation.add(memory);
				}
			}
		}

		if (DRIVE_LATENCY > 0) {
			try {
				Thread.sleep(DRIVE_LATENCY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
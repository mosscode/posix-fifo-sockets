/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of posix-fifo-sockets.
 *
 * posix-fifo-sockets is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * posix-fifo-sockets is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with posix-fifo-sockets; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.posixfifosockets;

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.moss.fskit.TempDir;
import com.moss.posixfifosockets.testharness.BashClientFactory;
import com.moss.posixfifosockets.testharness.JavaClientFactory;
import com.moss.posixfifosockets.testharness.MixedTestClientFactory;
import com.moss.posixfifosockets.testharness.TestClient;
import com.moss.posixfifosockets.testharness.TestClientFactory;
import com.moss.posixfifosockets.testharness.TestHarness;

public class PosixFifoServerSocketTest extends TestCase {
	private final Log log = LogFactory.getLog(getClass());
	
	private TempDir temp;

	private int CONCURRENT_TESTS_LENGTH = 15;
	private int SERIAL_TESTS_LENGTH = 30;
//	private int CONCURRENT_TESTS_LENGTH = 90*10;
//	private int SERIAL_TESTS_LENGTH = 180*10;
	

	@Override
	protected void setUp() throws Exception {
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		log.info("Setting up");
		temp = TempDir.create();
		log.info("Running from " + temp.getAbsolutePath());
	}
	
	@Override
	protected void tearDown() throws Exception {
		log.info("Tearing down");
		temp.deleteRecursively();
	}
	
	public void testMixedConcurrent() throws Exception {
		exec(temp, 15, new MixedTestClientFactory(CONCURRENT_TESTS_LENGTH, temp));
		
	}
	
	public void testMixedSerial() throws Exception {
		exec(temp, 1, new MixedTestClientFactory(SERIAL_TESTS_LENGTH, temp));
	}
	
	public void testJavaConcurrent() throws Exception {
		exec(temp, 15, new JavaClientFactory(CONCURRENT_TESTS_LENGTH));
	}
	
	public void testJavaSerial() throws Exception {
		
		exec(temp, 1, new JavaClientFactory(SERIAL_TESTS_LENGTH));
	}
	
	public void testBashConcurrent() throws Exception {
		
		exec(temp, 15, new BashClientFactory(CONCURRENT_TESTS_LENGTH, temp));
	}
	
	public void testBashSerial() throws Exception {
		
		exec(temp, 1, new BashClientFactory(SERIAL_TESTS_LENGTH, temp));
	}
	
	private void exec(TempDir dir, int x, TestClientFactory factory) throws Exception {
		TestHarness h = new TestHarness();
		try {
			final List<TestClient> clients = h.run(dir, x, factory);
			for(TestClient next : clients){
//			log.info("waiting for " + next.getName() + " to finish.");
//			next.join();
				if(next.error()!=null){
					next.error().printStackTrace();
					fail("client " + next.getName() + " failed on try " + next.progress() + " with " + next.error().getClass().getSimpleName() + ": " + next.error().getMessage());
				}
			}
		} finally {
			h.dispose();
		}
	}
}

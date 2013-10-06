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
/**
 * 
 */
package com.moss.posixfifosockets.testharness;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.moss.posixfifosockets.PosixFifoSocket;
import com.moss.posixfifosockets.PosixFifoSocketAddress;

class JavaTestClient extends TestClient {
		private final Log log = LogFactory.getLog(getClass());
		private PosixFifoSocket socket;

		public JavaTestClient(String name, int numCallsToMake, PosixFifoSocketAddress address) {
			super(name, numCallsToMake, address);
		}
		
		@Override
		String report() {
			return getName() + ", status \"" + status + "\" with " + socket;
		}
		
		private String status = "initial-state";
		
		private void log(String m){
			if(log.isDebugEnabled()){
				log.debug(m);
			}
			status = m;
		}
		@Override
		String send(String message) throws Exception {

			log("Connecting to server");
			socket = PosixFifoSocket.newClientConnection(address, 1000);
			log("Using socket " + socket);
			log("Opening output ");
			Writer w = new OutputStreamWriter(socket.out());
			log("Writing message ");
			w.write(message);
			log("Closing output");
			w.close();
			
			log("Opening response");
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.in()));
			
			char[] b = new char[1024];
			
			log("Reading response");
			
			StringBuilder text = new StringBuilder();
			for(int x = reader.read(b);x!=-1;x = reader.read(b)){
				text.append(b, 0, x);
			}
			log("Closing response");
			reader.close();
			
			log("Done (received " + message + ")");

//			String response = reader.readLine();
//			reader.close();
			socket.close();
//			System.out.println("Response " + response);
			return text.toString();
		}
	}
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.moss.posixfifosockets.PosixFifoSocketAddress;


class BashTestClient extends TestClient {
//		private final Log log = LogFactory.getLog(getClass());
		private final File script;
		
		public BashTestClient(String name, int numCallsToMake, File script, PosixFifoSocketAddress address) {
			super(name, numCallsToMake, address);
			this.script = script;
		}
		@Override
		String report() {
			return "bash script";
		}
		
		@Override
		String send(String message) throws Exception {
			
			Process p = Runtime.getRuntime().exec(
					new String[]{
							"bash", 
							script.getAbsolutePath(),
							address.controlPipe().getAbsolutePath(),
							address.socketsDir().getAbsolutePath(),
							message
					}
				);
//			
			OutputStream out = p.getOutputStream();
			out.write(message.getBytes());
			out.close();
//			
			String stdOut = read(p.getInputStream());
			String stdErr = read(p.getErrorStream());
//			
			int resultCode = p.waitFor();
//			
			if(resultCode!=0){
				throw new RuntimeException("Bash script failed with return code " + resultCode +".  The output was " + stdOut + ", stdErr was " + stdErr);
			}
			
			return stdOut.toString();
		}
		
		private String read(InputStream in) throws IOException {
			StringBuilder text = new StringBuilder();
			char[] b = new char[1024];
			
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			
			for(int x=r.read(b);x!=-1;x = r.read(b)){
				text.append(b, 0, x);
			}
			
			r.close();
//			for(String line = r.readLine();line!=null; line = r.readLine()){
//				text.append(line);
//			}
			return text.toString();
		}
	}
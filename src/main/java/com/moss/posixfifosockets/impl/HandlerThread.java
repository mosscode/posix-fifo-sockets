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
package com.moss.posixfifosockets.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.moss.posixfifosockets.PosixFifoSocket;
import com.moss.posixfifosockets.PosixFifoSocketHandler;

public class HandlerThread extends Thread{
		private final Log log = LogFactory.getLog(getClass());
		private final File socketsDirectory;
		private final long id;
		private final PosixFifoSocketHandler handler;
		private final SocketServerDebugHistory debug;
		
		Throwable e;
		
		public HandlerThread(long id, File socketsDirectory, PosixFifoSocketHandler handler, SocketServerDebugHistory debug) {
			super("SocketHandler" + id);
			this.id = id;
			this.socketsDirectory = socketsDirectory;
			this.handler = handler;
			this.debug = debug;
		}
		
		public void run(){
			try {
				File in = new File(socketsDirectory, id + ".fifo.in");
				File out = new File(socketsDirectory, id + ".fifo.out");
				File control = new File(socketsDirectory, id + ".fifo.control");

				PosixFifoSocket.createFifo(in);
				
				{// LET THE CLIENT KNOW THAT THE CONNECTION HAS BEEN ESTABLISHED
					if(!out.exists()){
						throw new RuntimeException("ERROR: FILE SHOULD HAVE BEEEN CREATED BY SOCKET CLIENT: " + out.getAbsolutePath());
					}
					if(!control.exists()){
						throw new RuntimeException("ERROR: FILE SHOULD HAVE BEEEN CREATED BY SOCKET CLIENT: " + out.getAbsolutePath());
					}
					Writer w = new FileWriter(control);
					w.write("OK\n");
					w.close();
				}
				
				
				PosixFifoSocket socket = new PosixFifoSocket(id, in, out);

				if(log.isDebugEnabled()) log.debug("Opened socket " + socket);
				handler.handle(socket);
				socket.close();
				
			} catch (Throwable e) {
				this.e = e;
				log.error("Error creating socket: " + e.getMessage(), e);
			} finally {
				if(debug!=null){
					debug.wentInactive(this);
				}

//				handlers.remove(this);
//				recentHandlers.add(this);
			}
		}
		
		
		@Override
		public String toString() {
			return "Handler for socket " + id + " (error : " + e + ")";
		}
	}
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.moss.posixfifosockets.impl.HandlerThread;
import com.moss.posixfifosockets.impl.SocketServerDebugHistory;

public class PosixFifoSocketServer {
	private static final char BEGIN_TOKEN = '{', END_TOKEN = '}';
	
	private final Log log = LogFactory.getLog(getClass());
	private final PosixFifoSocketAddress address;
	
	private final PosixFifoSocketHandler handler;
	
	private boolean run = false;
	private final Thread controlReader = new ControlPipeThread();
	
	public PosixFifoSocketServer(PosixFifoSocketAddress address, PosixFifoSocketHandler handler) throws IOException {
		this.address = address;
		this.handler = handler;
		
	}
	
	public synchronized void start(){
		if(address.controlPipe().exists() && !address.controlPipe().delete()){
			throw new RuntimeException("Error: server already exists at " + address.controlPipe().getAbsolutePath());
		}

		{
			try {
				int returnCode = Runtime.getRuntime().exec("mkfifo " + address.controlPipe()).waitFor();
				if(returnCode!=0){
					throw new RuntimeException("Could not create FIFO: " + address.controlPipe().getAbsolutePath());
				}
				
				if(log.isDebugEnabled()) log.debug("Created control pipe at " + address.controlPipe());
			} catch (Exception e) {
				throw new RuntimeException("Could not create FIFO: " + address.controlPipe().getAbsolutePath());
			}
		}
		
		run = true;
		controlReader.start();
	}
	
	public synchronized void stop(){
		run = false;
		if(!address.controlPipe().delete()){
			throw new RuntimeException("Could not delete: " + address.controlPipe().getAbsolutePath());
		}
	}
	
	public enum ControlStatus {
		START,
		READ,
		HANDLE,
		AFTER_HANDL
	}
	
	ControlStatus controlStatus = ControlStatus.START;
	
	public ControlStatus controlStatus() {
		return controlStatus;
	}
	
	class ControlPipeThread extends Thread{
		public ControlPipeThread() {
			super("ControlPipe");
		}
		
		public void run() {
			if(log.isDebugEnabled()) log.debug("Running at " + address.controlPipe());
			Reader r;
			try {
				r = new FileReader(address.controlPipe());
			} catch (FileNotFoundException e1) {
				throw new RuntimeException(e1);
			}
			
			int prev = -222;
			
			while(true){
				try {
					controlStatus = ControlStatus.START;
					
					int c = r.read();
					if(!run){
						return;
					}
					if(log.isDebugEnabled()) log.debug("Start marker " + (char)c + " (code " + c + ")");
					switch(c){
					case -1: // END OF STREAM
						if(prev==-1){
							try {
								r = new FileReader(address.controlPipe());
							} catch (FileNotFoundException e1) {
								throw new RuntimeException(e1);
							}
							break;
						}
					case 10: // LF
						// This stuff is harmless
						break;
					case BEGIN_TOKEN:
						StringBuffer request = new StringBuffer();
						controlStatus = ControlStatus.READ;
						
						for(c = r.read();c!=END_TOKEN;c = r.read()){
							if(c==BEGIN_TOKEN){
								log.warn("Protocol error: received a nested " + BEGIN_TOKEN);
								eatTokens(r);
							}
							request.append((char)c);
						}
						controlStatus = ControlStatus.HANDLE;
						handleRequest(request.toString());
						controlStatus = ControlStatus.AFTER_HANDL;
						break;
					default:
						log.warn("Error: invalid conversation start ... was expecting " + BEGIN_TOKEN + " but received '" + ((char)c) + "' (code " + c + ") with prev '" + ((char)prev) + "' (code " + prev + ")");
						break;
					}
					prev = c;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private void eatTokens(Reader r) throws IOException{
		for(int c = r.read();c!=END_TOKEN;c = r.read()){
			System.out.println("Eating " + (char)c);
			if(c==BEGIN_TOKEN){
				eatTokens(r);
			}
		}
	}
	private SocketServerDebugHistory debugInfo;
	
	public SocketServerDebugHistory debugInfo() {
		return debugInfo;
	}
	
	private void handleRequest(String r){
		if(log.isDebugEnabled()) log.debug("Handling request " + r);
		try {
			
			long id;
			try {
				id = Long.parseLong(r);
			} catch (Exception e) {
				log.warn("Error: id not an integer: " + r);
				return;
			}
			
			HandlerThread t = new HandlerThread(id, address.socketsDir(), handler, debugInfo);
			
			if(debugInfo!=null){
				debugInfo.addActive(t);
			}
			
			t.start();
			
		} catch (NumberFormatException e) {
			log.error("Invalid numeric socket ID: \"" + r + "\": " + e.getMessage(), e);
		}
	}
	
}

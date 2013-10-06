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
package com.moss.posixfifosockets.testharness;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.moss.fskit.TempDir;
import com.moss.posixfifosockets.PosixFifoSocket;
import com.moss.posixfifosockets.PosixFifoSocketAddress;
import com.moss.posixfifosockets.PosixFifoSocketHandler;
import com.moss.posixfifosockets.PosixFifoSocketServer;
import com.moss.posixfifosockets.impl.SocketServerDebugHistory;

public class TestHarness {
	private final Log log = LogFactory.getLog(getClass());
	private PosixFifoSocketServer server;
	
	
	class ServerActivity {
		final PosixFifoSocket socket;
		
		String status;
		
		public ServerActivity(PosixFifoSocket socket) {
			super();
			this.socket = socket;
		}
		
		private void log(String message){
			if(log.isDebugEnabled()){
				log.debug(message);
			}
			status = message;
		}
		
		@Override
		public String toString() {
			return "Activity for socket " + socket.id() + ": " + (status==null?"no-status-yet":status);
		}
		
		public void handle(){
			try {
				log("Handling client connection");

				log("Opening input");
				Reader r = new InputStreamReader(socket.in());
				
				log("Reading input");
				char[] b = new char[1024];
				StringBuilder input = new StringBuilder();
				
				for(int x=r.read(b);x!=-1;x = r.read(b)){
					input.append(b, 0, x);
				}
				
//				for(int c = r.read(); c!=-1; c = r.read()){
//					if(c=='\n'){
//						break;
//					}
//					input.append((char)c);
//				}
				
				r.close();
				
				log("Opening output");
				Writer w = new OutputStreamWriter(socket.out());
				log("Writing output");
				w.write(input.toString());
				w.flush();
				w.close();
				
				log("Closing");
//				socket.close();
				log("Done");
				status = "Done (read and returned \"" + input + "\" from/to socket " + socket + ")";
				
			} catch (Throwable e) {
				e.printStackTrace();
			}

		}
	}
	
	List<ServerActivity> serverActivity = Collections.synchronizedList(new LinkedList<ServerActivity>());
	
	public List<TestClient> run(TempDir temp, final int numClients, TestClientFactory factory) throws Exception {
			serverActivity = Collections.synchronizedList(new LinkedList<ServerActivity>());
			
			final PosixFifoSocketAddress address = new PosixFifoSocketAddress(temp);

			PosixFifoSocketHandler handler = new PosixFifoSocketHandler() {

				public void handle(PosixFifoSocket socket) {
					ServerActivity a = new ServerActivity(socket);
//					serverActivity.add(a);
					try{
						a.handle();
					}finally{
//						serverActivity.remove(a);
					}
				}
			};
			
			
			server = new PosixFifoSocketServer(address, handler);
			server.start();

			final List<TestClient> clients = new LinkedList<TestClient>();
			
			final long start = System.currentTimeMillis();
			for(int x=0;x<numClients;x++){
				
				final TestClient next = factory.newClient("Client " + x, address);
				
				clients.add(next);
				next.start();
			}
			
			Thread monitor = new Thread("Reporting Thread"){
				public void run() {
					int lastTodo = 0 ;
					long lastChange = System.currentTimeMillis();
					while(true){
						int totalMax = 0;
						int totalTodo = 0;
						
						boolean allClientsDone = true;
						List<TestClient> activeClients = new LinkedList<TestClient>();
						
						for(TestClient next : clients){
							totalMax += next.MAX;
							totalTodo += next.y;
							if(next.isAlive()){
								allClientsDone = false;
								activeClients.add(next);
							}
						}
						
						if(lastTodo!=totalTodo){
							lastTodo = totalTodo;
							lastChange = System.currentTimeMillis();
						}
						System.out.println("Finished " + totalTodo + " of " + totalMax);
						
						double elapsed = (double) (System.currentTimeMillis()-start);
						double rate = ((double)totalTodo)/(elapsed/1000.0);
						System.out.println(String.format(" Calls/Sec: %10.4f", rate));
						
						if(System.currentTimeMillis()-lastChange > 5000){
							SocketServerDebugHistory debug = server.debugInfo();
							System.out.println("Progress seems to have halted.");
							System.out.println("Server: " + server.controlStatus());
							System.out.println("Server has " + debug.activeHandlers().size() + " activte handlers");
							for(Object next : debug.activeHandlers()){
								System.out.println("Handler: " + next);
							}
							{// recent server activity
//								int x = server.getRecentHandlers().size()-10;
//								if(x<0) x=0;
								
								int x=0;
								
								for(;x<server.debugInfo().inactiveHandlers().size();x++){
									System.out.println(" OLD HANDLER " + server.debugInfo().inactiveHandlers().get(x));
								}
							}
							
							for(int x=0;x<serverActivity.size();x++){
								ServerActivity next = serverActivity.get(x);
								System.out.println("Activity: " + next.toString());
							}
							for(TestClient next : activeClients){
								ServerActivity activity = null;
								for(int x=0;x<serverActivity.size();x++){
									ServerActivity a = serverActivity.get(x);
									if(a.socket.id()==next.getId()){
										activity = a;
									}
								}
								
								System.out.println(next.report() + " has completed " + next.y + " of " + next.MAX);
								System.out.println("  Server activity: " + activity);
							}
							System.exit(-1);
						}else{
							System.out.println(activeClients.size() + " active clients");
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						if(allClientsDone){
							break;
						}
					}
				}
			};
			
			monitor.start();
			
			monitor.join();

			
			return clients;
	}
	
	
	public void dispose(){
		server.stop();
	}
}

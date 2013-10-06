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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PosixFifoSocket {

	private static final Random r = new Random();

	public static PosixFifoSocket newClientConnection(PosixFifoSocketAddress address, int timeoutMillis) throws IOException {
		final Log log = LogFactory.getLog(PosixFifoSocket.class);
		
		if(!address.controlPipe().exists()){
			throw new IOException("There is no server at " + address);
		}
		
		File in;
		File out;
		File control;
		long id;
		do{
			id = r.nextLong();
			in = new File(address.socketsDir(), id + ".fifo.out");
			out = new File(address.socketsDir(), id + ".fifo.in");
			control = new File(address.socketsDir(), id + ".fifo.control");
		}while(out.exists() || in.exists());

		createFifo(in);
		createFifo(control);
		
		final String registrationString = "{" + id + "}";
		if(log.isDebugEnabled()) log.debug("Sending registration " + registrationString);
		Writer w = new FileWriter(address.controlPipe());
		w.write(registrationString);
		w.flush();
		
		if(log.isDebugEnabled()) log.debug("Sent Registration " + registrationString);
		
		
		PosixFifoSocket socket = new PosixFifoSocket(id, in, out);
		Reader r = new FileReader(control);
		
		StringBuilder text = new StringBuilder();
		for(int c = r.read();c!=-1 && c!='\n';c = r.read()){
			// READ UNTIL THE FIRST LINE BREAK
			text.append((char)c);
		}
		r.close();
		if(!control.delete()){
			throw new RuntimeException("Could not delete file:" + control.getAbsolutePath());
		}
		if(!text.toString().equals("OK")){
			throw new RuntimeException("Connection error: received \"" + text + "\"");
		}
		return socket;
	}

	private final File inFifo;
	private final File outFifo;
	private final long id;

	private InputStream in;
	private OutputStream out;

	public PosixFifoSocket(File l, long id, String input, String output) throws IOException {
		this(
				id,
				new File(l, id + input),	
				new File(l, id + output)
		);
	}

	public long id() {
		return id;
	}

	public PosixFifoSocket(long name, File inFifo, File outFifo) throws IOException {
		super();
		this.id = name;
		this.inFifo = inFifo;
		this.outFifo = outFifo;

	}


	public static void createFifo(File path) throws IOException {
		if(path.exists()){
			throw new IOException("File already exists: " + path.getAbsolutePath());
		}
		if(!path.getParentFile().exists()){
			throw new FileNotFoundException(path.getParent());
		}
		try {
			if(path.exists()){
				throw new RuntimeException("Path really does exist: " + path.getAbsolutePath());
			}
			
			final Process p = Runtime.getRuntime().exec("mkfifo " + path.getAbsolutePath());
			int result = p.waitFor();
			if(result!=0){
				
				String stdOut = read(p.getInputStream());
				String stdErr = read(p.getErrorStream());
				throw new IOException("Error creating fifo at " + path.getAbsolutePath() + ": Received error code " + result + ", STDOUT: " + stdOut + ", STDERR: " + stdErr);
			}else if(!path.exists()){
				throw new RuntimeException("mkfifo didn't do its job: " + path.getAbsolutePath());
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static String read(InputStream in) throws IOException {
		StringBuilder text = new StringBuilder();
		char[] b = new char[1024];
		
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		
		for(int x=r.read(b);x!=-1;x = r.read(b)){
			text.append(b, 0, x);
		}
		
		r.close();
		return text.toString();
	}
	
	private boolean hasClosed = false;
	
	public void close() {
		if(hasClosed){
			throw new RuntimeException("Socket already closed");
		}
		if(!inFifo.delete()){
			throw new RuntimeException("Could not delete one or both FIFOS: " + this);
		}
		hasClosed = true;
	}

	public synchronized InputStream in() throws IOException {
		if(in==null){
			in = new FileInputStream(inFifo);
		}
		return in;
	}

	public synchronized OutputStream out() throws IOException {
		if(out==null){
			out = new FileOutputStream(outFifo);
		}
		return out;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " # " + id + ", reading from " + inFifo + ", writing to " + outFifo;
	}
}

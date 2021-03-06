/**
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package sword_client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


import org.apache.log4j.Logger;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.UnmarshallException;

/**
 * This is an example Client implementation to demonstrate how to connect to a
 * SWORD server. The client supports BASIC HTTP Authentication. This can be
 * initialised by setting a username and password.
 * 
 * @author Neil Taylor
 */
public class Client implements SWORDClient {
	/**
	 * The status field for the response code from the recent network access.
	 */
	private Status status;

	/**
	 * The name of the server to contact.
	 */
	private String server;

	/**
	 * The port number for the server.
	 */
	private int port;

	/**
	 * Specifies if the network access should use HTTP authentication.
	 */
	private boolean doAuthentication;

	/**
	 * The username to use for Basic Authentication.
	 */
	private String username;

	/**
	 * User password that is to be used.
	 */
	private String password;

	/**
	 * The client that is used to send data to the specified server.
	 */
	private DefaultHttpClient client;

	/**
	 * The default connection timeout. This can be modified by using the
	 * setSocketTimeout method.
	 */
	public static final int DEFAULT_TIMEOUT = 20000;

	/**
	 * Logger.
	 */
	private static Logger log = Logger.getLogger(Client.class);

	/**
	 * Create a new Client. The client will not use authentication by default.
	 */
	public Client() {
		client = new DefaultHttpClient();
		client.getParams().setParameter("http.socket.timeout",
				new Integer(DEFAULT_TIMEOUT));
//		log.debug("proxy host: " + client.getHostConfiguration().getProxyHost());
//		log.debug("proxy port: " + client.getHostConfiguration().getProxyPort());
		doAuthentication = false;
	}

	/**
	 * Initialise the server that will be used to send the network access.
	 * 
	 * @param server
	 * @param port
	 */
	public void setServer(String server, int port) {
		this.server = server;
		this.port = port;
	}

	/**
	 * Set the user credentials that will be used when making the access to the
	 * server.
	 * 
	 * @param username
	 *            The username.
	 * @param password
	 *            The password.
	 */
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		doAuthentication = true;
	}

	/**
	 * Set the basic credentials. You must have previously set the server and
	 * port using setServer.
	 * 
	 * @param username
	 * @param password
	 */
	private void setBasicCredentials(String username, String password) {
		log.debug("server: " + server + " port: " + port + " u: '" + username
				+ "' p '" + password + "'");
		client.getCredentialsProvider().setCredentials(new AuthScope(server, port),
				new UsernamePasswordCredentials(username, password));
	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server. The port is set to 80.
	 * 
	 * @param host
	 *            The hostname.
	 */
//	public void setProxy(String host) {
//		setProxy(host, 80);
//	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server.
	 * 
	 * @param host
	 *            The name of the host.
	 * @param port
	 *            The port.
	 */
	public void setProxy(String host, int port) {
//		client.getHostConfiguration().setProxy(host, port);
	}

	/**
	 * Clear the proxy setting.
	 */
	public void clearProxy() {
//		client.getHostConfiguration().setProxyHost(null);
	}

	/**
	 * Clear any user credentials that have been set for this client.
	 */
	public void clearCredentials() {
		client.getCredentialsProvider().clear();
		doAuthentication = false;
	}

	/**
	 * Set the connection timeout for the socket.
	 * 
	 * @param milliseconds
	 *            The time, expressed as a number of milliseconds.
	 */
	public void setSocketTimeout(int milliseconds) {
		client.getParams().setParameter("http.socket.timeout",
				new Integer(milliseconds));
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url)
			throws SWORDClientException {
		return getServiceDocument(url, null);
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url, String onBehalfOf)
			throws SWORDClientException {
		URL serviceDocURL = null;
		try {
			serviceDocURL = new URL(url);
		} catch (MalformedURLException e) {
			// Try relative URL
			URL baseURL = null;
			try {
				baseURL = new URL("http", server, Integer.valueOf(port), "/");
				serviceDocURL = new URL(baseURL, (url == null) ? "" : url);
			} catch (MalformedURLException e1) {
				// No dice, can't even form base URL...
				throw new SWORDClientException(url + " is not a valid URL ("
						+ e1.getMessage()
						+ "), and could not form a relative one from: "
						+ baseURL + " / " + url, e1);
			}
		}
		
		HttpGet httpget = new HttpGet(serviceDocURL.toExternalForm());
		if (doAuthentication) {
			// this does not perform any check on the username password. It
			// relies on the server to determine if the values are correct.
			setBasicCredentials(username, password);
//			httpget.setDoAuthentication(true);
		}

		// if (( onBehalfOf != null ) && ( ! onBehalfOf.trim().equals("") ))
		if (containsValue(onBehalfOf)) {
			log.debug("Setting on-behalf-of: " + onBehalfOf);
			httpget.addHeader(HttpHeaders.X_ON_BEHALF_OF,
					onBehalfOf);
		}

		ServiceDocument doc = null;

		try {
			HttpResponse response = client.execute(httpget);
			
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String message = EntityUtils.toString(response.getEntity());
				log.debug("returned message is: " + message);
				System.out.println("returned message is: " + message);
				doc = new ServiceDocument();
				doc.unmarshall(message);
			} else {
				throw new SWORDClientException(
						"Received error from service document request: "
								+ status);
			}
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage(), uex);
		} 

		return doc;
	}

	/**
	 * Post a file to the server. The different elements of the post are encoded
	 * in the specified message.
	 * 
	 * @param message
	 *            The message that contains the post information.
	 * 
	 * @throws SWORDClientException
	 *             if there is an error during the post operation.
	 */
	public DepositResponse postFile(PostMessage message)
			throws SWORDClientException {
		if (message == null) {
			throw new SWORDClientException("Message cannot be null.");
		}

		HttpPost httppost = new HttpPost(message.getDestination());

		if (doAuthentication) {
			setBasicCredentials(username, password);
//			httppost.setDoAuthentication(true);
		}

		DepositResponse response = null;
		FileInputStream stream = null;

		try {
			if (message.isUseMD5()) {
				String md5 = ChecksumUtils.generateMD5(message.getFilepath());
				if (message.getChecksumError()) {
					md5 = "1234567890";
				}
				log.debug("checksum error is: " + md5);
				if (md5 != null) {
					httppost.addHeader(
							HttpHeaders.CONTENT_MD5, md5);
				}
			}

			String filename = message.getFilename();
			if (filename != "") {
				httppost.addHeader(
						HttpHeaders.CONTENT_DISPOSITION, " filename="
								+ filename);
			}

			if (containsValue(message.getSlug())) {
				httppost.addHeader(HttpHeaders.SLUG, message
						.getSlug());
			}

			httppost.addHeader(HttpHeaders.X_NO_OP, Boolean
					.toString(message.isNoOp()));
			httppost.addHeader(HttpHeaders.X_VERBOSE, Boolean
					.toString(message.isVerbose()));

			String formatNamespace = message.getFormatNamespace();
			if (formatNamespace != null && formatNamespace.length() > 0) {
				httppost.addHeader(
						HttpHeaders.X_FORMAT_NAMESPACE, formatNamespace);
			}

			String onBehalfOf = message.getOnBehalfOf();
			if (containsValue(onBehalfOf)) {
				httppost.addHeader(
						HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
			}
			
			String xpackaging = message.getXpackaging();
			if(xpackaging != null) {
				httppost.addHeader(
						HttpHeaders.X_PACKAGING, xpackaging);
			}

			stream = new FileInputStream(message.getFilepath());
			File file= new File(message.getFilepath());

			FileEntity requestEntity = new FileEntity(file, message.getFiletype());
			
		    requestEntity.setChunked(true);
			httppost.setEntity(requestEntity);
			

	        HttpResponse resp = client.execute(httppost);

			
			
			
			log.info("Checking the status code: " + resp.getStatusLine().getStatusCode());

			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED
					|| resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
//				String messageBody = readResponse(httppost
//						.getResponseBodyAsStream());
				response = new DepositResponse(resp.getStatusLine().getStatusCode()); 
				// added call for the status code.
				response.unmarshall(resp.getStatusLine().getReasonPhrase());
			}
			else {
				throw new SWORDClientException("Error occured; server returned : "+ resp.getStatusLine().getReasonPhrase());
			}
			return response;

		} catch (NoSuchAlgorithmException nex) {
			throw new SWORDClientException("Unable to use MD5. "
					+ nex.getMessage(), nex);
//		} catch (HttpException ex) {
//			throw new SWORDClientException(ex.getMessage(), ex);
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage(), uex);
		} finally {
//			httppost.releaseConnection();
//			try {
//				conn.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException ioe) {
				log.error("Error closing a stream");
				throw new SWORDClientException(ioe.getMessage(), ioe);
			}
		}

	}


	/**
	 * Return the status information that was returned from the most recent
	 * request sent to the server.
	 * 
	 * @return The status code returned from the most recent access.
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Check to see if the specified item contains a non-empty string.
	 * 
	 * @param item
	 *            The string to check.
	 * @return True if the string is not null and has a length greater than 0
	 *         after any whitespace is trimmed from the start and end.
	 *         Otherwise, false.
	 */
	private boolean containsValue(String item) {
		return ((item != null) && (item.trim().length() > 0));
	}

}
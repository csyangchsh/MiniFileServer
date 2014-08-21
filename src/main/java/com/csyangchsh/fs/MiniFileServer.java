package com.csyangchsh.fs;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.*;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author csyangchsh
 * Date: 14/8/7
 */
public class MiniFileServer {

    private HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
    private ServerSocket serversocket;
    private HttpService httpService;



    public MiniFileServer(final int port,
                          final HttpService httpService,
                          final SSLServerSocketFactory sf) throws IOException {
        this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
        this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
        this.httpService = httpService;
    }

    public void listen() {
        System.out.println("Listening on port " + this.serversocket.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                // Set up HTTP connection
                Socket socket = this.serversocket.accept();
                System.out.println("Incoming connection from " + socket.getInetAddress());
                HttpServerConnection conn = this.connFactory.createConnection(socket);

                // Start worker thread
                Thread t = new WorkerThread(this.httpService, conn);
                t.setDaemon(true);
                t.start();
            } catch (InterruptedIOException ex) {
                break;
            } catch (IOException e) {
                System.err.println("I/O error initialising connection thread: "
                        + e.getMessage());
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Please specify document root directory");
            System.exit(1);
        }
        // Document root directory
        String docRoot = args[0];
        int port = 13131;

        HttpProcessor httpproc = HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer("FileServer/1.1"))
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();

        UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        reqistry.register("*", new FileListHandler(docRoot));

        HttpService httpService = new HttpService(httpproc, reqistry);

        MiniFileServer server = new MiniFileServer(port, httpService, (SSLServerSocketFactory)null);

        while (!Thread.interrupted()) {
            server.listen();
        }
    }

    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run() {
            System.out.println("New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    }

}

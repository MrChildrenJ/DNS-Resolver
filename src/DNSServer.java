import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static java.lang.System.*;

public class DNSServer {
    private static final int DEFAULT_PORT = 8053;  // Since we can't use privileged port 53
    private static final String GOOGLE_DNS = "8.8.8.8";
    private static final int GOOGLE_DNS_PORT = 53;
    private static final int MAX_PACKET_SIZE = 512;  // Standard DNS message size

    private final int port;
    private final DNSCache cache;
    private boolean running;

    public DNSServer(int port) {
        this.port = port;
        this.cache = new DNSCache();
        this.running = false;
    }

    public DNSServer() {
        this(DEFAULT_PORT);
    }

    public void start() {
        running = true;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("DNS Server started on port " + port);
            System.out.println("Listening for DNS queries...");

            while (running) {
                byte[] receiveData = new byte[MAX_PACKET_SIZE];         // Buffer for receiving data
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    socket.receive(receivePacket);                      // Wait for a DNS request
                    System.out.println("Received DNS query from " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                    handleRequest(socket, receivePacket);
                } catch (IOException e) {
                    System.err.println("Error handling DNS request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start DNS server: " + e.getMessage());
        }
    }

    private void handleRequest(DatagramSocket socket, DatagramPacket receivePacket) throws IOException {
        InetAddress clientAddress = receivePacket.getAddress();         // Get client info
        int clientPort = receivePacket.getPort();

        byte[] data = new byte[receivePacket.getLength()];              // Extract the actual data
        System.arraycopy(receivePacket.getData(), 0, data, 0, receivePacket.getLength());

        try {
            DNSMessage request = DNSMessage.decodeMessage(data);        // Decode the DNS message
            DNSMessage response = processRequest(request);

            byte[] responseData = response.toBytes();                   // Send response back to client
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, clientAddress, clientPort);
            socket.send(responsePacket);

        } catch (Exception e) {
            System.err.println("Error processing DNS message: " + e.getMessage());
        }
    }

    private DNSMessage processRequest(DNSMessage request) throws IOException {

        DNSQuestion question = request.getQuestions()[0];               // Check cache first, assume single question
        System.out.println("Processing query for: " + String.join(".", question.getQname()));
        System.out.println("Checking cache...");
        DNSRecord cachedRecord = cache.query(question);

        if (cachedRecord != null && !cachedRecord.isExpired()) {
            System.out.println("Cache hit! Returning cached response");
            return DNSMessage.buildResponse(request, new DNSRecord[]{cachedRecord});
        }

        System.out.println("Cache miss. Forwarding to Google DNS (" + GOOGLE_DNS + ")");
        // Can't find question in cache, forward to Google DNS
        try (DatagramSocket googleSocket = new DatagramSocket()) {
            InetAddress googleAddress = InetAddress.getByName(GOOGLE_DNS);              // Send request to Google
            byte[] requestData = request.toBytes();
            DatagramPacket googlePacket = new DatagramPacket(
                    requestData, requestData.length, googleAddress, GOOGLE_DNS_PORT);
            googleSocket.send(googlePacket);

            byte[] responseData = new byte[MAX_PACKET_SIZE];                            // Get response from Google
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
            googleSocket.receive(responsePacket);

            DNSMessage googleResponse = DNSMessage.decodeMessage(responseData);         // Decode Google's response

            if (googleResponse.getAnswers().length > 0) {                               // Cache the response
                System.out.println("Caching response for future queries");
                cache.insert(question, googleResponse.getAnswers()[0]);
            }
            System.out.println("Returning response to client");
            return googleResponse;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        DNSServer server = new DNSServer(port);
        server.start();
    }
}

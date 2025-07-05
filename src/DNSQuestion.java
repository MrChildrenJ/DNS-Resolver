import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.System.*;

public class DNSQuestion {
    private String[] qname;     // Domain name
    private int qtype;          // Type of query
    private int qclass;         // Class of query (usually 1 for Internet)

    public DNSQuestion(String[] qname, int qtype, int qclass) {
        this.qname = qname;
        this.qtype = qtype;
        this.qclass = qclass;
    }

    public static DNSQuestion decodeQuestion(InputStream input, DNSMessage message) throws IOException {
        String[] qname = message.readDomainName(input);     // Read the domain name
        int qtype = (input.read() << 8) | input.read();     // Read QTYPE (16 bits)
        int qclass = (input.read() << 8) | input.read();    // Read QCLASS (16 bits)

        return new DNSQuestion(qname, qtype, qclass);
    }

    public void writeBytes(ByteArrayOutputStream output, HashMap<String, Integer> domainNameLocations) throws IOException {
        DNSMessage.writeDomainName(output, domainNameLocations, qname);     // Write domain name

        output.write((qtype >> 8) & 0xFF);                                  // Write QTYPE (16 bits)
        output.write(qtype & 0xFF);

        output.write((qclass >> 8) & 0xFF);                                 // Write QCLASS (16 bits)
        output.write(qclass & 0xFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion dq = (DNSQuestion) o;
        return Arrays.equals(qname, dq.qname) && qtype == dq.qtype && qclass == dq.qclass;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(qname);
        result = 31 * result + qtype;
        result = 31 * result + qclass;
        return result;
    }

    // Getter for qname
    public String[] getQname() {
        return qname;
    }
}

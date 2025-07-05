import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.System.*;

public class DNSRecord {
    private String[] name;      // Domain name
    private int type;           // Type of record
    private int class_;         // Class of record (usually 1 for Internet)
    private int ttl;            // Time to live
    private int rdlength;       // Length of rdata
    private byte[] rdata;       // Record data
    private Date creationDate;  // When this record was created by our program

    public DNSRecord(String[] name, int type, int class_, int ttl, byte[] rdata) {
        this.name = name;
        this.type = type;
        this.class_ = class_;
        this.ttl = ttl;
        this.rdata = rdata;
        this.rdlength = rdata.length;
        this.creationDate = new Date();
    }

    public static DNSRecord decodeRecord(InputStream input, DNSMessage message) throws IOException {
        String[] name = message.readDomainName(input);      // Read the domain name
        int type = (input.read() << 8) | input.read();      // Read type (16 bits)
        int class_ = (input.read() << 8) | input.read();    // Read class (16 bits)
        int ttl = (input.read() << 24) | (input.read() << 16) | (input.read() << 8) | input.read(); // Read TTL (32 bits)
        int rdlength = (input.read() << 8) | input.read();  // Read RDLENGTH (16 bits)

        byte[] rdata = new byte[rdlength];                  // Read RDATA
        input.read(rdata);

        return new DNSRecord(name, type, class_, ttl, rdata);
    }

    public void writeBytes(ByteArrayOutputStream output, HashMap<String, Integer> domainNameLocations) throws IOException {
        DNSMessage.writeDomainName(output, domainNameLocations, name);  // Write domain name

        output.write((type >> 8) & 0xFF);           // Write type (16 bits)
        output.write(type & 0xFF);

        output.write((class_ >> 8) & 0xFF);         // Write class (16 bits)
        output.write(class_ & 0xFF);

        output.write((ttl >> 24) & 0xFF);           // Write ttl (32 bits)
        output.write((ttl >> 16) & 0xFF);
        output.write((ttl >> 8) & 0xFF);
        output.write(ttl & 0xFF);

        output.write((rdlength >> 8) & 0xFF);       // Write rdlength (16 bits)
        output.write(rdlength & 0xFF);

        output.write(rdata);                        // Write RDATA
    }

    public boolean isExpired() {
        Date now = new Date();
        return now.getTime() > creationDate.getTime() + (ttl * 1000L);
    }
}

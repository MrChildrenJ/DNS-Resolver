import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static java.lang.System.*;

public class DNSHeader {
    private int id;          // 2 bytes: query identification number

    // Flag: 2 bytes (16 bits, including qr, opcode, aa, tc, rd, ra, zero, rcode
    private boolean qr;      // 1 bit: query(0) or response(1)
    private int opcode;      // 4 bits: operation code
    private boolean aa;      // 1 bit: authoritative answer
    private boolean tc;      // 1 bit: truncated message
    private boolean rd;      // 1 bit: recursion desired
    private boolean ra;      // 1 bit: recursion available
    private int z;           // 3 bits: reserved
    private int rcode;       // 4 bits: response code

    private int qdcount;     // 2 bytes: number of question entries
    private int ancount;     // 2 bytes: number of answer entries
    private int nscount;     // 2 bytes: number of authority entries
    private int arcount;     // 2 bytes: number of additional entries

    private DNSHeader() {}

    public static DNSHeader decodeHeader(InputStream input) throws IOException {
        // InputStream only read 1 byte (8 bits) at a time
        DNSHeader header = new DNSHeader();

        header.id = (input.read() << 8) | input.read();     // Read byte two times for id (2 bytes)
        int flags1 = input.read();                          // Read qr, opcode, aa, tc, rd (1, 4, 1, 1, 1)
        int flags2 = input.read();                          // Read ra, z, rcode (1, 3, 4)

        // Parse flag1
        header.qr = (flags1 & 0x80) != 0;                   // get 0th bit, flag1 & 10000000
        header.opcode = (flags1 >> 3) & 0x0F;               // get 1-4th bit, left shift by 3, then & with 00001111
        header.aa = (flags1 & 0x04) != 0;                   // get 5th bit, flag1 & 00000100
        header.tc = (flags1 & 0x02) != 0;                   // get 6th bit, flag1 & 00000010
        header.rd = (flags1 & 0x01) != 0;                   // get 7th bit, flag1 & 00000001

        // Parse second byte of flags
        header.ra = (flags2 & 0x80) != 0;                   // 0th bit for ra
        header.z = (flags2 >> 4) & 0x07;                    // 1-3 bit for z
        header.rcode = flags2 & 0x0F;                       // 4-7 bit for rcode

        // Read remaining counts (each 16 bits)
        header.qdcount = (input.read() << 8) | input.read();    // 2 bytes
        header.ancount = (input.read() << 8) | input.read();
        header.nscount = (input.read() << 8) | input.read();
        header.arcount = (input.read() << 8) | input.read();

        return header;
    }

    public static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) {
        DNSHeader header = new DNSHeader();

        header.id = request.getHeader().getId();

        header.qr = true;                                   // Because it is a response
        header.opcode = request.getHeader().getOpcode();
        header.aa = false;                                  // Our server is not authoritative
        header.tc = false;                                  // Not truncated
        header.rd = request.getHeader().isRd();             // Copy recursion desired
        header.ra = true;                                   // Recursion is available (Because we forward to Google)
        header.z = 0;                                       // Must be zero
        header.rcode = 0;                                   // No error

        header.qdcount = 1;                                 // Simply ccho back the question
        header.ancount = response.getAnswers().length;
        header.nscount = 0;                                 // No authority records
        header.arcount = response.getAdditionalRecords().length;

        return header;
    }

    /**
     * Write the header to an output stream
     */
    public void writeBytes(OutputStream output) throws IOException {
        // Write ID
        output.write((id >> 8) & 0xFF);
        output.write(id & 0xFF);

        // Create and write first flags byte
        int flags1 = 0;
        if (qr) flags1 |= 0x80;
        flags1 |= (opcode << 3);
        if (aa) flags1 |= 0x04;
        if (tc) flags1 |= 0x02;
        if (rd) flags1 |= 0x01;
        output.write(flags1);

        // Create and write second flags byte
        int flags2 = 0;
        if (ra) flags2 |= 0x80;
        flags2 |= (z << 4);
        flags2 |= rcode;
        output.write(flags2);

        // Write counts
        output.write((qdcount >> 8) & 0xFF);
        output.write(qdcount & 0xFF);
        output.write((ancount >> 8) & 0xFF);
        output.write(ancount & 0xFF);
        output.write((nscount >> 8) & 0xFF);
        output.write(nscount & 0xFF);
        output.write((arcount >> 8) & 0xFF);
        output.write(arcount & 0xFF);
    }

    // Getters
    public int getId()      { return id; }
    public boolean isQr()   { return qr; }
    public int getOpcode()  { return opcode; }
    public boolean isAa()   { return aa; }
    public boolean isTc()   { return tc; }
    public boolean isRd()   { return rd; }
    public boolean isRa()   { return ra; }
    public int getZ()       { return z; }
    public int getRcode()   { return rcode; }
    public int getQdcount() { return qdcount; }
    public int getAncount() { return ancount; }
    public int getNscount() { return nscount; }
    public int getArcount() { return arcount; }

    @Override
    public String toString() {
        return String.format(
                "DNSHeader[id=%d, qr=%b, opcode=%d, aa=%b, tc=%b, rd=%b, ra=%b, z=%d, rcode=%d, " +
                        "qdcount=%d, ancount=%d, nscount=%d, arcount=%d]",
                id, qr, opcode, aa, tc, rd, ra, z, rcode, qdcount, ancount, nscount, arcount
        );
    }
}

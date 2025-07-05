import java.io.*;
import java.util.*;

import static java.lang.System.*;

public class DNSMessage {
    private DNSHeader header;
    private DNSQuestion[] questions;
    private DNSRecord[] answers;
    private DNSRecord[] authorityRecords;
    private DNSRecord[] additionalRecords;
    private byte[] messageBytes; // Original message bytes for compression

    public DNSMessage(DNSHeader header, DNSQuestion[] questions, DNSRecord[] answers,
                      DNSRecord[] authorityRecords, DNSRecord[] additionalRecords) {
        this.header = header;
        this.questions = questions;
        this.answers = answers;
        this.authorityRecords = authorityRecords;
        this.additionalRecords = additionalRecords;
    }

    public static DNSMessage decodeMessage(byte[] bytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        DNSHeader header = DNSHeader.decodeHeader(inputStream);

        // Create message instance
        DNSMessage message = new DNSMessage(header, null, null, null, null);
        message.messageBytes = bytes;  // Store original bytes for compression reference

        // Decode questions
        DNSQuestion[] questions = new DNSQuestion[header.getQdcount()];
        for (int i = 0; i < header.getQdcount(); i++) {
            questions[i] = DNSQuestion.decodeQuestion(inputStream, message);
        }

        // Decode answers
        DNSRecord[] answers = new DNSRecord[header.getAncount()];
        for (int i = 0; i < header.getAncount(); i++) {
            answers[i] = DNSRecord.decodeRecord(inputStream, message);
        }

        // Decode authority records
        DNSRecord[] authorityRecords = new DNSRecord[header.getNscount()];
        for (int i = 0; i < header.getNscount(); i++) {
            authorityRecords[i] = DNSRecord.decodeRecord(inputStream, message);
        }

        // Decode additional records
        DNSRecord[] additionalRecords = new DNSRecord[header.getArcount()];
        for (int i = 0; i < header.getArcount(); i++) {
            additionalRecords[i] = DNSRecord.decodeRecord(inputStream, message);
        }

        // Update message with decoded sections
        message.questions = questions;
        message.answers = answers;
        message.authorityRecords = authorityRecords;
        message.additionalRecords = additionalRecords;

        return message;
    }

    public static void writeDomainName(ByteArrayOutputStream outputStream, HashMap<String, Integer> domainLocations, String[] domainPieces) throws IOException {
        String fullDomain = String.join(".", domainPieces);

        // Check if we've seen this domain before
        Integer offset = domainLocations.get(fullDomain);
        if (offset != null) {
            // Write compressed pointer (2 bytes with first two bits set)
            outputStream.write((0xC0 | ((offset >> 8) & 0x3F)));
            outputStream.write(offset & 0xFF);
            return;
        }

        // Store the starting position of this domain name
        domainLocations.put(fullDomain, outputStream.size());

        // Write each label
        for (String label : domainPieces) {
            byte[] labelBytes = label.getBytes();
            outputStream.write(labelBytes.length);
            outputStream.write(labelBytes);
        }

        // Write terminating zero
        outputStream.write(0);
    }

    // Getters
    public DNSHeader getHeader() {
        return header;
    }
    public DNSQuestion[] getQuestions() {
        return questions;
    }
    public DNSRecord[] getAnswers() {
        return answers;
    }
    public DNSRecord[] getAuthorityRecords() {
        return authorityRecords;
    }
    public DNSRecord[] getAdditionalRecords() {
        return additionalRecords;
    }
    public byte[] getMessageBytes() {
        return messageBytes;
    }

    // Method to build a response
    public static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers) {
        DNSHeader responseHeader = DNSHeader.buildHeaderForResponse(request,
                new DNSMessage(null, new DNSQuestion[0], answers, new DNSRecord[0], new DNSRecord[0]));

        return new DNSMessage(
                responseHeader,
                request.getQuestions(),
                answers,
                new DNSRecord[0],  // No authority records
                request.getAdditionalRecords()  // Copy additional records from request
        );
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HashMap<String, Integer> domainNameLocations = new HashMap<>();

        header.writeBytes(outputStream);

        for (DNSQuestion question : questions) {
            question.writeBytes(outputStream, domainNameLocations);
        }

        for (DNSRecord record : answers) {
            record.writeBytes(outputStream, domainNameLocations);
        }

        for (DNSRecord record : authorityRecords) {
            record.writeBytes(outputStream, domainNameLocations);
        }

        for (DNSRecord record : additionalRecords) {
            record.writeBytes(outputStream, domainNameLocations);
        }

        return outputStream.toByteArray();
    }

    @Override
    public String toString() {
        return "DNSMessage{" +
                "header=" + header +
                ", questions=" + Arrays.toString(questions) +
                ", answers=" + Arrays.toString(answers) +
                ", authorityRecords=" + Arrays.toString(authorityRecords) +
                ", additionalRecords=" + Arrays.toString(additionalRecords) +
                '}';
    }

    public String[] readDomainName(InputStream input) throws IOException {
        List<String> labels = new ArrayList<>();
        int length;

        while ((length = input.read()) > 0) {
            // Check if this is a compression pointer, length & 11000000
            if ((length & 0xC0) == 0xC0) {                              // If it's a pointer - calculate offset and read from there
                int offset = ((length & 0x3F) << 8) | input.read();     // length & 00111111
                ByteArrayInputStream offsetInput = new ByteArrayInputStream(messageBytes, offset, messageBytes.length - offset);
                String[] remainingLabels = readDomainName(offsetInput);
                labels.addAll(Arrays.asList(remainingLabels));
                break;
            } else {
                byte[] label = new byte[length];                        // Regular label - read the specified number of bytes
                input.read(label);
                labels.add(new String(label));
            }
        }
        return labels.toArray(new String[0]);
    }
}

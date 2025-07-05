# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a DNS resolver implemented in Java that acts as a local DNS server. It receives DNS queries from clients, checks a local cache, and forwards uncached requests to Google's DNS servers (8.8.8.8) for resolution.

## Architecture

The system consists of six main components (rank by implementation order):

1. **DNSHeader**: Represents the DNS header section with flags, opcodes, and section counts. No dependencies, pure data structure.
2. **DNSQuestion**: Represents DNS questions with domain name, query type, and class. No dependencies, pure data structure.
3. **DNSRecord**: Represents DNS resource records with TTL tracking and expiration logic. No dependencies, pure data structure.
4. **DNSMessage**: Handles encoding/decoding of DNS messages, including DNS message compression using pointers. Depends on DNSHeader, DNSQuestion, DNSRecord.
5. **DNSCache**: Thread-safe cache using HashMap that stores DNS records with automatic expiration. Depends on DNSQuestion, DNSRecord.
6. **DNSServer**: Main server that listens on port 8053, handles incoming UDP packets, and manages the request/response cycle. Depends on all other classes

## Key Implementation Details

- Uses UDP sockets for DNS communication
- Implements DNS message compression for domain names
- Forwards queries to Google DNS (8.8.8.8:53) when not in cache
- Default server port is 8053 (since port 53 requires root privileges)
- Maximum packet size is 512 bytes (standard DNS limit)
- Cache entries expire based on TTL values from DNS records

## Development Commands

Since this is a pure Java project with no build system configuration files:

**Compile all classes:**

```bash
javac -d . src/*.java
```

**Run the DNS server:**

```bash
java DNSServer [port]
```
If no port is specified, it defaults to 8053.

**Test the server:**

```bash
dig @localhost -p 8053 google.com
nslookup google.com 127.0.0.1 -port=8053
```

## Security Considerations

- Server runs on unprivileged port 8053 by default
- All queries are forwarded to Google's public DNS servers
- No authentication or access control implemented
- Cache is stored in memory only (not persistent)
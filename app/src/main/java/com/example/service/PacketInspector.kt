package com.example.service

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Low-level IPv4, TCP, UDP, DNS, and TLS ClientHello SNI parser & packet rebuilder
 * used by [GoodbyeDpiVpnService] to inspect and log DPI desync operations.
 */
object PacketInspector {

    data class ParsedDnsPacket(
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val ipHeaderLength: Int,
        val dnsPayload: ByteArray,
        val queriedDomain: String
    )

    /**
     * Parses an IPv4 packet from the TUN interface to check if it is a UDP DNS query (port 53).
     */
    fun parseUdpDnsQuery(packet: ByteArray, length: Int): ParsedDnsPacket? {
        if (length < 28) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // 17 = UDP

        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)

        if (dstPort != 53 && dstPort != 1253 && dstPort != 9953 && dstPort != 5353) return null

        val udpHeaderLen = 8
        val payloadOffset = ihl + udpHeaderLen
        val payloadLen = length - payloadOffset
        if (payloadLen <= 12) return null

        val dnsPayload = packet.copyOfRange(payloadOffset, length)
        val domain = extractDnsQuestionDomain(dnsPayload) ?: "bilinmeyen.alan.adi"

        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)

        return ParsedDnsPacket(
            sourceIp = srcIp,
            destIp = dstIp,
            sourcePort = srcPort,
            destPort = dstPort,
            ipHeaderLength = ihl,
            dnsPayload = dnsPayload,
            queriedDomain = domain
        )
    }

    /**
     * Extracts QNAME from raw DNS query payload.
     */
    fun extractDnsQuestionDomain(dnsPayload: ByteArray): String? {
        if (dnsPayload.size < 13) return null
        val parts = mutableListOf<String>()
        var idx = 12
        while (idx < dnsPayload.size) {
            val labelLen = dnsPayload[idx].toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen > 63 || idx + 1 + labelLen > dnsPayload.size) return null
            val label = String(dnsPayload, idx + 1, labelLen, StandardCharsets.US_ASCII)
            parts.add(label)
            idx += 1 + labelLen
        }
        return if (parts.isEmpty()) null else parts.joinToString(".")
    }

    /**
     * Builds a valid IPv4 + UDP response packet with computed IPv4 header checksum
     * to inject the DNS response back into the Android TUN interface.
     */
    fun buildUdpResponsePacket(
        originalQuery: ParsedDnsPacket,
        responsePayload: ByteArray
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + responsePayload.size
        val buffer = ByteBuffer.allocate(totalLen)

        // IPv4 Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        buffer.put(0x00.toByte()) // DSCP / ECN
        buffer.putShort(totalLen.toShort())
        buffer.putShort(0x1337.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags: Don't Fragment
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP (17)
        buffer.putShort(0.toShort()) // Checksum placeholder (offset 10)

        // Swap Source & Destination IP
        buffer.put(originalQuery.destIp)
        buffer.put(originalQuery.sourceIp)

        // Compute IPv4 header checksum
        val rawArray = buffer.array()
        val checksum = computeIpv4Checksum(rawArray, 0, ipHeaderLen)
        buffer.putShort(10, checksum.toShort())

        // UDP Header
        buffer.putShort(originalQuery.destPort.toShort())
        buffer.putShort(originalQuery.sourcePort.toShort())
        buffer.putShort((udpHeaderLen + responsePayload.size).toShort())
        buffer.putShort(0.toShort()) // UDP checksum 0 (optional in IPv4)

        // DNS Payload
        buffer.put(responsePayload)
        return rawArray
    }

    private fun computeIpv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            if ((sum and 0xFFFF0000.toInt()) != 0) {
                sum = (sum and 0xFFFF) + (sum ushr 16)
            }
            i += 2
        }
        return sum.inv() and 0xFFFF
    }

    /**
     * Inspects a TLS ClientHello payload and extracts the Server Name Indication (SNI) hostname
     * along with its byte offset, illustrating where GoodbyeDPI splits the packet (--frag-by-sni).
     */
    fun extractTlsClientHelloSni(payload: ByteArray): Pair<String, Int>? {
        try {
            // TLS Record layer: ContentType 0x16 (Handshake), Version 0x0301..0x0303
            if (payload.size < 43 || payload[0].toInt() != 0x16 || payload[1].toInt() != 0x03) {
                return null
            }
            // Handshake Type 0x01 (ClientHello) at index 5
            if (payload[5].toInt() != 0x01) return null

            var pos = 43 // Skip Record Header (5) + Handshake Header (4) + Version (2) + Random (32)
            if (pos >= payload.size) return null

            // Session ID
            val sessionIdLen = payload[pos].toInt() and 0xFF
            pos += 1 + sessionIdLen
            if (pos + 2 > payload.size) return null

            // Cipher Suites
            val cipherSuitesLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
            pos += 2 + cipherSuitesLen
            if (pos + 1 > payload.size) return null

            // Compression Methods
            val compMethodsLen = payload[pos].toInt() and 0xFF
            pos += 1 + compMethodsLen
            if (pos + 2 > payload.size) return null

            // Extensions total length
            val extensionsLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
            pos += 2
            val extensionsEnd = (pos + extensionsLen).coerceAtMost(payload.size)

            while (pos + 4 <= extensionsEnd) {
                val extType = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                val extLen = ((payload[pos + 2].toInt() and 0xFF) shl 8) or (payload[pos + 3].toInt() and 0xFF)
                pos += 4
                if (extType == 0x0000 && pos + 5 <= extensionsEnd) { // server_name extension
                    val nameType = payload[pos + 2].toInt() and 0xFF
                    val nameLen = ((payload[pos + 3].toInt() and 0xFF) shl 8) or (payload[pos + 4].toInt() and 0xFF)
                    val sniStartOffset = pos + 5
                    if (nameType == 0 && sniStartOffset + nameLen <= extensionsEnd) {
                        val sniHost = String(payload, sniStartOffset, nameLen, StandardCharsets.US_ASCII)
                        return sniHost to sniStartOffset
                    }
                }
                pos += extLen
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }
}

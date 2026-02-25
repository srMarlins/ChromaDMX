package com.chromadmx.platform

/**
 * iOS UDP transport implementation stub for DMX protocol communication.
 *
 * When implemented, this will use Apple's Network.framework (NWConnection)
 * via Kotlin/Native cinterop to send and receive UDP datagrams for
 * Art-Net and sACN protocols.
 *
 * Network.framework is preferred over POSIX sockets on iOS because:
 * - It supports the modern networking stack with better Wi-Fi performance
 * - It handles network transitions (Wi-Fi to cellular) gracefully
 * - It integrates with iOS network permissions and local network access
 * - It provides built-in support for Bonjour/mDNS service discovery
 *
 * The cinterop definition will be added to the Gradle build when this
 * module is implemented:
 * ```
 * kotlin {
 *     iosArm64 {
 *         compilations.getByName("main") {
 *             cinterops {
 *                 val network by creating {
 *                     // Network.framework cinterop
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Key APIs to wrap:
 * - NWConnection (UDP mode) for sending/receiving datagrams
 * - NWListener for receiving broadcast packets
 * - NWBrowser for Bonjour service discovery (Art-Net nodes)
 * - NWParameters for configuring UDP settings
 */
class IosUdpTransport {
    // TODO: Implement when shared/networking module defines the expect UdpTransport interface
    //
    // Expected interface:
    //   actual class IosUdpTransport : UdpTransport {
    //       override suspend fun send(data: ByteArray, host: String, port: Int)
    //       override suspend fun receive(buffer: ByteArray): UdpPacket
    //       override suspend fun broadcast(data: ByteArray, port: Int)
    //       override fun close()
    //   }
}

package app.kreate.android.utils

import kotlinx.serialization.Serializable
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * One configured proxy entry.
 *
 * [scheme] is [Proxy.Type.name] rather than [Proxy.Type] itself — kotlinx.serialization can't
 * annotate the external `java.net.Proxy.Type` enum, so it's stored as a plain string and mapped
 * back with [Proxy.Type.valueOf].
 */
@Serializable
data class ProxyEntry(
    val scheme: String = Proxy.Type.HTTP.name,
    val host: String,
    val port: Int
) {

    val type: Proxy.Type
        get() = runCatching { Proxy.Type.valueOf(scheme) }.getOrDefault(Proxy.Type.HTTP)

    fun toProxy(): Proxy = Proxy(type, InetSocketAddress(host, port))

    override fun toString(): String = "${type.name} $host:$port"
}

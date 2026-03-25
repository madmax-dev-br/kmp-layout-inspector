package com.example.kmppoc

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

private val adbPath: String by lazy {
    // Try to find adb on PATH
    try {
        val p = ProcessBuilder("which", "adb")
            .redirectErrorStream(true)
            .start()
        val result = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        if (p.exitValue() == 0 && result.isNotEmpty()) return@lazy result
    } catch (_: Exception) { }

    // Try ANDROID_HOME / ANDROID_SDK_ROOT
    val home = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
    if (home != null) {
        val path = "$home/platform-tools/adb"
        if (java.io.File(path).exists()) return@lazy path
    }

    // Hardcoded fallback
    "/home/raphael/Android/Sdk/platform-tools/adb"
}

fun main() {
    println("Using adb at: $adbPath")
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            val html = this::class.java.classLoader
                .getResourceAsStream("inspector.html")
                ?.bufferedReader()?.readText()
                ?: "inspector.html not found on classpath"
            call.respondText(html, ContentType.Text.Html)
        }

        get("/api/components") {
            val json = ComponentJson.encodeToString(
                ListSerializer(UiComponent.serializer()),
                defaultComponents,
            )
            call.respondText(json, ContentType.Application.Json)
        }

        get("/api/screenshot") {
            try {
                val process = ProcessBuilder(adbPath, "exec-out", "screencap", "-p")
                    .redirectErrorStream(false)
                    .start()
                val bytes = process.inputStream.readBytes()
                process.waitFor()
                if (bytes.isEmpty()) {
                    call.respondText(
                        """{"error":"No screenshot data. Is the device connected?"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.ServiceUnavailable
                    )
                } else {
                    call.respondBytes(bytes, ContentType.Image.PNG)
                }
            } catch (e: Exception) {
                call.respondText(
                    """{"error":"${e.message?.replace("\"", "'")}"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        }

        get("/api/hierarchy") {
            try {
                // Dump UI hierarchy via uiautomator
                val dumpProcess = ProcessBuilder(
                    adbPath, "shell",
                    "uiautomator", "dump", "/data/local/tmp/uidump.xml"
                ).redirectErrorStream(true).start()
                dumpProcess.inputStream.bufferedReader().readText()
                dumpProcess.waitFor()

                // Read the dumped file
                val catProcess = ProcessBuilder(
                    adbPath, "shell", "cat", "/data/local/tmp/uidump.xml"
                ).redirectErrorStream(false).start()
                val xml = catProcess.inputStream.bufferedReader().readText()
                catProcess.waitFor()

                val xmlStart = xml.indexOf("<?xml")
                val xmlEnd = xml.indexOf("</hierarchy>")
                if (xmlStart == -1 || xmlEnd == -1) {
                    call.respondText("[]", ContentType.Application.Json)
                    return@get
                }

                val cleanXml = xml.substring(xmlStart, xmlEnd + "</hierarchy>".length)
                val json = parseHierarchyToJson(cleanXml)
                call.respondText(json, ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText(
                    """[{"error":"${e.message?.replace("\"", "'")}"}]""",
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        }

        get("/api/compose-tree") {
            try {
                // Forward port from computer to device's embedded inspector server
                val fwd = ProcessBuilder(adbPath, "forward", "tcp:8082", "tcp:8082")
                    .redirectErrorStream(true).start()
                fwd.inputStream.bufferedReader().readText()
                fwd.waitFor()

                // Fetch the compose tree from the app's embedded server
                val url = java.net.URL("http://localhost:8082/compose-tree")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 5000
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                call.respondText(json, ContentType.Application.Json)
            } catch (e: java.net.ConnectException) {
                call.respondText(
                    """{"error":"App inspector not reachable. Is the app running?"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable
                )
            } catch (e: Exception) {
                call.respondText(
                    """{"error":"${e.message?.replace("\"", "'")}"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        }

        get("/api/interaction-status") {
            try {
                val fwd = ProcessBuilder(adbPath, "forward", "tcp:8082", "tcp:8082")
                    .redirectErrorStream(true).start()
                fwd.inputStream.bufferedReader().readText()
                fwd.waitFor()

                val url = java.net.URL("http://localhost:8082/interaction-status")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                call.respondText(json, ContentType.Application.Json)
            } catch (_: Exception) {
                call.respondText(
                    """{"idle":false,"generation":0}""",
                    ContentType.Application.Json
                )
            }
        }

        get("/api/device-info") {
            try {
                val process = ProcessBuilder(adbPath, "shell", "wm", "size")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()

                // Parse "Physical size: WxH" or "Override size: WxH"
                // Take the last match (override takes precedence)
                val matches = Regex("(\\d+)x(\\d+)").findAll(output).toList()
                val last = matches.lastOrNull()
                val width = last?.groupValues?.get(1) ?: "1080"
                val height = last?.groupValues?.get(2) ?: "2400"

                call.respondText(
                    """{"width":$width,"height":$height}""",
                    ContentType.Application.Json
                )
            } catch (e: Exception) {
                call.respondText(
                    """{"width":1080,"height":2400,"error":"${e.message?.replace("\"", "'")}"}""",
                    ContentType.Application.Json
                )
            }
        }
    }
}

private fun parseHierarchyToJson(xml: String): String {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

    val nodes = mutableListOf<String>()

    fun escapeJson(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    fun traverse(node: Node) {
        if (node is Element && node.tagName == "node") {
            val bounds = node.getAttribute("bounds")
            val className = node.getAttribute("class") ?: ""
            val text = node.getAttribute("text") ?: ""
            val resourceId = node.getAttribute("resource-id") ?: ""
            val contentDesc = node.getAttribute("content-desc") ?: ""
            val packageName = node.getAttribute("package") ?: ""

            val boundsMatch = Regex("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").find(bounds)
            if (boundsMatch != null) {
                val left = boundsMatch.groupValues[1].toInt()
                val top = boundsMatch.groupValues[2].toInt()
                val right = boundsMatch.groupValues[3].toInt()
                val bottom = boundsMatch.groupValues[4].toInt()
                val shortClass = className.substringAfterLast(".")

                nodes.add(buildString {
                    append("{")
                    append("\"fullClass\":\"${escapeJson(className)}\",")
                    append("\"shortClass\":\"${escapeJson(shortClass)}\",")
                    append("\"text\":\"${escapeJson(text)}\",")
                    append("\"resourceId\":\"${escapeJson(resourceId)}\",")
                    append("\"contentDesc\":\"${escapeJson(contentDesc)}\",")
                    append("\"pkg\":\"${escapeJson(packageName)}\",")
                    append("\"bounds\":{\"left\":$left,\"top\":$top,\"right\":$right,\"bottom\":$bottom},")
                    append("\"width\":${right - left},")
                    append("\"height\":${bottom - top}")
                    append("}")
                })
            }
        }

        val children = node.childNodes
        for (i in 0 until children.length) {
            traverse(children.item(i))
        }
    }

    traverse(document.documentElement)
    return "[${nodes.joinToString(",")}]"
}

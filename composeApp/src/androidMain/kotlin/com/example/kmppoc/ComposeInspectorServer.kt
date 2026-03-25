package com.example.kmppoc

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.AnnotatedString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ComposeInspectorServer(private val activity: Activity) {

    private var serverSocket: ServerSocket? = null
    @Volatile
    private var running = false

    // Interaction idle tracking
    @Volatile
    private var idleGeneration = 0
    @Volatile
    private var isIdle = false
    private var idleHandler: Handler? = null
    private var idleRunnable: Runnable? = null

    fun start(port: Int = 8082) {
        running = true
        setupTouchDetection()
        Thread(null, {
            try {
                serverSocket = ServerSocket(port)
                while (running) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (_: Exception) {
                        if (!running) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, "compose-inspector").start()
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        activity.runOnUiThread {
            idleRunnable?.let { idleHandler?.removeCallbacks(it) }
        }
    }

    private fun setupTouchDetection() {
        activity.runOnUiThread {
            idleHandler = Handler(Looper.getMainLooper())
            idleRunnable = Runnable {
                isIdle = true
                idleGeneration++
            }

            val originalCallback = activity.window.callback
            activity.window.callback = object : Window.Callback by originalCallback {
                override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                    if (event != null) {
                        onUserTouch()
                    }
                    return originalCallback.dispatchTouchEvent(event)
                }
            }
        }
    }

    private fun onUserTouch() {
        isIdle = false
        idleRunnable?.let { r ->
            idleHandler?.removeCallbacks(r)
            idleHandler?.postDelayed(r, 1000)
        }
    }

    private fun handleClient(client: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val requestLine = reader.readLine() ?: return@Thread

                // Consume headers
                while (true) {
                    val line = reader.readLine()
                    if (line.isNullOrEmpty()) break
                }

                val json = when {
                    requestLine.contains("/interaction-status") ->
                        """{"idle":$isIdle,"generation":$idleGeneration}"""
                    requestLine.contains("/compose-tree") -> getComposeTreeOnUiThread()
                    else -> """{"error":"unknown endpoint"}"""
                }

                val bytes = json.toByteArray(Charsets.UTF_8)
                val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n" +
                    "Content-Length: ${bytes.size}\r\n\r\n"

                client.getOutputStream().apply {
                    write(header.toByteArray(Charsets.US_ASCII))
                    write(bytes)
                    flush()
                }
            } catch (_: Exception) {
            } finally {
                try { client.close() } catch (_: Exception) {}
            }
        }.start()
    }

    private fun getComposeTreeOnUiThread(): String {
        var result = "[]"
        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            result = try {
                extractComposeTree()
            } catch (e: Exception) {
                """{"error":"${esc(e.message ?: "unknown")}"}"""
            }
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
        return result
    }

    private fun extractComposeTree(): String {
        val rootView = activity.window.decorView
        val composeView = findComposeView(rootView)
            ?: return """{"error":"No AndroidComposeView found"}"""

        val semanticsOwner: SemanticsOwner = try {
            val method = composeView.javaClass.getMethod("getSemanticsOwner")
            method.invoke(composeView) as SemanticsOwner
        } catch (e: Exception) {
            return """{"error":"Cannot access SemanticsOwner: ${esc(e.message ?: "")}"}"""
        }

        val rootNode = try {
            semanticsOwner.unmergedRootSemanticsNode
        } catch (_: Exception) {
            semanticsOwner.rootSemanticsNode
        }

        val nodes = mutableListOf<String>()
        traverseNode(rootNode, -1, null, null, nodes)
        return "[${nodes.joinToString(",")}]"
    }

    /**
     * Traverse the semantics tree. When we find a node with a testTag matching
     * "Component:<Type>:<id>", we propagate that componentType/componentId to
     * all descendants so the web inspector can group children under their parent component.
     */
    private fun traverseNode(
        node: SemanticsNode,
        parentId: Int,
        inheritedComponentType: String?,
        inheritedComponentId: String?,
        out: MutableList<String>,
    ) {
        try {
            val b = node.boundsInWindow
            val left = clamp(b.left)
            val top = clamp(b.top)
            val right = clamp(b.right)
            val bottom = clamp(b.bottom)
            val w = (right - left).coerceAtLeast(0)
            val h = (bottom - top).coerceAtLeast(0)

            // Collect all semantic properties
            val props = mutableMapOf<String, String>()
            var text = ""
            var role = ""
            var contentDesc = ""
            var testTag = ""

            for (entry in node.config) {
                val keyName = entry.key.name
                val rawValue = entry.value
                val formatted = formatValue(rawValue)
                props[keyName] = formatted

                when (keyName) {
                    "Text" -> text = extractTextValue(rawValue)
                    "Role" -> role = extractRoleValue(rawValue)
                    "ContentDescription" -> contentDesc = extractListStrValue(rawValue)
                    "TestTag" -> testTag = rawValue?.toString() ?: ""
                }
            }

            // Detect Component wrapper via testTag "Component:<Type>:<id>"
            var componentType = inheritedComponentType ?: ""
            var componentId = inheritedComponentId ?: ""
            var isComponentRoot = false

            if (testTag.startsWith("Component:")) {
                val parts = testTag.split(":", limit = 3)
                if (parts.size == 3) {
                    componentType = parts[1]
                    componentId = parts[2]
                    isComponentRoot = true
                }
            }

            // Derive display name
            val displayName = when {
                isComponentRoot -> componentType
                role.isNotEmpty() -> role
                text.isNotEmpty() -> "Text"
                props.containsKey("EditableText") -> "TextField"
                props.containsKey("ProgressBarRangeInfo") -> "ProgressBar"
                props.containsKey("VerticalScrollAxisRange") || props.containsKey("HorizontalScrollAxisRange") -> "Scrollable"
                props.containsKey("Heading") -> "Heading"
                else -> "Node"
            }

            val childIds = node.children.map { it.id }

            val merges = try {
                val m = node.javaClass.getMethod("isMergingSemanticsOfDescendants")
                m.invoke(node) as? Boolean ?: false
            } catch (_: Exception) { false }

            val propsJson = props.entries.joinToString(",") { (k, v) ->
                "\"${esc(k)}\":\"${esc(v)}\""
            }

            out.add(buildString {
                append("{\"id\":${node.id},")
                append("\"parentId\":$parentId,")
                append("\"displayName\":\"${esc(displayName)}\",")
                append("\"role\":\"${esc(role)}\",")
                append("\"text\":\"${esc(text)}\",")
                append("\"contentDesc\":\"${esc(contentDesc)}\",")
                append("\"testTag\":\"${esc(testTag)}\",")
                append("\"mergesDescendants\":$merges,")
                append("\"componentType\":\"${esc(componentType)}\",")
                append("\"componentId\":\"${esc(componentId)}\",")
                append("\"isComponentRoot\":$isComponentRoot,")
                append("\"bounds\":{\"left\":$left,\"top\":$top,\"right\":$right,\"bottom\":$bottom},")
                append("\"width\":$w,\"height\":$h,")
                append("\"childIds\":[${childIds.joinToString(",")}],")
                append("\"properties\":{$propsJson}}")
            })

            for (child in node.children) {
                traverseNode(child, node.id, componentType, componentId, out)
            }
        } catch (_: Exception) {
            // Skip nodes that can't be read
        }
    }

    private fun extractTextValue(value: Any?): String {
        if (value is List<*>) {
            return value.filterNotNull().joinToString(", ") {
                when (it) {
                    is AnnotatedString -> it.text
                    else -> it.toString()
                }
            }
        }
        return value?.toString() ?: ""
    }

    private fun extractRoleValue(value: Any?): String {
        if (value == null) return ""
        try {
            val field = value.javaClass.getDeclaredField("value")
            field.isAccessible = true
            return when (field.getInt(value)) {
                0 -> "Button"
                1 -> "Checkbox"
                2 -> "Switch"
                3 -> "RadioButton"
                4 -> "Tab"
                5 -> "Image"
                6 -> "DropdownList"
                else -> value.toString()
            }
        } catch (_: Exception) {}
        return value.toString()
    }

    private fun extractListStrValue(value: Any?): String {
        if (value is List<*>) return value.filterNotNull().joinToString(", ") { it.toString() }
        return value?.toString() ?: ""
    }

    private fun formatValue(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        is Boolean -> value.toString()
        is Number -> value.toString()
        is Unit -> "true"
        is List<*> -> value.filterNotNull().joinToString(", ") {
            when (it) {
                is AnnotatedString -> it.text
                else -> it.toString()
            }
        }
        else -> value.toString()
    }

    private fun findComposeView(view: View): View? {
        if (view.javaClass.name.contains("AndroidComposeView")) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findComposeView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun clamp(v: Float): Int {
        if (v.isNaN() || v.isInfinite()) return 0
        return v.coerceIn(-10000f, 100000f).toInt()
    }

    private fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

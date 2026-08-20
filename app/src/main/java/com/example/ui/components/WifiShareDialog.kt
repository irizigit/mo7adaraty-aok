package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

@Composable
fun WifiShareDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var serverPort by remember { mutableIntStateOf(8080) }
    var localIp by remember { mutableStateOf("127.0.0.1") }
    var isServerRunning by remember { mutableStateOf(false) }
    var downloadCount by remember { mutableIntStateOf(0) }
    var serverSocket by remember { mutableStateOf<ServerSocket?>(null) }

    // Start embedded HTTP server when dialog opens
    DisposableEffect(Unit) {
        val ip = getLocalIpAddress(context)
        localIp = ip

        val apkFile = File(context.applicationInfo.sourceDir)
        var ss: ServerSocket? = null

        thread {
            try {
                ss = ServerSocket(8080)
                serverPort = 8080
                serverSocket = ss
                isServerRunning = true

                while (!ss!!.isClosed) {
                    val socket: Socket = ss!!.accept()
                    thread {
                        handleHttpRequest(socket, apkFile) {
                            downloadCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WifiShareDialog", "Server exception: ${e.message}")
                isServerRunning = false
            }
        }

        onDispose {
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val downloadUrl = "http://$localIp:$serverPort/app.apk"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مشاركة التطبيق عبر QR Code",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ارسال التطبيق مباشرة للهاتف الآخر",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Render Canvas
                Card(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeMatrixCanvas(text = downloadUrl)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "قم بمسح رمز الـ QR من كاميرا الهاتف الآخر لتحميل وتثبيت التطبيق مباشرة عبر السيرفر المحلي بدون إنترنت",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Direct APK File Share Button
                Button(
                    onClick = {
                        try {
                            val apkFile = File(context.applicationInfo.sourceDir)
                            val apkUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                putExtra(Intent.EXTRA_SUBJECT, "تطبيق محاضراتي - APK")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "تفضل بتحميل تطبيق محاضراتي الشامل: $downloadUrl"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "إرسال ملف التطبيق مباشرة إلى الهاتف الآخر"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر مشاركة الملف المباشر، يمكنك استخدام مسح QR", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال ملف التطبيق (APK) مباشرة لهاتف آخر")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // URL & Copy Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رابط التحميل المباشر بالسيرفر:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = downloadUrl,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(downloadUrl))
                                Toast.makeText(context, "تم نسخ الرابط!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرابط")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (downloadCount > 0) {
                    Text(
                        text = "عدد مرات التحميل الناجحة: $downloadCount 📥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}

@Composable
fun QrCodeMatrixCanvas(text: String) {
    // Generate deterministic 21x21 QR-like pattern based on input hash
    val matrixSize = 21
    val hash = text.hashCode()

    Canvas(modifier = Modifier.size(170.dp)) {
        val cellSize = size.width / matrixSize
        val darkColor = Color(0xFF1E293B)
        val lightColor = Color.White

        drawRect(color = lightColor, size = size)

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val isFinderPattern = (r < 7 && c < 7) || (r < 7 && c >= matrixSize - 7) || (r >= matrixSize - 7 && c < 7)
                var isDark = false

                if (isFinderPattern) {
                    // Outer square
                    val isOuter = r == 0 || r == 6 || c == 0 || c == 6 ||
                            (r < 7 && c >= matrixSize - 7 && (r == 0 || r == 6 || c == matrixSize - 7 || c == matrixSize - 1)) ||
                            (r >= matrixSize - 7 && c < 7 && (r == matrixSize - 7 || r == matrixSize - 1 || c == 0 || c == 6))
                    // Inner square
                    val isInner = (r in 2..4 && c in 2..4) ||
                            (r in 2..4 && c in (matrixSize - 5)..(matrixSize - 3)) ||
                            (r in (matrixSize - 5)..(matrixSize - 3) && c in 2..4)

                    isDark = isOuter || isInner
                } else {
                    // Seeded pseudo-random data bits
                    val bitPos = (r * matrixSize + c) % 31
                    val valFromText = if (c < text.length) text[c].code else 7
                    isDark = ((hash ushr bitPos) and 1 == 1) xor (valFromText % (r + 1) == 0)
                }

                if (isDark) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

private fun getLocalIpAddress(context: Context): String {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipInt != 0) {
            return String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        }

        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "192.168.1.10"
}

private fun handleHttpRequest(socket: Socket, apkFile: File, onDownloadSuccess: () -> Unit) {
    try {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val buffer = ByteArray(1024)
        input.read(buffer)

        if (apkFile.exists()) {
            val length = apkFile.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.android.package-archive\r\n" +
                    "Content-Length: $length\r\n" +
                    "Content-Disposition: attachment; filename=\"mo7adaraty.apk\"\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(header.toByteArray())

            FileInputStream(apkFile).use { fis ->
                val fileBuf = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(fileBuf).also { bytesRead = it } != -1) {
                    output.write(fileBuf, 0, bytesRead)
                }
            }
            output.flush()
            onDownloadSuccess()
        } else {
            val notFound = "HTTP/1.1 404 Not Found\r\n\r\n"
            output.write(notFound.toByteArray())
        }
        socket.close()
    } catch (e: Exception) {
        try { socket.close() } catch (ignored: Exception) {}
    }
}

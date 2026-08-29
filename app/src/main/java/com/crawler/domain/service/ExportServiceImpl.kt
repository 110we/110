package com.crawler.domain.service

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.crawler.domain.model.*
import com.crawler.util.PermissionHelper
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportServiceImpl @Inject constructor(
    private val context: Context,
    private val permissionHelper: PermissionHelper
) : ExportService {

    private val json = Json { prettyPrint = true }

    override suspend fun exportCsv(results: List<CrawlResult>, config: ExportConfig): ExportResult {
        val fileName = "crawler_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        val uri = createExportUri(fileName, "text/csv")
        val sensitiveRemoved = resultSensitiveFieldCount(results, config)
        uri?.let { writeCsv(it, results, config) }
        return ExportResult(uri!!, results.size, getFileSize(uri!!), sensitiveRemoved)
    }

    override suspend fun exportJson(results: List<CrawlResult>, config: ExportConfig): ExportResult {
        val fileName = "crawler_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        val uri = createExportUri(fileName, "application/json")
        val sensitiveRemoved = resultSensitiveFieldCount(results, config)
        uri?.let { writeJson(it, results, config) }
        return ExportResult(uri!!, results.size, getFileSize(uri!!), sensitiveRemoved)
    }

    override suspend fun exportExcel(results: List<CrawlResult>, config: ExportConfig): ExportResult {
        val fileName = "crawler_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"
        val uri = createExportUri(fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        val sensitiveRemoved = resultSensitiveFieldCount(results, config)
        uri?.let { writeExcel(it, results, config) }
        return ExportResult(uri!!, results.size, getFileSize(uri!!), sensitiveRemoved)
    }

    private fun createExportUri(fileName: String, mimeType: String): Uri? {
        if (permissionHelper.checkManageStorage()) {
            // 使用 MANAGE_EXTERNAL_STORAGE 直接写入 Downloads
            return createFileInDownloads(fileName, mimeType)
        } else {
            // 回退到 MediaStore
            return createMediaStoreFile(fileName, mimeType)
        }
    }

    private fun createFileInDownloads(fileName: String, mimeType: String): Uri? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            createMediaStoreFile(fileName, mimeType)
        }
    }

    private fun createMediaStoreFile(fileName: String, mimeType: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun writeCsv(uri: Uri, results: List<CrawlResult>, config: ExportConfig) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            val writer = java.io.OutputStreamWriter(output, "UTF-8")
            // 写入 BOM
            writer.write("\uFEFF")
            
            // 确定列（含敏感字段过滤）
            val selectedFields = config.resolveFields(getAllFields(results))

            // 表头
            writer.write(selectedFields.joinToString(",") { escapeCsv(it) })
            writer.write("\n")

            // 数据行
            for (result in results) {
                val row = selectedFields.map { field ->
                    val value = result.data[field]?.toString() ?: ""
                    escapeCsv(value)
                }
                writer.write(row.joinToString(","))
                writer.write("\n")
            }
            writer.flush()
        }
    }

    private fun writeJson(uri: Uri, results: List<CrawlResult>, config: ExportConfig) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            val writer = java.io.OutputStreamWriter(output, "UTF-8")
            writer.write("[\n")
            val selectedFields = config.resolveFields(getAllFields(results)).toSet()
            for (i in results.indices) {
                val result = results[i]
                val filteredData = result.data.filterKeys { it in selectedFields }
                val jsonStr = json.encodeToString(filteredData)
                writer.write("  $jsonStr${if (i < results.lastIndex) "," else ""}\n")
            }
            writer.write("]")
            writer.flush()
        }
    }

    private fun writeExcel(uri: Uri, results: List<CrawlResult>, config: ExportConfig) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            val workbook = SXSSFWorkbook(100) // 窗口大小 100 行，防止 OOM
            val sheet = workbook.createSheet("Crawl Results")
            val headerStyle = workbook.createCellStyle()
            val font = workbook.createFont()
            font.bold = true
            headerStyle.setFont(font)

            val selectedFields = config.resolveFields(getAllFields(results))

            // 表头
            val headerRow = sheet.createRow(0)
            selectedFields.forEachIndexed { index, field ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(field)
                cell.cellStyle = headerStyle
            }

            // 数据
            results.forEachIndexed { rowIndex, result ->
                val row = sheet.createRow(rowIndex + 1)
                selectedFields.forEachIndexed { colIndex, field ->
                    val value = result.data[field]?.toString() ?: ""
                    val cell = row.createCell(colIndex)
                    cell.setCellValue(value)
                }
            }

            // 自动调整列宽
            selectedFields.indices.forEach { sheet.autoSizeColumn(it) }

            workbook.write(output)
            workbook.dispose()
        }
    }

    private fun getAllFields(results: List<CrawlResult>): List<String> {
        return results.flatMap { it.data.keys }.distinct().sorted()
    }

    private fun resultSensitiveFieldCount(results: List<CrawlResult>, config: ExportConfig): Int {
        if (!config.excludeSensitiveFields) return 0
        val allFields = getAllFields(results)
        val selected = config.resolveFields(allFields)
        return allFields.size - selected.size
    }

    private fun escapeCsv(value: String): String {
        // 防 CSV 公式注入：以 =、+、-、@ 开头的单元格值加单引号前缀，防止在 Excel/WPS 中被当作公式执行
        val sanitized = if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) {
            "'" + value
        } else {
            value
        }
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
            return "\"${sanitized.replace("\"", "\"\"")}\""
        }
        return sanitized
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
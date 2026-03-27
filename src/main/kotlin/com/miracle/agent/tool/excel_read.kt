package com.miracle.agent.tool

import com.intellij.openapi.project.Project
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.JsonField
import com.miracle.utils.normalizeFilePath
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.*
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.reflect.KFunction

data class ExcelRow(
    val rowNumber: Int,
    val values: List<String>,
)

data class ExcelReadToolOutput(
    val filePath: String,
    val fileName: String?,
    val sheetIndex: Int,
    val sheetName: String,
    val rows: List<ExcelRow>,
)

/**
 * 读取 Excel 并转换成 JSON 的工具
 */
object ExcelReadTool : Tool<ExcelReadToolOutput> {

    private const val DEFAULT_SHEET_INDEX = 0
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private val executeCache: MutableMap<String, ToolCallResult<ExcelReadToolOutput>> = ConcurrentHashMap()

    // 工具定义：对外暴露参数 schema，供模型选择调用
    private val SPEC = ToolSpecification.builder()
        .name("ReadExcel")
        .description(
            """
            Reads an Excel workbook (XLS/XLSX) and returns the specified sheet as JSON.
            Supports absolute file paths and direct HTTP(S) downloads without authentication.
            """.trimIndent()
        )
        .parameters(
            JsonObjectSchema.builder()
                .addStringProperty(
                    "file_path",
                    "Absolute path or HTTP(S) URL of the Excel file. "
                )
                .addStringProperty(
                    "file_name",
                    //告诉模型如果能从文件路径中识别就给这个参数赋值
                    "Optional friendly file name. If omitted, it is derived from the file path."
                )
                .addIntegerProperty(
                    "sheet_index",
                    "Zero-based index of the sheet to read. Defaults to 0 (the first sheet)."
                )
                .required("file_path")
                .build()
        )
        .build()

    override fun getToolSpecification(): ToolSpecification = SPEC

    override fun getExecuteFunc(): KFunction<ToolCallResult<ExcelReadToolOutput>> = ::execute

    override fun renderResultForAssistant(output: ExcelReadToolOutput): String {
        val json = buildJsonObject {
            put("file_path", output.filePath)
            output.fileName?.let { put("file_name", it) }
            put("sheet_index", output.sheetIndex)
            put("sheet_name", output.sheetName)
            put("rows", buildJsonArray {
                output.rows.forEach { row ->
                    add(
                        buildJsonObject {
                            put("row_number", row.rowNumber)
                            putJsonArray("values") {
                                row.values.forEach { value ->
                                    add(JsonPrimitive(value))
                                }
                            }
                        }
                    )
                }
            })
        }
        return json.toString()
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        val jsonObject = input as? JsonObject ?: return
        val filePath = jsonObject["file_path"]?.jsonPrimitive?.contentOrNull
            ?: throw MissingToolParameterException(getName(), "file_path")
        val sheetIndex = jsonObject["sheet_index"]?.jsonPrimitive?.int ?: DEFAULT_SHEET_INDEX
        if (sheetIndex < 0) {
            throw ToolParameterException("sheet_index must be greater than or equal to 0")
        }
        if (!isRemotePath(filePath)) {
            val resolvedPath = normalizeFilePath(filePath, taskState.project)
            val file = File(resolvedPath)
            if (!file.exists() || !file.isFile) {
                throw ToolParameterException("Excel file does not exist: $resolvedPath")
            }
        }
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null
        val filePath = partialArgs["file_path"]?.value ?: return null
        val sheetIndex = partialArgs["sheet_index"]?.value?.toIntOrNull() ?: DEFAULT_SHEET_INDEX
        val resolvedFileName = resolveFileName(null, filePath, taskState.project)
        val result = execute(filePath,resolvedFileName,sheetIndex, taskState)
        // Parse the JSON string into a JsonElement
        val jsonElement = Json.parseToJsonElement(result.resultForAssistant)
        // To get the pretty-printed string
        val json = Json { prettyPrint = true }
        val prettyJsonString = json.encodeToString(JsonElement.serializer(), jsonElement)
        // You can now use prettyJsonString if you need the formatted string.

        return ToolSegment(
            name = UiToolName.EXCEL_READ,
            toolCommand = resolvedFileName ?: filePath,
            toolContent = prettyJsonString,
            params = mapOf(
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                )
            )
        )
    }

    fun execute(
        file_path: String,
        file_name: String? = null,
        sheet_index: Int? = null,
        taskState: TaskState,
    ): ToolCallResult<ExcelReadToolOutput> {
        val sheetIndex = sheet_index ?: DEFAULT_SHEET_INDEX
        val resolvedFileName = resolveFileName(file_name, file_path, taskState.project)
        if (executeCache.contains(file_path)) return executeCache.remove(file_path)!!
        try {
            // 根据入参判断是读取本地文件还是远程下载
            val excelInput = openExcelInputStream(file_path, taskState.project)
            excelInput.stream.use { stream ->
                WorkbookFactory.create(stream).use { workbook ->
                    if (sheetIndex < 0 || sheetIndex >= workbook.numberOfSheets) {
                        throw ToolExecutionException(
                            "sheet_index $sheetIndex out of bounds. Workbook has ${workbook.numberOfSheets} sheets."
                        )
                    }

                    val sheet = workbook.getSheetAt(sheetIndex)
                    val sheetData = extractSheet(sheet, workbook)
                    val data = ExcelReadToolOutput(
                        filePath = excelInput.localPath ?: file_path,
                        fileName = resolvedFileName,
                        sheetIndex = sheetIndex,
                        sheetName = sheet.sheetName,
                        rows = sheetData.rows,
                    )
                    val resultForAssistant = renderResultForAssistant(data)
                    val toolCallResult = ToolCallResult(
                        type = "result",
                        data = data,
                        resultForAssistant = resultForAssistant
                    )
                    executeCache[file_path] = toolCallResult
                    return toolCallResult
                }
            }
        } catch (e: Exception) {
            throw ToolExecutionException("Failed to read Excel file: ${e.message}", e)
        }
    }

    private fun resolveFileName(explicitName: String?, filePath: String, project: Project): String? {
        if (!explicitName.isNullOrBlank()) {
            return explicitName
        }
        return if (isRemotePath(filePath)) {
            var sanitized = filePath.substringAfterLast('/').substringBefore('?').substringBefore('#')
            sanitized = "remote://$sanitized"
            sanitized.ifBlank { null }
        } else {
            runCatching { File(normalizeFilePath(filePath, project)).name }.getOrNull()
        }
    }

    private fun isRemotePath(path: String): Boolean {
        return path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)
    }

    /**
     * 打开 Excel 输入流
     */
    private fun openExcelInputStream(filePath: String, project: Project): ExcelInput {
        return if (isRemotePath(filePath)) {
            val url = URL(filePath)
            val connection = url.openConnection()
            if (connection is HttpURLConnection) {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
            }else{
                throw ToolExecutionException("unsupported connection type for $filePath")
            }
            ExcelInput(BufferedInputStream(connection.getInputStream()), null)
        } else {
            val resolvedPath = normalizeFilePath(filePath, project)
            val file = File(resolvedPath)
            if (!file.exists() || !file.isFile) {
                throw ToolExecutionException("Excel file not found: $resolvedPath")
            }
            ExcelInput(BufferedInputStream(file.inputStream()), resolvedPath)
        }
    }

    private data class SheetExtractionResult(
        val rows: List<ExcelRow>,
    )

    private fun extractSheet(sheet: Sheet, workbook: Workbook): SheetExtractionResult {
        val formatter = DataFormatter()
        val evaluator = workbook.creationHelper.createFormulaEvaluator()

        val rawRows = mutableListOf<Pair<Int, List<String>>>()

        val rowIterator = sheet.rowIterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            //过滤掉隐藏及筛选过滤的行!!!
            if (row.zeroHeight) {
                continue
            }
            val lastCellIndex = max(row.lastCellNum.toInt(), 0)
            if (lastCellIndex == 0) {
                rawRows.add(row.rowNum to emptyList())
                continue
            }

            val values = MutableList(lastCellIndex) { cellIndex ->
                val cell = row.getCell(cellIndex)
                if (cell != null) {
                    formatter.formatCellValue(cell, evaluator)
                } else {
                    ""
                }
            }
            rawRows.add(row.rowNum to values)
        }

        if (rawRows.isEmpty()) {
            return SheetExtractionResult(
                rows = emptyList(),
            )
        }
        val results = rawRows.map { (rowNumber, values) ->
            ExcelRow(rowNumber = rowNumber + 1, values = values)
        }
        return SheetExtractionResult(
            rows = results,
        )
    }

    private data class ExcelInput(
        val stream: InputStream,
        val localPath: String?
    )
}

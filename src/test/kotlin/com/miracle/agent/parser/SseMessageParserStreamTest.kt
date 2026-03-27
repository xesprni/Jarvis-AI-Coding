package com.miracle.agent.parser

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SseMessageParserStreamTest {

    /**
     * Simulates streaming by breaking input into random chunks
     */
    private fun simulateStreaming(
        parser: SseMessageParser,
        input: String,
        minChunkSize: Int = 1,
        maxChunkSize: Int = 10,
        seed: Long? = null
    ): List<Segment> {
        val random = seed?.let { Random(it) } ?: Random
        val allSegments = mutableListOf<Segment>()
        var position = 0

        while (position < input.length) {
            val chunkSize = random.nextInt(minChunkSize, maxChunkSize + 1)
                .coerceAtMost(input.length - position)
            val chunk = input.substring(position, position + chunkSize)

            val segments = parser.parse(chunk)
            allSegments.addAll(segments)

            position += chunkSize

            // Simulate network delay
            Thread.sleep(random.nextLong(5, 20))
        }

        return allSegments
    }

    @Test
    fun shouldHandleStreamedCodeBlock() {
        val parser = SseMessageParser()
        val input = """
            Here is some code:
            ```kotlin:MyFile.kt
            fun main() {
                println("Hello, World!")
            }
            ```
            Done!
        """.trimIndent()

        val segments = simulateStreaming(parser, input, seed = 42)

        val segmentTypes = segments.map { it::class.simpleName }
        assertTrue(segmentTypes.containsAll(listOf("TextSegment", "CodeHeader", "Code", "CodeEnd")))
        val codeSegments = segments.filterIsInstance<Code>()
        assertTrue(codeSegments.isNotEmpty())
        assertTrue(codeSegments.last().code.contains("println(\"Hello, World!\")"))
    }

    @Test
    fun shouldHandleStreamedSearchReplace() {
        val parser = SseMessageParser()
        val input = """
            ```kotlin:MyFile.kt
            <<<<<<< SEARCH
            fun oldFunction() {
                return "old"
            }
            =======
            fun newFunction() {
                return "new"
            }
            >>>>>>> REPLACE
            ```
        """.trimIndent()

        val segments = simulateStreaming(parser, input, minChunkSize = 3, maxChunkSize = 15, seed = 123)

        val searchReplaceSegments = segments.filterIsInstance<SearchReplace>()
        assertEquals(searchReplaceSegments.size, 1)
        val sr = searchReplaceSegments[0]
        assertTrue(sr.search.contains("oldFunction"))
        assertTrue(sr.replace.contains("newFunction"))
        assertEquals(sr.codeLanguage, "kotlin")
        assertEquals(sr.codeFilePath,"MyFile.kt")
    }

    @Test
    fun shouldHandleSearchReplaceWithInvalidEnding() {
        val parser = SseMessageParser()
        val input = """
            Here's some text.
            
            ```kotlin:MyFile.kt
            <<<<<<< SEARCH
            fun oldFunction() {
                return "old"
            }
            =======
            fun newFunction() {
                return "new"
            }
            ```
            
            Here's some other text.
        """.trimIndent()

        val segments = simulateStreaming(parser, input, minChunkSize = 3, maxChunkSize = 15, seed = 123)

        val searchReplaceSegments = segments.filterIsInstance<SearchReplace>()
        assertTrue(searchReplaceSegments.isEmpty())
        val codeEndSegments = segments.filterIsInstance<CodeEnd>()
        assertEquals(codeEndSegments.size, 1)
        val textSegmentSegments = segments.filterIsInstance<TextSegment>()
        assertTrue(textSegmentSegments.any { it.text.contains("Here's some other text") })
        val replaceWaitingSegments = segments.filterIsInstance<ReplaceWaiting>()
        assertTrue(replaceWaitingSegments.size < 10)
    }

    @Test
    fun shouldHandleMultipleCodeBlocksStreamed() {
        val parser = SseMessageParser()
        val input = """
            First:
            ```java
            System.out.println("1");
            ```
            Second:
            ```python
            print("2")
            ```
            Third:
            ```javascript
            console.log("3");
            ```
        """.trimIndent()

        val segments = simulateStreaming(parser, input, minChunkSize = 5, maxChunkSize = 20, seed = 789)

        val codeHeaders = segments.filterIsInstance<CodeHeader>()
        assertEquals(codeHeaders.size, 3)
        assertTrue(codeHeaders.map { it.codeLanguage }.containsAll(listOf("java", "python", "javascript")))
        val codeSegments = segments.filterIsInstance<Code>()
        assertTrue(codeSegments.any { it.code.contains("System.out.println") })
        assertTrue(codeSegments.any { it.code.contains("print(\"2\")") })
        assertTrue(codeSegments.any { it.code.contains("console.log") })
    }

    @Test
    fun shouldHandleMixedContentStreamed() {
        val parser = SseMessageParser()
        val input = """
            Starting analysis...
            <think>
            Processing request...
            </think>
            Here's the code:
            ```kotlin:Solution.kt
            <<<<<<< SEARCH
            val old = 1
            =======
            val new = 2
            >>>>>>> REPLACE
            ```
            And a simple block:
            ```python
            print("done")
            ```
        """.trimIndent()

        val segments = simulateStreaming(parser, input, minChunkSize = 3, maxChunkSize = 12, seed = 999)

        val segmentTypeSet = segments.map { it::class.simpleName }.toSet()
        assertTrue(segmentTypeSet.containsAll(listOf(
            "TextSegment", "CodeHeader", "SearchWaiting",
            "ReplaceWaiting", "SearchReplace", "Code", "CodeEnd"
        )))
    }

    @Test
    fun shouldHandleRandomChunkingConsistently() {
        val input = """
            ```kotlin:Test.kt
            class Test {
                fun method() {
                    println("Hello")
                }
            }
            ```
        """.trimIndent()

        repeat(5) { iteration ->
            val parser = SseMessageParser()
            val segments = simulateStreaming(parser, input, seed = iteration.toLong())

            val codeSegments = segments.filterIsInstance<Code>()
            assertTrue(codeSegments.isNotEmpty())
            val finalCode = codeSegments.last().code
            assertTrue(finalCode.contains("class Test"))
            assertTrue(finalCode.contains("println(\"Hello\")"))
        }
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.typedb.driver.tool.docs.examples

import picocli.CommandLine
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.system.exitProcess


fun main(args: Array<String>): Unit = exitProcess(CommandLine(TestExamplesParser()).execute(*args))

@CommandLine.Command(name = "TestExamplesParser", mixinStandardHelpOptions = true)
class TestExamplesParser : Callable<Unit> {
    @Parameters(paramLabel = "<input>", description = ["Input test file"])
    private lateinit var inputTestFileName: String

    @CommandLine.Option(names = ["--output", "-o"], required = true)
    private lateinit var outputFileName: String

    /**
     * --remove-lines=string: remove lines where this string is encountered
     */
    @CommandLine.Option(names = ["--remove-lines", "-rl"], required = false)
    private lateinit var removedLines: HashSet<String>

    /**
     * --change-words=string=string: change encountered words to the specified word
     */
    @CommandLine.Option(names = ["--change-words", "-cw"], required = false)
    private lateinit var changedWords: HashMap<String, String>

    companion object {
        const val INDENT_CHAR = ' '
        const val TAB_CHAR = '\t'
    }

    override fun call() {
        val outputFile = System.getenv("BUILD_WORKSPACE_DIRECTORY")?.let { Paths.get(it).resolve(outputFileName) }
            ?: Paths.get(outputFileName)

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile)
        }

        val inputFile = File(inputTestFileName)
        val outputLines = mutableListOf<String>()
        var insideExampleBlock = false
        var currentIndentation = Int.MAX_VALUE
        var blockNumber = 0

        inputFile.forEachLine { line ->
            when {
                line.contains("EXAMPLE START MARKER") -> {
                    insideExampleBlock = true
                    blockNumber++
                    currentIndentation = Int.MAX_VALUE
                }

                line.contains("EXAMPLE END MARKER") -> {
                    insideExampleBlock = false
                }

                insideExampleBlock -> {
                    if (lineAllowed(line)) {
                        val processedLine = applyWordChanges(line)
                        if (currentIndentation == Int.MAX_VALUE && processedLine.isNotBlank()) {
                            currentIndentation = detectIndentation(processedLine)
                        }
                        outputLines.add(adjustIndentation(processedLine, currentIndentation, blockNumber))
                    } else {
                        println("Removing line: $line")
                    }
                }
            }
        }

        Files.write(outputFile, outputLines)
    }

    private fun lineAllowed(line: String): Boolean {
        return removedLines.none { line.lowercase().contains(it.lowercase()) }
    }

    private fun applyWordChanges(line: String): String {
        var modifiedLine = line
        for ((wordToChange, replacementWord) in changedWords) {
            modifiedLine = modifiedLine.replace(wordToChange, replacementWord)
        }

        if (modifiedLine != line) {
            println("Changed line: $line")
            println("     To line: $modifiedLine")
        }

        return modifiedLine
    }

    private fun detectIndentation(line: String): Int {
        val indentation = line.takeWhile { it == INDENT_CHAR || it == TAB_CHAR }
        if (indentation.contains(TAB_CHAR)) {
            throw IllegalArgumentException("Tab character found in indentation. Use spaces instead and try again:\n$line")
        }
        return indentation.length
    }

    private fun adjustIndentation(line: String, indentation: Int, blockNumber: Int): String {
        return if (line.isNotBlank() && indentation != Int.MAX_VALUE) {
            if (line.take(indentation).any { it != INDENT_CHAR }) {
                throw IllegalArgumentException("Indentation is not consistent in the block number $blockNumber. Expected to remove $indentation space characters from line:\n$line")
            }
            line.drop(indentation)
        } else {
            line
        }
    }
}

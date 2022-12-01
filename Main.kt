package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.experimental.xor

val messageEndIndicator = byteArrayOf(0, 0, 3)

enum class SupportedTask(val cmd: String) {
    SHOW("show"),
    HIDE("hide"),
    EXIT("exit")
}

fun saveUpdatedImage(outputFileName: String, bufferedImage: BufferedImage) {
    ImageIO.write(bufferedImage, "png", File(outputFileName))
    println("Message saved in $outputFileName image.")
}

fun getFinalMessageByteArrayToHide(pwd: String, message: String): ByteArray {
    val encryptedPwd = pwd.encodeToByteArray()
    val intermediateEncodedMessage = message.encodeToByteArray()
    val isPwdShort = encryptedPwd.size < intermediateEncodedMessage.size
    val minArraySize = minOf(encryptedPwd.size, intermediateEncodedMessage.size)
    for (i in 0 until minArraySize) {
        intermediateEncodedMessage[i] = intermediateEncodedMessage[i] xor encryptedPwd[i]
    }
    if (isPwdShort) {
        for (i in encryptedPwd.size until intermediateEncodedMessage.size) {
            intermediateEncodedMessage[i] = intermediateEncodedMessage[i] xor encryptedPwd[i % encryptedPwd.size]
        }
    }
    return intermediateEncodedMessage
}

fun processMessageHiding(inputFileName: String, outputFileName: String, pwd: String, message: String) = try {
    val encodedMessage = getFinalMessageByteArrayToHide(pwd, message) + messageEndIndicator
    val imageMinSize = encodedMessage.size * 8
    val bufferedImage = ImageIO.read(File(inputFileName))

    if (with(bufferedImage) { width * height } < imageMinSize) {
        println("The input image is not large enough to hold this message.")
    } else {
        val messageBitRepresentation = encodedMessage.joinToString(separator = "") {
            it.toString(2).padStart(8, '0')
        }

        var bitIndexToHide = 0

        main@ for (y in 0 until bufferedImage.height) {
            for (x in 0 until bufferedImage.width) {
                val targetPixelColor = Color(bufferedImage.getRGB(x, y))
                val targetPixelNewColor =
                    Color(
                        targetPixelColor.red,
                        targetPixelColor.green,
                        (targetPixelColor.blue and 1.inv()) or messageBitRepresentation[bitIndexToHide].digitToInt()
                    )
                bufferedImage.setRGB(x, y, targetPixelNewColor.rgb)
                bitIndexToHide++
                if (bitIndexToHide >= messageBitRepresentation.length) {
                    break@main
                }
            }
        }
        saveUpdatedImage(outputFileName, bufferedImage)
    }
} catch (ex: IOException) {
    println("Can't read input file!")
}

fun handleHideTask() {
    println("Input image file:")
    val inputImageFileName = readln()
    println("Output image file:")
    val outputImageFileName = readln()
    println("Message to hide:")
    val messageToHide = readln()
    println("Password:")
    val pwd = readln()
    processMessageHiding(inputImageFileName, outputImageFileName, pwd, messageToHide)
}

fun getFinalMessageByteArrayToShow(pwd: String, message: ByteArray): ByteArray {
    val encryptedPwd = pwd.encodeToByteArray()
    val isPwdShort = encryptedPwd.size < message.size
    val minArraySize = minOf(encryptedPwd.size, message.size)
    for (i in 0 until minArraySize) {
        message[i] = message[i] xor encryptedPwd[i]
    }
    if (isPwdShort) {
        for (i in encryptedPwd.size until message.size) {
            message[i] = message[i] xor encryptedPwd[i % encryptedPwd.size]
        }
    }
    return message
}

fun showMessageFromImage(inputFileName: String, pwd: String) = try {
    val bufferedImage = ImageIO.read(File(inputFileName))
    val messageEndIndicatorInBit = messageEndIndicator.joinToString(separator = "") {
        it.toString(2).padStart(8, '0')
    }
    var hiddenMessageInBit = ""
    main@ for (y in 0 until bufferedImage.height) {
        for (x in 0 until bufferedImage.width) {
            val targetPixelColor = Color(bufferedImage.getRGB(x, y))
            hiddenMessageInBit += targetPixelColor.blue.toString(2).last()
            if (hiddenMessageInBit.length % 8 == 0 && hiddenMessageInBit.takeLast(8 * 3) == messageEndIndicatorInBit) {
                break@main
            }
        }
    }
    hiddenMessageInBit = hiddenMessageInBit.substringBefore(messageEndIndicatorInBit)

    val messageBytes = ByteArray(hiddenMessageInBit.length / 8)

    for (i in hiddenMessageInBit.indices step 8) {
        messageBytes[i / 8] = hiddenMessageInBit.substring(i, i + 8).toByte(2)
    }

    println("Message:")
    println(getFinalMessageByteArrayToShow(pwd, messageBytes).toString(Charsets.UTF_8))

} catch (ex: IOException) {
    println("Can't read input file!")
}

fun handleShowTask() {
    println("Input image file:")
    val inputImageFileName = readln()
    println("Password:")
    val pwd = readln()
    showMessageFromImage(inputImageFileName, pwd)
}

fun handleUserInput(userInput: SupportedTask): SupportedTask {
    when (userInput) {
        SupportedTask.EXIT -> println("Bye!")
        SupportedTask.HIDE -> handleHideTask()
        SupportedTask.SHOW -> handleShowTask()
    }
    return userInput
}

fun userInputValidation(): String {
    lateinit var userInput: String
    do {
        println("Task (hide, show, exit):")
        userInput = readln()
    } while ((userInput !in enumValues<SupportedTask>().map(SupportedTask::cmd)).also {
            if (it) {
                println("Wrong task: $userInput")
            }
        })
    return userInput
}

fun askUser() {
    do {
        val userInputString = userInputValidation()
        val userInput = handleUserInput(enumValues<SupportedTask>().first { it.cmd == userInputString })
    } while (userInput != SupportedTask.EXIT)
}

fun main() {
    askUser()
}

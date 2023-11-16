package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

private var maxEnergyValue = 0.0

fun main(args: Array<String>) {
    if (args.contains("-in") && args.contains("-out") && args.contains("-width") && args.contains("-height")){
        val inputFilename = args[1]
        val outputFilename = args[3]
        val imageWidth = args[5].toInt()
        val imageHeight = args[7].toInt()

      /*  val normalImage = loadImage(inputFilename)
        val energyMatrix = calculatePixelEnergy(normalImage)

        val transposedMatrix = transposeEnergyMatrix(energyMatrix) // for horizontal seam

        val verticalSeam = findSeam(transposedMatrix)
        val seamImage = convertToSeamImage(normalImage, verticalSeam, true)
*/

        val normalImage = loadImage(inputFilename)
        val reducedImage = reduceImage(normalImage, imageWidth, imageHeight)

        saveImage(reducedImage, outputFilename)
    }
}

fun drawPicture(height: Int, width: Int): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    val graphics = image.createGraphics()

    graphics.color = Color.RED
    graphics.drawLine(0,0,width - 1,height - 1)
    graphics.drawLine(width - 1,0,0,height - 1)
    return image
}

fun saveImage(image: BufferedImage, filename: String) {
    val imageFile = File(filename)
    ImageIO.write(image, "png", imageFile)
}

fun loadImage(filename: String): BufferedImage {
    val inputFile = File(filename)

    return ImageIO.read(inputFile)
}

fun convertToSeamImage(image: BufferedImage, seamList: List<Int>, isHorizontal:Boolean = false): BufferedImage{
    for (index in seamList.indices){
        val colorNew = Color(255, 0, 0)
        if (isHorizontal){
            image.setRGB(index, seamList[index], colorNew.rgb)
        } else{
            image.setRGB(seamList[index], index, colorNew.rgb)
        }
    }

    return image
}

fun convertToNegativeImage(image: BufferedImage): BufferedImage{
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val color = Color(image.getRGB(x, y))
            val colorNew = Color(255 - color.red, 255 - color.green, 255 - color.blue)
            image.setRGB(x, y, colorNew.rgb)
        }
    }
    return image
}

fun convertToEnergyImage(image: BufferedImage, energyMatrix: Array<Array<Double>> ): BufferedImage{
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val energy = energyMatrix[y][x]
            val intensity = (255.0 * energy / maxEnergyValue).toInt()
            val colorNew = Color(intensity, intensity, intensity)
            image.setRGB(x, y, colorNew.rgb)
        }
    }
    return image
}

fun calculatePixelEnergy(image: BufferedImage): Array<Array<Double>>{
    val matrix: Array<Array<Double>> = Array(image.height) { Array(image.width) { 0.0 } }

    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            var xGradient = 0.0
            var yGradient = 0.0

            if ((x > 0 && x < image.width - 1) && (y > 0 && y < image.height - 1)){
                 xGradient = xGradient(image, x, y)
                 yGradient = yGradient(image, x, y)
            }
            ////////////////////////////////////////////////////////////////////////////
            else if (x == 0 && y == 0){
                xGradient = xGradient(image, 1, 0)
                yGradient = yGradient(image, 0, 1)
            }
            else if (x == image.width - 1 && y == 0){
                xGradient = xGradient(image, image.width - 2, 0)
                yGradient = yGradient(image, image.width - 1, 1)
            }
            else if (x == 0 && y == image.height - 1){
                xGradient = xGradient(image, 1, image.height - 1)
                yGradient = yGradient(image, 0, image.height - 2)
            }
            else if (x == image.width - 1 && y == image.height - 1){
                xGradient = xGradient(image, image.width - 2, image.height - 1)
                yGradient = yGradient(image, image.width - 1, image.height - 2)
            }
            //////////////////////////////////////////////////////////////////////////
            else if (x == 0 && (y > 0 && y < image.height - 1)){
                xGradient = xGradient(image, 1, y)
                yGradient = yGradient(image, x, y)
            }
            else if (x == image.width - 1 && (y > 0 && y < image.height - 1)){
                xGradient = xGradient(image, image.width - 2, y)
                yGradient = yGradient(image, x, y)
            }
            else if ((x > 0 && x < image.width - 1) && y == 0){
                xGradient = xGradient(image, x, y)
                yGradient = yGradient(image, x, 1)
            }
            else if ((x > 0 && x < image.width - 1) && y == image.height - 1){
                xGradient = xGradient(image, x, y)
                yGradient = yGradient(image, x, image.height - 2)
            }

            val energy = sqrt(xGradient + yGradient)
            if (energy > maxEnergyValue)
                maxEnergyValue = energy
            matrix[y][x] = energy
        }
    }
    return matrix
}

fun pow(i: Int): Double{
    return i.toDouble().pow(2)
}

fun xGradient(image: BufferedImage, x: Int, y: Int): Double{
    val xMinusColor = Color(image.getRGB(x - 1, y))
    val xPlusColor = Color(image.getRGB(x + 1, y))

    return (pow(xMinusColor.red - xPlusColor.red)) +
            (pow(xMinusColor.green - xPlusColor.green)) +
            (pow(xMinusColor.blue - xPlusColor.blue))
}

fun yGradient(image: BufferedImage, x: Int, y: Int): Double{
    val yMinusColor = Color(image.getRGB(x, y - 1))
    val yPlusColor = Color(image.getRGB(x, y + 1))

    return (pow(yMinusColor.red - yPlusColor.red)) +
            (pow(yMinusColor.green - yPlusColor.green)) +
            (pow(yMinusColor.blue - yPlusColor.blue))
}

fun findSeam(energyMatrix: Array<Array<Double>>): List<Int> {
    val height = energyMatrix.size
    val width = energyMatrix[0].size

    // Initialize a 2D array to store the cumulative minimum energy
    val cumulativeEnergy = Array(height) { Array(width) { 0.0 } }

    // Copy the first row of energyMatrix to cumulativeEnergy
    for (i in 0 until width) {
        cumulativeEnergy[0][i] = energyMatrix[0][i]
    }

    // Compute cumulative minimum energy for each pixel
    for (i in 1 until height) {
        for (j in 0 until width) {
            val left = if (j > 0) cumulativeEnergy[i - 1][j - 1] else Double.POSITIVE_INFINITY
            val middle = cumulativeEnergy[i - 1][j]
            val right = if (j < width - 1) cumulativeEnergy[i - 1][j + 1] else Double.POSITIVE_INFINITY

            cumulativeEnergy[i][j] = energyMatrix[i][j] + minOf(left, middle, right)
        }
    }

    // Find the pixel with the minimum cumulative energy in the last row
    var minEnergyIndex = 0
    for (i in 1 until width) {
        if (cumulativeEnergy[height - 1][i] < cumulativeEnergy[height - 1][minEnergyIndex]) {
            minEnergyIndex = i
        }
    }

    // Backtrack to find the seam
    val seam = mutableListOf(minEnergyIndex)
    var currentCol = minEnergyIndex
    for (i in height - 2 downTo 0) {
        val left = if (currentCol > 0) cumulativeEnergy[i][currentCol - 1] else Double.POSITIVE_INFINITY
        val middle = cumulativeEnergy[i][currentCol]
        val right = if (currentCol < width - 1) cumulativeEnergy[i][currentCol + 1] else Double.POSITIVE_INFINITY

        val minEnergy = minOf(left, middle, right)
        currentCol += when (minEnergy) {
            left -> -1
            right -> 1
            else -> 0
        }

        seam.add(0, currentCol)
    }

    return seam
}

fun transposeEnergyMatrix(matrix: Array<Array<Double>>): Array<Array<Double>> {
    val numRows = matrix.size
    val numCols = matrix[0].size

    // Create a new matrix with swapped rows and columns
    val transposedMatrix = Array(numCols) { Array(numRows) { 0.0 } }

    for (i in 0 until numRows) {
        for (j in 0 until numCols) {
            transposedMatrix[j][i] = matrix[i][j]
        }
    }

    return transposedMatrix
}

fun reduceImage(normalImage: BufferedImage, width: Int, height: Int): BufferedImage{
    var currentImage = normalImage

    for (i in 1..width){
        val energyMatrix = calculatePixelEnergy(currentImage)
        val verticalSeam = findSeam(energyMatrix)
        currentImage = removeVerticalSeam(currentImage, verticalSeam)
    }

    for (i in 1..height){
        val energyMatrix = calculatePixelEnergy(currentImage)
        val transposedMatrix = transposeEnergyMatrix(energyMatrix) // for horizontal seam

        val horizontalSeam = findSeam(transposedMatrix)
        currentImage = removeHorizontalSeam(currentImage, horizontalSeam)
    }

    return currentImage
}

fun removeVerticalSeam(image: BufferedImage, seam: List<Int>): BufferedImage {
    val newImage = BufferedImage(image.width - 1, image.height, BufferedImage.TYPE_INT_RGB)

    for (y in 0 until image.height) {
        for (x in 0 until seam[y]) {
            newImage.setRGB(x, y, image.getRGB(x, y))
        }

        for (x in seam[y] until newImage.width) {
            newImage.setRGB(x, y, image.getRGB(x + 1, y))
        }
    }

    return newImage
}

fun removeHorizontalSeam(image: BufferedImage, seam: List<Int>): BufferedImage {
    val newImage = BufferedImage(image.width, image.height - 1, BufferedImage.TYPE_INT_RGB)

    for (x in 0 until image.width) {
        for (y in 0 until seam[x]) {
            newImage.setRGB(x, y, image.getRGB(x, y))
        }

        for (y in seam[x] until newImage.height) {
            newImage.setRGB(x, y, image.getRGB(x, y + 1))
        }
    }

    return newImage
}
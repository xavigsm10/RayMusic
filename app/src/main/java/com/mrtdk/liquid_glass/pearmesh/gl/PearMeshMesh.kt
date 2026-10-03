package com.mrtdk.liquid_glass.pearmesh.gl

internal data class MeshData(
    val vertices: FloatArray,
    val indices: ShortArray,
)

/** Catmull-Clark subdivision of PearMesh's compact and wide control maps. */
internal object PearMeshMesh {
    const val PortraitPresetCount = 4
    const val LandscapePresetCount = 5

    fun create(isPortrait: Boolean, presetIndex: Int): MeshData {
        val controlPointCount = if (isPortrait) 6 else 9
        val presets = if (isPortrait) PearMeshPresets.portrait else PearMeshPresets.landscape
        val preset = presets[presetIndex.coerceIn(0, presets.lastIndex)]
        val expectedPointCount = controlPointCount * controlPointCount
        require(preset.from.size == expectedPointCount) {
            "Expected $expectedPointCount source control points, got ${preset.from.size}"
        }
        require(preset.to.size == expectedPointCount) {
            "Expected $expectedPointCount target control points, got ${preset.to.size}"
        }
        var from = toGrid(preset.from, controlPointCount)
        var to = toGrid(preset.to, controlPointCount)
        repeat(3) {
            from = subdivide(from)
            to = subdivide(to)
        }

        val rows = from.size
        val columns = from[0].size
        val vertices = FloatArray(rows * columns * 6)
        var vertexOffset = 0
        for (row in 0 until rows) {
            val v = row.toFloat() / (rows - 1f)
            for (column in 0 until columns) {
                val u = column.toFloat() / (columns - 1f)
                val fromPoint = from[row][column]
                val toPoint = to[row][column]
                vertices[vertexOffset++] = fromPoint.x * 2f - 1f
                vertices[vertexOffset++] = fromPoint.y * 2f - 1f
                vertices[vertexOffset++] = toPoint.x * 2f - 1f
                vertices[vertexOffset++] = toPoint.y * 2f - 1f
                vertices[vertexOffset++] = u
                vertices[vertexOffset++] = v
            }
        }

        val indices = ShortArray((rows - 1) * (columns - 1) * 6)
        var indexOffset = 0
        for (row in 0 until rows - 1) {
            for (column in 0 until columns - 1) {
                val bottomLeft = row * columns + column
                val bottomRight = bottomLeft + 1
                val topLeft = bottomLeft + columns
                val topRight = topLeft + 1
                indices[indexOffset++] = bottomLeft.toShort()
                indices[indexOffset++] = topLeft.toShort()
                indices[indexOffset++] = topRight.toShort()
                indices[indexOffset++] = topRight.toShort()
                indices[indexOffset++] = bottomRight.toShort()
                indices[indexOffset++] = bottomLeft.toShort()
            }
        }
        return MeshData(vertices, indices)
    }

    private fun toGrid(points: Array<Vec2>, size: Int): Array<Array<Vec2>> =
        Array(size) { row -> Array(size) { column -> points[row * size + column] } }

    private fun subdivide(source: Array<Array<Vec2>>): Array<Array<Vec2>> {
        val rows = source.size
        val columns = source[0].size
        val facePoints = Array(rows - 1) { row ->
            Array(columns - 1) { column ->
                (source[row][column] + source[row][column + 1] +
                        source[row + 1][column] + source[row + 1][column + 1]) / 4f
            }
        }
        val result = Array(rows * 2 - 1) { Array(columns * 2 - 1) { Vec2.Zero } }

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val boundaryRow = row == 0 || row == rows - 1
                val boundaryColumn = column == 0 || column == columns - 1
                val point = source[row][column]
                result[row * 2][column * 2] = when {
                    boundaryRow && boundaryColumn -> point
                    boundaryRow ->
                        (source[row][column - 1] + point * 6f + source[row][column + 1]) / 8f

                    boundaryColumn ->
                        (source[row - 1][column] + point * 6f + source[row + 1][column]) / 8f

                    else -> {
                        val faceAverage = (
                                facePoints[row - 1][column - 1] + facePoints[row - 1][column] +
                                        facePoints[row][column - 1] + facePoints[row][column]
                                ) / 4f
                        val edgeAverage = (
                                (point + source[row - 1][column]) / 2f +
                                        (point + source[row + 1][column]) / 2f +
                                        (point + source[row][column - 1]) / 2f +
                                        (point + source[row][column + 1]) / 2f
                                ) / 4f
                        (faceAverage + edgeAverage * 2f + point) / 4f
                    }
                }
            }
        }

        for (row in 0 until rows) {
            for (column in 0 until columns - 1) {
                val first = source[row][column]
                val second = source[row][column + 1]
                result[row * 2][column * 2 + 1] =
                    if (row == 0 || row == rows - 1) {
                        (first + second) / 2f
                    } else {
                        (first + second + facePoints[row - 1][column] + facePoints[row][column]) / 4f
                    }
            }
        }

        for (row in 0 until rows - 1) {
            for (column in 0 until columns) {
                val first = source[row][column]
                val second = source[row + 1][column]
                result[row * 2 + 1][column * 2] =
                    if (column == 0 || column == columns - 1) {
                        (first + second) / 2f
                    } else {
                        (first + second + facePoints[row][column - 1] + facePoints[row][column]) / 4f
                    }
            }
        }

        for (row in 0 until rows - 1) {
            for (column in 0 until columns - 1) {
                result[row * 2 + 1][column * 2 + 1] = facePoints[row][column]
            }
        }
        return result
    }
}

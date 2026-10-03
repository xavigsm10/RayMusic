package com.mrtdk.liquid_glass.pearmesh.gl

import android.content.Context
import android.opengl.GLES30
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class GlProgram(
    context: Context,
    vertexAsset: String,
    fragmentAsset: String,
) : Closeable {
    val id: Int
    private val uniforms = HashMap<String, Int>()

    init {
        val vertexShader = compileShader(
            GLES30.GL_VERTEX_SHADER,
            context.assets.open(vertexAsset).bufferedReader().use { it.readText() },
            vertexAsset,
        )
        val fragmentShader = compileShader(
            GLES30.GL_FRAGMENT_SHADER,
            context.assets.open(fragmentAsset).bufferedReader().use { it.readText() },
            fragmentAsset,
        )
        id = GLES30.glCreateProgram()
        GLES30.glAttachShader(id, vertexShader)
        GLES30.glAttachShader(id, fragmentShader)
        GLES30.glLinkProgram(id)
        val status = IntArray(1)
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, status, 0)
        val log = GLES30.glGetProgramInfoLog(id)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        check(status[0] == GLES30.GL_TRUE) {
            "Could not link $vertexAsset + $fragmentAsset:\n$log"
        }
    }

    fun use() = GLES30.glUseProgram(id)

    fun int(name: String, value: Int) = GLES30.glUniform1i(location(name), value)
    fun float(name: String, value: Float) = GLES30.glUniform1f(location(name), value)
    fun vec2(name: String, x: Float, y: Float) = GLES30.glUniform2f(location(name), x, y)
    fun vec3(name: String, x: Float, y: Float, z: Float) =
        GLES30.glUniform3f(location(name), x, y, z)

    fun vec4(name: String, x: Float, y: Float, z: Float, w: Float) =
        GLES30.glUniform4f(location(name), x, y, z, w)

    private fun location(name: String): Int = uniforms.getOrPut(name) {
        GLES30.glGetUniformLocation(id, name).also {
            check(it >= 0) { "Uniform $name is not active in program $id" }
        }
    }

    override fun close() {
        GLES30.glDeleteProgram(id)
    }

    private companion object {
        fun compileShader(type: Int, source: String, label: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] != GLES30.GL_TRUE) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                error("Could not compile $label:\n$log")
            }
            return shader
        }
    }
}

internal class GlGeometry private constructor(
    vertices: FloatArray,
    indices: ShortArray,
    attributes: List<VertexAttribute>,
    private val indexCount: Int,
) : Closeable {
    private val vao: Int
    private val vertexBuffer: Int
    private val indexBuffer: Int

    init {
        val handles = IntArray(1)
        GLES30.glGenVertexArrays(1, handles, 0)
        vao = handles[0]
        GLES30.glGenBuffers(1, handles, 0)
        vertexBuffer = handles[0]
        GLES30.glGenBuffers(1, handles, 0)
        indexBuffer = handles[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer)
        val vertexBytes = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply { position(0) }
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            vertexBytes,
            GLES30.GL_STATIC_DRAW,
        )

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
        val indexBytes = ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
            .apply { position(0) }
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * Short.SIZE_BYTES,
            indexBytes,
            GLES30.GL_STATIC_DRAW,
        )

        attributes.forEach { attribute ->
            GLES30.glEnableVertexAttribArray(attribute.location)
            GLES30.glVertexAttribPointer(
                attribute.location,
                attribute.components,
                GLES30.GL_FLOAT,
                false,
                attribute.strideFloats * Float.SIZE_BYTES,
                attribute.offsetFloats * Float.SIZE_BYTES,
            )
        }
        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    }

    override fun close() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vertexBuffer), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(indexBuffer), 0)
    }

    companion object {
        fun quad(): GlGeometry = GlGeometry(
            vertices = floatArrayOf(
                -1f, -1f, 0f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
                1f, -1f, 1f, 0f,
            ),
            indices = shortArrayOf(0, 1, 2, 2, 3, 0),
            attributes = listOf(
                VertexAttribute(0, 2, 4, 0),
                VertexAttribute(1, 2, 4, 2),
            ),
            indexCount = 6,
        )

        fun mesh(data: MeshData): GlGeometry = GlGeometry(
            vertices = data.vertices,
            indices = data.indices,
            attributes = listOf(
                VertexAttribute(0, 2, 6, 0),
                VertexAttribute(1, 2, 6, 2),
                VertexAttribute(2, 2, 6, 4),
            ),
            indexCount = data.indices.size,
        )
    }
}

private data class VertexAttribute(
    val location: Int,
    val components: Int,
    val strideFloats: Int,
    val offsetFloats: Int,
)

internal class RenderTarget private constructor(
    val width: Int,
    val height: Int,
    val texture: Int,
    val framebuffer: Int,
) : Closeable {
    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        GLES30.glViewport(0, 0, width, height)
    }

    override fun close() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    companion object {
        fun create(width: Int, height: Int): RenderTarget {
            val handle = IntArray(1)
            GLES30.glGenTextures(1, handle, 0)
            val texture = handle[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA8,
                width,
                height,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                null,
            )

            GLES30.glGenFramebuffers(1, handle, 0)
            val framebuffer = handle[0]
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                texture,
                0,
            )
            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer is incomplete: 0x${status.toString(16)}"
            }
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return RenderTarget(width, height, texture, framebuffer)
        }
    }
}

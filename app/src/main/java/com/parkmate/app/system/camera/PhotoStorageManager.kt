package com.parkmate.app.system.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AnnotationTool {
    ARROW,
    CIRCLE,
    TEXT
}

data class PhotoAnnotationItem(
    val id: String = UUID.randomUUID().toString(),
    val tool: AnnotationTool,
    val normalizedX: Float, // Center X for CIRCLE/TEXT, Start X for ARROW [0f..1f]
    val normalizedY: Float, // Center Y for CIRCLE/TEXT, Start Y for ARROW [0f..1f]
    val normalizedEndX: Float = (normalizedX + 0.18f).coerceAtMost(0.95f), // End X for ARROW
    val normalizedEndY: Float = (normalizedY - 0.16f).coerceAtLeast(0.05f), // End Y for ARROW
    val radiusNormalized: Float = 0.11f, // Radius for CIRCLE as fraction of min(width, height) [0.04f..0.42f]
    val scale: Float = 1.0f, // Scale multiplier for TEXT and stroke thickness [0.5f..2.6f]
    val label: String = "Pillar C4"
)

@Singleton
class PhotoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val photosDir: File
        get() = File(context.filesDir, "spot_photos").apply { mkdirs() }

    fun createNewPhotoFile(): File {
        return File(photosDir, "spot_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    }

    suspend fun copyGalleryUriToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val destFile = createNewPhotoFile()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
                normalizePhotoOrientation(destFile.absolutePath)
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Reads the EXIF orientation tag of [filePath], rotates/flips the pixel buffer so it is physically upright,
     * and overwrites the file with normal orientation so Coil and BitmapFactory always agree 100%.
     */
    suspend fun normalizePhotoOrientation(filePath: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) return@withContext filePath
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED
            ) {
                return@withContext filePath
            }
            val upright = decodeUprightBitmap(file.absolutePath) ?: return@withContext filePath
            FileOutputStream(file).use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            upright.recycle()
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
            file.absolutePath
        }.getOrDefault(filePath)
    }

    private fun decodeUprightBitmap(filePath: String): Bitmap? {
        val rawBitmap = BitmapFactory.decodeFile(filePath) ?: return null
        val exif = runCatching { ExifInterface(filePath) }.getOrNull() ?: return rawBitmap
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return rawBitmap
        }
        return runCatching {
            val rotated = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        }.getOrDefault(rawBitmap)
    }

    /**
     * Generates a clean local underground parking placeholder photo (used when emulator camera is unavailable).
     */
    suspend fun createSampleParkingPhoto(): String = withContext(Dispatchers.IO) {
        val destFile = createNewPhotoFile()
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Concrete garage gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#252B37"), Color.parseColor("#3E4656"), Color.parseColor("#2B303C")),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Garage pillar with blue stripe & "C4 / LEVEL -2" signage
        val pillarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8DCE3")
        }
        canvas.drawRoundRect(RectF(620f, 180f, 920f, 1180f), 18f, 18f, pillarPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B6CF6")
        }
        canvas.drawRect(620f, 420f, 920f, 580f, stripePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 78f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("C4", 715f, 525f, textPaint)

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1F26")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("LEVEL -2", 675f, 660f, subTextPaint)

        // Parking bay lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5C451")
            strokeWidth = 14f
        }
        canvas.drawLine(120f, 1320f, 440f, 980f, linePaint)
        canvas.drawLine(440f, 980f, 940f, 980f, linePaint)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        destFile.absolutePath
    }

    /**
     * Renders annotations (arrow, circle, text) with their exact custom positions and sizes into the JPEG file.
     */
    suspend fun burnAnnotationsIntoPhoto(
        sourcePath: String,
        annotations: List<PhotoAnnotationItem>
    ): String = withContext(Dispatchers.IO) {
        if (annotations.isEmpty()) return@withContext sourcePath
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return@withContext sourcePath

        val original = decodeUprightBitmap(sourcePath) ?: return@withContext sourcePath
        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        if (mutable != original) {
            original.recycle()
        }

        val canvas = Canvas(mutable)
        val w = mutable.width.toFloat()
        val h = mutable.height.toFloat()
        val minDim = min(w, h)
        val baseScale = minDim / 1000f

        annotations.forEach { item ->
            val itemScale = item.scale.coerceIn(0.5f, 2.8f)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3B6CF6")
                style = Paint.Style.STROKE
                strokeWidth = 12f * baseScale * itemScale
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 18f * baseScale * itemScale
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val startX = item.normalizedX * w
            val startY = item.normalizedY * h

            when (item.tool) {
                AnnotationTool.ARROW -> {
                    val endX = item.normalizedEndX * w
                    val endY = item.normalizedEndY * h
                    val length = hypot((endX - startX).toDouble(), (endY - startY).toDouble()).toFloat()
                    val headLen = (length * 0.28f).coerceIn(32f * baseScale, 72f * baseScale) * itemScale
                    drawArrowOnCanvas(canvas, startX, startY, endX, endY, headLen, outlinePaint)
                    drawArrowOnCanvas(canvas, startX, startY, endX, endY, headLen, strokePaint)
                }
                AnnotationTool.CIRCLE -> {
                    val radius = (item.radiusNormalized.coerceIn(0.03f, 0.48f) * minDim)
                    canvas.drawCircle(startX, startY, radius, outlinePaint)
                    canvas.drawCircle(startX, startY, radius, strokePaint)
                }
                AnnotationTool.TEXT -> {
                    val label = item.label.ifBlank { "Spot" }
                    val textSizePx = 42f * baseScale * itemScale
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = textSizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val textOutlineBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 4f * baseScale
                    }
                    val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#EB3B6CF6")
                        style = Paint.Style.FILL
                    }

                    val textWidth = textPaint.measureText(label)
                    val padH = 26f * baseScale * itemScale
                    val padV = 18f * baseScale * itemScale
                    val rect = RectF(
                        startX - textWidth / 2f - padH,
                        startY - textSizePx / 2f - padV,
                        startX + textWidth / 2f + padH,
                        startY + textSizePx / 2f + padV * 0.8f
                    )
                    val corner = 22f * baseScale * itemScale
                    canvas.drawRoundRect(rect, corner, corner, textBgPaint)
                    canvas.drawRoundRect(rect, corner, corner, textOutlineBoxPaint)
                    canvas.drawText(label, startX - textWidth / 2f, startY + textSizePx * 0.34f, textPaint)
                }
            }
        }

        val outFile = createNewPhotoFile()
        FileOutputStream(outFile).use { out ->
            mutable.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        mutable.recycle()
        outFile.absolutePath
    }

    private fun drawArrowOnCanvas(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        headLength: Float,
        paint: Paint
    ) {
        canvas.drawLine(startX, startY, endX, endY, paint)
        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val arrowAngle = Math.toRadians(28.0)
        val x1 = endX - (headLength * cos(angle - arrowAngle)).toFloat()
        val y1 = endY - (headLength * sin(angle - arrowAngle)).toFloat()
        val x2 = endX - (headLength * cos(angle + arrowAngle)).toFloat()
        val y2 = endY - (headLength * sin(angle + arrowAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(endX, endY)
            lineTo(x1, y1)
            moveTo(endX, endY)
            lineTo(x2, y2)
        }
        canvas.drawPath(headPath, paint)
    }
}

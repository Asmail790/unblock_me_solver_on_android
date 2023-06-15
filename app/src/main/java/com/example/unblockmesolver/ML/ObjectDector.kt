package com.example.unblockmesolver.ML

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.torchvision.TensorImageUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.streams.toList

class ObjectDector(
   val context:Context,
   classesFile: String = "classes.txt",
   detectorFile:String = "dector.torchscript",
   val imageSize:Int = 640,
   val objectScoreThreshold:Float = 0.7F,
   val IouThreshold: Float = 0.01F,
   val limit: Int = 30
):DefaultLifecycleObserver {
   companion object {
      const val  TAG ="OBJECT_DECTOR"
        val NO_MEAN_RGB = floatArrayOf(0.0F, 0.0F, 0.0F)
        val NO_STD_RGB = floatArrayOf(1F, 1F, 1F)
   }

    private val colorForPadding = "#727272"

   private val dector = LiteModuleLoader.loadModuleFromAsset(context.assets, detectorFile)
   private val classes = context.assets.open(classesFile).let {
      val f =  BufferedReader(InputStreamReader(it))
      val resf = f.lines().toList().map{ x -> x.strip() }
         f.close()
         resf
   }


    fun infer(screenshot: Bitmap):Pair<ArrayList<Result>, com.example.unblockmesolver.ML.Result> {
        val img = reScaleAndPadImage(screenshot)
        val  inputTensor = TensorImageUtils.bitmapToFloat32Tensor(img, NO_MEAN_RGB,NO_STD_RGB);
        val outputTuple = dector.forward(IValue.from(inputTensor)).toTuple();

        val outputTensor = outputTuple[0].toTensor();
        val outputs = outputTensor.dataAsFloatArray;
        val results = outputsToNMSPredictions(outputs, objectScoreThreshold,IouThreshold, limit)
        reScaleBoundingBoxes(arrayListOf(results.second),screenshot)
        reScaleBoundingBoxes(results.first,screenshot)
        return results
    }


    private fun reScaleBoundingBoxes(boxes: ArrayList<Result>, screenshot: Bitmap){
        val largestSideInfo =  arrayListOf(
            Pair("width",screenshot.width),
            Pair("height", screenshot.height)
        ).maxBy { it.second }

        val centerX =  screenshot.width/2F
        val centerY = screenshot.height/2F
        val matrix = Matrix().apply {
            postTranslate((screenshot.width - imageSize)/2F ,(screenshot.height - imageSize)/2F)
            postScale(
                largestSideInfo.second/imageSize.toFloat(),
                largestSideInfo.second/imageSize.toFloat(),
                centerX,
                centerY
            )

        }
        boxes.forEach { matrix.mapRect(it.rect) }

        val externalFilesDir = context.getExternalFilesDir(null)!!;

        externalFilesDir.absolutePath
        Log.d(TAG, externalFilesDir.absolutePath)

        val f = File(externalFilesDir.absolutePath , "shot.JPG")
        Log.d(TAG, f.exists().toString())
        f.createNewFile()
        val copy = screenshot.copy(screenshot.config,true)

        val canvas = Canvas(copy)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 5F
            style = Paint.Style.STROKE
        }
        boxes.forEach {
            canvas.drawRect(it.rect,paint)
        }

        copy.compress(Bitmap.CompressFormat.JPEG,100,f.outputStream())
        Log.d(TAG, f.exists().toString())


    }



    private fun outputsToNMSPredictions(outputs:FloatArray, objectScoreThreshold:Float,IouThreshold: Float, limit: Int):Pair<ArrayList<Result>,com.example.unblockmesolver.ML.Result> {
        // left, top, right, bottom, objectScore and class probabilities (7 of them)
        val cellSize = 5 + classes.size
        val cells = outputs.toList().chunked(cellSize)
            .map{
                val boundingBoxCenterX = it[0]
                val boundingBoxCenterY = it[1]
                val boundingBoxWidth = it[2]
                val boundingBoxHeight = it[3]
                val objectScore = it[4]
                val classIndex = it.slice(5 until it.size).withIndex().maxBy {x -> x.value }.index

                val left  =  boundingBoxCenterX-boundingBoxWidth/2F
                val top  = boundingBoxCenterY-boundingBoxHeight/2F
                val right =   boundingBoxCenterX+boundingBoxWidth/2F
                val bottom =   boundingBoxCenterY+boundingBoxHeight/2F
                val  boundingBox =  RectF(left,top,right,bottom)
                return@map Result(classIndex, objectScore,boundingBox)
            }
            .filter {
                it.score>=objectScoreThreshold
            }

        val gridClass = classes.indexOf("grid")
        val gridsPrediction = cells.groupBy { x -> x.classIndex == gridClass  }




        val blocks =  nonMaxSuppression(gridsPrediction[false]!!,limit,IouThreshold)
        val grid = gridsPrediction[true]!!.maxBy { it.score }
        return Pair(blocks,grid)
    }

    private fun IOU(a: RectF, b: RectF): Float {
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        if (areaA <= 0.0) return 0.0f
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        if (areaB <= 0.0) return 0.0f
        val intersectionMinX = max(a.left, b.left)
        val intersectionMinY = max(a.top, b.top)
        val intersectionMaxX = min(a.right, b.right)
        val intersectionMaxY = min(a.bottom, b.bottom)
        val intersectionArea =max(intersectionMaxY - intersectionMinY, 0f) *
                max(intersectionMaxX - intersectionMinX, 0f)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }


    fun nonMaxSuppression(
        boxes: List<Result>,
        limit: Int,
        IouThreshold: Float
    ): ArrayList<Result> {
        boxes.sortedWith(Comparator { o1, o2 -> o1.score.compareTo(o2.score) })
        val selected = ArrayList<Result>()
        val active = BooleanArray(boxes.size)
        Arrays.fill(active, true)
        var numActive = active.size

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        var done = false
        var i = 0
        while (i < boxes.size && !done) {
            if (active[i]) {
                val boxA = boxes[i]
                selected.add(boxA)
                if (selected.size >= limit) break
                for (j in i + 1 until boxes.size) {
                    if (active[j]) {
                        val (_, _, rect) = boxes[j]
                        if (IOU(
                                boxA.rect,
                                rect
                            ) > IouThreshold
                        ) {
                            active[j] = false
                            numActive -= 1
                            if (numActive <= 0) {
                                done = true
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

   private fun reScaleAndPadImage(screenshot:Bitmap):Bitmap {

       val largestSideInfo =  arrayListOf(
       Pair("width",screenshot.width),
       Pair("height", screenshot.height)
       ).maxBy { it.second }
       
       val squareBox = createBitmap(
           largestSideInfo.second,
           largestSideInfo.second,
           screenshot.config
       )
       
       val canvas = Canvas(squareBox)
       val centerAndRescaleMatrix = Matrix().apply {
          val dxToCenterScreenshot = squareBox.width.toFloat()/2F-screenshot.width/2F
          val dyToCenterScreenshot =  squareBox.height.toFloat()/2F-screenshot.height/2F

          if (largestSideInfo.first == "height") {
              postTranslate(
                  dxToCenterScreenshot,
                  0F,
              )
          } else if (largestSideInfo.first == "width") {
              postTranslate(
                  0F,
                  dyToCenterScreenshot,
              )
          }
           val rescaleToFitImageSize = 640F/largestSideInfo.second
           val centerPointX = squareBox.width/2F
           val centerPointY = squareBox.height/2F

         postScale(
             rescaleToFitImageSize,
             rescaleToFitImageSize,
             centerPointX,
             centerPointY
         )
      }
      canvas.drawColor(Color.parseColor(colorForPadding))
      canvas.drawBitmap(screenshot,centerAndRescaleMatrix, null)

       val paddedImageStartX = ((squareBox.width.toFloat()/2F)-(imageSize/2F)).roundToInt()
       val paddedImageStartY = ((squareBox.height.toFloat()/2F)-(imageSize/2F)).roundToInt()

       val resizedAndPaddedImage =  Bitmap.createBitmap(
           squareBox,
           paddedImageStartX,
           paddedImageStartY,
           imageSize,
           imageSize
       )

       val externalFilesDir = context.getExternalFilesDir(null)!!;

       externalFilesDir.absolutePath
       Log.d(TAG, externalFilesDir.absolutePath)

      val f = File(externalFilesDir.absolutePath , "shot.JPG")
      Log.d(TAG, f.exists().toString())
      f.createNewFile()

      resizedAndPaddedImage.compress(Bitmap.CompressFormat.JPEG,100,f.outputStream())
      Log.d(TAG, f.exists().toString())
       return resizedAndPaddedImage
   }

    override fun onDestroy(owner: LifecycleOwner) {

    }
}


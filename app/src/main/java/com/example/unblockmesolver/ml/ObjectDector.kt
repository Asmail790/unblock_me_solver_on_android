package com.example.unblockmesolver.ml

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
      val resf = f.lines().toList().map{ x -> x.trim() }
         f.close()
         resf
   }


    fun infer(screenshot: Bitmap):Pair<ArrayList<Result>, com.example.unblockmesolver.ml.Result> {
        val img = reScaleAndPadImage(screenshot)
        val  inputTensor = TensorImageUtils.bitmapToFloat32Tensor(img, NO_MEAN_RGB,NO_STD_RGB);
        val k = IValue.from(inputTensor)
        val d = dector.forward(k)
        val outputTuple = d.toTensor();


        val outputs = outputTuple.dataAsFloatArray;
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

        val f = File(externalFilesDir.absolutePath , "screenshot_with_bounding_boexes.jpg")
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



    private fun outputsToNMSPredictions(outputs:FloatArray, objectScoreThreshold:Float,IouThreshold: Float, limit: Int):Pair<ArrayList<Result>,com.example.unblockmesolver.ml.Result> {
        // left, top, right, bottom, objectScore and class probabilities (7 of them)


        val centerxs = outputs.slice(8400*0..8400*1-1)
        val centerys = outputs.slice(8400*1..8400*2-1)
        val widths = outputs.slice(8400*2..8400*3-1)
        val heights =  outputs.slice(8400*3..8400*4-1)


        val c1s = outputs.slice(8400*4..8400*5-1)
        val c2s = outputs.slice(8400*5..8400*6-1)
        val c3s = outputs.slice(8400*6..8400*7-1)
        val c4s = outputs.slice(8400*7..8400*8-1)
        val c5s = outputs.slice(8400*8..8400*9-1)
        val c6s = outputs.slice(8400*9..8400*10-1)
        val c7s = outputs.slice(8400*10..8400*11-1)
        //val cellSize = 4 + classes.size

        /*
        torch.Size([1, 11, 8400]) == [batch_size,4+number_of_class,grids_cell]
        A tensor of shape (batch_size, num_classes + 4 + num_masks, num_boxes)

        0 ... 8400-1 center_x # row 0
        0 ... 8400-1 center_y # row 1
        0 ... 8400-1 width # row 2
        0 ... 8400-1 height # row 3
        ----------- classes # row 4
        0 ... 8400-1 c1 # row 5
        0 ... 8400-1 c2 # row 6
        0 ... 8400-1 c3 # row 7
        .	.
        .	.
        .	.
        0 ... 8400-1 c7 # row 11
         */
        val unfilterCells = mutableListOf<com.example.unblockmesolver.ml.Result>()

        for (i in 0..8400-1) {
            val classIndex = arrayListOf<Float>(c1s[i],c2s[i],c3s[i],c4s[i],c5s[i],c6s[i],c7s[i]).withIndex().maxBy {x -> x.value }.index
            val classprob =  arrayListOf<Float>(c1s[i],c2s[i],c3s[i],c4s[i],c5s[i],c6s[i],c7s[i]).withIndex().maxBy {x -> x.value }.value
            val left  =  centerxs[i]-widths[i]/2F
            val top  = centerys[i]-heights[i]/2F
            val right =   centerxs[i]+widths[i]/2F
            val bottom =   centerys[i]+heights[i]/2F
            val  boundingBox =  RectF(left,top,right,bottom)
            val res = Result(classIndex, classprob,boundingBox)
            unfilterCells.add(res)
        }
        val cells = unfilterCells.filter {
                it.score>=objectScoreThreshold
            }

        val gridClass = classes.indexOf("Grid")
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
       val f1 = File(externalFilesDir.absolutePath , "screenshot.jpg")
       val f2 = File(externalFilesDir.absolutePath , "resized_screenshot.jpg")
      Log.d(TAG, f2.exists().toString() + " " + f2.exists().toString())
       f1.createNewFile()
       f2.createNewFile()

       screenshot.compress(Bitmap.CompressFormat.JPEG,100,f1.outputStream())
       resizedAndPaddedImage.compress(Bitmap.CompressFormat.JPEG,100,f2.outputStream())
       return resizedAndPaddedImage
   }

    override fun onDestroy(owner: LifecycleOwner) {

    }
}


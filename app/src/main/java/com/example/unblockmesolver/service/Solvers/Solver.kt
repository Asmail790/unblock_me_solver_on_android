package com.example.unblockmesolver.service.Solvers

import android.graphics.Point
import com.example.unblockmesolver.service.nextstepInformation.NextStep
import   com.example.unblockmesolver.ml.Result;
import kotlin.math.roundToInt

interface Solver {
    fun solve(results: Pair<ArrayList<Result>, Result>):String

    fun guide( results: Pair<ArrayList<Result>, Result>): NextStep

    fun getCenters(nextStep: NextStep):Pair<Point, Point> {
        val oldCenter = Point(nextStep.currentBlockPosition.centerX().roundToInt(),nextStep.currentBlockPosition.centerY().roundToInt())
        val newCenter =  Point(nextStep.newBlockPosition.centerX().roundToInt(),nextStep.newBlockPosition.centerY().roundToInt())
        return Pair(oldCenter,newCenter)
    }

    fun setClassIdMapping(classes: List<String>)
    //@Throws(IOException::class)
    fun checkValidResult(){

    }
}

class InvalidResult{
    
}
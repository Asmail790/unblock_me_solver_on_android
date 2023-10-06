package com.example.unblockmesolver.service.Solvers

import android.graphics.RectF
import com.example.unblockmesolver.ml.Result
import  com.example.unblockmesolver.service.nextstepInformation.NextStep

object CPPSolver:Solver {
    init {
        System.loadLibrary("UnblockMeSolverCpp")

    }
    @JvmStatic
    private external fun setMlClasses(Ids:MLClassIds)
    @JvmStatic
    private external fun inferAllSteps(boundingBoxOfBlocks:Array<Result>,gridBoundingBox:Result):Array<NextStep>
    @JvmStatic
    private external fun infer(boundingBoxOfBlocks:Array<Result>,gridBoundingBox:Result): NextStep
    override fun solve(results: Pair<ArrayList<Result>, Result>): String {
        val allSteps =inferAllSteps( results.first.toTypedArray(),results.second)
        val builder = StringBuilder()
        builder.append("su root -c ")
        allSteps.map { x -> getCenters(x) }.forEach { p->
            val command = """ input touchscreen swipe ${p.first.x} ${p.first.y} ${p.second.x} ${p.second.y} 100;"""
            builder.append(command)
        }
        return  builder.toString()
    }

    override fun guide(results: Pair<ArrayList<Result>, Result>): NextStep {
        return infer(results.first.toTypedArray(),results.second)
    }

    override fun setClassIdMapping(classes:List<String>) {
       val classesMap = classes.withIndex().map{ (i,x) ->x.trim() to i}.toMap()
        val correctMLIds = MLClassIds(
            mainBlock = classesMap["MainBlock"]!!,
            vertical2XBlock = classesMap["V2X"]!!,
            vertical3XBlock = classesMap["V3X"]!!,
            horizontal2XBlock = classesMap["H2X"]!!,
            horizontal3XBlock = classesMap["H3X"]!!,
            fixedBlock = classesMap["FixedBlock"]!!,
            grid = classesMap["Grid"]!!
        )
        setMlClasses(correctMLIds)
    }
}


data class MLClassIds(
    @JvmField val mainBlock:Int,
    @JvmField val vertical2XBlock:Int,
    @JvmField val vertical3XBlock:Int,
    @JvmField val horizontal2XBlock:Int,
    @JvmField val horizontal3XBlock:Int,
    @JvmField val fixedBlock:Int,
    @JvmField val grid:Int
)


package com.example.unblockmesolver.service.Solvers

import com.chaquo.python.Python
import com.example.unblockmesolver.ml.Result
import com.example.unblockmesolver.service.nextstepInformation.NextStep

object PythonSolver:Solver {
    private val py = Python.getInstance()
    private val  main = py.getModule("android_main")

    override fun solve(results: Pair<ArrayList<Result>, Result>):String{
        val allSteps = main.callAttr("infer_all_steps", results.first.toArray(),results.second).toJava(Array<NextStep>::class.java)
        val builder = StringBuilder()
        builder.append("su root -c ")
        allSteps.map { x -> getCenters(x)}.forEach {p->
            val command = """ input touchscreen swipe ${p.first.x} ${p.first.y} ${p.second.x} ${p.second.y} 100;"""
            builder.append(command)
        }
        return  builder.toString()
    }
    override fun guide(results: Pair<ArrayList<Result>, Result>): NextStep {
        val nextStep = main.callAttr("infer", results.first.toArray(),results.second).toJava(
            NextStep::class.java)
        return nextStep
    }

    override fun setClassIdMapping(classes:List<String>) {
        // do nothing
        // TODO make mapping set direclty from setClassIdMapping
    }
}

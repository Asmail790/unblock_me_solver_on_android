package com.example.unblockmesolver.service.nextstepInformation

import android.graphics.RectF

open class NextStep(
    @JvmField val currentBlockPosition: RectF,
    @JvmField val newBlockPosition: RectF,
    @JvmField val explanation:String): NextStepInformation()
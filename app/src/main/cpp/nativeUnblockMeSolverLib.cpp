#include <jni.h>
#include "entry.hpp"
#include <vector>


UnblockMe::Utils::Entry::SimpleGuider guider{};

jfieldID  fIdbottom = nullptr;
jfieldID  fIdleft = nullptr;
jfieldID  fIdright = nullptr;
jfieldID  fIdtop = nullptr;
jmethodID rectConstructor = nullptr;

jfieldID fIdboundingBox = nullptr;
jfieldID fIdclass_ = nullptr;

jfieldID fIdCurrentBlockPosition = nullptr;
jfieldID fIdNewBlockPosition = nullptr;


jclass resultClass = nullptr;
jclass rectFClass = nullptr;
jclass nextStepClass = nullptr;
jmethodID  nextStepConstructor = nullptr;


void cacheIds(JNIEnv *env) {
    if(resultClass == nullptr) {
        const auto res = env->FindClass("com/example/unblockmesolver/ml/Result");

        if (res == nullptr){
            // TODO throw
        }
        resultClass = reinterpret_cast<jclass>(env->NewGlobalRef(res));
    }

    if(rectFClass == nullptr) {
        const auto res = env->FindClass("android/graphics/RectF");

        if (res == nullptr){
            // TODO throw
        }
        rectFClass = reinterpret_cast<jclass>(env->NewGlobalRef(res));
    }

    if(nextStepClass == nullptr) {
        const auto res = env->FindClass("com/example/unblockmesolver/service/nextstepInformation/NextStep");

        if (res == nullptr){
            // TODO throw
        }
        nextStepClass = reinterpret_cast<jclass>(env->NewGlobalRef(res));
    }


    if (fIdbottom== nullptr) {
        const auto res  = env->GetFieldID(rectFClass, "bottom", "F");
        if (res == nullptr) {
            // TODO Throw
        }
        fIdbottom = res;
    }
    if (fIdleft== nullptr) {
        const auto res  = env->GetFieldID(rectFClass, "left", "F");
        if (res == nullptr) {
            // TODO Throw
        }
        fIdleft = res;
    }
    if (fIdright== nullptr) {
        const auto res  = env->GetFieldID(rectFClass, "right", "F");
        if (res == nullptr) {
            // TODO Throw
        }
        fIdright = res;
    }
    if (fIdtop== nullptr) {
        const auto res  = env->GetFieldID(rectFClass, "top", "F");
        if (res == nullptr) {
            // TODO Throw
        }
        fIdtop = res;
    }
    if (rectConstructor== nullptr) {
        const auto res  = env->GetMethodID(rectFClass, "<init>", "(FFFF)V");
        if (res == nullptr) {
            // TODO Throw
        }
        rectConstructor = res;
    }

    if (nextStepConstructor == nullptr) {
        const auto res = env->GetMethodID(
                nextStepClass,
                "<init>",
                "(Landroid/graphics/RectF;Landroid/graphics/RectF;Ljava/lang/String;)V"
        );
        if (res == nullptr) {
            // TODO Throw
        }
        nextStepConstructor = res;
    }



    if (fIdboundingBox == nullptr) {
        const auto res = env->GetFieldID(resultClass, "rect", "Landroid/graphics/RectF;");
        if (res == nullptr) {
            // TODO throw
        }
        fIdboundingBox = res;
    }
    if (fIdclass_ == nullptr) {
        const auto res = env->GetFieldID(resultClass, "classIndex", "I");
        if (res == nullptr) {
            // TODO throw
        }
        fIdclass_ = res;
    }

    if (fIdCurrentBlockPosition == nullptr) {
        const auto res = env->GetFieldID(nextStepClass, "currentBlockPosition", "Landroid/graphics/RectF;");
        if (res == nullptr) {
            // TODO throw
        }
        fIdCurrentBlockPosition = res;
    }
    if (fIdNewBlockPosition == nullptr) {
        const auto res = env->GetFieldID(nextStepClass, "newBlockPosition", "Landroid/graphics/RectF;");
        if (res == nullptr) {
            // TODO throw
        }
        fIdNewBlockPosition = res;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_unblockmesolver_service_Solvers_CPPSolver_setMlClasses(JNIEnv *env, jclass clazz,jobject ids) {
    using UnblockMe::Utils::Entry::MLClassIds;
    using UnblockMe::Utils::Entry::MainBlockLabel;
    using UnblockMe::Utils::Entry::Vertical2XBlockLabel;
    using UnblockMe::Utils::Entry::Vertical3XBlockLabel;
    using UnblockMe::Utils::Entry::Horizontal2XBlockLabel;
    using UnblockMe::Utils::Entry::Horizontal3XBlockLabel;
    using UnblockMe::Utils::Entry::FixedBlockLabel;
    using UnblockMe::Utils::Entry::GridLabel;


    jclass   mlCls = env->GetObjectClass(ids);
    jfieldID fIdMain = env->GetFieldID( mlCls, "mainBlock","I");
    jfieldID fIdV2X = env->GetFieldID(mlCls, "vertical2XBlock", "I");
    jfieldID fIdV3X = env->GetFieldID(mlCls, "vertical3XBlock", "I");
    jfieldID fIdH2X = env->GetFieldID(mlCls, "horizontal2XBlock", "I");
    jfieldID fIdH3X = env->GetFieldID(mlCls, "horizontal3XBlock", "I");
    jfieldID fIdFixed = env->GetFieldID( mlCls, "fixedBlock","I");
    jfieldID fIdGrid = env->GetFieldID( mlCls, "grid","I");


    // TODO throw
    // out of memory or incorrect field
    if (fIdMain == nullptr) {
        return;
    }
    if (fIdH2X == nullptr) {
        return;
    }
    if (fIdH3X == nullptr) {
        return;
    }
    if (fIdV2X == nullptr) {
        return;
    }
    if (fIdFixed == nullptr) {
        return;
    }
    if (fIdGrid == nullptr) {
        return;
    }
    if (fIdV3X == nullptr) {
        return;
    }

    auto  MainValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdMain));
    auto  Block2hValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdH2X));
    auto  Block3hValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdH3X));
    auto  Block2vValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdV2X));
    auto  FixedValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdFixed));
    auto  GridValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdGrid));
    auto  Block3vValue = static_cast<unsigned  char> (env->GetIntField(ids, fIdV3X));

    const auto mlClassIds = MLClassIds{
            MainBlockLabel{MainValue},
            Vertical2XBlockLabel{Block2vValue},
            Vertical3XBlockLabel{Block3vValue},
            Horizontal2XBlockLabel{Block2hValue},
            Horizontal3XBlockLabel{Block3hValue},
            FixedBlockLabel{FixedValue},
            GridLabel{GridValue}
    };

    guider.setMLClassIds(mlClassIds);
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_unblockmesolver_service_Solvers_CPPSolver_inferAllSteps(JNIEnv *env, jclass clazz,jobjectArray bounding_box_of_blocks,jobject grid_bounding_box) {
    using UnblockMe::Utils::Entry::BoundingBox;
    using UnblockMe::Utils::Entry::Top;
    using UnblockMe::Utils::Entry::Left;
    using UnblockMe::Utils::Entry::Right;
    using UnblockMe::Utils::Entry::Bottom;
    using UnblockMe::Utils::Entry::Class_;
    using UnblockMe::Utils::Entry::StepSizeX;
    using UnblockMe::Utils::Entry::StepSizeY;

    cacheIds(env);
    jsize size = env->GetArrayLength(bounding_box_of_blocks);
    auto boundingBoxOfBlocks =std::vector<BoundingBox>{};

    for(jsize i =0; i < size; i++) {
        jobject obj = env->GetObjectArrayElement(bounding_box_of_blocks,i);

        jobject rect = env->GetObjectField(obj,fIdboundingBox);

        const auto bottom = static_cast<float>(env->GetFloatField(rect,fIdbottom));
        const auto left = static_cast<float>(env->GetFloatField(rect,fIdleft));
        const auto right = static_cast<float>(env->GetFloatField(rect,fIdright));
        const auto top = static_cast<float>(env->GetFloatField(rect,fIdtop));

        const auto class_ = static_cast<unsigned char>(env->GetIntField(obj,fIdclass_));

        boundingBoxOfBlocks.emplace_back(
                Left{left},
                Top{top},
                Right{right},
                Bottom{bottom},
                Class_{class_}
        );
    }
    jobject gridRect = env->GetObjectField(grid_bounding_box,fIdboundingBox);

    const auto bottom = static_cast<float>(env->GetFloatField(gridRect,fIdbottom));
    const auto left = static_cast<float>(env->GetFloatField(gridRect,fIdleft));
    const auto right = static_cast<float>(env->GetFloatField(gridRect,fIdright));
    const auto top = static_cast<float>(env->GetFloatField(gridRect,fIdtop));

    const auto class_ = static_cast<unsigned char>(env->GetIntField(grid_bounding_box,fIdclass_));

    const auto gridBoundingBox = BoundingBox(
            Left{left},
            Top{top},
            Right{right},
            Bottom{bottom},
            Class_{class_}
    );

    const auto allSteps = guider.inferAllSteps(boundingBoxOfBlocks, gridBoundingBox);
    jobjectArray returnArray = env->NewObjectArray(static_cast<jsize>(allSteps.size()),nextStepClass,nullptr );

    for(size_t i = 0; i < allSteps.size();i++) {
        const auto& nextStep = allSteps.at(i);
        jstring message =  env->NewStringUTF(nextStep.message.c_str());
        // float left, float top, float right, float bottom
        jobject oldBoundingBox = env->NewObject(
                rectFClass,
                rectConstructor,
                static_cast<jfloat>(nextStep.from.left),
                static_cast<jfloat>(nextStep.from.top),
                static_cast<jfloat>(nextStep.from.right),
                static_cast<jfloat>(nextStep.from.bottom)
        );

        jobject newBoundingBox = env->NewObject(
                rectFClass,
                rectConstructor,
                static_cast<jfloat>(nextStep.to.left),
                static_cast<jfloat>(nextStep.to.top),
                static_cast<jfloat>(nextStep.to.right),
                static_cast<jfloat>(nextStep.to.bottom)
        );

        jobject jNextStep = env->NewObject(nextStepClass,nextStepConstructor,oldBoundingBox,newBoundingBox,message);
        env->SetObjectArrayElement(returnArray,static_cast<jsize>(i),jNextStep );
    }

    return returnArray;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_unblockmesolver_service_Solvers_CPPSolver_infer(JNIEnv *env, jclass clazz,jobjectArray bounding_box_of_blocks,jobject grid_bounding_box) {
    using UnblockMe::Utils::Entry::BoundingBox;
    using UnblockMe::Utils::Entry::Top;
    using UnblockMe::Utils::Entry::Left;
    using UnblockMe::Utils::Entry::Right;
    using UnblockMe::Utils::Entry::Bottom;
    using UnblockMe::Utils::Entry::Class_;
    using UnblockMe::Utils::Entry::StepSizeX;
    using UnblockMe::Utils::Entry::StepSizeY;

    cacheIds(env);
    jsize size = env->GetArrayLength(bounding_box_of_blocks);
    auto boundingBoxOfBlocks =std::vector<BoundingBox>{};


    for(jsize i =0; i < size; i++) {
        jobject obj = env->GetObjectArrayElement(bounding_box_of_blocks,i);

        jobject rect = env->GetObjectField(obj,fIdboundingBox);

        const auto bottom = static_cast<float>(env->GetFloatField(rect,fIdbottom));
        const auto left = static_cast<float>(env->GetFloatField(rect,fIdleft));
        const auto right = static_cast<float>(env->GetFloatField(rect,fIdright));
        const auto top = static_cast<float>(env->GetFloatField(rect,fIdtop));

        const auto class_ = static_cast<unsigned char>(env->GetIntField(obj,fIdclass_));

        boundingBoxOfBlocks.emplace_back(
                Left{left},
                Top{top},
                Right{right},
                Bottom{bottom},
                Class_{class_}
        );
    }

    jobject gridRect = env->GetObjectField(grid_bounding_box,fIdboundingBox);

    const auto bottom = static_cast<float>(env->GetFloatField(gridRect,fIdbottom));
    const auto left = static_cast<float>(env->GetFloatField(gridRect,fIdleft));
    const auto right = static_cast<float>(env->GetFloatField(gridRect,fIdright));
    const auto top = static_cast<float>(env->GetFloatField(gridRect,fIdtop));

    const auto class_ = static_cast<unsigned char>(env->GetIntField(grid_bounding_box,fIdclass_));

    const auto gridBoundingBox = BoundingBox(
            Left{left},
            Top{top},
            Right{right},
            Bottom{bottom},
            Class_{class_}
    );

    const auto nextStep = guider.inferOneStep(boundingBoxOfBlocks, gridBoundingBox);

    jstring message =  env->NewStringUTF(nextStep.message.c_str());
    // float left, float top, float right, float bottom
    jobject oldBoundingBox = env->NewObject(
            rectFClass,
            rectConstructor,
            static_cast<jfloat>(nextStep.from.left),
            static_cast<jfloat>(nextStep.from.top),
            static_cast<jfloat>(nextStep.from.right),
            static_cast<jfloat>(nextStep.from.bottom)
    );

    jobject newBoundingBox = env->NewObject(
            rectFClass,
            rectConstructor,
            static_cast<jfloat>(nextStep.to.left),
            static_cast<jfloat>(nextStep.to.top),
            static_cast<jfloat>(nextStep.to.right),
            static_cast<jfloat>(nextStep.to.bottom)
    );

    jobject returnValue = env->NewObject(nextStepClass,nextStepConstructor,oldBoundingBox,newBoundingBox,message);
    return returnValue;
}
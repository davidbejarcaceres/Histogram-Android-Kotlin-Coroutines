#include <jni.h>
#include <string>
#include <unistd.h> // Used to sleep the thread 5 secons
#include <android/log.h>
#include <chrono>
#include <vector>
#include <thread>
#include <omp.h>
using namespace std;


//Can only be called from the MainActivity
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// Can only be called from the Native Fragment (Second)
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    sleep(3); //Calls this to sleep the thread for 5 seconds

    return env->NewStringUTF(hello.c_str());
}


//TODO: Add here the histogram calculation
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_calcHistogram(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    jboolean isCopyA, isCopyB;
    jlong aux = 0;
    if (imagen == NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            jlong aux = 0;
            for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                for (int k = 0; k < 3; k++) h[k + i] = 0;
            }
            for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                }
            }

            for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                }
            }

            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] - h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                            h[k + x] = (int) (aux % 256);
                        }
                        imagen[k + i + j] = (short) (imagen[k + i + j] *h[k + imagen[k + i + j]]);
                    }
                }
            }
        }

        __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
        if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
        if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

        std::string hello = "Hello from C++";
        jint timeCOnsumed = 0;

        return env->NewStringUTF(hello.c_str());
    }
}


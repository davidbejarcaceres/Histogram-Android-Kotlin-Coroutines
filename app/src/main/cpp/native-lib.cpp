#include <jni.h>
#include <string>
#include <unistd.h> // Used to sleep the thread 5 secons

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
        jobject /* this */) {
    std::string hello = "Hello from C++";
    jint timeCOnsumed = 0;
    sleep(3); //Calls this to sleep the thread for 5 seconds

    return env->NewStringUTF(hello.c_str());
}
